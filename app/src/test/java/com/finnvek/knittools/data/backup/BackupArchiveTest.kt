package com.finnvek.knittools.data.backup

import com.finnvek.knittools.data.local.KNITTOOLS_DATABASE_VERSION
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun roundTripChecksAllTablesAndFileBytes() {
        val (source, manifest) = fixture()
        assertEquals(KNITTOOLS_DATABASE_VERSION, manifest.schemaVersion)
        assertEquals(1, manifest.formatVersion)
        assertEquals(1, manifest.dataVersion)
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val target = temporary.newFolder()
        assertEquals(manifest, BackupArchive.extract(archive, target))
        manifest.entries.forEach { assertEquals(it.sha256, BackupFormat.digest(File(target, it.path))) }
    }

    @Test fun schema25And26BackupsRetainEveryTableAndFileOnExtraction() {
        val (source, current) = fixture()
        for (version in listOf(25, 26)) {
            val legacy = current.copy(schemaVersion = version)
            val archive = temporary.newFile()
            BackupArchive.write(source, archive, legacy)
            val target = temporary.newFolder()
            assertEquals(legacy, BackupArchive.extract(archive, target))
            legacy.entries.forEach { assertEquals(it.sha256, BackupFormat.digest(File(target, it.path))) }
        }
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

    @Test fun unsupportedFormatDataAndSchemaAreRejected() {
        val (source, manifest) = fixture()
        listOf(
            manifest.copy(formatVersion = 2),
            manifest.copy(dataVersion = 2),
            manifest.copy(schemaVersion = 24),
            manifest.copy(schemaVersion = KNITTOOLS_DATABASE_VERSION + 1),
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

    @Test fun swappedLocalNamesAreRejectedEvenWhenInventoryAndContentsMatch() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val bytes = archive.readBytes()
        val first = "tables/counter_history.jsonl".toByteArray()
        val second = "tables/progress_photos.jsonl".toByteArray()
        assertEquals(first.size, second.size)
        val firstOffset =
            (0..bytes.size - first.size).first { index ->
                first.indices.all { bytes[index + it] == first[it] }
            }
        val secondOffset =
            (0..bytes.size - second.size).first { index ->
                second.indices.all { bytes[index + it] == second[it] }
            }
        second.copyInto(bytes, firstOffset)
        first.copyInto(bytes, secondOffset)
        archive.writeBytes(bytes)
        assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
    }

    @Test fun centralCompressedSizeCannotExceedTheLocalStreamSize() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val bytes = archive.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val central = buffer.getInt(bytes.size - 22 + 16)
        buffer.putInt(central + 20, buffer.getInt(central + 20) + 1)
        archive.writeBytes(bytes)
        assertThrows(BackupException::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
    }

    @Test fun zip64ResolvedSizesAndOffsetsMustMatchTheLocalStream() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val original = archive.readBytes()
        val buffer = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN)
        val end = original.size - 22
        val central = buffer.getInt(end + 16)
        val nameEnd = central + 46 + (buffer.getShort(central + 28).toInt() and 0xffff)
        assertEquals(0, buffer.getShort(central + 30).toInt())
        val compressed = buffer.getInt(central + 20).toLong() and 0xffffffffL
        val uncompressed = buffer.getInt(central + 24).toLong() and 0xffffffffL
        val local = buffer.getInt(central + 42).toLong() and 0xffffffffL
        buffer.putInt(central + 20, -1)
        buffer.putInt(central + 24, -1)
        buffer.putInt(central + 42, -1)
        buffer.putShort(central + 30, 28)
        buffer.putInt(end + 12, buffer.getInt(end + 12) + 28)
        for (mismatch in 0..2) {
            val extra =
                ByteBuffer
                    .allocate(28)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(1)
                    .putShort(24)
                    .putLong(uncompressed + if (mismatch == 1) 1 else 0)
                    .putLong(compressed + if (mismatch == 2) 1 else 0)
                    .putLong(local)
                    .array()
            archive.writeBytes(original.copyOfRange(0, nameEnd) + extra + original.copyOfRange(nameEnd, original.size))
            if (mismatch == 0) {
                assertEquals(manifest, BackupArchive.extract(archive, temporary.newFolder()))
            } else {
                assertThrows(Exception::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
            }
        }
    }

    @Test fun overlappingOffsetsEncryptedMethodsAndTruncatedStreamsAreRejected() {
        val (source, manifest) = fixture()
        val archive = temporary.newFile()
        BackupArchive.write(source, archive, manifest)
        val original = archive.readBytes()
        for (mutation in 0..4) {
            val bytes = original.copyOf()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val central = buffer.getInt(bytes.size - 22 + 16)
            when (mutation) {
                0 -> buffer.putInt(central + 42, 30)
                1 -> {
                    buffer.putShort(6, (buffer.getShort(6).toInt() or 1).toShort())
                    buffer.putShort(central + 8, (buffer.getShort(central + 8).toInt() or 1).toShort())
                }
                2 -> {
                    buffer.putShort(8, 99)
                    buffer.putShort(central + 10, 99)
                }
                3 -> buffer.putInt(central + 20, 1)
                4 -> buffer.putInt(bytes.size - 22 + 12, 1)
            }
            archive.writeBytes(bytes)
            assertThrows(Exception::class.java) { BackupArchive.extract(archive, temporary.newFolder()) }
        }
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

    @Test fun manifestBudgetsAcceptExactMobileLimitsAndRejectLimitPlusOne() {
        fun tableEntries(sizes: Map<String, Long> = emptyMap()) =
            BackupFormat.tablePaths.map { path -> BackupEntry(path, sizes[path] ?: 0L, "0".repeat(64)) }

        fun file(
            index: Int,
            size: Long,
        ): BackupEntry = BackupEntry("files/${index.toString(16).padStart(64, '0')}.bin", size, "0".repeat(64))

        fun manifest(entries: List<BackupEntry>) =
            BackupManifest(createdAt = 1, versionCode = 1, versionName = "1.0", entries = entries)

        val tablePaths = BackupFormat.tablePaths.toList()
        val exact =
            tableEntries(
                mapOf(
                    tablePaths[0] to BackupLimits.MAX_TABLE_BYTES,
                    tablePaths[1] to BackupLimits.MAX_TABLE_BYTES,
                ),
            ) +
                listOf(
                    file(1, BackupLimits.MAX_DURABLE_FILE_BYTES),
                    file(2, BackupLimits.MAX_DURABLE_FILE_BYTES),
                    file(3, BackupLimits.MAX_DURABLE_BYTES - 2 * BackupLimits.MAX_DURABLE_FILE_BYTES),
                )
        BackupArchive.validateManifest(manifest(exact))

        assertThrows(BackupException::class.java) {
            BackupArchive.validateManifest(
                manifest(tableEntries(mapOf(tablePaths[0] to BackupLimits.MAX_TABLE_BYTES + 1))),
            )
        }
        assertThrows(BackupException::class.java) {
            BackupArchive.validateManifest(
                manifest(
                    tableEntries(
                        mapOf(
                            tablePaths[0] to BackupLimits.MAX_TABLE_BYTES,
                            tablePaths[1] to BackupLimits.MAX_TABLE_BYTES,
                            tablePaths[2] to 1,
                        ),
                    ),
                ),
            )
        }
        assertThrows(BackupException::class.java) {
            BackupArchive.validateManifest(
                manifest(tableEntries() + file(1, BackupLimits.MAX_DURABLE_FILE_BYTES + 1)),
            )
        }
        assertThrows(BackupException::class.java) {
            BackupArchive.validateManifest(
                manifest(
                    tableEntries() +
                        listOf(
                            file(1, BackupLimits.MAX_DURABLE_FILE_BYTES),
                            file(2, BackupLimits.MAX_DURABLE_FILE_BYTES),
                            file(3, BackupLimits.MAX_DURABLE_BYTES - 2 * BackupLimits.MAX_DURABLE_FILE_BYTES + 1),
                        ),
                ),
            )
        }

        val exactFileCount =
            tableEntries() +
                List(BackupLimits.MAX_DURABLE_FILE_COUNT.toInt()) { index -> file(index, 0) }
        BackupArchive.validateManifest(manifest(exactFileCount))
        assertThrows(BackupException::class.java) {
            BackupArchive.validateManifest(manifest(exactFileCount + file(Int.MAX_VALUE, 0)))
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
