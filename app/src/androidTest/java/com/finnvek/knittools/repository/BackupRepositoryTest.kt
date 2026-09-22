package com.finnvek.knittools.repository

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.backup.BackupArchive
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupLimits
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.backup.BackupRestoreFiles
import com.finnvek.knittools.data.backup.BackupTables
import com.finnvek.knittools.data.backup.ContentResolverBackupProviderIo
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.domain.calculator.evaluateActiveSessionTime
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var db: KnitToolsDatabase
    private lateinit var repository: BackupRepository
    private lateinit var patternFiles: PatternFileReferenceCoordinator
    private lateinit var yarnCards: YarnCardRepository
    private var selectionId = UUID.randomUUID()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun prepare(uri: Uri): BackupPreview {
        selectionId = UUID.randomUUID()
        return repository.prepare(uri, selectionId)
    }

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        directory = File(app.cacheDir, "backup-test-${UUID.randomUUID()}").apply { mkdirs() }
        context =
            object : ContextWrapper(app) {
                override fun getApplicationContext(): Context = this

                override fun getFilesDir() = File(directory, "files").apply { mkdirs() }

                override fun getNoBackupFilesDir() = File(directory, "private").apply { mkdirs() }
            }
        db =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "live.db").absolutePath)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        patternFiles = PatternFileReferenceCoordinator()
        yarnCards =
            YarnCardRepository(
                db.yarnCardDao(),
                db.counterProjectDao(),
                context,
                RoomDatabaseTransactionRunner(db),
                Dispatchers.IO,
            )
        repository = createRepository(ContentResolverBackupProviderIo(context))
    }

    @After fun tearDown() {
        db.close()
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test fun allEighteenTablesFilesRelationshipsAndCompletionEventsSurviveRealReplacement() =
        runBlocking {
            seed()
            val archive = File(directory, "backup.knittools-backup")
            repository.export(archive.toUri())
            if (InstrumentationRegistry.getArguments().getString("backupFixture") == "true") {
                val app = ApplicationProvider.getApplicationContext<Context>()
                archive.copyTo(File(app.cacheDir, "backup-device-fixture.knittools-backup"), overwrite = true)
            }
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Later edit'")
            val preview = prepare(archive.toUri())
            assertEquals(1, preview.projects)
            assertEquals(2, preview.patterns)
            assertEquals(1, preview.yarnCards)
            assertEquals(4, preview.files)
            repository.restore(selectionId)
            BackupFormat.tables.forEach {
                assertEquals(
                    if (it in
                        setOf("saved_patterns", "project_documents")
                    ) {
                        2L
                    } else {
                        1L
                    },
                    number("SELECT COUNT(*) FROM `$it`"),
                )
            }
            val project = db.counterProjectDao().getAllProjectsOnce().single()
            assertTrue(project.id > 1)
            assertEquals("Cardigan", project.name)
            assertEquals("Private notes", project.notes)
            assertEquals(12, project.count)
            val restoredHistory = counterHistoryTestRepository(db, context).observeCounterHistory(project.id).first()
            assertEquals(1, restoredHistory.size)
            assertEquals(1_700_000_000_000L, restoredHistory.single().timestamp)
            assertEquals(CounterHistoryAction.INCREASE, restoredHistory.single().action)
            assertEquals(11 to 12, restoredHistory.single().let { it.previousValue to it.newValue })
            assertEquals(number("SELECT id FROM counter_history"), restoredHistory.single().id)
            assertEquals(
                number("SELECT previousValue FROM counter_history").toInt(),
                restoredHistory.single().previousValue,
            )
            assertEquals(number("SELECT newValue FROM counter_history").toInt(), restoredHistory.single().newValue)

            assertEquals(project.id, number("SELECT projectId FROM project_completions"))
            assertEquals(1_700_000_000_000L, number("SELECT completedAt FROM project_completions"))
            assertEquals(project.id, number("SELECT projectId FROM pattern_bookmarks"))
            assertEquals(number("SELECT id FROM yarn_cards"), number("SELECT yarnCardId FROM project_yarn_usage"))
            assertEquals(
                text("SELECT documentKey FROM pattern_annotation_layers"),
                text("SELECT documentKey FROM pattern_bookmarks"),
            )
            val active = db.sessionDao().getActiveSession()
            assertEquals(null, active?.bootCount)
            assertEquals("BOOT_IDENTITY_UNAVAILABLE", active?.recoveryReason)
            assertEquals(null, active?.recoverySuggestedDurationSeconds)
            assertEquals(120L, active?.checkpointedDurationSeconds)
            val evaluation =
                evaluateActiveSessionTime(
                    checkNotNull(active).toDomain().timingAnchors,
                    SessionTimeSnapshot(1_900_000_000_000L, 8_000_000L, 1, "Europe/Helsinki"),
                )
            assertTrue(evaluation is ActiveSessionTimeEvaluation.NeedsReview)
            val references = BackupTables.references(db.openHelper.writableDatabase)
            assertEquals(4, references.size)
            references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.isFile == true) }
            val pdfUri = text("SELECT localPdfUri FROM project_documents WHERE isPrimary = 1")
            PdfRenderer(
                ParcelFileDescriptor.open(File(checkNotNull(pdfUri.toUri().path)), ParcelFileDescriptor.MODE_READ_ONLY),
            ).use {
                assertEquals(1, it.pageCount)
            }
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun failedValidationAndCancelledPreviewDoNotChangeCurrentRowsOrFiles() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val invalid = File(directory, "invalid").apply { writeText("not a backup") }
            expectBackupFailure { prepare(invalid.toUri()) }
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.cancelPreview(selectionId)
            assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
            references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.exists() == true) }
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun oversizedFieldsAreRejectedBeforePreviewAndAgainAtConfirmationWithoutLiveMutation() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val archive = File(directory, "oversized-source")
            repository.export(archive.toUri())
            rewriteTableText(
                archive,
                "counter_projects",
                "notes",
                "x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1),
            )

            expectBackupFailure { prepare(archive.toUri()) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())

            val valid = File(directory, "valid-source")
            repository.export(valid.toUri())
            prepare(valid.toUri())
            val staged = File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip")
            rewriteTableText(
                staged,
                "counter_projects",
                "notes",
                "x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1),
            )

            expectBackupFailure { repository.restore(selectionId) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun exportRefusesContentThatTheRestoreBudgetWouldReject() =
        runBlocking {
            seed()
            val oversized = "x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1)
            db.openHelper.writableDatabase.execSQL(
                "UPDATE counter_projects SET notes = ?",
                arrayOf(oversized),
            )
            val archive = File(directory, "rejected-export")

            expectBackupFailure { repository.export(archive.toUri()) }

            assertEquals(oversized.length.toLong(), number("SELECT LENGTH(notes) FROM counter_projects"))
            assertFalse(archive.exists())
        }

    @Test fun oversizedSessionsAreRejectedBeforePreviewAndAtConfirmationWithoutLiveMutation() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val archive = File(directory, "oversized-sessions")
            repository.export(archive.toUri())
            rewriteOversizedSessions(archive)
            expectBackupFailure { prepare(archive.toUri()) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
            val valid = File(directory, "valid-sessions")
            repository.export(valid.toUri())
            prepare(valid.toUri())
            rewriteOversizedSessions(File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip"))
            expectBackupFailure { repository.restore(selectionId) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun exportRefusesSessionsAboveTheRestoreCeiling() =
        runBlocking {
            seed()
            val sql = db.openHelper.writableDatabase
            sql.execSQL("DELETE FROM sessions")
            sql.execSQL(
                "WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x + 1 FROM n WHERE x < ?) " +
                    "INSERT INTO sessions(projectId, startedAt, endedAt, startRow, endRow, " +
                    "durationMinutes, durationSeconds, rowsWorked) " +
                    "SELECT (SELECT MIN(id) FROM counter_projects), 1000, 2000, 0, 1, 1, 1, 1 FROM n",
                arrayOf(BackupLimits.MAX_SESSION_ROWS + 1),
            )
            val archive = File(directory, "rejected-session-export")
            expectBackupFailure { repository.export(archive.toUri()) }
            assertFalse(archive.exists())
            assertEquals(BackupLimits.MAX_SESSION_ROWS + 1, number("SELECT COUNT(*) FROM sessions"))
        }

    private fun rewriteOversizedSessions(archive: File) {
        val payload = File(directory, "session-rewrite-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/sessions.jsonl")
            val lines = table.readLines()
            table.bufferedWriter().use { writer ->
                writer.appendLine(lines.first())
                repeat((BackupLimits.MAX_SESSION_ROWS + 1).toInt()) { writer.appendLine(lines[1]) }
            }
            val updated =
                manifest.copy(
                    entries =
                        manifest.entries.map { entry ->
                            val file = File(payload, entry.path)
                            entry.copy(size = file.length(), sha256 = BackupFormat.digest(file))
                        },
                )
            BackupArchive.write(payload, archive, updated)
        } finally {
            payload.deleteRecursively()
        }
    }

    @Test fun stalePreviewCannotRestoreOrDiscardAnotherSelectionsBackup() =
        runBlocking {
            seed()
            val first = File(directory, "first")
            val second = File(directory, "second")
            repository.export(first.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Second backup'")
            repository.export(second.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()
            repository.prepare(first.toUri(), firstId)
            repository.prepare(second.toUri(), secondId)
            repository.cancelPreview(firstId)
            repository.releasePreview(firstId)
            expectBackupFailure { repository.restore(firstId) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            repository.restore(secondId)
            assertEquals("Second backup", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun competingPreparationsCannotPublishOrDeleteAnotherSelectionsPreview() =
        runBlocking {
            seed()
            val archive = File(directory, "racing-source")
            repository.export(archive.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val racingIo = RacingReadProviderIo(archive)
            val racingRepository = createRepository(racingIo)
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()
            val first = async(Dispatchers.IO) { runCatching { racingRepository.prepare(mockUri("first"), firstId) } }
            racingIo.firstStarted.await()

            val second = async(Dispatchers.IO) { racingRepository.prepare(mockUri("second"), secondId) }
            assertEquals(1, second.await().projects)
            racingIo.releaseFirst.complete(Unit)

            assertTrue(first.await().exceptionOrNull() is CancellationException)
            racingRepository.cancelPreview(firstId)
            racingRepository.restore(secondId)
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun stalledExternalReferenceDoesNotHoldBackupContentOrDatabaseLocks() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL(
                "UPDATE yarn_cards SET photoUri = 'content://stalled.provider/photo'",
            )
            val stalledIo = StalledReadProviderIo()
            val stalledRepository = createRepository(stalledIo)
            val export = async(Dispatchers.IO) { stalledRepository.export(mockUri("destination")) }
            stalledIo.started.await()

            withTimeout(2_000) {
                stalledRepository.cancelPreview(null)
                patternFiles.withReferenceLock {
                    yarnCards.withPhotoStorageLock {
                        db.withTransaction {
                            db.openHelper.writableDatabase
                                .query("SELECT COUNT(*) FROM yarn_cards")
                                .close()
                        }
                    }
                }
            }

            export.cancelAndJoin()
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun providerWriteFailureMapsToWriteAndCleansLocalArchive() =
        runBlocking {
            seed()
            val failingRepository = createRepository(FailingWriteProviderIo())

            val failure = runCatching { failingRepository.export(mockUri("destination")) }.exceptionOrNull()

            assertTrue(failure is BackupException)
            assertEquals(BackupError.WRITE, (failure as BackupException).error)
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun liveInsertFailureRollsBackRowsAndKeepsOldFiles() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_backup BEFORE INSERT ON counter_projects " +
                    "BEGIN SELECT RAISE(ABORT, 'test failure'); END",
            )
            expectBackupFailure { repository.restore(selectionId) }
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
            references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.exists() == true) }
            assertFalse(context.filesDir.walkTopDown().any { it.isFile && it.name.startsWith("restore-") })
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun validHashesCannotHideBrokenForeignKeysOrInvalidColumnTypes() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            val payload = File(directory, "tampered").apply { mkdirs() }
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/counter_history.jsonl")
            val original = table.readLines()
            val header = BackupFormat.json.parseToJsonElement(original.first()).jsonArray
            val column = header.indexOf(JsonPrimitive("projectId"))
            for (value in listOf(JsonPrimitive(999), JsonPrimitive("not a number"))) {
                val row =
                    BackupFormat.json
                        .parseToJsonElement(original[1])
                        .jsonArray
                        .toMutableList()
                row[column] = value
                table.writeText(original.first() + "\n" + JsonArray(row) + "\n")
                val updated =
                    manifest.copy(
                        entries =
                            manifest.entries.map { entry ->
                                val file = File(payload, entry.path)
                                entry.copy(size = file.length(), sha256 = BackupFormat.digest(file))
                            },
                    )
                BackupArchive.write(payload, archive, updated)
                expectBackupFailure { prepare(archive.toUri()) }
                assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
                assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
            }
        }

    @Test fun startupJournalRecoveryKeepsCommittedReferencesAndRemovesUncommittedFiles() =
        runBlocking {
            seed()
            for (committed in listOf(false, true)) {
                val operation =
                    File(
                        context.noBackupFilesDir,
                        "manual-backup/${UUID.randomUUID()}/payload",
                    ).apply { mkdirs() }
                val content =
                    File(operation, "files/${"a".repeat(64)}.bin").apply {
                        checkNotNull(parentFile).mkdirs()
                        writeText("photo")
                    }
                val files = BackupRestoreFiles(context, operation)
                val row =
                    mutableMapOf<String, JsonElement>(
                        "photoUri" to JsonPrimitive(content.relativeTo(operation).invariantSeparatorsPath),
                        "id" to JsonPrimitive(1),
                    )
                files.rebase("yarn_cards", row)
                files.journal(emptySet())
                files.publish {}
                val newUri = row.getValue("photoUri").toString().trim('"')
                if (committed) {
                    db.openHelper.writableDatabase.execSQL(
                        "UPDATE yarn_cards SET photoUri = ?",
                        arrayOf(newUri),
                    )
                }
                repository.recoverInterruptedOperations()
                assertEquals(committed, AppFileStorage.resolveAppOwnedFile(context, newUri.toUri())?.exists())
                assertFalse(checkNotNull(operation.parentFile).exists())
            }
        }

    @Test fun chartTrackerIdsAndDanglingMetadataAreRebasedWithoutAccidentalLinks() =
        runBlocking {
            seed()
            val payload =
                """
                {"region":{"bounds":{"left":0.1,"top":0.2,"right":0.9,"bottom":0.8},
                "name":"Chart","rows":20,"columns":30,"rowDirection":"BOTTOM_TO_TOP",
                "columnDirection":"ALTERNATING"},"trackingMode":"CROSSHAIR","counterType":"EXTRA",
                "extraCounterId":1,"counterStartValue":0,"gridStartIndex":0,"wrapAtEnd":true,
                "highlightArgb":1,"highlightAlpha":0.4}
                """.trimIndent()
            db.openHelper.writableDatabase.execSQL(
                "UPDATE pattern_annotations SET kind = 'CHART_TRACKER', payloadJson = ?",
                arrayOf(payload),
            )
            db.openHelper.writableDatabase.execSQL("UPDATE project_yarn_notes SET savedYarnCardId = 999")
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.restore(selectionId)

            fun counterId(): Long =
                BackupFormat.json
                    .parseToJsonElement(
                        text("SELECT payloadJson FROM pattern_annotations"),
                    ).jsonObject
                    .getValue("extraCounterId")
                    .jsonPrimitive.long
            assertEquals(number("SELECT id FROM project_counters"), counterId())
            assertEquals(0L, number("SELECT COUNT(*) FROM project_yarn_notes WHERE savedYarnCardId IS NOT NULL"))
            db.openHelper.writableDatabase.execSQL("DELETE FROM project_counters")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.restore(selectionId)
            val missing = counterId()
            insert("project_counters", mapOf("id" to null, "projectId" to number("SELECT id FROM counter_projects")))
            assertTrue(number("SELECT id FROM project_counters") > missing)
        }

    @Test fun orphanTempAndInstallationStateAreNeverArchivedOrReplaced() =
        runBlocking {
            val preferences = PreferencesManager(context)
            preferences.setThemeMode(ThemeMode.DARK)
            preferences.setUseImperial(true)
            val before = preferences.preferences.first()
            seed()
            val sentinel =
                File(context.filesDir, "datastore/trial_state.preferences_pb").apply {
                    checkNotNull(parentFile).mkdirs()
                    writeText("trial sentinel")
                }
            val orphan =
                File(context.filesDir, "pattern_captures/999/abandoned.jpg").apply {
                    checkNotNull(parentFile).mkdirs()
                    writeText("orphan")
                }
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            java.util.zip.ZipFile(archive).use { zip ->
                val names =
                    zip
                        .entries()
                        .asSequence()
                        .map { it.name }
                        .toList()
                assertTrue(names.all { it == BackupFormat.MANIFEST || BackupFormat.allowedPath(it) })
                assertEquals(BackupFormat.tables.size + 5, names.size)
            }
            prepare(archive.toUri())
            repository.restore(selectionId)
            assertEquals("trial sentinel", sentinel.readText())
            assertEquals("orphan", orphan.readText())
            assertEquals(before, preferences.preferences.first())
        }

    private suspend fun seed() {
        val pdf = File(context.filesDir, "patterns/legacy.pdf").apply { checkNotNull(parentFile).mkdirs() }
        writePdf(pdf, 100, 100)
        val secondPdf = File(context.filesDir, "pattern_pdfs/1/second.pdf").apply { checkNotNull(parentFile).mkdirs() }
        writePdf(secondPdf, 120, 150)
        val photo = File(context.filesDir, "progress_photos/1/photo.jpg").apply { checkNotNull(parentFile).mkdirs() }
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.eraseColor(android.graphics.Color.RED)
        val yarn = File(context.filesDir, "yarn_photos/1/yarn.jpg").apply { checkNotNull(parentFile).mkdirs() }
        yarn.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        val annotationJson =
            """{"points":[{"x":0.1,"y":0.1},{"x":0.5,"y":0.5}],"argb":-16777216,"strokeWidth":2.0}"""
        db.withTransaction {
            BackupFormat.tables.forEach { table ->
                val changes: Map<String, Any?> =
                    when (table) {
                        "counter_projects" ->
                            mapOf(
                                "name" to "Cardigan",
                                "notes" to "Private notes",
                                "count" to 12,
                                "yarnCardIds" to "1",
                                "patternUri" to
                                    "content://${AppFileStorage.fileProviderAuthority(context)}/patterns/legacy.pdf",
                                "linkedPatternId" to 1,
                            )
                        "saved_patterns" ->
                            mapOf(
                                "source" to "LOCAL_FILE",
                                "name" to "Chart",
                                "localPdfUri" to pdf.toUri().toString(),
                                "availability" to "unknown",
                            )
                        "yarn_cards" -> mapOf("photoUri" to yarn.toUri().toString(), "linkedProjectId" to 1)
                        "progress_photos" -> mapOf("photoUri" to photo.toUri().toString())
                        "pattern_annotation_layers" ->
                            mapOf(
                                "savedPatternId" to null,
                                "documentKey" to "saved:1:v1",
                                "isActive" to 1,
                            )
                        "pattern_bookmarks" -> mapOf("documentKey" to "saved:1:v1")
                        "pattern_annotations" ->
                            mapOf(
                                "kind" to "FREEHAND",
                                "payloadVersion" to 1,
                                "payloadJson" to annotationJson,
                            )
                        "project_documents" ->
                            mapOf(
                                "documentKey" to "saved:1:v1",
                                "localPdfUri" to pdf.toUri().toString(),
                                "isPrimary" to 1,
                            )
                        "project_completions" ->
                            mapOf(
                                "completedAt" to 1_700_000_000_000L,
                                "zoneId" to "Europe/Helsinki",
                            )
                        "counter_history" ->
                            mapOf(
                                "action" to "increment",
                                "previousValue" to 11,
                                "newValue" to 12,
                                "timestamp" to 1_700_000_000_000L,
                            )
                        "active_sessions" ->
                            mapOf(
                                "singletonId" to 1,
                                "sessionToken" to "old-session",
                                "bootCount" to 1,
                                "checkpointedDurationSeconds" to 120,
                                "startZoneId" to "Europe/Helsinki",
                            )
                        else -> emptyMap()
                    }
                insert(table, changes)
            }
            insert(
                "saved_patterns",
                mapOf(
                    "id" to 2,
                    "source" to "LOCAL_FILE",
                    "localPdfUri" to secondPdf.toUri().toString(),
                ),
            )
            insert(
                "project_documents",
                mapOf(
                    "id" to 2,
                    "savedPatternId" to 2,
                    "documentKey" to "saved:2:v1",
                    "localPdfUri" to secondPdf.toUri().toString(),
                    "isPrimary" to 0,
                ),
            )
        }
    }

    private fun writePdf(
        file: File,
        width: Int,
        height: Int,
    ) {
        val document = PdfDocument()
        try {
            document.finishPage(document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create()))
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
    }

    private fun rewriteTableText(
        archive: File,
        tableName: String,
        columnName: String,
        value: String,
    ) {
        val payload = File(directory, "rewrite-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/$tableName.jsonl")
            val lines = table.readLines()
            val header = BackupFormat.json.parseToJsonElement(lines.first()).jsonArray
            val column = header.indexOf(JsonPrimitive(columnName))
            val row =
                BackupFormat.json
                    .parseToJsonElement(lines[1])
                    .jsonArray
                    .toMutableList()
            row[column] = JsonPrimitive(value)
            table.writeText(lines.first() + "\n" + JsonArray(row) + "\n")
            val updated =
                manifest.copy(
                    entries =
                        manifest.entries.map { entry ->
                            val file = File(payload, entry.path)
                            entry.copy(size = file.length(), sha256 = BackupFormat.digest(file))
                        },
                )
            BackupArchive.write(payload, archive, updated)
        } finally {
            payload.deleteRecursively()
        }
    }

    private fun insert(
        table: String,
        changes: Map<String, Any?>,
    ) {
        val sql = db.openHelper.writableDatabase
        val columns = BackupTables.columns(sql, table)
        val values =
            columns.map { column ->
                if (changes.containsKey(column.name)) {
                    changes[column.name]
                } else {
                    when {
                        column.name == "id" || column.name.endsWith("Id") -> 1
                        !column.required -> null
                        column.type == "TEXT" -> ""
                        else -> 0
                    }
                }
            }
        sql.execSQL(
            "INSERT INTO `$table` (${columns.joinToString(
                ",",
            ) { "`${it.name}`" }}) VALUES (${columns.joinToString(",") { "?" }})",
            values.toTypedArray(),
        )
    }

    private fun number(query: String): Long =
        db.openHelper.writableDatabase.query(query).use {
            it.moveToFirst()
            it.getLong(0)
        }

    private fun text(query: String): String =
        db.openHelper.writableDatabase.query(query).use {
            it.moveToFirst()
            it.getString(0)
        }

    private fun createRepository(providerIo: BackupProviderIo): BackupRepository =
        BackupRepository(
            context,
            db,
            patternFiles,
            yarnCards,
            Dispatchers.IO,
            scope,
            providerIo,
        )

    private fun mockUri(name: String): Uri = "content://backup-test/$name".toUri()

    private suspend fun expectBackupFailure(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected backup failure")
        } catch (_: BackupException) {
        }
    }

    private class RacingReadProviderIo(
        private val archive: File,
    ) : BackupProviderIo {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        private val reads = AtomicInteger()

        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            if (reads.incrementAndGet() == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            check()
            archive.copyTo(target)
        }

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected write")
    }

    private class StalledReadProviderIo : BackupProviderIo {
        val started = CompletableDeferred<Unit>()

        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            started.complete(Unit)
            awaitCancellation()
        }

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected write")
    }

    private class FailingWriteProviderIo : BackupProviderIo {
        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected read")

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = throw IOException("provider unavailable")
    }
}
