package com.finnvek.knittools.data.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class YarnPhotoStorageTest {
    @Test
    fun `bounded yarn photo copy accepts the limit`() {
        val source = byteArrayOf(1, 2, 3, 4)
        val output = ByteArrayOutputStream()

        val copied = copyYarnPhotoWithLimit(ByteArrayInputStream(source), output, maxBytes = source.size.toLong())

        assertEquals(source.size.toLong(), copied)
        assertArrayEquals(source, output.toByteArray())
    }

    @Test
    fun `bounded yarn photo copy rejects an empty provider stream`() {
        assertThrows(IOException::class.java) {
            copyYarnPhotoWithLimit(ByteArrayInputStream(byteArrayOf()), ByteArrayOutputStream())
        }
    }

    @Test
    fun `bounded yarn photo copy rejects bytes beyond the limit`() {
        val output = ByteArrayOutputStream()

        assertThrows(IOException::class.java) {
            copyYarnPhotoWithLimit(ByteArrayInputStream(ByteArray(5)), output, maxBytes = 4L)
        }
        assertEquals(0, output.size())
    }

    @Test
    fun `yarn photo copy stops before a write that would use the reserve`() {
        val input =
            object : InputStream() {
                private var nextByte = 1

                override fun read(): Int = nextByte++

                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    buffer[offset] = read().toByte()
                    return 1
                }
            }
        val output = ByteArrayOutputStream()
        val reserve = YarnPhotoStorage.YARN_PHOTO_FREE_SPACE_RESERVE_BYTES

        assertThrows(IOException::class.java) {
            copyYarnPhotoWithLimit(input, output, usableSpace = { reserve + 1 - output.size() })
        }

        assertArrayEquals(byteArrayOf(1), output.toByteArray())
    }

    @Test
    fun `interrupted yarn photo copy leaves the source and output untouched`() {
        val input = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val output = ByteArrayOutputStream()
        Thread.currentThread().interrupt()
        try {
            val error =
                assertThrows(IOException::class.java) {
                    copyYarnPhotoWithLimit(input, output)
                }
            assertEquals("Yarn photo import cancelled", error.message)
            assertEquals(3, input.available())
            assertEquals(0, output.size())
        } finally {
            Thread.interrupted()
        }
    }
}
