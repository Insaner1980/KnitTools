package com.finnvek.knittools.data.backup

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun roundTripChecksAllTablesAndFileBytes() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val target = temporary.newFolder()
        assertEquals(manifest, BackupArchive.extract(archive, target))
        manifest.entries.forEach { assertEquals(it.sha256, BackupFormat.digest(File(target, it.path))) }
    }

    @Test fun changedBytesAndHashAndSizeAreRejected() {
        val (source, manifest) = fixture()
        val first = manifest.entries.first()
        listOf(
            first.copy(sha256 = "0".repeat(64)),
            first.copy(size = first.size + 1),
            first.copy(size = first.size - 1),
        ).forEach { bad -> rejected(source, manifest.copy(entries = listOf(bad) + manifest.entries.drop(1))) }
        File(source, first.path).appendText("changed")
        rejected(source, manifest)
    }

    @Test fun futureFormatDataAndSchemaAreRejected() {
        val (source, manifest) = fixture()
        listOf(
            manifest.copy(formatVersion = 2),
            manifest.copy(dataVersion = 2),
            manifest.copy(schemaVersion = 26),
        ).forEach {
            val error = rejected(source, it)
            assertEquals(BackupError.UNSUPPORTED, error.error)
        }
    }

    @Test fun truncatedArchiveIsRejectedEvenWhenAllEntryPayloadsExist() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        archive.writeBytes(archive.readBytes().dropLast(1).toByteArray())
        assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
    }

    @Test fun traversalAbsoluteBackslashAndUnexpectedNamesAreRejected() {
        listOf(
            "../outside",
            "/outside",
            "C:/outside",
            "files\\..\\outside",
            "tables//counter_projects.jsonl",
            "cache/file",
            "files/ABC.bin",
        ).forEach { name ->
            assertFalse(BackupFormat.allowedPath(name))
            val archive = rawZip(mapOf(name to byteArrayOf(1)))
            assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
        }
    }

    @Test fun missingManifestMissingRequiredTableAndUnexpectedEntryAreRejected() {
        val (source, manifest) = fixture()
        val noManifest = rawZip(mapOf(manifest.entries.first().path to byteArrayOf(1)))
        assertThrows(BackupException::class.java) { BackupArchive.extract(noManifest, temporary.newFolder()) }
        rejected(source, manifest.copy(entries = manifest.entries.drop(1)))
        val content =
            manifest.entries.associate { it.path to File(source, it.path).readBytes() } +
                (BackupFormat.MANIFEST to BackupFormat.json.encodeToString(manifest).toByteArray()) +
                ("files/${"a".repeat(64)}.bin" to byteArrayOf(1))
        assertThrows(BackupException::class.java) { BackupArchive.extract(rawZip(content), temporary.newFolder()) }
    }

    @Test fun malformedManifestAndDuplicateEntriesAreRejected() {
        val bad = rawZip(mapOf(BackupFormat.MANIFEST to "{broken".toByteArray()))
        assertThrows(Exception::class.java) { BackupArchive.extract(bad, temporary.newFolder()) }
        val archive = rawZip(mapOf("manifest.json" to byteArrayOf(1), "manifesx.json" to byteArrayOf(2)))
        val bytes = archive.readBytes()
        val needle = "manifesx.json".toByteArray()
        for (index in 0..bytes.size - needle.size) {
            if (needle.indices.all { bytes[index + it] == needle[it] }) bytes[index + 7] = 't'.code.toByte()
        }
        archive.writeBytes(bytes)
        assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
    }

    @Test fun decompressionAndManifestSizeLimitsAreEnforced() {
        assertThrows(BackupException::class.java) {
            BackupFormat.copy(ByteArrayInputStream(ByteArray(1024)), ByteArrayOutputStream(), 100)
        }
        val (source, manifest) = fixture()
        rejected(source, manifest.copy(entries = manifest.entries.map { it.copy(size = BackupFormat.MAX_FILE + 1) }))
        rejected(source, manifest.copy(entries = List(BackupFormat.MAX_ENTRIES) { manifest.entries.first() }))
    }

    @Test fun cancellationPropagatesFromLongArchiveOperations() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        assertThrows(kotlinx.coroutines.CancellationException::class.java) {
            BackupArchive.write(source, archive, manifest) { throw kotlinx.coroutines.CancellationException() }
        }
    }

    private fun fixture(): Pair<File, BackupManifest> {
        val directory = temporary.newFolder()
        val paths = BackupFormat.tablePaths + "files/${"a".repeat(64)}.bin"
        val entries =
            paths.map { path ->
                val file = File(directory, path)
                checkNotNull(file.parentFile).mkdirs()
                file.writeText("[]\n")
                BackupEntry(path, file.length(), BackupFormat.digest(file))
            }
        return directory to BackupManifest(createdAt = 1, versionCode = 1, versionName = "1.0", entries = entries)
    }

    private fun rejected(
        source: File,
        manifest: BackupManifest,
    ): BackupException {
        val archive = temporary.newFile()
        rawZip(
            manifest.entries.distinctBy { it.path }.associate { it.path to File(source, it.path).readBytes() } +
                (BackupFormat.MANIFEST to BackupFormat.json.encodeToString(manifest).toByteArray()),
            archive,
        )
        return assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
    }

    private fun rawZip(
        entries: Map<String, ByteArray>,
        file: File = temporary.newFile(),
    ): File {
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }
}
