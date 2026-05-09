package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RepositoryDomainApiTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(R.string.imported_pattern_designer) } returns "Imported"
    }

    @Test
    fun `saved pattern repository exposes domain models and writes entities`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 1L,
                                ravelryId = 123,
                                name = "Cardigan",
                                designerName = "Designer",
                                patternUrl = "https://example.com/cardigan",
                                savedAt = 100L,
                            ),
                        ),
                )
            val repository = SavedPatternRepository(dao, context)

            val patterns: List<SavedPattern> = repository.getAll().first()
            val byId: SavedPattern? = repository.getById(1L)
            val savedId =
                repository.save(
                    SavedPattern(
                        ravelryId = 456,
                        name = "Hat",
                        designerName = "Maker",
                        patternUrl = "content://hat",
                        savedAt = 200L,
                    ),
                )

            assertEquals("Cardigan", patterns.single().name)
            assertEquals("Cardigan", byId?.name)
            assertEquals(99L, savedId)
            assertEquals(
                SavedPatternEntity(
                    ravelryId = 456,
                    name = "Hat",
                    designerName = "Maker",
                    patternUrl = "content://hat",
                    savedAt = 200L,
                ),
                dao.lastInserted,
            )
        }

    @Test
    fun `saved pattern repository creates imported pattern as domain id without exposing entity`() =
        runTest {
            val dao = FakeSavedPatternDao()
            val repository = SavedPatternRepository(dao, context)

            val importedId = repository.saveImportedPatternIfMissing("content://pattern", "Local pattern")
            val ignoredId = repository.saveImportedPatternIfMissing("https://example.com/pattern", "Remote pattern")

            assertEquals(99L, importedId)
            assertNull(ignoredId)
            assertEquals("Imported", dao.lastInserted?.designerName)
        }

    @Test
    fun `yarn card repository exposes domain models and writes entities`() =
        runTest {
            val dao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(
                                id = 2L,
                                brand = "Finn Wool",
                                yarnName = "Soft DK",
                                quantityInStash = 3,
                                status = "IN_USE",
                            ),
                        ),
                )
            val repository = YarnCardRepository(dao)

            val cards: List<YarnCard> = repository.getAllCards().first()
            val card: YarnCard? = repository.getCard(2L)
            val savedId =
                repository.saveCard(
                    YarnCard(
                        brand = "Novita",
                        yarnName = "Nalle",
                        createdAt = 123L,
                        quantityInStash = 5,
                        status = "IN_STASH",
                    ),
                )

            assertEquals("Soft DK", cards.single().yarnName)
            assertEquals("Finn Wool", card?.brand)
            assertEquals(88L, savedId)
            assertEquals(
                YarnCardEntity(
                    brand = "Novita",
                    yarnName = "Nalle",
                    createdAt = 123L,
                    quantityInStash = 5,
                    status = "IN_STASH",
                ),
                dao.lastUpserted,
            )
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
            val repository = ProgressPhotoRepository(dao, storage, context)

            val photos: List<ProgressPhoto> = repository.getAllPhotos().first()
            val projectPhotos: List<ProgressPhoto> = repository.getPhotosForProject(7L).first()
            repository.deletePhoto(photos.single())

            assertEquals("Sleeve split", photos.single().note)
            assertEquals(12, projectPhotos.single().rowNumber)
            assertEquals(3L, dao.lastDeletedId)
        }

    @Test
    fun `pattern annotation repository exposes domain models`() =
        runTest {
            val dao =
                FakePatternAnnotationDao(
                    patternAnnotations =
                        listOf(
                            PatternAnnotationEntity(
                                id = 4L,
                                projectId = 7L,
                                page = 2,
                                pathData = "M 0 0 L 1 1",
                                color = "#123456",
                                strokeWidth = 3f,
                                createdAt = 400L,
                            ),
                        ),
                )
            val repository = PatternAnnotationRepository(dao)

            val annotations: List<PatternAnnotation> = repository.getAnnotationsForPage(7L, 2).first()
            val addedId = repository.addAnnotation(7L, 2, "M 1 1 L 2 2", "#654321", 4f)

            assertEquals("#123456", annotations.single().color)
            assertEquals(77L, addedId)
            assertEquals("M 1 1 L 2 2", dao.lastInserted?.pathData)
        }

    private class FakeSavedPatternDao(
        private val savedPatterns: List<SavedPatternEntity> = emptyList(),
    ) : SavedPatternDao {
        var lastInserted: SavedPatternEntity? = null

        override fun getAll(): Flow<List<SavedPatternEntity>> = flowOf(savedPatterns)

        override suspend fun getById(id: Long): SavedPatternEntity? = savedPatterns.firstOrNull { it.id == id }

        override suspend fun getByRavelryId(ravelryId: Int): SavedPatternEntity? =
            savedPatterns.firstOrNull { it.ravelryId == ravelryId }

        override suspend fun getByPatternUrl(patternUrl: String): SavedPatternEntity? =
            savedPatterns.firstOrNull { it.patternUrl == patternUrl }

        override suspend fun insert(pattern: SavedPatternEntity): Long {
            lastInserted = pattern
            return 99L
        }

        override suspend fun deleteById(id: Long) = Unit

        override suspend fun deleteByIds(ids: List<Long>) = Unit

        override fun getCount(): Flow<Int> = flowOf(savedPatterns.size)
    }

    private class FakeYarnCardDao(
        private val yarnCards: List<YarnCardEntity> = emptyList(),
    ) : YarnCardDao {
        var lastUpserted: YarnCardEntity? = null

        override fun getAllCards(): Flow<List<YarnCardEntity>> = flowOf(yarnCards)

        override suspend fun getCard(id: Long): YarnCardEntity? = yarnCards.firstOrNull { it.id == id }

        override suspend fun getCards(ids: List<Long>): List<YarnCardEntity> = yarnCards.filter { it.id in ids }

        override suspend fun upsert(card: YarnCardEntity): Long {
            lastUpserted = card
            return 88L
        }

        override fun getCardCount(): Flow<Int> = flowOf(yarnCards.size)

        override suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ) = Unit

        override suspend fun updateStatus(
            id: Long,
            status: String,
        ) = Unit

        override suspend fun updateLinkedProjectId(
            id: Long,
            projectId: Long?,
        ) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun deleteByIds(ids: List<Long>) = Unit
    }

    private class FakeProgressPhotoDao(
        private val progressPhotos: List<ProgressPhotoEntity> = emptyList(),
    ) : ProgressPhotoDao {
        var lastDeletedId: Long? = null

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

        override suspend fun insert(photo: ProgressPhotoEntity): Long = 66L

        override suspend fun updateNote(
            id: Long,
            note: String?,
        ) = Unit

        override suspend fun delete(id: Long) {
            lastDeletedId = id
        }

        override suspend fun deleteByIds(ids: List<Long>) = Unit

        override suspend fun getByIds(ids: List<Long>): List<ProgressPhotoEntity> =
            progressPhotos.filter { it.id in ids }
    }

    private class FakePatternAnnotationDao(
        private val patternAnnotations: List<PatternAnnotationEntity> = emptyList(),
    ) : PatternAnnotationDao {
        var lastInserted: PatternAnnotationEntity? = null

        override fun getAnnotationsForPage(
            projectId: Long,
            page: Int,
        ): Flow<List<PatternAnnotationEntity>> =
            flowOf(patternAnnotations.filter { it.projectId == projectId && it.page == page })

        override suspend fun insert(annotation: PatternAnnotationEntity): Long {
            lastInserted = annotation
            return 77L
        }

        override suspend fun deleteForProject(projectId: Long) = Unit

        override suspend fun deleteForPage(
            projectId: Long,
            page: Int,
        ) = Unit

        override suspend fun deleteById(id: Long) = Unit
    }
}
