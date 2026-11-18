package com.finnvek.knittools.repository

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YarnCardPhotoReplacementRuntimeTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var database: KnitToolsDatabase

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        root = File(app.cacheDir, "photo-runtime-${UUID.randomUUID()}").apply { mkdirs() }
        context =
            object : ContextWrapper(app) {
                override fun getFilesDir(): File = root
            }
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
        root.deleteRecursively()
        assertFalse(root.exists())
    }

    @Test
    fun cancellationAfterRoomCommitPreservesPublishedPhoto() = verifyCancellation(published = true)

    @Test
    fun cancellationBeforeRoomCommitRemovesUnpublishedPhoto() = verifyCancellation(published = false)

    @Test
    fun failedReadbackKeepsPhotoUntilReferenceAwarePruning() =
        verifyCancellation(published = false, failReadback = true)

    @Test
    fun emptyReplacementPreservesExistingPhotoAndCard() =
        verifyRejectedReplacement(byteArrayOf(), "Yarn photo is empty")

    @Test
    fun nonImageReplacementPreservesExistingPhotoAndCard() =
        verifyRejectedReplacement(
            "RW-03I deterministic non-image source".toByteArray(Charsets.UTF_8),
            "Yarn photo is not a readable image",
        )

    @Test
    fun validJpegReplacementPublishesDecodedCopyAndRemovesOldPhoto() =
        runBlocking {
            val source = File(root, "source.jpg")
            writeTestBitmap(source, 6, Bitmap.CompressFormat.JPEG, 90)
            val sourceBytes = source.readBytes()
            val oldPhoto = File(root, "yarn_photos/1/old.jpg").apply { requireNotNull(parentFile).mkdirs() }
            val oldCard = seedExistingPhoto(oldPhoto)
            val dao = database.yarnCardDao()
            val repository = repository(dao)

            assertTrue(repository.updatePhotoUri(1, Uri.fromFile(source)))

            val updatedCard = requireNotNull(dao.getCard(1))
            assertTrue(updatedCard.photoUri != oldCard.photoUri)
            val copiedUri = Uri.parse(updatedCard.photoUri)
            assertEquals("file", copiedUri.scheme)
            val copiedPhoto = File(requireNotNull(copiedUri.path))
            assertEquals(oldPhoto.parentFile?.canonicalFile, copiedPhoto.parentFile?.canonicalFile)
            assertEquals(oldCard.copy(photoUri = updatedCard.photoUri), updatedCard)
            assertArrayEquals(sourceBytes, copiedPhoto.readBytes())
            val decoded = requireNotNull(BitmapFactory.decodeFile(copiedPhoto.path))
            try {
                assertEquals(6, decoded.width)
                assertEquals(6, decoded.height)
            } finally {
                decoded.recycle()
            }
            assertFalse(oldPhoto.exists())
            assertArrayEquals(sourceBytes, source.readBytes())
            assertEquals(setOf(source, copiedPhoto), root.walkTopDown().filter { it.isFile }.toSet())
        }

    private fun verifyRejectedReplacement(
        sourceBytes: ByteArray,
        expectedMessage: String,
    ) = runBlocking {
        val source = File(root, "source.jpg").apply { writeBytes(sourceBytes) }
        val oldPhoto = File(root, "yarn_photos/1/old.jpg").apply { requireNotNull(parentFile).mkdirs() }
        val oldCard = seedExistingPhoto(oldPhoto)
        val oldBytes = oldPhoto.readBytes()
        val dao = database.yarnCardDao()
        val repository = repository(dao)

        val failure = runCatching { repository.updatePhotoUri(1, Uri.fromFile(source)) }.exceptionOrNull()

        assertTrue("Expected image validation IOException, got $failure", failure is IOException)
        assertEquals(expectedMessage, failure?.message)
        assertEquals(oldCard, dao.getCard(1))
        assertArrayEquals(oldBytes, oldPhoto.readBytes())
        assertArrayEquals(sourceBytes, source.readBytes())
        assertEquals(setOf(source, oldPhoto), root.walkTopDown().filter { it.isFile }.toSet())
    }

    private fun verifyCancellation(
        published: Boolean,
        failReadback: Boolean = false,
    ) = runBlocking {
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
        val repository = repository(gatedDao)
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
    }

    private suspend fun seedExistingPhoto(photo: File): YarnCardEntity {
        writeTestBitmap(photo, 4, Bitmap.CompressFormat.JPEG, 90)
        val card =
            YarnCardEntity(
                id = 1,
                brand = "Synthetic brand",
                yarnName = "Runtime wool",
                fiberContent = "Wool",
                quantityInStash = 3,
                createdAt = 1234L,
                photoUri = Uri.fromFile(photo).toString(),
            )
        database.yarnCardDao().upsert(card)
        return card
    }

    private fun repository(dao: YarnCardDao): YarnCardRepository =
        YarnCardRepository(
            dao,
            database.counterProjectDao(),
            context,
            RoomDatabaseTransactionRunner(database),
            Dispatchers.IO,
        )
}
