package com.finnvek.knittools.data.backup

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry

internal object BackupZipStructure {
    private const val ZIP64_SENTINEL = 0xffffffffL
    private const val DATA_DESCRIPTOR = 0x0008
    private const val UTF8 = 0x0800
    private const val DEFLATE_OPTIONS = 0x0006

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
                if (count == 0xffff || size == ZIP64_SENTINEL || offset == ZIP64_SENTINEL) {
                    val extended = zip64(input, end)
                    BackupFormat.requireValid(
                        (count == 0xffff || count.toLong() == extended.count) &&
                            (size == ZIP64_SENTINEL || size == extended.size) &&
                            (offset == ZIP64_SENTINEL || offset == extended.offset),
                    )
                    extended
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
        BackupFormat.requireValid(directory.offset in 0..directory.end)
        BackupFormat.requireValid(directory.size == directory.end - directory.offset)
        input.seek(directory.offset)
        val members = mutableListOf<Member>()
        var count = 0L
        while (input.filePointer < directory.end) {
            check()
            BackupFormat.requireValid(++count <= directory.count && directory.end - input.filePointer >= 46)
            BackupFormat.requireValid(input.readInt() == 0x504b0102)
            input.skipBytes(4)
            val flags = input.short()
            val method = input.short()
            val permittedFlags = permittedFlags(method)
            BackupFormat.requireValid(flags and permittedFlags == flags)
            input.skipBytes(4)
            val crc = input.uint()
            var compressedSize = input.uint()
            var uncompressedSize = input.uint()
            val nameLength = input.short()
            val extraLength = input.short()
            val commentLength = input.short()
            val variableBytes = nameLength + extraLength + commentLength
            BackupFormat.requireValid(input.short() == 0)
            input.skipBytes(6)
            var localOffset = input.uint()
            BackupFormat.requireValid(variableBytes <= directory.end - input.filePointer)
            val nextEntry = input.filePointer + variableBytes
            val name = ByteArray(nameLength).also(input::readFully)
            val uncompressedZip64 = uncompressedSize == ZIP64_SENTINEL
            val compressedZip64 = compressedSize == ZIP64_SENTINEL
            val offsetZip64 = localOffset == ZIP64_SENTINEL
            if (uncompressedZip64 || compressedZip64 || offsetZip64) {
                val fieldCount = listOf(uncompressedZip64, compressedZip64, offsetZip64).count { it }
                input.seekZip64Extra(extraLength, fieldCount * 8)
                if (uncompressedZip64) uncompressedSize = input.long()
                if (compressedZip64) compressedSize = input.long()
                if (offsetZip64) localOffset = input.long()
            }
            BackupFormat.requireValid(localOffset in 0..directory.offset - 30L)
            BackupFormat.requireValid(compressedSize in 0..directory.offset)
            BackupFormat.requireValid(uncompressedSize in 0..BackupFormat.MAX_TOTAL)
            BackupFormat.requireValid(method != ZipEntry.STORED || compressedSize == uncompressedSize)
            val member = Member(localOffset, name, flags, method, crc, compressedSize, uncompressedSize)
            members.add(member)
            input.seek(nextEntry)
        }
        BackupFormat.requireValid(count == directory.count)
        val ordered = members.sortedBy { it.offset }
        BackupFormat.requireValid(ordered.first().offset == 0L)
        ordered.forEachIndexed { index, member ->
            check()
            verifyLocalMember(input, member, ordered.getOrNull(index + 1)?.offset ?: directory.offset)
        }
    }

    private fun permittedFlags(method: Int): Int =
        when (method) {
            ZipEntry.STORED -> UTF8
            ZipEntry.DEFLATED -> UTF8 or DATA_DESCRIPTOR or DEFLATE_OPTIONS
            else -> throw BackupException(BackupError.CORRUPT)
        }

    private fun RandomAccessFile.seekZip64Extra(
        extraLength: Int,
        requiredBytes: Int,
    ) {
        val end = filePointer + extraLength
        while (end - filePointer >= 4) {
            val tag = short()
            val size = short()
            BackupFormat.requireValid(size <= end - filePointer)
            if (tag == 1) {
                BackupFormat.requireValid(size >= requiredBytes)
                return
            }
            skipBytes(size)
        }
        throw BackupException(BackupError.CORRUPT)
    }

    private fun verifyLocalMember(
        input: RandomAccessFile,
        member: Member,
        nextOffset: Long,
    ) {
        BackupFormat.requireValid(member.offset in 0..nextOffset - 30L)
        input.seek(member.offset)
        BackupFormat.requireValid(input.readInt() == 0x504b0304)
        input.skipBytes(2)
        BackupFormat.requireValid(input.short() == member.flags && input.short() == member.method)
        input.skipBytes(4)
        val crc = input.uint()
        var compressedSize = input.uint()
        var uncompressedSize = input.uint()
        val nameLength = input.short()
        val extraLength = input.short()
        BackupFormat.requireValid(nameLength == member.name.size)
        BackupFormat.requireValid(nameLength + extraLength <= nextOffset - input.filePointer)
        val actual = ByteArray(nameLength).also(input::readFully)
        BackupFormat.requireValid(actual.contentEquals(member.name))
        val dataOffset = input.filePointer + extraLength
        val uncompressedZip64 = uncompressedSize == ZIP64_SENTINEL
        val compressedZip64 = compressedSize == ZIP64_SENTINEL
        if (uncompressedZip64 || compressedZip64) {
            input.seekZip64Extra(extraLength, if (uncompressedZip64 && compressedZip64) 16 else 8)
            if (uncompressedZip64) uncompressedSize = input.long()
            if (compressedZip64) compressedSize = input.long()
        }
        val hasDescriptor = member.flags and DATA_DESCRIPTOR != 0
        verifyLocalMetadata(member, crc, compressedSize, uncompressedSize, hasDescriptor)
        BackupFormat.requireValid(member.compressedSize <= nextOffset - dataOffset)
        val payloadEnd = dataOffset + member.compressedSize
        if (hasDescriptor) {
            verifyDescriptor(input, member, payloadEnd, nextOffset - payloadEnd)
        } else {
            BackupFormat.requireValid(payloadEnd == nextOffset)
        }
    }

    private fun verifyLocalMetadata(
        member: Member,
        crc: Long,
        compressedSize: Long,
        uncompressedSize: Long,
        hasDescriptor: Boolean,
    ) {
        BackupFormat.requireValid(crc == member.crc || (hasDescriptor && crc == 0L))
        BackupFormat.requireValid(compressedSize == member.compressedSize || (hasDescriptor && compressedSize == 0L))
        BackupFormat.requireValid(
            uncompressedSize == member.uncompressedSize || (hasDescriptor && uncompressedSize == 0L),
        )
    }

    private fun verifyDescriptor(
        input: RandomAccessFile,
        member: Member,
        offset: Long,
        length: Long,
    ) {
        BackupFormat.requireValid(length in setOf(12L, 16L, 20L, 24L))
        input.seek(offset)
        if (length == 16L || length == 24L) BackupFormat.requireValid(input.readInt() == 0x504b0708)
        BackupFormat.requireValid(input.uint() == member.crc)
        val zip64 = length >= 20L
        val compressedSize = if (zip64) input.long() else input.uint()
        val uncompressedSize = if (zip64) input.long() else input.uint()
        BackupFormat.requireValid(
            compressedSize == member.compressedSize && uncompressedSize == member.uncompressedSize,
        )
    }

    private data class Member(
        val offset: Long,
        val name: ByteArray,
        val flags: Int,
        val method: Int,
        val crc: Long,
        val compressedSize: Long,
        val uncompressedSize: Long,
    )

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
