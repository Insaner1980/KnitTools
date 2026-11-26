package com.finnvek.knittools.data.storage

import android.content.Context
import android.content.ContextWrapper
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupRestoreFiles
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class AuditPart13BoundaryTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val token = UUID.randomUUID().toString()
    private val directory = File(app.cacheDir, "audit13-$token")
    private val preferenceNames = mutableSetOf<String>()
    private val context =
        object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this

            override fun getFilesDir() = File(directory, "files").apply { mkdirs() }

            override fun getNoBackupFilesDir() = File(directory, "no-backup").apply { mkdirs() }

            override fun getSharedPreferences(
                name: String,
                mode: Int,
            ) = app.getSharedPreferences("audit13-$token-$name".also(preferenceNames::add), mode)
        }

    @Before fun setUp() {
        check(app.packageName.endsWith(".activepolicytest"))
        check(directory.mkdirs())
    }

    @After fun tearDown() {
        preferenceNames.forEach(app::deleteSharedPreferences)
        directory.deleteRecursively()
    }

    @Test fun realPreferencesRetainExactlyOneHundredTokensAndConsumeOnce() {
        for (count in listOf(99, 100, 101)) {
            context
                .getSharedPreferences("counter_launch_tokens", 0)
                .edit()
                .clear()
                .commit()
            val tokens = List(count) { CounterLaunchTokenStore.issueLaunchId(context, 1_000L) }
            tokens.forEachIndexed { index, id ->
                assertEquals(count <= 100 || index > 0, CounterLaunchTokenStore.consumeLaunchId(context, id, 1_001L))
                assertFalse(CounterLaunchTokenStore.consumeLaunchId(context, id, 1_001L))
            }
        }
    }

    @Test fun realPreferencesEnforceExactExpiryBeforeAtAndAfterTwentyFourHours() {
        for (age in listOf(86_399_999L, 86_400_000L, 86_400_001L)) {
            val id = CounterLaunchTokenStore.issueLaunchId(context, 1_000L)
            assertEquals(age < 86_400_000L, CounterLaunchTokenStore.consumeLaunchId(context, id, 1_000L + age))
        }
    }

    private fun preparedFiles(): Pair<BackupRestoreFiles, File> {
        val payload = File(context.noBackupFilesDir, UUID.randomUUID().toString()).apply { mkdirs() }
        val old =
            File(context.filesDir, "pattern_pdfs/0/old.pdf").apply {
                parentFile!!.mkdirs()
                writeText("old bytes")
            }
        val files = BackupRestoreFiles(context, payload)
        repeat(2) { index ->
            val path = "files/${(index + 1).toString().repeat(64)}.bin"
            File(payload, path).apply {
                parentFile!!.mkdirs()
                writeText("new bytes $index")
            }
            files.rebase("saved_patterns", mutableMapOf<String, JsonElement>("localPdfUri" to JsonPrimitive(path)))
        }
        assertEquals(2, files.archivePaths.size)
        assertTrue(old.isFile)
        return files to payload
    }

    @Test fun journalWriteAndRenameFailuresLeaveOriginalBytesIntact() {
        for (blockTemporary in listOf(true, false)) {
            val (files, payload) = preparedFiles()
            val name = BackupRestoreFiles.JOURNAL + if (blockTemporary) ".tmp" else ""
            File(payload, "$name/blocker").apply {
                parentFile!!.mkdirs()
                writeText("block")
            }
            assertNotNull(runCatching { files.journal(emptySet()) }.exceptionOrNull())
            assertEquals("old bytes", File(context.filesDir, "pattern_pdfs/0/old.pdf").readText())
            assertFalse(context.filesDir.walkTopDown().any { it.name.startsWith("restore-") })
        }
    }

    @Test fun interruptedPublicationCleansPartialCopiesAndKeepsOriginal() {
        val (files, payload) = preparedFiles()
        val old = File(context.filesDir, "pattern_pdfs/0/old.pdf")
        files.journal(setOf(old.toUri().toString()))
        assertNotNull(
            runCatching {
                files.publish {
                    if (context.filesDir.walkTopDown().count { it.isFile && it.name.startsWith("restore-") } >= 1) {
                        throw java.io.IOException("Synthetic interrupted publication")
                    }
                }
            }.exceptionOrNull(),
        )
        assertTrue(BackupRestoreFiles.cleanup(context, payload, setOf(old.toUri().toString())))
        assertEquals("old bytes", old.readText())
        assertFalse(context.filesDir.walkTopDown().any { it.name.startsWith("restore-") })
    }

    @Test fun failedCleanupRetainsJournalAndRetryKeepsCommittedBytes() {
        val (files, payload) = preparedFiles()
        val old = File(context.filesDir, "pattern_pdfs/0/old.pdf")
        files.journal(setOf(old.toUri().toString()))
        files.publish {}
        val committed =
            context.filesDir
                .walkTopDown()
                .filter { it.isFile && it.name.startsWith("restore-") }
                .toList()
        assertEquals(2, committed.size)
        val hashes = committed.map { BackupFormat.digest(it) {} }
        check(old.delete())
        val blocker =
            File(old, "blocker").apply {
                parentFile!!.mkdirs()
                writeText("prevent deletion")
            }
        val references = committed.map { it.toUri().toString() }.toSet()
        assertFalse(BackupRestoreFiles.cleanup(context, payload, references))
        assertTrue(File(payload, BackupRestoreFiles.JOURNAL).isFile)
        assertEquals(hashes, committed.map { BackupFormat.digest(it) {} })
        check(blocker.delete())
        assertTrue(BackupRestoreFiles.cleanup(context, payload, references))
        assertFalse(old.exists())
        assertEquals(hashes, committed.map { BackupFormat.digest(it) {} })
    }
}
