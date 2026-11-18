package com.finnvek.knittools.repository

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(Parameterized::class)
class AuditPart07PhotoBoundaryTest(
    private val prompt: Int,
) {
    @Test
    fun authoredPhotoBoundary() =
        runBlocking {
            check(ApplicationProvider.getApplicationContext<Context>().packageName.endsWith(".activepolicytest"))
            val fixture = PhotoFixture()
            try {
                fixture.seed()
                withTimeout(20_000) {
                    when (prompt) {
                        1079 -> fixture.pruneDuringReplacement()
                        1080 -> fixture.failedPublication()
                        1081 -> fixture.racingReplacements()
                        else -> error("Unmapped prompt $prompt")
                    }
                }
                fixture.assertUnrelatedState()
            } finally {
                fixture.close()
            }
        }

    private class PhotoFixture {
        private val app = ApplicationProvider.getApplicationContext<Context>()
        private val root = File(app.cacheDir, "photo-stress-${UUID.randomUUID()}").apply { mkdirs() }
        private val context =
            object : ContextWrapper(app) {
                override fun getFilesDir(): File = root
            }
        private val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        private val dao = database.yarnCardDao()
        private val old = File(root, "yarn_photos/1/old.jpg")
        private val other = File(root, "yarn_photos/2/other.jpg")
        private val source = File(root, "source.jpg")
        private val secondSource = File(root, "second.jpg")
        private val before = YarnCardEntity(id = 1, yarnName = "Wool", photoUri = uri(old), quantityInStash = 9)
        private val otherBefore = YarnCardEntity(id = 2, yarnName = "Other", photoUri = uri(other))
        private val projectBefore = CounterProjectEntity(id = 1, name = "Untouched", count = 42)
        private val reached = CompletableDeferred<String>()
        private val release = CompletableDeferred<Unit>()
        private val blockFirst = AtomicBoolean(true)
        private val gatedDao =
            object : YarnCardDao by dao {
                override suspend fun updatePhotoUri(
                    id: Long,
                    photoUri: String,
                ): Int {
                    if (blockFirst.compareAndSet(true, false)) {
                        reached.complete(photoUri)
                        release.await()
                    }
                    return dao.updatePhotoUri(id, photoUri)
                }
            }
        private val repository =
            YarnCardRepository(
                gatedDao,
                database.counterProjectDao(),
                context,
                RoomDatabaseTransactionRunner(database),
                Dispatchers.IO,
            )

        suspend fun seed() {
            writeJpeg(source, Color.RED)
            writeJpeg(secondSource, Color.BLUE)
            writeJpeg(old, Color.GREEN)
            writeJpeg(other, Color.YELLOW)
            dao.upsert(before)
            dao.upsert(otherBefore)
            database.counterProjectDao().insert(projectBefore)
        }

        suspend fun pruneDuringReplacement() =
            coroutineScope {
                val pending = async(Dispatchers.IO) { repository.updatePhotoUri(1, Uri.fromFile(source)) }
                val copied = File(requireNotNull(Uri.parse(reached.await()).path))
                assertArrayEquals(source.readBytes(), copied.readBytes())
                assertEquals(before, dao.getCard(1))
                assertTrue(old.exists())
                val prune = async(start = CoroutineStart.UNDISPATCHED) { repository.pruneUnreferencedPhotoFiles() }
                assertFalse("Prune must wait for unpublished copy", prune.isCompleted)
                assertTrue(copied.exists())
                release.complete(Unit)
                assertTrue(pending.await())
                prune.await()
                assertEquals(before.copy(photoUri = uri(copied)), dao.getCard(1))
                assertTrue(copied.exists())
                assertFalse(old.exists())
            }

        suspend fun failedPublication() {
            blockFirst.set(false)
            val sqlite = database.openHelper.writableDatabase
            sqlite.execSQL(
                "CREATE TEMP TRIGGER photo_failure BEFORE UPDATE OF photoUri ON yarn_cards " +
                    "BEGIN SELECT RAISE(ABORT, 'photo-precommit-failure'); END",
            )
            try {
                val failure = runCatching { repository.updatePhotoUri(1, Uri.fromFile(source)) }.exceptionOrNull()
                assertTrue(
                    "Owning SQLite failure must propagate",
                    failure?.message?.contains("photo-precommit-failure") == true,
                )
                assertEquals(before, dao.getCard(1))
                assertEquals(listOf(old.name), old.parentFile?.listFiles()?.map(File::getName))
                assertTrue(old.exists())
            } finally {
                sqlite.execSQL("DROP TRIGGER photo_failure")
            }
            val copyFailure =
                runCatching { repository.updatePhotoUri(1, Uri.fromFile(File(root, "missing-source.jpg"))) }
            assertTrue(copyFailure.isFailure)
            assertEquals(before, dao.getCard(1))
            assertEquals(listOf(old.name), old.parentFile?.listFiles()?.map(File::getName))
        }

        suspend fun racingReplacements() =
            coroutineScope {
                val first = async(Dispatchers.IO) { repository.updatePhotoUri(1, Uri.fromFile(source)) }
                val firstUri = reached.await()
                val firstCopy = File(requireNotNull(Uri.parse(firstUri).path))
                assertArrayEquals(source.readBytes(), firstCopy.readBytes())
                val second =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        repository.updatePhotoUri(1, Uri.fromFile(secondSource))
                    }
                assertFalse("Second replacement must wait for first URI swap", second.isCompleted)
                assertEquals(before, dao.getCard(1))
                release.complete(Unit)
                assertTrue(first.await())
                assertTrue(second.await())
                val finalCard = requireNotNull(dao.getCard(1))
                assertNotEquals(firstUri, finalCard.photoUri)
                assertEquals(before.copy(photoUri = finalCard.photoUri), finalCard)
                assertArrayEquals(
                    secondSource.readBytes(),
                    File(requireNotNull(Uri.parse(finalCard.photoUri).path)).readBytes(),
                )
                assertFalse(firstCopy.exists())
                assertFalse(old.exists())
                assertEquals(1, old.parentFile?.listFiles()?.size)
            }

        suspend fun assertUnrelatedState() {
            assertEquals(otherBefore, dao.getCard(2))
            assertEquals(projectBefore, database.counterProjectDao().getProject(1))
            assertTrue(other.exists())
            assertTrue(source.exists())
            assertTrue(secondSource.exists())
        }

        fun close() {
            database.close()
            root.deleteRecursively()
            assertFalse(root.exists())
        }

        private fun writeJpeg(
            file: File,
            color: Int,
        ) {
            requireNotNull(file.parentFile).mkdirs()
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(color)
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            } finally {
                bitmap.recycle()
            }
        }

        private fun uri(file: File): String = Uri.fromFile(file).toString()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = (1079..1081).map { arrayOf(it) }
    }
}
