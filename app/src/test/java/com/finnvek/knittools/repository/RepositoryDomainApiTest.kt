package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    FakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

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
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    FakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val importedId = repository.saveImportedPatternIfMissing("content://pattern", "Local pattern")
            val ignoredId = repository.saveImportedPatternIfMissing("https://example.com/pattern", "Remote pattern")

            assertEquals(99L, importedId)
            assertNull(ignoredId)
            assertEquals("Imported", dao.lastInserted?.designerName)
        }

    @Test
    fun `saved pattern repository reuses existing ravelry pattern`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 7L,
                                ravelryId = 42,
                                name = "Old name",
                                designerName = "Designer",
                            ),
                        ),
                )
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    FakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveRavelryPatternIfMissing(
                    SavedPattern(
                        ravelryId = 42,
                        name = "New name",
                        designerName = "Designer",
                    ),
                )

            assertEquals(7L, savedId)
            assertEquals(0, dao.insertCount)
        }

    @Test
    fun `saved pattern repository prunes missing app owned pattern on viewer load`() =
        runTest {
            val missingUri = "file:///data/data/com.finnvek.knittools/files/pattern_pdfs/1/missing.pdf"
            val filesDir =
                java.nio.file.Files
                    .createTempDirectory("knittools-files")
                    .toFile()
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 7L,
                                ravelryId = 0,
                                name = "Missing",
                                designerName = "Imported",
                                patternUrl = missingUri,
                            ),
                        ),
                )
            every { context.filesDir } returns filesDir
            withParsedFileUri(missingUri, java.io.File(filesDir, "pattern_pdfs/1/missing.pdf").absolutePath) {
                val repository =
                    SavedPatternRepository(
                        dao,
                        context,
                        FakeCounterProjectDao(),
                        ImmediateDatabaseTransactionRunner,
                        UnconfinedTestDispatcher(testScheduler),
                    )

                val pattern = repository.getByIdIfAvailable(7L)

                assertNull(pattern)
                assertEquals(listOf(7L), dao.deletedIds)
            }
        }

    @Test
    fun `saved pattern repository finds reusable imported PDF with same content`() =
        runTest {
            val filesDir =
                java.nio.file.Files
                    .createTempDirectory("knittools-files")
                    .toFile()
            val existingFile = java.io.File(filesDir, "pattern_pdfs/1/pattern.pdf")
            val candidateFile = java.io.File(filesDir, "pattern_pdfs/1/pattern-1.pdf")
            requireNotNull(existingFile.parentFile).mkdirs()
            existingFile.writeText("pdf bytes")
            candidateFile.writeText("pdf bytes")
            val existingUri = "file://${existingFile.absolutePath.replace('\\', '/')}"
            val candidateUri = "file://${candidateFile.absolutePath.replace('\\', '/')}"
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 7L,
                                ravelryId = 0,
                                name = "Pattern.pdf",
                                designerName = "Imported",
                                patternUrl = existingUri,
                            ),
                        ),
                )
            every { context.filesDir } returns filesDir
            withParsedFileUris(
                existingUri to existingFile.absolutePath,
                candidateUri to candidateFile.absolutePath,
            ) {
                val repository =
                    SavedPatternRepository(
                        dao,
                        context,
                        FakeCounterProjectDao(),
                        ImmediateDatabaseTransactionRunner,
                        UnconfinedTestDispatcher(testScheduler),
                    )

                val reusableUri = repository.findReusableImportedPatternUrl(candidateUri, "Pattern.pdf")

                assertEquals(existingUri, reusableUri)
            }
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
            val repository =
                YarnCardRepository(
                    dao,
                    FakeCounterProjectDao(),
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

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
    fun `yarn card repository removes deleted card ids from projects`() =
        runTest {
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(id = 2L, yarnName = "Deleted"),
                        ),
                )
            val projectDao =
                FakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,2,3"),
                            CounterProjectEntity(id = 11L, yarnCardIds = "2"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteCards(listOf(2L))

            assertEquals(mapOf(10L to "1,3", 11L to ""), projectDao.updatedYarnCardIds)
            assertEquals(listOf(2L), yarnDao.deletedIds)
        }

    @Test
    fun `yarn card repository reports rejected detail updates`() =
        runTest {
            val yarnDao = FakeYarnCardDao()
            val repository =
                YarnCardRepository(
                    yarnDao,
                    FakeCounterProjectDao(),
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            assertEquals(false, repository.updateQuantity(99L, 4))
            assertEquals(false, repository.updateStatus(99L, "USED_UP"))
            assertEquals(false, repository.updateLinkedProjectId(99L, null))
        }

    @Test
    fun `yarn card save keeps linked project ids consistent`() =
        runTest {
            val yarnDao = FakeYarnCardDao()
            val projectDao =
                FakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,2"),
                            CounterProjectEntity(id = 11L, yarnCardIds = "3"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveCard(
                    YarnCard(
                        brand = "Novita",
                        yarnName = "Nalle",
                        linkedProjectId = 10L,
                    ),
                )

            assertEquals(88L, savedId)
            assertEquals(10L, yarnDao.lastUpserted?.linkedProjectId)
            assertEquals(mapOf(10L to "1,2,88"), projectDao.updatedYarnCardIds)
        }

    @Test
    fun `yarn card save removes stale project link when saved unlinked`() =
        runTest {
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(id = 5L, yarnName = "Sock", linkedProjectId = 10L),
                        ),
                )
            val projectDao =
                FakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,5"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveCard(
                    YarnCard(
                        id = 5L,
                        brand = "Novita",
                        yarnName = "Nalle",
                        linkedProjectId = null,
                    ),
                )

            assertEquals(5L, savedId)
            assertEquals(null, yarnDao.lastUpserted?.linkedProjectId)
            assertEquals(mapOf(10L to "1"), projectDao.updatedYarnCardIds)
        }

    @Test
    fun `saved pattern repository clears linked projects before deleting patterns`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(id = 4L, ravelryId = 4, name = "Pattern", designerName = "Designer"),
                        ),
                )
            val projectDao = FakeCounterProjectDao()
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    projectDao,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteByIds(listOf(4L))

            assertEquals(listOf(4L), projectDao.clearedPatternIds)
            assertEquals(listOf(4L), dao.deletedIds)
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
        var deletedIds: List<Long> = emptyList()
        var insertCount: Int = 0

        override fun getAll(): Flow<List<SavedPatternEntity>> = flowOf(savedPatterns)

        override suspend fun getById(id: Long): SavedPatternEntity? = savedPatterns.firstOrNull { it.id == id }

        override suspend fun getByRavelryId(ravelryId: Int): SavedPatternEntity? =
            savedPatterns.firstOrNull { it.ravelryId == ravelryId }

        override suspend fun getByPatternUrl(patternUrl: String): SavedPatternEntity? =
            savedPatterns.firstOrNull { it.patternUrl == patternUrl }

        override suspend fun getImportedPatternsOnce(): List<SavedPatternEntity> =
            savedPatterns.filter { it.ravelryId == 0 && it.patternUrl.isNotBlank() }

        override suspend fun getByIds(ids: List<Long>): List<SavedPatternEntity> = savedPatterns.filter { it.id in ids }

        override suspend fun insert(pattern: SavedPatternEntity): Long {
            insertCount += 1
            lastInserted = pattern
            return 99L
        }

        override suspend fun deleteById(id: Long) {
            deletedIds = listOf(id)
        }

        override suspend fun deleteByIds(ids: List<Long>) {
            deletedIds = ids
        }

        override fun getCount(): Flow<Int> = flowOf(savedPatterns.size)
    }

    private class FakeYarnCardDao(
        private val yarnCards: List<YarnCardEntity> = emptyList(),
    ) : YarnCardDao {
        var lastUpserted: YarnCardEntity? = null
        var clearedProjectId: Long? = null
        var deletedIds: List<Long> = emptyList()

        override fun getAllCards(): Flow<List<YarnCardEntity>> = flowOf(yarnCards)

        override suspend fun getCard(id: Long): YarnCardEntity? = yarnCards.firstOrNull { it.id == id }

        override fun observeCard(id: Long): Flow<YarnCardEntity?> = flowOf(yarnCards.firstOrNull { it.id == id })

        override suspend fun getCards(ids: List<Long>): List<YarnCardEntity> = yarnCards.filter { it.id in ids }

        override suspend fun upsert(card: YarnCardEntity): Long {
            lastUpserted = card
            return 88L
        }

        override fun getCardCount(): Flow<Int> = flowOf(yarnCards.size)

        override suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ): Int = if (yarnCards.any { it.id == id }) 1 else 0

        override suspend fun updateStatus(
            id: Long,
            status: String,
        ): Int = if (yarnCards.any { it.id == id }) 1 else 0

        override suspend fun updateLinkedProjectId(
            id: Long,
            projectId: Long?,
        ): Int = if (yarnCards.any { it.id == id }) 1 else 0

        override suspend fun clearLinkedProject(projectId: Long) {
            clearedProjectId = projectId
        }

        override suspend fun delete(id: Long) {
            deletedIds = listOf(id)
        }

        override suspend fun deleteByIds(ids: List<Long>) {
            deletedIds = ids
        }
    }

    private class FakeCounterProjectDao(
        private val projects: List<CounterProjectEntity> = emptyList(),
    ) : CounterProjectDao {
        val updatedYarnCardIds = linkedMapOf<Long, String>()
        var clearedPatternIds: List<Long> = emptyList()

        override fun getAllProjects(): Flow<List<CounterProjectEntity>> = flowOf(projects)

        override suspend fun getAllProjectsOnce(): List<CounterProjectEntity> = projects

        override suspend fun getProject(id: Long): CounterProjectEntity? = projects.firstOrNull { it.id == id }

        override fun observeProject(id: Long): Flow<CounterProjectEntity?> = flowOf(getProjectSync(id))

        override suspend fun insert(project: CounterProjectEntity): Long = 0L

        override suspend fun update(project: CounterProjectEntity) = Unit

        override suspend fun adjustCount(
            id: Long,
            delta: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun adjustCountAndStepSize(
            id: Long,
            delta: Int,
            stepSize: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateCounterState(
            id: Long,
            count: Int,
            stepSize: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateName(
            id: Long,
            name: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateNotes(
            id: Long,
            notes: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateSecondaryCount(
            id: Long,
            secondaryCount: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateSectionName(
            id: Long,
            sectionName: String?,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateStitchCount(
            id: Long,
            stitchCount: Int?,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateCurrentStitch(
            id: Long,
            stitch: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateStitchTrackingEnabled(
            id: Long,
            enabled: Boolean,
            updatedAt: Long,
        ) = Unit

        override suspend fun updatePattern(
            id: Long,
            patternUri: String?,
            patternName: String?,
            currentPatternPage: Int,
            patternRowMapping: String?,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateCurrentPatternPage(
            id: Long,
            page: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updatePatternRowMapping(
            id: Long,
            mapping: String?,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateStepSize(
            id: Long,
            stepSize: Int,
            updatedAt: Long,
        ) = Unit

        override suspend fun updateYarnCardIds(
            id: Long,
            yarnCardIds: String,
            updatedAt: Long,
        ) {
            updatedYarnCardIds[id] = yarnCardIds
        }

        override suspend fun clearLinkedPatternIds(
            patternIds: List<Long>,
            updatedAt: Long,
        ) {
            clearedPatternIds = patternIds
        }

        override suspend fun countProjectsUsingPatternUri(patternUri: String): Int =
            projects.count { it.patternUri == patternUri }

        override suspend fun archiveProject(
            id: Long,
            totalRows: Int,
            completedAt: Long,
            updatedAt: Long,
        ) = Unit

        override suspend fun reactivateProject(
            id: Long,
            updatedAt: Long,
        ) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun getProjectCount(): Int = projects.size

        override suspend fun getLatestActiveProject(): CounterProjectEntity? = projects.firstOrNull()

        override suspend fun insertHistory(entry: com.finnvek.knittools.data.local.CounterHistoryEntity) = Unit

        override suspend fun deleteHistoryBefore(
            projectId: Long,
            before: Long,
        ) = Unit

        override suspend fun getLatestHistory(
            projectId: Long,
        ): com.finnvek.knittools.data.local.CounterHistoryEntity? = null

        override suspend fun deleteHistoryById(id: Long) = Unit

        override suspend fun updateCount(
            id: Long,
            count: Int,
            updatedAt: Long,
        ) = Unit

        override fun getActiveProjects(): Flow<List<CounterProjectEntity>> = flowOf(projects)

        override fun getActiveProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(projects)

        override fun getActiveProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(projects)

        override fun getCompletedProjects(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

        override fun getCompletedProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

        override fun getCompletedProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

        override suspend fun getActiveProjectCount(): Int = projects.size

        override suspend fun updateTargetRows(
            id: Long,
            targetRows: Int?,
            updatedAt: Long,
        ) = Unit

        private fun getProjectSync(id: Long): CounterProjectEntity? = projects.firstOrNull { it.id == id }
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

    private suspend inline fun withParsedFileUri(
        uriString: String,
        path: String,
        block: suspend () -> Unit,
    ) {
        withParsedFileUris(uriString to path, block = block)
    }

    private suspend inline fun withParsedFileUris(
        vararg mappings: Pair<String, String>,
        block: suspend () -> Unit,
    ) {
        mockkStatic(Uri::class)
        mappings.forEach { (uriString, path) ->
            val uri = mockk<Uri>()
            every { Uri.parse(uriString) } returns uri
            every { uri.scheme } returns "file"
            every { uri.path } returns path
        }
        try {
            block()
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}
