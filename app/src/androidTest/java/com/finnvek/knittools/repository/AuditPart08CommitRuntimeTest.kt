package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectYarnUsageEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException

/** Gates surround real SQLite transactions; no DAO result or database is mocked. */
@RunWith(Parameterized::class)
class AuditPart08CommitRuntimeTest(
    private val operation: String,
    private val phase: String,
) {
    @Test
    fun committedStateAndFilesFollowTheActualCommitBoundary() =
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Context>()
            check(app.packageName == "com.finnvek.knittools.activepolicytest")
            val owner = System.nanoTime()
            val directory = File(app.filesDir, "progress_photos/$owner").apply { check(mkdirs()) }
            val source = File(app.cacheDir, "audit08-$owner.jpg")
            val db = Room.inMemoryDatabaseBuilder(app, KnitToolsDatabase::class.java).build()
            try {
                db.counterProjectDao().insert(CounterProjectEntity(id = owner, name = "Boundary owner", count = 7))
                val projectBefore = db.counterProjectDao().getProject(owner)
                val original = File(directory, "original.jpg")
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                try {
                    source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                } finally {
                    bitmap.recycle()
                }
                source.copyTo(original)
                val originalBytes = original.readBytes()
                val photos = db.progressPhotoDao()
                val photoId =
                    photos.insert(
                        ProgressPhotoEntity(
                            projectId = owner,
                            photoUri = Uri.fromFile(original).toString(),
                            rowNumber = 7,
                            note = "Before",
                        ),
                    )
                val beforePhoto = photos.getByIds(listOf(photoId)).single()
                val usages = db.projectYarnUsageDao()
                val usageId =
                    usages.insert(
                        ProjectYarnUsageEntity(
                            projectId = owner,
                            sourceNameSnapshot = "Wool",
                            usedMeters = 5.0,
                            createdAt = 1,
                            updatedAt = 1,
                        ),
                    )
                val beforeUsage = usages.getById(usageId)
                val reached = CompletableDeferred<Unit>()
                val gate =
                    object : DatabaseTransactionRunner {
                        override suspend fun <T> run(block: suspend () -> T): T {
                            val result =
                                db.withTransaction {
                                    val value = block()
                                    if (phase == "failure") throw IOException("Actual transaction rollback")
                                    if (phase == "before") {
                                        reached.complete(Unit)
                                        awaitCancellation()
                                    }
                                    value
                                }
                            if (phase == "after") {
                                reached.complete(Unit)
                                awaitCancellation()
                            }
                            return result
                        }
                    }
                val usageRepository =
                    ProjectYarnUsageRepository(usages, db.projectYarnNoteDao(), db.yarnCardDao(), gate, Dispatchers.IO)
                val gatedPhotos =
                    object : ProgressPhotoDao by photos {
                        override suspend fun insert(photo: ProgressPhotoEntity): Long =
                            gate.run { photos.insert(photo) }

                        override suspend fun updateNote(
                            id: Long,
                            note: String?,
                        ) = gate.run { photos.updateNote(id, note) }

                        override suspend fun delete(id: Long) = gate.run { photos.delete(id) }
                    }
                val photoRepository =
                    ProgressPhotoRepository(
                        gatedPhotos,
                        ProgressPhotoStorage(),
                        app,
                        Dispatchers.IO,
                        transactionRunner = RoomDatabaseTransactionRunner(db),
                    )
                val mutate: suspend () -> Any = {
                    when (operation) {
                        "usage-update" -> usageRepository.update(owner, usageId, 1, YarnUsageAmounts(usedMeters = 9.0))
                        "usage-delete" -> usageRepository.delete(owner, usageId, 1)
                        "photo-note" -> photoRepository.updatePhotoNote(photoId, "After")
                        "photo-delete" -> photoRepository.deletePhoto(beforePhoto.toDomain())
                        "photo-save" -> photoRepository.savePhoto(owner, Uri.fromFile(source), 7)
                        else -> error(operation)
                    }
                }
                if (phase == "failure") {
                    if (operation.startsWith("usage-")) {
                        assertEquals(YarnUsageResult.PersistenceFailure, mutate())
                    } else {
                        try {
                            mutate()
                            error("Expected actual storage failure")
                        } catch (_: IOException) {
                            // The transaction executed its real DAO statement and then rolled back.
                        }
                    }
                } else {
                    val mutation = launch(Dispatchers.IO) { mutate() }
                    try {
                        withTimeout(10_000) { reached.await() }
                    } finally {
                        mutation.cancelAndJoin()
                    }
                    assertTrue(mutation.isCancelled)
                }
                assertEquals(projectBefore, db.counterProjectDao().getProject(owner))
                assertTrue(originalBytes.contentEquals(original.readBytes()))
                if (phase != "after") {
                    assertEquals(beforeUsage, usages.getById(usageId))
                    assertEquals(listOf(beforePhoto), photos.getAllPhotosOnce())
                    assertEquals(listOf(original.name), directory.listFiles()!!.map { it.name })
                } else {
                    when (operation) {
                        "usage-update" -> assertEquals(9.0, usages.getById(usageId)!!.usedMeters)
                        "usage-delete" -> assertEquals(null, usages.getById(usageId))
                        "photo-note" -> assertEquals("After", photos.getByIds(listOf(photoId)).single().note)
                        "photo-delete" -> assertTrue(photos.getByIds(listOf(photoId)).isEmpty())
                        "photo-save" -> {
                            val saved = photos.getAllPhotosOnce().single { it.id != photoId }
                            assertTrue(File(Uri.parse(saved.photoUri).path!!).isFile)
                        }
                    }
                }
            } finally {
                db.close()
                directory.deleteRecursively()
                source.delete()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<String>> =
            listOf("usage-update", "usage-delete", "photo-note", "photo-delete", "photo-save").flatMap { operation ->
                listOf("failure", "before", "after").map { phase -> arrayOf(operation, phase) }
            }
    }
}
