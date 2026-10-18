package com.finnvek.knittools.repository

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YarnCardPhotoReplacementRuntimeTest {
    @Test
    fun cancellationAfterRoomCommitPreservesPublishedPhoto() = verifyCancellation(published = true)

    @Test
    fun cancellationBeforeRoomCommitRemovesUnpublishedPhoto() = verifyCancellation(published = false)

    @Test
    fun failedReadbackKeepsPhotoUntilReferenceAwarePruning() =
        verifyCancellation(published = false, failReadback = true)

    private fun verifyCancellation(
        published: Boolean,
        failReadback: Boolean = false,
    ) = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = File(app.cacheDir, "photo-runtime-${UUID.randomUUID()}").apply { mkdirs() }
        val context =
            object : ContextWrapper(app) {
                override fun getFilesDir(): File = root
            }
        val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        try {
            val source = File(root, "source.jpg")
            writeTestBitmap(source, 4, Bitmap.CompressFormat.JPEG, 90)
            val oldPhoto = File(root, "yarn_photos/1/old.jpg").apply { requireNotNull(parentFile).mkdirs() }
            source.copyTo(oldPhoto)
            val oldUri = Uri.fromFile(oldPhoto).toString()
            val realDao = database.yarnCardDao()
            realDao.upsert(YarnCardEntity(id = 1, yarnName = "Runtime wool", photoUri = oldUri))
            val boundary = CompletableDeferred<String>()
            val gatedDao =
                object : YarnCardDao by realDao {
                    override suspend fun updatePhotoUri(
                        id: Long,
                        photoUri: String,
                    ): Int {
                        if (published) realDao.updatePhotoUri(id, photoUri)
                        boundary.complete(photoUri)
                        awaitCancellation()
                    }

                    override suspend fun getCard(id: Long): YarnCardEntity? {
                        if (failReadback && boundary.isCompleted) throw IOException("Injected readback failure")
                        return realDao.getCard(id)
                    }
                }
            val repository =
                YarnCardRepository(
                    gatedDao,
                    database.counterProjectDao(),
                    context,
                    RoomDatabaseTransactionRunner(database),
                    Dispatchers.IO,
                )
            withTimeout(15_000) {
                val job = launch(Dispatchers.IO) { repository.updatePhotoUri(1, Uri.fromFile(source)) }
                val copiedUri = boundary.await()
                job.cancelAndJoin()
                assertTrue(job.isCancelled)
                val copiedPhoto = File(requireNotNull(Uri.parse(copiedUri).path))
                assertEquals(if (published) copiedUri else oldUri, realDao.getCard(1)?.photoUri)
                assertEquals(published || failReadback, copiedPhoto.exists())
                assertTrue(oldPhoto.exists())
                if (copiedPhoto.exists()) assertArrayEquals(source.readBytes(), copiedPhoto.readBytes())

                repository.pruneUnreferencedPhotoFiles()
                assertEquals(published, copiedPhoto.exists())
                assertEquals(!published, oldPhoto.exists())
                assertTrue(source.exists())
            }
        } finally {
            database.close()
            root.deleteRecursively()
            assertFalse(root.exists())
        }
    }
}
