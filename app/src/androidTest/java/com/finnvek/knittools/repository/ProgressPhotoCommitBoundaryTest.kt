package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.storage.writeTestBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ProgressPhotoCommitBoundaryTest {
    @Test
    fun cancellationAfterCommitPreservesPublishedPhoto() = verifyCancellation(published = true)

    @Test
    fun cancellationBeforeCommitRemovesUnpublishedPhoto() = verifyCancellation(published = false)

    @Test
    fun failedReadbackPreservesPossiblyPublishedPhoto() = verifyCancellation(published = true, failReadback = true)

    private fun verifyCancellation(
        published: Boolean,
        failReadback: Boolean = false,
    ) = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        check(app.packageName.endsWith(".activepolicytest"))
        val projectId = System.nanoTime()
        val root = File(app.filesDir, "progress_photos/$projectId").apply { check(mkdirs()) }
        val database = Room.inMemoryDatabaseBuilder(app, KnitToolsDatabase::class.java).build()
        try {
            database.counterProjectDao().insert(CounterProjectEntity(id = projectId, name = "Synthetic photo owner"))
            val source = File(root, "source.jpg")
            writeTestBitmap(source, 4, Bitmap.CompressFormat.JPEG, 90)
            val realDao = database.progressPhotoDao()
            val boundary = CompletableDeferred<String>()
            val gatedDao =
                object : ProgressPhotoDao by realDao {
                    override suspend fun insert(photo: ProgressPhotoEntity): Long {
                        if (published) realDao.insert(photo)
                        boundary.complete(photo.photoUri)
                        awaitCancellation()
                    }

                    override suspend fun getAllPhotosOnce(): List<ProgressPhotoEntity> {
                        if (failReadback) throw IOException("Injected readback failure")
                        return realDao.getAllPhotosOnce()
                    }
                }
            val repository = ProgressPhotoRepository(gatedDao, ProgressPhotoStorage(), app, Dispatchers.IO)
            withTimeout(15_000) {
                val job = launch(Dispatchers.IO) { repository.savePhoto(projectId, Uri.fromFile(source), 7) }
                val uri = boundary.await()
                val copied = File(requireNotNull(Uri.parse(uri).path))
                val bytes = copied.readBytes()
                job.cancelAndJoin()
                assertTrue(job.isCancelled)
                val rows = realDao.getAllPhotosOnce()
                assertEquals(if (published) 1 else 0, rows.size)
                assertEquals(published || failReadback, copied.exists())
                if (published) {
                    assertEquals(uri, rows.single().photoUri)
                    assertEquals(projectId, rows.single().projectId)
                    assertEquals(7, rows.single().rowNumber)
                    assertArrayEquals(bytes, copied.readBytes())
                }
            }
        } finally {
            database.close()
            root.deleteRecursively()
        }
    }
}
