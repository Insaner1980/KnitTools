package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectPhotoCount
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.ProgressPhoto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotoRepositoryDomainApiTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    @Test
    fun `progress photo repository exposes domain models and deletes by domain photo`() =
        runTest {
            val dao =
                FakeProgressPhotoDao(
                    progressPhotos =
                        listOf(
                            ProgressPhotoEntity(
                                id = 3L,
                                projectId = 7L,
                                photoUri = "file:///photo.jpg",
                                rowNumber = 12,
                                note = "Sleeve split",
                                createdAt = 300L,
                            ),
                        ),
                )
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            every { storage.isPhotoAvailable(context, "file:///photo.jpg") } returns true
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            val photos: List<ProgressPhoto> = repository.getAllPhotos().first()
            val projectPhotos: List<ProgressPhoto> = repository.getPhotosForProject(7L).first()
            repository.deletePhoto(photos.single())

            assertEquals("Sleeve split", photos.single().note)
            assertEquals(12, projectPhotos.single().rowNumber)
            assertEquals(3L, dao.lastDeletedId)
        }

    @Test
    fun `progress photo repository prunes unavailable photos before exposing galleries`() =
        runTest {
            val unavailableUri = "file:///missing.jpg"
            val availableUri = "file:///available.jpg"
            val dao =
                FakeProgressPhotoDao(
                    progressPhotos =
                        listOf(
                            ProgressPhotoEntity(id = 3L, projectId = 7L, photoUri = unavailableUri, rowNumber = 12),
                            ProgressPhotoEntity(id = 4L, projectId = 7L, photoUri = availableUri, rowNumber = 13),
                        ),
                )
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            every { storage.isPhotoAvailable(context, unavailableUri) } returns false
            every { storage.isPhotoAvailable(context, availableUri) } returns true
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            val allPhotos = repository.getAllPhotos().first()
            val projectPhotos = repository.getPhotosForProject(7L).first()

            assertEquals(listOf(4L), allPhotos.map { it.id })
            assertEquals(listOf(4L), projectPhotos.map { it.id })
            assertEquals(listOf(3L, 3L), dao.deletedIds)
        }

    @Test
    fun `progress photo repository loads photo counts for projects in one dao call`() =
        runTest {
            val dao =
                FakeProgressPhotoDao(
                    progressPhotos =
                        listOf(
                            ProgressPhotoEntity(id = 1L, projectId = 7L, photoUri = "file:///a.jpg", rowNumber = 1),
                            ProgressPhotoEntity(id = 2L, projectId = 7L, photoUri = "file:///b.jpg", rowNumber = 2),
                            ProgressPhotoEntity(id = 3L, projectId = 8L, photoUri = "file:///c.jpg", rowNumber = 3),
                        ),
                )
            val repository =
                ProgressPhotoRepository(
                    dao,
                    mockk(relaxed = true),
                    context,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val counts = repository.getPhotoCountsByProjectIds(listOf(7L, 8L, 7L, 9L))

            assertEquals(mapOf(7L to 2, 8L to 1), counts)
            assertEquals(listOf(7L, 8L, 9L), dao.lastCountProjectIds)
        }

    @Test
    fun `progress photo repository saves compressed photo and removes temporary source`() =
        runTest {
            val dao = FakeProgressPhotoDao()
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val targetFile =
                Files
                    .createTempFile("knittools-progress", ".jpg")
                    .toFile()
            val targetUriString = "file:///saved-progress.jpg"
            val sourceUri = mockk<Uri>()
            every { storage.createPhotoFile(context, 7L) } returns (targetFile to mockk())
            every { storage.compressAndSave(context, sourceUri, targetFile) } returns true
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            val savedId =
                withUriFromFile(targetFile, targetUriString) {
                    repository.savePhoto(7L, sourceUri, rowNumber = 42, note = "a".repeat(120))
                }

            assertEquals(66L, savedId)
            assertEquals(7L, dao.lastInserted?.projectId)
            assertEquals(targetUriString, dao.lastInserted?.photoUri)
            assertEquals(42, dao.lastInserted?.rowNumber)
            assertEquals(100, dao.lastInserted?.note?.length)
            verify { storage.deleteTemporarySource(context, sourceUri) }
        }

    @Test
    fun `progress photo repository deletes target file when compression fails`() =
        runTest {
            val dao = FakeProgressPhotoDao()
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val targetFile =
                Files
                    .createTempFile("knittools-progress", ".jpg")
                    .toFile()
            val targetUriString = "file:///failed-progress.jpg"
            val sourceUri = mockk<Uri>()
            every { storage.createPhotoFile(context, 7L) } returns (targetFile to mockk())
            every { storage.compressAndSave(context, sourceUri, targetFile) } returns false
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            val savedId =
                withUriFromFile(targetFile, targetUriString) {
                    repository.savePhoto(7L, sourceUri, rowNumber = 42)
                }

            assertEquals(0L, savedId)
            assertEquals(0, dao.insertCount)
            verify { storage.deletePhoto(targetUriString) }
            verify { storage.deleteTemporarySource(context, sourceUri) }
        }

    @Test
    fun `progress photo repository deletes selected photos by id`() =
        runTest {
            val dao =
                FakeProgressPhotoDao(
                    progressPhotos =
                        listOf(
                            ProgressPhotoEntity(id = 3L, projectId = 7L, photoUri = "file:///first.jpg", rowNumber = 1),
                            ProgressPhotoEntity(
                                id = 4L,
                                projectId = 7L,
                                photoUri = "file:///second.jpg",
                                rowNumber = 2,
                            ),
                        ),
                )
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            repository.deletePhotos(listOf(3L, 4L))

            assertEquals(listOf(3L, 4L), dao.deletedIds)
            verify { storage.deletePhoto("file:///first.jpg") }
            verify { storage.deletePhoto("file:///second.jpg") }
        }

    @Test
    fun `progress photo repository normalizes blank note updates`() =
        runTest {
            val dao = FakeProgressPhotoDao()
            val repository =
                ProgressPhotoRepository(
                    dao,
                    mockk(relaxed = true),
                    context,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.updatePhotoNote(3L, "   ")
            repository.updatePhotoNote(4L, "a".repeat(120))

            assertEquals(mapOf(3L to null, 4L to "a".repeat(100)), dao.updatedNotes)
        }

    private class FakeProgressPhotoDao(
        private val progressPhotos: List<ProgressPhotoEntity> = emptyList(),
    ) : ProgressPhotoDao {
        var lastInserted: ProgressPhotoEntity? = null
        var insertCount: Int = 0
        var lastDeletedId: Long? = null
        val deletedIds = mutableListOf<Long>()
        var lastCountProjectIds: List<Long> = emptyList()
        val updatedNotes = linkedMapOf<Long, String?>()

        override fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhotoEntity>> =
            flowOf(progressPhotos.filter { it.projectId == projectId })

        override fun getLatestPhotos(
            projectId: Long,
            limit: Int,
        ): Flow<List<ProgressPhotoEntity>> = flowOf(progressPhotos.filter { it.projectId == projectId }.take(limit))

        override fun getPhotoCount(projectId: Long): Flow<Int> =
            flowOf(progressPhotos.count { it.projectId == projectId })

        override fun getAllPhotos(): Flow<List<ProgressPhotoEntity>> = flowOf(progressPhotos)

        override fun getAllPhotoCount(): Flow<Int> = flowOf(progressPhotos.size)

        override suspend fun getPhotoCountsByProjectIds(projectIds: List<Long>): List<ProjectPhotoCount> {
            lastCountProjectIds = projectIds
            return progressPhotos
                .filter { it.projectId in projectIds }
                .groupingBy { it.projectId }
                .eachCount()
                .map { (projectId, count) -> ProjectPhotoCount(projectId, count) }
        }

        override suspend fun insert(photo: ProgressPhotoEntity): Long {
            insertCount += 1
            lastInserted = photo
            return 66L
        }

        override suspend fun updateNote(
            id: Long,
            note: String?,
        ) {
            updatedNotes[id] = note
        }

        override suspend fun delete(id: Long) {
            lastDeletedId = id
            deletedIds += id
        }

        override suspend fun deleteByIds(ids: List<Long>) = Unit

        override suspend fun getByIds(ids: List<Long>): List<ProgressPhotoEntity> =
            progressPhotos.filter { it.id in ids }
    }

    private suspend inline fun <T> withUriFromFile(
        file: File,
        uriString: String,
        block: suspend () -> T,
    ): T {
        mockkStatic(Uri::class)
        val uri = mockk<Uri>()
        every { Uri.fromFile(file) } returns uri
        every { uri.toString() } returns uriString
        return try {
            block()
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}
