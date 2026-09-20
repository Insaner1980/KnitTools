package com.finnvek.knittools.data.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

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
    fun `bounded yarn photo copy rejects bytes beyond the limit`() {
        val output = ByteArrayOutputStream()

        assertThrows(IOException::class.java) {
            copyYarnPhotoWithLimit(ByteArrayInputStream(ByteArray(5)), output, maxBytes = 4L)
        }
        assertEquals(0, output.size())
    }
}
