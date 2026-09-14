package com.finnvek.knittools.data.backup

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupZipStructureTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun classicAndZip64DirectoriesAreAccepted() {
        val archive = temporary.newFile()
        ZipOutputStream(archive.outputStream()).use {
            it.putNextEntry(ZipEntry("example"))
            it.write(byteArrayOf(1, 2, 3))
            it.closeEntry()
        }
        BackupZipStructure.verify(archive) {}
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
        BackupZipStructure.verify(archive) {}
        val zip64 = archive.readBytes()
        for (offset in listOf(end + 16, end + 20)) {
            val invalid = zip64.copyOf()
            ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, 1)
            archive.writeBytes(invalid)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
    }

    @Test fun eitherNonzeroClassicDiskFieldIsRejected() {
        val archive = temporary.newFile()
        ZipOutputStream(archive.outputStream()).use {
            it.putNextEntry(ZipEntry("example"))
            it.closeEntry()
        }
        val valid = archive.readBytes()
        for (offset in listOf(4, 6)) {
            val invalid = valid.copyOf()
            ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putShort(valid.size - 22 + offset, 1)
            archive.writeBytes(invalid)
            assertThrows(BackupException::class.java) { BackupZipStructure.verify(archive) {} }
        }
    }

    @Test fun falseCountsOversizedDirectoryAndInvalidOffsetsAreRejectedBeforeZipAllocation() {
        val archive = temporary.newFile()
        ZipOutputStream(archive.outputStream()).use {
            it.putNextEntry(ZipEntry("example"))
            it.closeEntry()
        }
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
}
