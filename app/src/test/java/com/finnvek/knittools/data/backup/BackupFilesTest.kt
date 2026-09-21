package com.finnvek.knittools.data.backup

import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI

class BackupFilesTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var files: File
    private lateinit var payload: File

    @Before fun setUp() {
        files = temporary.newFolder()
        payload = temporary.newFolder()
        context =
            mockk {
                every { filesDir } returns files
            }
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { uri(firstArg()) }
        every { Uri.fromFile(any()) } answers { uri(firstArg<File>().toURI().toString()) }
    }

    @After fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private fun uri(text: String): Uri {
        val result = mockk<Uri>()
        every { result.scheme } returns URI(text).scheme
        every { result.path } returns URI(text).path
        every { result.authority } returns URI(text).authority
        every { result.toString() } returns text
        return result
    }

    private fun photo(name: String = "image.jpg"): File =
        File(files, "yarn_photos/1/$name").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("photo bytes")
        }

    @Test fun exportCopiesOwnedAndProviderFilesAndDeduplicatesByContent() {
        val providerUri = "content://provider/image"
        val providerFile = temporary.newFile().apply { writeText("photo bytes") }
        val exporter = BackupFiles(context, payload, mapOf(providerUri to providerFile))
        val photo = photo()
        val first = mutableMapOf<String, JsonElement>("photoUri" to JsonPrimitive(photo.toURI().toString()))
        exporter.export("yarn_cards", first) {}
        val stored = File(payload, first.getValue("photoUri").jsonPrimitive.content)
        assertEquals(photo.readText(), stored.readText())
        for (source in listOf(
            photo.toURI().toString(),
            photo("second.jpg").toURI().toString(),
            providerUri,
        )) {
            val row = mutableMapOf<String, JsonElement>("photoUri" to JsonPrimitive(source))
            exporter.export("yarn_cards", row) {}
            assertEquals(first, row)
        }
        assertEquals(1, File(payload, "files").listFiles()?.size)
        assertFalse(File(payload, "file-copy.tmp").exists())
        assertTrue(photo.exists())
    }

    @Test fun absentMetadataIsPreservedAndNonDurableSourcesAreRejected() {
        val exporter = BackupFiles(context, payload)
        for (row in listOf(
            mutableMapOf<String, JsonElement>(),
            mutableMapOf<String, JsonElement>("photoUri" to JsonNull),
            mutableMapOf<String, JsonElement>(
                "photoUri" to JsonPrimitive(" "),
            ),
        )) {
            val before = row.toMap()
            exporter.export("yarn_cards", row) {}
            exporter.export("sessions", row) {}
            assertEquals(before, row)
        }
        for (file in listOf(
            temporary.newFile(),
            files,
            File(files, "preferences.xml").apply {
                writeText("private")
            },
            File(files, "yarn_photos/missing"),
        )) {
            assertThrows(BackupException::class.java) { exporter.requireDurable(file) }
        }
        assertThrows(BackupException::class.java) {
            exporter.export("yarn_cards", mutableMapOf("photoUri" to JsonPrimitive("https://example.com/image"))) {}
        }
        assertThrows(BackupException::class.java) {
            exporter.export("yarn_cards", mutableMapOf("photoUri" to JsonPrimitive("content://provider/missing"))) {}
        }
    }

    @Test fun cancellationRemovesPartialCopyAndTemporaryCleanupDoesNotHideFailure() {
        val row = mutableMapOf<String, JsonElement>("photoUri" to JsonPrimitive(photo().toURI().toString()))
        assertThrows(CancellationException::class.java) {
            BackupFiles(context, payload).export("yarn_cards", row) { throw CancellationException() }
        }
        assertFalse(File(payload, "file-copy.tmp").exists())
        val blocked = temporary.newFolder()
        File(blocked, "retained").writeText("keep")
        BackupFiles.deleteTemporaryFile(blocked)
        assertTrue(File(blocked, "retained").exists())
        val removable = temporary.newFile()
        BackupFiles.deleteTemporaryFile(removable)
        BackupFiles.deleteTemporaryFile(removable)
        assertFalse(removable.exists())
    }

    @Test fun restorePublishesRebasedCopiesAndCleanupPreservesReferencedAndForeignFiles() {
        val source =
            File(payload, "files/${"a".repeat(64)}.bin").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("restored")
            }
        val restore = BackupRestoreFiles(context, payload)
        val rows =
            listOf(
                "yarn_cards" to "photoUri",
                "progress_photos" to "photoUri",
                "project_documents" to "localPdfUri",
            ).map { (table, column) ->
                val row =
                    mutableMapOf<String, JsonElement>(
                        "id" to JsonPrimitive(1),
                        "projectId" to JsonPrimitive(2),
                        column to JsonPrimitive(source.relativeTo(payload).invariantSeparatorsPath),
                    )
                restore.rebase(table, row)
                row.getValue(column).jsonPrimitive.content
            }
        assertEquals(source.length() * 3, restore.requiredBytes)
        val old = photo()
        restore.journal(setOf(old.toURI().toString()))
        restore.publish {}
        rows.forEach { assertEquals("restored", File(URI(it)).readText()) }
        assertTrue(BackupRestoreFiles.cleanup(context, payload, setOf(rows.first())))
        assertTrue(File(URI(rows.first())).exists())
        rows.drop(1).forEach { assertFalse(File(URI(it)).exists()) }
        assertFalse(old.exists())
        assertTrue(BackupRestoreFiles.cleanup(context, payload, emptySet()))
        val foreign = temporary.newFile()
        val private = File(files, "private.txt").apply { writeText("keep") }
        File(
            payload,
            BackupRestoreFiles.JOURNAL,
        ).writeText(
            BackupFormat.json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
                listOf(foreign.path, private.path),
            ),
        )
        assertTrue(BackupRestoreFiles.cleanup(context, payload, emptySet()))
        assertTrue(foreign.exists())
        assertTrue(private.exists())
    }

    @Test fun restoreRejectsInvalidPathsAndNeverOverwritesPublishedFiles() {
        val restore = BackupRestoreFiles(context, payload)
        restore.rebase("sessions", mutableMapOf())
        restore.rebase("yarn_cards", mutableMapOf<String, JsonElement>("photoUri" to JsonNull))
        restore.rebase("yarn_cards", mutableMapOf("photoUri" to JsonPrimitive("")))
        assertThrows(BackupException::class.java) {
            restore.rebase(
                "yarn_cards",
                mutableMapOf<String, JsonElement>(
                    "photoUri" to JsonPrimitive("../bad"),
                ),
            )
        }
        val source =
            File(payload, "files/${"b".repeat(64)}.bin").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("data")
            }
        val row =
            mutableMapOf<String, JsonElement>(
                "id" to JsonPrimitive(1),
                "photoUri" to JsonPrimitive(source.relativeTo(payload).invariantSeparatorsPath),
            )
        restore.rebase("yarn_cards", row)
        restore.publish {}
        assertThrows(BackupException::class.java) { restore.publish {} }
        assertEquals("data", File(URI(row.getValue("photoUri").jsonPrimitive.content)).readText())
    }

    @Test fun restoredProgressPhotoRowsReceiveDistinctFilesForSharedArchiveContent() {
        val source =
            File(payload, "files/${"c".repeat(64)}.bin").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("shared photo")
            }
        val restore = BackupRestoreFiles(context, payload)
        val rows =
            listOf(11, 12).map { id ->
                mutableMapOf<String, JsonElement>(
                    "id" to JsonPrimitive(id),
                    "projectId" to JsonPrimitive(7),
                    "photoUri" to JsonPrimitive(source.relativeTo(payload).invariantSeparatorsPath),
                ).also { restore.rebase("progress_photos", it) }
            }

        val restoredUris = rows.map { it.getValue("photoUri").jsonPrimitive.content }
        assertEquals(2, restoredUris.toSet().size)
        restore.publish {}
        restoredUris.forEach { assertEquals("shared photo", File(URI(it)).readText()) }
    }
}
