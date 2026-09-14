package com.finnvek.knittools.data.backup

import java.io.File
import java.io.RandomAccessFile

internal object BackupZipStructure {
    fun verify(
        archive: File,
        check: () -> Unit,
    ) {
        BackupFormat.requireValid(archive.length() in 22..BackupFormat.MAX_TOTAL, BackupError.INVALID)
        RandomAccessFile(archive, "r").use { input ->
            val end = archive.length() - 22
            input.seek(end)
            BackupFormat.requireValid(input.readInt() == 0x504b0506)
            val diskNumber = input.short()
            val directoryDisk = input.short()
            BackupFormat.requireValid(diskNumber == 0 && directoryDisk == 0)
            val diskCount = input.short()
            val count = input.short()
            val size = input.uint()
            val offset = input.uint()
            BackupFormat.requireValid(diskCount == count && input.short() == 0)
            val directory =
                if (count == 0xffff || size == 0xffffffffL || offset == 0xffffffffL) {
                    zip64(input, end)
                } else {
                    Directory(count.toLong(), size, offset, end)
                }
            scan(input, directory, check)
        }
    }

    private fun zip64(
        input: RandomAccessFile,
        end: Long,
    ): Directory {
        BackupFormat.requireValid(end >= 76)
        input.seek(end - 20)
        BackupFormat.requireValid(input.readInt() == 0x504b0607 && input.uint() == 0L)
        val position = input.long()
        BackupFormat.requireValid(input.uint() == 1L && position in 0..end - 76)
        input.seek(position)
        BackupFormat.requireValid(input.readInt() == 0x504b0606)
        val recordSize = input.long()
        BackupFormat.requireValid(recordSize == 44L && position + 56 == end - 20)
        input.skipBytes(4)
        val diskNumber = input.uint()
        val directoryDisk = input.uint()
        BackupFormat.requireValid(diskNumber == 0L && directoryDisk == 0L)
        val diskCount = input.long()
        val count = input.long()
        BackupFormat.requireValid(diskCount == count)
        return Directory(count, input.long(), input.long(), position)
    }

    private fun scan(
        input: RandomAccessFile,
        directory: Directory,
        check: () -> Unit,
    ) {
        BackupFormat.requireValid(directory.count in 1..BackupFormat.MAX_ENTRIES.toLong())
        BackupFormat.requireValid(directory.size in 1..BackupFormat.MAX_ENTRIES * 512L)
        BackupFormat.requireValid(directory.offset >= 0 && directory.offset <= directory.end)
        BackupFormat.requireValid(directory.size == directory.end - directory.offset)
        input.seek(directory.offset)
        var count = 0L
        while (input.filePointer < directory.end) {
            check()
            BackupFormat.requireValid(++count <= directory.count && directory.end - input.filePointer >= 46)
            BackupFormat.requireValid(input.readInt() == 0x504b0102)
            input.skipBytes(24)
            val variableBytes = input.short() + input.short() + input.short()
            input.skipBytes(12)
            BackupFormat.requireValid(variableBytes <= directory.end - input.filePointer)
            input.seek(input.filePointer + variableBytes)
        }
        BackupFormat.requireValid(count == directory.count)
    }

    private data class Directory(
        val count: Long,
        val size: Long,
        val offset: Long,
        val end: Long,
    )

    private fun RandomAccessFile.short(): Int =
        java.lang.Short
            .reverseBytes(readShort())
            .toInt() and 0xffff

    private fun RandomAccessFile.uint(): Long = Integer.reverseBytes(readInt()).toLong() and 0xffffffffL

    private fun RandomAccessFile.long(): Long = java.lang.Long.reverseBytes(readLong())
}
