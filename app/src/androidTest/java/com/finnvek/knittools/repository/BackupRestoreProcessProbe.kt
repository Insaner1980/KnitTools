package com.finnvek.knittools.repository

import android.content.Context
import android.os.Process
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupRestoreFiles
import com.finnvek.knittools.data.backup.BackupTables
import com.finnvek.knittools.data.storage.AppFileStorage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch

internal class BackupRestoreProcessProbe(
    private val directory: File,
) {
    private var armed = false
    private var changed = false
    private val phase = requireNotNull(phase())
    private var committed = false
    private var deferCleanup = false
    private var cleanupDatabase: SupportSQLiteDatabase? = null
    private var obsoleteFiles = emptyList<File>()
    private lateinit var cleanupJournal: File
    private lateinit var restoreContext: Context

    fun arm(
        database: SupportSQLiteDatabase,
        context: Context,
    ) {
        restoreContext = context
        BackupTables.export(database, File(directory, "before"), { _, _ -> }, check = {})
        val hashes =
            BackupTables
                .references(database)
                .map {
                    checkNotNull(
                        AppFileStorage.resolveAppOwnedFile(context, android.net.Uri.parse(it)),
                    ).canonicalFile
                }.distinct()
                .map { BackupFormat.digest(it) {} }
                .sorted()
        File(directory, "file-hashes.txt").writeText(hashes.joinToString("\n"))
        if (phase == "precommit") File(directory, "precommit-owned-files.json").writeText(ownedFiles(context))
        armed = true
    }

    fun verifyPreCommitFiles(context: Context) {
        assertEquals(File(directory, "precommit-owned-files.json").readText(), ownedFiles(context))
    }

    private fun ownedFiles(context: Context): String =
        fileSnapshot(
            context.filesDir
                .walkTopDown()
                .filter(File::isFile)
                .toList()
                .sortedBy(File::getPath),
        )

    val factory =
        interceptTestDatabase { database ->
            object : SupportSQLiteDatabase by database {
                override fun execSQL(sql: String) {
                    database.execSQL(sql)
                    if (armed && sql.startsWith("DELETE FROM ")) {
                        changed = true
                        if (phase == "precommit") pause(database.inTransaction())
                    }
                }

                override fun endTransaction() {
                    database.endTransaction()
                    if (armed && changed && phase == "postcommit" && !database.inTransaction()) {
                        recordPostCommitBoundary(database)
                        pause(false)
                    }
                    if (armed && changed && phase == "cleanup" && !committed && !database.inTransaction()) {
                        committed = true
                        deferCleanup = true
                    }
                }
            }
        }

    private fun pause(inTransaction: Boolean): Nothing {
        check(inTransaction == (phase == "precommit"))
        FileOutputStream(File(directory, "checkpoint.txt")).use {
            it.write("${Process.myPid()} $phase $inTransaction".toByteArray())
            it.fd.sync()
        }
        CountDownLatch(1).await()
        error("Process checkpoint cannot resume")
    }

    private fun recordPostCommitBoundary(database: SupportSQLiteDatabase) {
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(restoreContext)
                .name(checkNotNull(database.path))
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(database.version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = error("Expected committed database")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected migration")
                    },
                ).build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { reader ->
            val sql = reader.readableDatabase
            check(!sql.inTransaction())
            sql.query("SELECT name, count, id FROM counter_projects").use {
                check(it.moveToFirst())
                check(it.getString(0) == "Cardigan" && it.getInt(1) == 12 && it.getLong(2) > 1)
                check(!it.moveToNext())
            }
            val baseline = File(directory, "committed")
            BackupTables.export(sql, baseline, { _, _ -> }, check = {})
            recordContent(sql, restoreContext, baseline)
            val journal =
                File(directory, "private/manual-backup").walkTopDown().single {
                    it.name == BackupRestoreFiles.JOURNAL
                }
            journal.copyTo(File(baseline, BackupRestoreFiles.JOURNAL))
            val paths =
                BackupFormat.json.parseToJsonElement(journal.readText()).jsonArray.map {
                    File(it.jsonPrimitive.content).canonicalFile
                }
            val referenced =
                BackupTables
                    .references(sql)
                    .map {
                        checkNotNull(
                            AppFileStorage.resolveAppOwnedFile(restoreContext, android.net.Uri.parse(it)),
                        ).canonicalFile
                    }.toSet()
            check(referenced.size == 4 && paths.containsAll(referenced))
            check(paths.all { it.isFile })
            val obsolete = paths.filter { it !in referenced }
            check(obsolete.size == 4)
            File(baseline, "obsolete.txt").writeText(obsolete.joinToString("\n") { it.path })
            File(baseline, "boundary.txt").writeText(
                "independent_connection=committed\njournal=pending\nreferenced=4\nobsolete=4\n",
            )
        }
    }

    private fun assertRestoredTables(after: File) {
        val baseline = File(directory, "committed")
        (BackupFormat.tables.map { "tables/$it.jsonl" } + listOf("sequences.json", "referenced-files.json")).forEach {
            assertEquals(it, File(baseline, it).readText(), File(after, it).readText())
        }
    }

    fun verifyPostCommitRecovery(
        database: SupportSQLiteDatabase,
        context: Context,
        after: File,
    ) {
        val baseline = File(directory, "committed")
        recordContent(database, context, after)
        assertRestoredTables(after)
        File(baseline, "obsolete.txt").readLines().forEach { assertFalse(File(it).exists()) }
    }

    fun beforeFilesAccess() {
        if (deferCleanup) {
            throw IOException("Test-only initial cleanup failure after committed replacement")
        }
        val database = cleanupDatabase ?: return
        if (Thread.currentThread().stackTrace.none {
                it.className == BackupRestoreFiles.Companion::class.java.name && it.methodName == "cleanup"
            }
        ) {
            return
        }
        if (obsoleteFiles.first().exists()) return
        cleanupDatabase = null
        check(database.inTransaction())
        check(obsoleteFiles.drop(1).all { it.isFile })
        check(cleanupJournal.isFile)
        val partial = File(directory, "partial-cleanup").apply { check(mkdir()) }
        cleanupJournal.copyTo(File(partial, BackupRestoreFiles.JOURNAL))
        File(partial, "owned-files.json").writeText(
            fileSnapshot(
                File(directory, "files")
                    .walkTopDown()
                    .filter { it.isFile }
                    .toList()
                    .sortedBy { it.path },
            ),
        )
        File(partial, "deleted.txt").writeText(obsoleteFiles.first().canonicalPath)
        File(partial, "remaining.txt").writeText(obsoleteFiles.drop(1).joinToString("\n") { it.canonicalPath })
        File(directory, "checkpoint.txt").writeText("${Process.myPid()} cleanup true")
        CountDownLatch(1).await()
        error("Partial-cleanup checkpoint cannot resume")
    }

    fun resumeFileAccess() {
        if (phase != "cleanup") return
        check(committed && deferCleanup)
        deferCleanup = false
    }

    fun armCleanup(
        database: SupportSQLiteDatabase,
        context: Context,
    ) {
        check(phase == "cleanup" && committed && !deferCleanup)
        cleanupJournal =
            File(directory, "private/manual-backup").walkTopDown().single {
                it.name == BackupRestoreFiles.JOURNAL
            }
        val paths =
            BackupFormat.json.parseToJsonElement(cleanupJournal.readText()).jsonArray.map {
                File(it.jsonPrimitive.content).canonicalFile
            }
        val referenced =
            BackupTables
                .references(database)
                .map {
                    checkNotNull(AppFileStorage.resolveAppOwnedFile(context, android.net.Uri.parse(it))).canonicalFile
                }.toSet()
        check(paths.containsAll(referenced))
        obsoleteFiles = paths.filter { it !in referenced }
        check(obsoleteFiles.size >= 2 && obsoleteFiles.all { it.isFile })
        val sentinel = File(context.filesDir, "pattern_pdfs/0/unrelated-sentinel.txt")
        check(sentinel !in paths && !sentinel.exists())
        sentinel.writeText("Unrelated RW-03G test-owned bytes")
        val baseline = File(directory, "committed")
        BackupTables.export(database, baseline, { _, _ -> }, check = {})
        recordContent(database, context, baseline)
        cleanupJournal.copyTo(File(baseline, BackupRestoreFiles.JOURNAL))
        File(baseline, "owned-files.json").writeText(
            fileSnapshot(
                context.filesDir
                    .walkTopDown()
                    .filter { it.isFile }
                    .toList()
                    .sortedBy { it.path },
            ),
        )
        cleanupDatabase = database
    }

    fun verifyCleanupRecovery(
        database: SupportSQLiteDatabase,
        context: Context,
        after: File,
    ) {
        recordContent(database, context, after)
        val baseline = File(directory, "committed")
        assertRestoredTables(after)
        database.query("PRAGMA integrity_check").use {
            assertTrue(it.moveToFirst())
            assertEquals("ok", it.getString(0))
            assertFalse(it.moveToNext())
        }
        val partial = File(directory, "partial-cleanup")
        val obsolete = File(partial, "remaining.txt").readLines() + File(partial, "deleted.txt").readText()
        obsolete.forEach { assertFalse(File(it).exists()) }
        val beforeFiles = BackupFormat.json.parseToJsonElement(File(baseline, "owned-files.json").readText()).jsonArray
        val expected = JsonArray(beforeFiles.filter { it.jsonArray[0].jsonPrimitive.content !in obsolete })
        assertEquals(
            expected.toString(),
            fileSnapshot(
                context.filesDir
                    .walkTopDown()
                    .filter { it.isFile }
                    .toList()
                    .sortedBy { it.path },
            ),
        )
        assertEquals(
            "Unrelated RW-03G test-owned bytes",
            File(context.filesDir, "pattern_pdfs/0/unrelated-sentinel.txt").readText(),
        )
        File(after, "cleanup-verification.txt").writeText("integrity_check=ok\nobsolete=absent\nsentinel=unchanged\n")
    }

    fun recordRecovery(
        database: SupportSQLiteDatabase,
        context: Context,
        after: File,
        first: File,
        stage: String,
    ) {
        recordContent(database, context, after)
        File(after, "owned-files.json").writeText(
            fileSnapshot(
                context.filesDir
                    .walkTopDown()
                    .filter { it.isFile }
                    .toList()
                    .sortedBy { it.path },
            ),
        )
        File(after, "recovery-state.txt").writeText(
            "schema=${database.version}\nquick_check=ok\nforeign_keys=0\nstaging=empty\n",
        )
        if (stage == "second") {
            assertFalse(File(first, "process.txt").readText().toInt() == Process.myPid())
            val snapshots =
                BackupFormat.tables.map { "tables/$it.jsonl" } +
                    listOf("sequences.json", "referenced-files.json", "owned-files.json", "recovery-state.txt")
            snapshots.forEach { path ->
                assertEquals(path, File(first, path).readText(), File(after, path).readText())
            }
        }
        File(after, "process.txt").writeText(Process.myPid().toString())
    }

    private fun recordContent(
        database: SupportSQLiteDatabase,
        context: Context,
        after: File,
    ) {
        val sequences =
            database.query("SELECT name, seq FROM sqlite_sequence ORDER BY name").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(JsonArray(listOf(JsonPrimitive(cursor.getString(0)), JsonPrimitive(cursor.getLong(1)))))
                    }
                }
            }
        File(after, "sequences.json").writeText(JsonArray(sequences).toString())
        val references =
            BackupTables
                .references(database)
                .map {
                    checkNotNull(AppFileStorage.resolveAppOwnedFile(context, android.net.Uri.parse(it))).canonicalFile
                }.distinct()
                .sortedBy { it.path }
        File(after, "referenced-files.json").writeText(fileSnapshot(references))
    }

    private fun fileSnapshot(files: List<File>): String =
        JsonArray(
            files.map { file ->
                check(file.isFile)
                JsonArray(
                    listOf(
                        JsonPrimitive(file.canonicalPath),
                        JsonPrimitive(file.length()),
                        JsonPrimitive(BackupFormat.digest(file) {}),
                    ),
                )
            },
        ).toString()

    companion object {
        fun recoveryStage(): String? =
            InstrumentationRegistry.getArguments().getString("backupRecoveryStage")?.also {
                check(it == "first" || it == "second")
                checkNotNull(phase())
                check(InstrumentationRegistry.getArguments().getString("backupCrashVerify") == "true")
                check(!InstrumentationRegistry.getArguments().getString("backupCrashRun").isNullOrBlank())
            }

        fun phase(): String? =
            InstrumentationRegistry.getArguments().getString("backupCrashPhase")?.also {
                check(it == "precommit" || it == "postcommit" || it == "cleanup")
                check(ApplicationProvider.getApplicationContext<Context>().packageName.endsWith(".activepolicytest"))
            }
    }
}
