package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
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
class SavedPatternRepositoryDomainApiTest {
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
                                source = SavedPatternSource.Ravelry.persistedValue,
                                ravelryPatternId = 123,
                                name = "Cardigan",
                                designerName = "Designer",
                                originalUrl = "https://example.com/cardigan",
                                canonicalUrl = "https://example.com/cardigan",
                                savedAt = 100L,
                            ),
                        ),
                )
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    RepositoryDomainFakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val patterns: List<SavedPattern> = repository.getAll().first()
            val byId: SavedPattern? = repository.getById(1L)
            val savedId =
                repository.save(
                    SavedPattern(
                        source = SavedPatternSource.LocalFile,
                        ravelryPatternId = 456,
                        name = "Hat",
                        designerName = "Maker",
                        originalUrl = "content://hat",
                        localPdfUri = "content://hat",
                        isAvailableOffline = true,
                        savedAt = 200L,
                    ),
                )

            assertEquals("Cardigan", patterns.single().name)
            assertEquals("Cardigan", byId?.name)
            assertEquals(99L, savedId)
            assertEquals(
                SavedPatternEntity(
                    source = SavedPatternSource.LocalFile.persistedValue,
                    ravelryPatternId = 456,
                    name = "Hat",
                    designerName = "Maker",
                    originalUrl = "content://hat",
                    localPdfUri = "content://hat",
                    isAvailableOffline = true,
                    savedAt = 200L,
                    updatedAt = 200L,
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
                    RepositoryDomainFakeCounterProjectDao(),
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
    fun `saved pattern repository exposes batch domain lookup`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 1L,
                                source = SavedPatternSource.Ravelry.persistedValue,
                                ravelryPatternId = 1,
                                name = "Palmikot",
                                designerName = "Designer",
                            ),
                            SavedPatternEntity(
                                id = 2L,
                                source = SavedPatternSource.Ravelry.persistedValue,
                                ravelryPatternId = 2,
                                name = "Ribbi",
                                designerName = "Designer",
                            ),
                        ),
                )
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    RepositoryDomainFakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val patterns = repository.getByIds(listOf(1L, 2L))

            assertEquals(listOf("Palmikot", "Ribbi"), patterns.map { it.name })
            assertEquals(listOf(1L, 2L), dao.lastRequestedIds)
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
                                source = SavedPatternSource.Ravelry.persistedValue,
                                ravelryPatternId = 42,
                                name = "Old name",
                                designerName = "Designer",
                                canonicalUrl = "https://example.com/patterns/42",
                            ),
                        ),
                )
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    RepositoryDomainFakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveRavelryPatternIfMissing(
                    SavedPattern(
                        source = SavedPatternSource.Ravelry,
                        ravelryPatternId = 42,
                        name = "New name",
                        designerName = "Designer",
                        canonicalUrl = "https://example.com/patterns/42",
                    ),
                )

            assertEquals(7L, savedId)
            assertEquals(0, dao.insertCount)
        }

    @Test
    fun `saved pattern repository reuses existing ravelry pattern by normalized original url`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 7L,
                                source = SavedPatternSource.Other.persistedValue,
                                ravelryPatternId = null,
                                name = "Old name",
                                designerName = "Designer",
                                originalUrl = "https://carts.ravelry.com/patterns/library/delight-cardigan/",
                                canonicalUrl = "",
                            ),
                        ),
                )
            val repository =
                SavedPatternRepository(
                    dao,
                    context,
                    RepositoryDomainFakeCounterProjectDao(),
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveRavelryPatternIfMissing(
                    SavedPattern(
                        source = SavedPatternSource.Ravelry,
                        ravelryPatternId = 99,
                        name = "Delight Cardigan",
                        designerName = "Designer",
                        originalUrl =
                            "https://www.ravelry.com/patterns/library/delight-cardigan?utm_source=share#notes",
                        canonicalUrl = "https://www.ravelry.com/patterns/library/delight-cardigan",
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
                                source = SavedPatternSource.LocalFile.persistedValue,
                                ravelryPatternId = null,
                                name = "Missing",
                                designerName = "Imported",
                                originalUrl = missingUri,
                                localPdfUri = missingUri,
                                isAvailableOffline = true,
                            ),
                        ),
                )
            every { context.filesDir } returns filesDir
            withParsedFileUri(missingUri, java.io.File(filesDir, "pattern_pdfs/1/missing.pdf").absolutePath) {
                val repository =
                    SavedPatternRepository(
                        dao,
                        context,
                        RepositoryDomainFakeCounterProjectDao(),
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
                                source = SavedPatternSource.LocalFile.persistedValue,
                                ravelryPatternId = null,
                                name = "Pattern.pdf",
                                designerName = "Imported",
                                originalUrl = existingUri,
                                localPdfUri = existingUri,
                                isAvailableOffline = true,
                            ),
                        ),
                )
            every { context.filesDir } returns filesDir
            withParsedFileUrisForDomainApi(
                existingUri to existingFile.absolutePath,
                candidateUri to candidateFile.absolutePath,
            ) {
                val repository =
                    SavedPatternRepository(
                        dao,
                        context,
                        RepositoryDomainFakeCounterProjectDao(),
                        ImmediateDatabaseTransactionRunner,
                        UnconfinedTestDispatcher(testScheduler),
                    )

                val reusableUri = repository.findReusableImportedPatternUrl(candidateUri, "Pattern.pdf")

                assertEquals(existingUri, reusableUri)
            }
        }

    @Test
    fun `saved pattern repository clears linked projects before deleting patterns`() =
        runTest {
            val dao =
                FakeSavedPatternDao(
                    savedPatterns =
                        listOf(
                            SavedPatternEntity(
                                id = 4L,
                                source = SavedPatternSource.Ravelry.persistedValue,
                                ravelryPatternId = 4,
                                name = "Pattern",
                                designerName = "Designer",
                            ),
                        ),
                )
            val projectDao = RepositoryDomainFakeCounterProjectDao()
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
                            PatternAnnotation(
                                id = 4L,
                                layerId = 7L,
                                page = 2,
                                kind = PatternAnnotationKind.FREEHAND,
                                payload = freehandPayload(argb = 0xFF123456.toInt()),
                                zIndex = 0L,
                                createdAt = 400L,
                                updatedAt = 500L,
                            ).toEntity(),
                        ),
                )
            val repository = PatternAnnotationRepository(dao)
            val added =
                PatternAnnotation(
                    layerId = 7L,
                    page = 2,
                    kind = PatternAnnotationKind.FREEHAND,
                    payload = freehandPayload(argb = 0xFF654321.toInt()),
                    zIndex = 1L,
                    createdAt = 600L,
                )

            val annotations: List<PatternAnnotation> = repository.observePage(7L, 2).first()
            val addedId = repository.insertAnnotation(added)

            assertEquals(0xFF123456.toInt(), (annotations.single().payload as FreehandPayload).argb)
            assertEquals(77L, addedId)
            assertEquals(0xFF654321.toInt(), (dao.lastInserted?.toDomain()?.payload as FreehandPayload).argb)
        }

    private fun freehandPayload(argb: Int) =
        FreehandPayload(
            points = listOf(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f)),
            argb = argb,
            strokeWidth = 3f,
        )
}

internal fun detailedYarnCardEntity() =
    YarnCardEntity(
        id = 5L,
        brand = "Old brand",
        yarnName = "Old yarn",
        fiberContent = "75% wool",
        weightGrams = "100",
        lengthMeters = "200",
        needleSize = "3.5 mm",
        gaugeInfo = "22 sts",
        colorName = "Old color",
        colorNumber = "12",
        dyeLot = "A",
        weightCategory = "DK",
        careSymbols = 15L,
        photoUri = "content://photo",
        createdAt = 123L,
        quantityInStash = 2,
        status = "IN_USE",
        linkedProjectId = 10L,
    )

internal fun editedYarnCard() =
    YarnCard(
        id = 5L,
        brand = "New brand",
        yarnName = "New yarn",
        colorName = "New color",
        colorNumber = "34",
        dyeLot = "B",
        weightCategory = "Fingering",
    )

internal class FakeSavedPatternDao(
    private val savedPatterns: List<SavedPatternEntity> = emptyList(),
) : SavedPatternDao {
    var lastInserted: SavedPatternEntity? = null
    var deletedIds: List<Long> = emptyList()
    var insertCount: Int = 0
    var lastRequestedIds: List<Long> = emptyList()

    override fun getAll(): Flow<List<SavedPatternEntity>> = flowOf(savedPatterns)

    override suspend fun getById(id: Long): SavedPatternEntity? = savedPatterns.firstOrNull { it.id == id }

    override suspend fun getByRavelryPatternId(ravelryPatternId: Int): SavedPatternEntity? =
        savedPatterns.firstOrNull { it.ravelryPatternId == ravelryPatternId }

    override suspend fun getByCanonicalUrl(canonicalUrl: String): SavedPatternEntity? =
        savedPatterns.firstOrNull { it.canonicalUrl == canonicalUrl }

    override suspend fun getByOriginalUrl(originalUrl: String): SavedPatternEntity? =
        savedPatterns.firstOrNull { it.originalUrl == originalUrl }

    override suspend fun getByLocalPdfUri(localPdfUri: String): SavedPatternEntity? =
        savedPatterns.firstOrNull { it.localPdfUri == localPdfUri }

    override suspend fun getByTitleAndDesignerName(
        name: String,
        designerName: String,
    ): SavedPatternEntity? = savedPatterns.firstOrNull { it.name == name && it.designerName == designerName }

    override suspend fun getAllOnce(): List<SavedPatternEntity> = savedPatterns

    override suspend fun getImportedPatternsOnce(): List<SavedPatternEntity> =
        savedPatterns.filter {
            it.source == SavedPatternSource.LocalFile.persistedValue && it.localPdfUri != null
        }

    override suspend fun getByIds(ids: List<Long>): List<SavedPatternEntity> {
        lastRequestedIds = ids
        return savedPatterns.filter { it.id in ids }
    }

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

internal class FakeYarnCardDao(
    private val yarnCards: List<YarnCardEntity> = emptyList(),
) : YarnCardDao {
    var lastUpserted: YarnCardEntity? = null
    var lastLinkedProjectUpdate: Pair<Long, Long?>? = null
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

    override suspend fun updatePhotoUri(
        id: Long,
        photoUri: String,
    ): Int = if (yarnCards.any { it.id == id }) 1 else 0

    override suspend fun updateLinkedProjectId(
        id: Long,
        projectId: Long?,
    ): Int {
        if (yarnCards.none { it.id == id }) return 0
        lastLinkedProjectUpdate = id to projectId
        return 1
    }

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

internal class RepositoryDomainFakeCounterProjectDao(
    projects: List<CounterProjectEntity> = emptyList(),
) : StubCounterProjectDao(projects) {
    val updatedYarnCardIds = linkedMapOf<Long, String>()
    var clearedPatternIds: List<Long> = emptyList()

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
}

internal class FakePatternAnnotationDao(
    private val patternAnnotations: List<PatternAnnotationEntity> = emptyList(),
) : PatternAnnotationDao {
    var lastInserted: PatternAnnotationEntity? = null

    override fun observePage(
        layerId: Long,
        page: Int,
    ): Flow<List<PatternAnnotationEntity>> =
        flowOf(patternAnnotations.filter { it.layerId == layerId && it.page == page })

    override suspend fun insert(annotation: PatternAnnotationEntity): Long {
        lastInserted = annotation
        return 77L
    }

    override suspend fun restoreBatch(annotations: List<PatternAnnotationEntity>) = Unit

    override suspend fun update(annotation: PatternAnnotationEntity) = Unit

    override suspend fun deleteForProject(projectId: Long) = Unit

    override suspend fun deleteForPage(
        layerId: Long,
        page: Int,
    ) = Unit

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun updateZIndex(
        id: Long,
        zIndex: Long,
        updatedAt: Long,
    ) = Unit
}

internal suspend inline fun withParsedFileUrisForDomainApi(
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
