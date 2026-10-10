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

    @Test
    fun `pruneStaleCaptureImages removes old captures while keeping recent pending capture`() {
        val filesDir = Files.createTempDirectory("knittools-files").toFile()
        val targetDir = File(filesDir, "pattern_captures/7").apply { mkdirs() }
        val oldCapture = File(targetDir, "old.jpg").apply { writeText("old") }
        val recentCapture = File(targetDir, "recent.jpg").apply { writeText("recent") }
        val unknownAgeCapture = File(targetDir, "unknown.jpg").apply { writeText("unknown") }
        val now = 10L * ONE_DAY_MILLIS
        oldCapture.setLastModified(now - TWO_DAYS_MILLIS)
        recentCapture.setLastModified(now - ONE_HOUR_MILLIS)
        unknownAgeCapture.setLastModified(0L)
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir

        PatternDocumentStorage().pruneStaleCaptureImages(
            context = context,
            nowMillis = now,
            maxAgeMillis = ONE_DAY_MILLIS,
        )

        assertFalse(oldCapture.exists())
        assertTrue(recentCapture.exists())
        assertTrue(unknownAgeCapture.exists())
        assertTrue(targetDir.exists())
    }

    private companion object {
        const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
        const val ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS
        const val TWO_DAYS_MILLIS = 2L * ONE_DAY_MILLIS
    }
}
