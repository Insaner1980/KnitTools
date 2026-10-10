package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PatternDocumentStorageTest {
    @Test
    fun `pdf copy moves storage access to injected dispatcher`() =
        runTest {
            val context = mockk<Context>()
            val storageManager = mockk<StorageManager>()
            val sourceUri =
                mockk<Uri> {
                    every { scheme } returns "https"
                }
            val filesDir = File("files")
            val storageUuid = UUID.randomUUID()
            var storageChecked = false
            every { context.filesDir } returns filesDir
            every { context.getSystemService(StorageManager::class.java) } answers {
                storageChecked = true
                storageManager
            }
            every { storageManager.getUuidForPath(filesDir) } returns storageUuid
            every { storageManager.getAllocatableBytes(storageUuid) } returns 0L
            val storage = PatternDocumentStorage(StandardTestDispatcher(testScheduler))

            val result =
                async(start = CoroutineStart.UNDISPATCHED) {
                    storage.copyPdfToInternal(context, 7L, sourceUri, "pattern.pdf")
                }

            assertFalse(storageChecked)
            assertFalse(result.isCompleted)
            runCurrent()
            assertNull(result.await())
            assertTrue(storageChecked)
        }

    @Test
    fun `deleteProjectCaptureImages removes only project capture directory`() {
        val filesDir = Files.createTempDirectory("knittools-files").toFile()
        val targetDir = File(filesDir, "pattern_captures/7").apply { mkdirs() }
        val otherDir = File(filesDir, "pattern_captures/8").apply { mkdirs() }
        File(targetDir, "capture.jpg").writeText("target")
        File(otherDir, "capture.jpg").writeText("other")
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir

        PatternDocumentStorage(Dispatchers.Unconfined).deleteProjectCaptureImages(context, 7L)

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

        PatternDocumentStorage(Dispatchers.Unconfined).pruneStaleCaptureImages(
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
