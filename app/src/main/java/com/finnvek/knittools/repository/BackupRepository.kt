package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Room
import androidx.room.withTransaction
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.data.backup.BackupArchive
import com.finnvek.knittools.data.backup.BackupEntry
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupFiles
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupIdentityMap
import com.finnvek.knittools.data.backup.BackupManifest
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.backup.BackupRestoreFiles
import com.finnvek.knittools.data.backup.BackupTables
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_ARCHIVE_NAME = "backup.zip"

@Singleton
class BackupRepository
    @Inject
    internal constructor(
        @param:ApplicationContext private val context: Context,
        private val database: KnitToolsDatabase,
        private val patternFiles: PatternFileReferenceCoordinator,
        private val yarnCards: YarnCardRepository,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val providerIo: BackupProviderIo,
    ) {
        private val operation = Mutex()
        private val root get() = File(context.noBackupFilesDir, "manual-backup").apply { mkdirs() }
        private val activeDirectories = mutableSetOf<File>()

        @Volatile private var pending: File? = null
        private var latestSelectionId: UUID? = null

        suspend fun export(destination: Uri) =
            withContext(ioDispatcher) {
                val coroutine = currentCoroutineContext()
                val check = { coroutine.ensureActive() }
                val directory =
                    operation.withLock {
                        newDirectory().also { activeDirectories += it }
                    }
                try {
                    val externalFiles = stageExternalFiles(directory, check)
                    val archive =
                        operation.withLock {
                            operationResult(BackupError.WRITE) {
                                buildLocalArchive(directory, externalFiles, check)
                            }
                        }
                    operationResult(BackupError.WRITE) {
                        providerIo.write(archive, destination, BackupFormat.MAX_TOTAL, check)
                    }
                } finally {
                    withContext(NonCancellable) {
                        operation.withLock {
                            activeDirectories -= directory
                            directory.deleteRecursively()
                        }
                    }
                }
            }

        suspend fun prepare(
            source: Uri,
            selectionId: UUID,
        ): BackupPreview =
            withContext(ioDispatcher) {
                val directory =
                    operation.withLock {
                        discardPending()
                        latestSelectionId = null
                        newDirectory(selectionId).also {
                            activeDirectories += it
                            latestSelectionId = selectionId
                        }
                    }
                try {
                    val coroutine = currentCoroutineContext()
                    val check = { coroutine.ensureActive() }
                    val archive = File(directory, BACKUP_ARCHIVE_NAME)
                    operationResult(BackupError.READ) {
                        providerIo.read(source, archive, BackupFormat.MAX_TOTAL) {
                            check()
                            BackupFormat.space(directory, 64 * 1024)
                        }
                    }
                    operation.withLock {
                        if (latestSelectionId != selectionId) {
                            throw CancellationException("Backup selection was replaced")
                        }
                        operationResult(BackupError.CORRUPT) {
                            val payload = File(directory, "payload").apply { mkdirs() }
                            val preview = validate(archive, payload, check)
                            if (latestSelectionId != selectionId) {
                                throw CancellationException("Backup selection was replaced")
                            }
                            pending = directory
                            preview
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        operation.withLock {
                            activeDirectories -= directory
                            if (pending != directory) {
                                if (latestSelectionId == selectionId) latestSelectionId = null
                                directory.deleteRecursively()
                            }
                        }
                    }
                }
            }

        suspend fun restore(selectionId: UUID) =
            withContext(ioDispatcher) {
                operation.withLock {
                    val directory = pending ?: throw BackupException(BackupError.RESTORE)
                    BackupFormat.requireValid(directory.name == selectionId.toString(), BackupError.VALIDATION)
                    pending = null
                    if (latestSelectionId == selectionId) latestSelectionId = null
                    try {
                        operationResult(BackupError.RESTORE) {
                            val coroutine = currentCoroutineContext()
                            val check = { coroutine.ensureActive() }
                            val payload = File(directory, "payload")
                            // Tarkistetaan valmistellut tavut uudelleen juuri ennen vahvistettua palautusta.
                            validate(File(directory, BACKUP_ARCHIVE_NAME), payload, check)
                            withContentLocks {
                                withShadow(payload) { source ->
                                    source.withTransaction {
                                        BackupTables.import(source.openHelper.writableDatabase, payload, check = check)
                                    }
                                    database.withTransaction {
                                        val live = database.openHelper.writableDatabase
                                        val identities = BackupIdentityMap(source.openHelper.writableDatabase, live)
                                        val files = BackupRestoreFiles(context, payload)
                                        val replacement = File(directory, "replacement").apply { mkdirs() }
                                        source.withTransaction {
                                            BackupTables
                                                .export(source.openHelper.writableDatabase, replacement, { table, row ->
                                                    identities.apply(table, row)
                                                    files.rebase(table, row)
                                                }, check)
                                        }
                                        BackupFormat.space(
                                            context.filesDir,
                                            files.requiredBytes + databaseSpace(payload) +
                                                live.path.orEmpty().let {
                                                    File(
                                                        it,
                                                    ).length() + File("$it-wal").length()
                                                },
                                        )
                                        files.journal(BackupTables.references(live))
                                        files.publish(check)
                                        check()
                                        BackupTables.clear(live)
                                        BackupTables.import(live, replacement, check = check)
                                        identities.reserveIds(live)
                                        BackupTables.references(live).forEach { uri ->
                                            val file = AppFileStorage.resolveAppOwnedFile(context, uri.toUri())
                                            BackupFormat.requireValid(file?.isFile == true, BackupError.RESTORE)
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        withContext(NonCancellable) { cleanup(directory) }
                    }
                    refreshWidgets()
                }
            }

        suspend fun cancelPreview(selectionId: UUID?) =
            withContext(ioDispatcher) {
                operation.withLock {
                    if (selectionId != null) {
                        if (latestSelectionId == selectionId) latestSelectionId = null
                        if (pending?.name == selectionId.toString()) discardPending()
                    }
                }
            }

        private fun refreshWidgets() {
            applicationScope.launch {
                try {
                    com.finnvek.knittools.widget.CounterWidgetState
                        .refreshAll(context)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    // Widgetin piirron virhe ei peruuta jo vahvistettua sisällön palautusta.
                }
            }
        }

        fun releasePreview(selectionId: UUID?) {
            if (selectionId == null) return
            applicationScope.launch {
                cancelPreview(selectionId)
            }
        }

        suspend fun recoverInterruptedOperations() =
            withContext(ioDispatcher) {
                operation.withLock {
                    root
                        .listFiles()
                        ?.filter { it.isDirectory && it != pending && it !in activeDirectories }
                        ?.forEach { cleanup(it) }
                }
            }

        private suspend fun buildLocalArchive(
            directory: File,
            externalFiles: Map<String, File>,
            check: () -> Unit,
        ): File {
            val payload = File(directory, "payload").apply { mkdirs() }
            val files = BackupFiles(context, payload, externalFiles)
            withContentLocks {
                database.withTransaction {
                    BackupTables.export(database.openHelper.writableDatabase, payload, { table, row ->
                        files.export(table, row, check)
                    }, check)
                }
            }
            val entries =
                payload
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { file ->
                        BackupEntry(
                            file.relativeTo(payload).invariantSeparatorsPath,
                            file.length(),
                            BackupFormat.digest(file, check),
                        )
                    }.sortedBy { it.path }
                    .toList()
            val manifest =
                BackupManifest(
                    createdAt = System.currentTimeMillis(),
                    versionCode = BuildConfig.VERSION_CODE.toLong(),
                    versionName = BuildConfig.VERSION_NAME,
                    entries = entries,
                )
            BackupFormat.space(directory, entries.sumOf { it.size } + BackupFormat.MAX_MANIFEST)
            val archive = File(directory, BACKUP_ARCHIVE_NAME)
            BackupArchive.write(payload, archive, manifest, check)
            val validation = File(directory, "validation").apply { mkdirs() }
            validate(archive, validation, check)
            return archive
        }

        private suspend fun stageExternalFiles(
            directory: File,
            check: () -> Unit,
        ): Map<String, File> {
            val references =
                withContentLocks {
                    database.withTransaction {
                        BackupTables.references(database.openHelper.writableDatabase)
                    }
                }
            val external =
                references
                    .map { it to it.toUri() }
                    .filter { (_, uri) ->
                        AppFileStorage.resolveAppOwnedFile(context, uri) == null && uri.scheme == "content"
                    }
            if (external.isEmpty()) return emptyMap()
            val staging = File(directory, "provider-staging").apply { mkdirs() }
            return external
                .mapIndexed { index, (reference, uri) ->
                    check()
                    val file = File(staging, "$index.tmp")
                    operationResult(BackupError.READ) {
                        providerIo.read(uri, file, BackupFormat.MAX_FILE) {
                            check()
                            BackupFormat.space(directory, 64 * 1024)
                        }
                    }
                    reference to file
                }.toMap()
        }

        private suspend fun validate(
            archive: File,
            payload: File,
            check: () -> Unit,
        ): BackupPreview {
            val manifest = BackupArchive.extract(archive, payload, check)
            BackupFormat.space(payload, databaseSpace(payload))
            return withShadow(payload) { db ->
                db.withTransaction {
                    val sql = db.openHelper.writableDatabase
                    BackupTables.preflightYarnCardIds(
                        payload,
                        BackupTables.columns(sql, "counter_projects").map { it.name },
                        check,
                    )
                    BackupTables.import(sql, payload, check = check)
                    val references = BackupTables.references(sql)
                    val entries =
                        manifest.entries
                            .filter { it.path.startsWith("files/") }
                            .map { it.path }
                            .toSet()
                    BackupFormat.requireValid(references == entries, BackupError.VALIDATION)
                    val identities = BackupIdentityMap(sql, sql)
                    BackupFormat.tables.forEach { table ->
                        BackupTables.read(
                            payload,
                            table,
                            BackupTables.columns(sql, table).map { it.name },
                            check,
                        ) { row ->
                            identities.apply(table, row)
                        }
                    }

                    fun count(table: String): Int =
                        sql.query("SELECT COUNT(*) FROM `$table`").use {
                            it.moveToFirst()
                            it.getInt(0)
                        }
                    BackupPreview(
                        manifest.createdAt,
                        manifest.versionName,
                        count("counter_projects"),
                        count("saved_patterns"),
                        count("yarn_cards"),
                        entries.size,
                    )
                }
            }
        }

        private fun shadow(directory: File): KnitToolsDatabase {
            val path = File(directory, "validation-${UUID.randomUUID()}.db").absolutePath
            return Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, path)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        }

        private suspend fun <T> withShadow(
            directory: File,
            block: suspend (KnitToolsDatabase) -> T,
        ): T {
            val db = shadow(directory)
            var path: String? = null
            try {
                path = db.openHelper.writableDatabase.path
                return block(db)
            } finally {
                try {
                    db.close()
                } catch (_: Exception) {
                    // Tarkistuskannan sulkeminen ei saa raportoida vahvistettua palautusta peruuntuneeksi.
                }
                path?.let { name ->
                    listOf(name, "$name-wal", "$name-shm", "$name-journal").forEach {
                        BackupFiles.deleteTemporaryFile(File(it))
                    }
                }
            }
        }

        private suspend fun <T> withContentLocks(block: suspend () -> T): T =
            patternFiles.withReferenceLock { yarnCards.withPhotoStorageLock(block) }

        private suspend fun cleanup(directory: File) {
            try {
                withContentLocks {
                    database.withTransaction {
                        if (BackupRestoreFiles.cleanup(
                                context,
                                File(directory, "payload"),
                                BackupTables.references(database.openHelper.writableDatabase),
                            )
                        ) {
                            directory.deleteRecursively()
                        }
                    }
                }
            } catch (_: Exception) {
                // Loki säilyy seuraavaan käynnistykseen; vahvistettu tietokanta määrää säilytettävät tiedostot.
            }
        }

        private fun newDirectory(id: UUID = UUID.randomUUID()): File =
            File(root, id.toString()).apply {
                BackupFormat.requireValid(mkdirs(), BackupError.SPACE)
                BackupFormat.space(this, 0)
            }

        private fun databaseSpace(payload: File): Long =
            BackupFormat.tablePaths.sumOf { File(payload, it).length() } * 4

        private fun discardPending() {
            pending?.deleteRecursively()
            pending = null
        }

        private suspend fun <T> operationResult(
            error: BackupError,
            block: suspend () -> T,
        ): T =
            runCatching { block() }.getOrElse { failure ->
                if (failure is Exception && failure !is CancellationException) throw classified(failure, error)
                throw failure
            }

        private fun classified(
            failure: Exception,
            fallback: BackupError,
        ): BackupException =
            failure as? BackupException ?: BackupException(
                if (failure is android.database.sqlite.SQLiteFullException) BackupError.SPACE else fallback,
                failure,
            )
    }
