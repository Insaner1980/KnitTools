package com.finnvek.knittools.data.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PatternDocumentStorageTest {
    @Test
    fun `deleteProjectCaptureImages removes only project capture directory`() {
        val filesDir = Files.createTempDirectory("knittools-files").toFile()
        val targetDir = File(filesDir, "pattern_captures/7").apply { mkdirs() }
        val otherDir = File(filesDir, "pattern_captures/8").apply { mkdirs() }
        File(targetDir, "capture.jpg").writeText("target")
        File(otherDir, "capture.jpg").writeText("other")
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir

        PatternDocumentStorage().deleteProjectCaptureImages(context, 7L)

        assertFalse(targetDir.exists())
        assertTrue(otherDir.exists())
    }
}
