package com.finnvek.knittools.data.backup

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupZipStructureTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun classicAndZip64DirectoriesAreAccepted() {
        val archive = archiveWithExampleEntry()
        BackupZipStructure.verify(archive) {}
        val bytes = archive.readBytes()
        val end = bytes.size - 22
        val original = ByteBuffer.wrap(bytes, end, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
        val directorySize = original.getInt(12)
        val directoryOffset = original.getInt(16)
        appendZip64Directory(archive)
        BackupZipStructure.verify(archive) {}
        val zip64 = archive.readBytes()
        for (offset in listOf(end + 16, end + 20)) {
            val invalid = zip64.copyOf()
            ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, 1)
            archive.writeBytes(invalid)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
        val conflictingSize = zip64.copyOf()
        ByteBuffer.wrap(conflictingSize).order(ByteOrder.LITTLE_ENDIAN).putInt(conflictingSize.size - 10, 1)
        archive.writeBytes(conflictingSize)
        assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }

        for (field in listOf(10, 12, 16)) {
            val compatible = zip64.copyOf()
            val classic =
                ByteBuffer.wrap(compatible, compatible.size - 22, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
            when (field) {
                10 -> classic.putShort(8, 1).putShort(10, 1)
                12 -> classic.putInt(12, directorySize)
                16 -> classic.putInt(16, directoryOffset)
            }
            archive.writeBytes(compatible)
            BackupZipStructure.verify(archive) {}

            when (field) {
                10 -> classic.putShort(8, 2).putShort(10, 2)
                12 -> classic.putInt(12, directorySize + 1)
                16 -> classic.putInt(16, directoryOffset + 1)
            }
            archive.writeBytes(compatible)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
    }

    private fun appendZip64Directory(archive: File) {
        val bytes = archive.readBytes()
        val end = bytes.size - 22
        val original = ByteBuffer.wrap(bytes, end, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
        val trailer = ByteBuffer.allocate(98).order(ByteOrder.LITTLE_ENDIAN)
        trailer
            .putInt(0x06064b50)
            .putLong(44)
            .putShort(45)
            .putShort(45)
            .putInt(0)
            .putInt(0)
        trailer
            .putLong(1)
            .putLong(1)
            .putLong(original.getInt(12).toLong())
            .putLong(original.getInt(16).toLong())
        trailer
            .putInt(0x07064b50)
            .putInt(0)
            .putLong(end.toLong())
            .putInt(1)
        original
            .putShort(8, -1)
            .putShort(10, -1)
            .putInt(12, -1)
            .putInt(16, -1)
        trailer.put(bytes, end, 22)
        archive.writeBytes(bytes.copyOf(end) + trailer.array())
    }

    @Test fun localAndCentralCompressionMethodsMustAgree() {
        val archive = archiveWithExampleEntry()
        val bytes = archive.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val directory = buffer.getInt(bytes.size - 6)
        buffer.putShort(directory + 10, ZipEntry.STORED.toShort())
        archive.writeBytes(bytes)

        assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
    }

    @Test fun localAndCentralFlagsMustAgree() {
        val archive = archiveWithExampleEntry(withContent = false)
        val bytes = archive.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val directory = buffer.getInt(bytes.size - 6)
        val flags = buffer.getShort(directory + 8).toInt()
        buffer.putShort(directory + 8, (flags xor 0x800).toShort())
        archive.writeBytes(bytes)

        assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
    }

    @Test fun storedMemberCannotCountTrailingBytesAsCompressedData() {
        val archive = temporary.newFile()
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(
                ZipEntry("manifest.json").apply {
                    method = ZipEntry.STORED
                    size = 0
                    crc = 0
                },
            )
            zip.closeEntry()
        }
        BackupZipStructure.verify(archive) {}

        val original = archive.readBytes()
        val buffer = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN)
        val directoryField = original.size - 6
        val central = buffer.getInt(directoryField)
        buffer.putInt(18, 1)
        buffer.putInt(central + 20, 1)
        buffer.putInt(directoryField, central + 1)
        val modified = original.copyOfRange(0, central) + byteArrayOf(0) + original.copyOfRange(central, original.size)
        archive.writeBytes(modified)

        assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
    }

    @Test fun eitherNonzeroClassicDiskFieldIsRejected() {
        val archive = archiveWithExampleEntry(withContent = false)
        val valid = archive.readBytes()
        for (offset in listOf(4, 6)) {
            val invalid = valid.copyOf()
            ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putShort(valid.size - 22 + offset, 1)
            archive.writeBytes(invalid)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
    }

    @Test fun zip64LocalOffsetsAreCheckedAgainstLocalNames() {
        val archive = archiveWithExampleEntry(withContent = false)
        val original = archive.readBytes()
        val end = original.size - 22
        val buffer = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN)
        val central = buffer.getInt(end + 16)
        val nameEnd = central + 46 + "example".length
        buffer.putInt(central + 42, -1)
        buffer.putShort(central + 30, 12)
        buffer.putInt(end + 12, buffer.getInt(end + 12) + 12)
        val extra =
            ByteBuffer
                .allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(1)
                .putShort(8)
                .putLong(0)
                .array()
        val bytes = original.copyOfRange(0, nameEnd) + extra + original.copyOfRange(nameEnd, original.size)
        archive.writeBytes(bytes)
        BackupZipStructure.verify(archive) {}
        bytes[30] = 'X'.code.toByte()
        archive.writeBytes(bytes)
        assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
    }

    @Test fun falseCountsOversizedDirectoryAndInvalidOffsetsAreRejectedBeforeZipAllocation() {
        val archive = archiveWithExampleEntry(withContent = false)
        val valid = archive.readBytes()
        for (variant in 0..2) {
            val bytes = valid.copyOf()
            val end = ByteBuffer.wrap(bytes, bytes.size - 22, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
            when (variant) {
                0 -> end.putShort(8, 2).putShort(10, 2)
                1 -> end.putInt(12, Int.MAX_VALUE)
                else -> end.putInt(16, -100)
            }
            archive.writeBytes(bytes)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
    }

    private fun archiveWithExampleEntry(withContent: Boolean = true): File {
        val archive = temporary.newFile()
        ZipOutputStream(archive.outputStream()).use {
            it.putNextEntry(ZipEntry("example"))
            if (withContent) it.write(byteArrayOf(1, 2, 3))
            it.closeEntry()
        }
        return archive
    }
}
