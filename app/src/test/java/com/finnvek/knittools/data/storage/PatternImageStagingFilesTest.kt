package com.finnvek.knittools.data.storage

import com.finnvek.knittools.ui.screens.pattern.PatternImageImportLimits
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class PatternImageStagingFilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

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
    fun `bounded copy keeps the free space reserve before each write`() {
        val target = spyk(targetFile())
        every { target.usableSpace } answers {
            PatternImageImportLimits.MIN_FREE_SPACE_RESERVE_BYTES + 8_192L - target.length()
        }

        assertThrows(IOException::class.java) {
            PatternImageStagingFiles.copyBounded(ByteArrayInputStream(ByteArray(8_193)), target, 8_193L)
        }

        assertFalse(target.exists())
    }

    @Test
    fun `bounded copy accepts bytes that leave exactly the reserve`() {
        val target = spyk(targetFile())
        every { target.usableSpace } answers {
            PatternImageImportLimits.MIN_FREE_SPACE_RESERVE_BYTES + 3L - target.length()
        }

        assertEquals(3L, PatternImageStagingFiles.copyBounded(ByteArrayInputStream(ByteArray(3)), target, 3L))
        assertEquals(3L, target.length())
    }

    @Test
    fun `session directory uses project and collision safe session ownership`() {
        val filesDir = temporaryFolder.newFolder()

        val first = PatternImageStagingFiles.sessionDirectory(filesDir, 7L, "session-a")
        val second = PatternImageStagingFiles.sessionDirectory(filesDir, 7L, "session-b")

        assertTrue(first.path.endsWith("pattern_captures${File.separator}7${File.separator}session-a"))
        assertTrue(second.path.endsWith("pattern_captures${File.separator}7${File.separator}session-b"))
        assertFalse(first == second)
    }

    private fun targetFile(): File = File(temporaryFolder.newFolder(), "page.img")
}
