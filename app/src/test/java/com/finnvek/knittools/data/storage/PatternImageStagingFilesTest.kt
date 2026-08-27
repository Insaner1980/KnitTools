package com.finnvek.knittools.data.storage

import com.finnvek.knittools.ui.screens.pattern.PatternImageImportLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

class PatternImageStagingFilesTest {
    @Test
    fun `bounded copy writes accepted bytes once`() {
        val target = targetFile()

        val copied = PatternImageStagingFiles.copyBounded(ByteArrayInputStream(byteArrayOf(1, 2, 3)), target, 3L)

        assertEquals(3L, copied)
        assertEquals(listOf<Byte>(1, 2, 3), target.readBytes().toList())
    }

    @Test
    fun `bounded copy removes partial target when byte limit is exceeded`() {
        val target = targetFile()

        assertThrows(PatternImageTooLargeException::class.java) {
            PatternImageStagingFiles.copyBounded(
                ByteArrayInputStream(ByteArray(4)),
                target,
                3L,
            )
        }

        assertFalse(target.exists())
    }

    @Test
    fun `bounded copy removes partial target when provider read fails`() {
        val target = targetFile()
        val input =
            object : InputStream() {
                private var reads = 0

                override fun read(): Int {
                    if (reads++ == 0) return 1
                    throw IOException("provider failed")
                }
            }

        assertThrows(IOException::class.java) {
            PatternImageStagingFiles.copyBounded(
                input,
                target,
                PatternImageImportLimits.MAX_BYTES_PER_IMAGE,
            )
        }

        assertFalse(target.exists())
    }

    @Test
    fun `session directory uses project and collision safe session ownership`() {
        val filesDir = Files.createTempDirectory("knittools-files").toFile()

        val first = PatternImageStagingFiles.sessionDirectory(filesDir, 7L, "session-a")
        val second = PatternImageStagingFiles.sessionDirectory(filesDir, 7L, "session-b")

        assertTrue(first.path.endsWith("pattern_captures${File.separator}7${File.separator}session-a"))
        assertTrue(second.path.endsWith("pattern_captures${File.separator}7${File.separator}session-b"))
        assertFalse(first == second)
    }

    private fun targetFile(): File = File(Files.createTempDirectory("knittools-stage").toFile(), "page.img")
}
