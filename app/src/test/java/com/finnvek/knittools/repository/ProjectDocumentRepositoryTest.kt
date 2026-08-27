package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ProjectDocumentRepositoryTest {
    private lateinit var documentDao: ProjectDocumentDao
    private lateinit var projectDao: CounterProjectDao
    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var layerRepository: PatternAnnotationLayerRepository
    private lateinit var availability: ProjectDocumentFileAvailability
    private lateinit var repository: ProjectDocumentRepository

    @Before
    fun setUp() {
        documentDao = mockk(relaxed = true)
        projectDao = mockk(relaxed = true)
        savedPatternRepository = mockk(relaxed = true)
        layerRepository = mockk(relaxed = true)
        availability = mockk(relaxed = true)
        coEvery { projectDao.getProject(PROJECT_ID) } returns CounterProjectEntity(id = PROJECT_ID)
        coEvery { documentDao.getForProject(PROJECT_ID) } returns emptyList()
        coEvery { documentDao.getBySavedPatternId(any(), any()) } returns null
        coEvery { documentDao.getByUri(any(), any()) } returns null
        coEvery { documentDao.getByDocumentKey(any(), any()) } returns null
        coEvery { documentDao.insert(any()) } returns 41L
        coEvery { documentDao.getHighestSortOrder(PROJECT_ID) } returns -1
        coEvery { documentDao.updateLabel(any(), any(), any(), any()) } returns 1
        coEvery { documentDao.updateSortOrder(any(), any(), any(), any()) } returns 1
        coEvery { documentDao.clearPrimary(any(), any()) } returns 1
        coEvery { documentDao.setPrimary(any(), any(), any()) } returns 1
        coEvery { documentDao.delete(any(), any()) } returns 1
        coEvery { layerRepository.getActiveProjectLayer(any()) } returns null
        coEvery { layerRepository.activateProjectLayerInTransaction(any(), any()) } returns
            PatternAnnotationLayer(
                id = 1,
                owner = PatternAnnotationOwner.Project(PROJECT_ID, "local:a"),
                isActive = true,
                createdAt = 1,
                updatedAt = 1,
            )
        coEvery { availability.isAvailable(any()) } returns true
        coEvery { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) } returns 70L
        repository = createRepository()
    }

    @Test
    fun `first document becomes primary and activates its annotation layer`() =
        runTest {
            val inserted = slot<ProjectDocumentEntity>()
            coEvery { documentDao.insert(capture(inserted)) } returns 41L

            val result = repository.addImportedPdf(PROJECT_ID, URI_A, "  Main chart  ", "local:a")

            assertTrue(result is ProjectDocumentMutationResult.Added)
            assertEquals(true, inserted.captured.isPrimary)
            assertEquals(0, inserted.captured.sortOrder)
            assertEquals("Main chart", inserted.captured.label)
            coVerify { layerRepository.activateProjectLayerInTransaction(PROJECT_ID, "local:a") }
        }

    @Test
    fun `secondary addition appends without changing the primary`() =
        runTest {
            coEvery { documentDao.getForProject(PROJECT_ID) } returns listOf(document(id = 10, primary = true))
            coEvery { documentDao.getHighestSortOrder(PROJECT_ID) } returns 0
            val inserted = slot<ProjectDocumentEntity>()
            coEvery { documentDao.insert(capture(inserted)) } returns 42L

            repository.addImportedPdf(PROJECT_ID, URI_B, "Chart", "local:b")

            assertEquals(false, inserted.captured.isPrimary)
            assertEquals(1, inserted.captured.sortOrder)
            coVerify(exactly = 0) { documentDao.clearPrimary(any(), any()) }
            coVerify(exactly = 0) { layerRepository.activateProjectLayerInTransaction(any(), any()) }
        }

    @Test
    fun `saved pattern requires a readable PDF and rejects stale duplicates`() =
        runTest {
            coEvery { savedPatternRepository.getById(90L) } returns
                SavedPattern(
                    id = 90,
                    source = SavedPatternSource.LocalFile,
                    name = "Metadata",
                    designerName = "",
                )
            assertEquals(
                ProjectDocumentMutationResult.MetadataOnlyPattern,
                repository.addSavedPattern(PROJECT_ID, 90L),
            )

            coEvery { savedPatternRepository.getById(91L) } returns
                SavedPattern(
                    id = 91,
                    source = SavedPatternSource.LocalFile,
                    name = "Chart",
                    designerName = "",
                    localPdfUri = URI_A,
                )
            coEvery { documentDao.getBySavedPatternId(PROJECT_ID, 91L) } returns document(id = 7, savedPatternId = 91)
            assertEquals(
                ProjectDocumentMutationResult.AlreadyAttached,
                repository.addSavedPattern(PROJECT_ID, 91L),
            )
        }

    @Test
    fun `reorder swaps adjacent order without changing primary`() =
        runTest {
            coEvery { documentDao.getForProject(PROJECT_ID) } returns
                listOf(
                    document(id = 10, order = 0, primary = true),
                    document(id = 11, order = 1),
                )

            val result = repository.moveEarlier(PROJECT_ID, 11L)

            assertEquals(ProjectDocumentMutationResult.Reordered, result)
            coVerifyOrder {
                documentDao.updateSortOrder(11L, PROJECT_ID, 0, any())
                documentDao.updateSortOrder(10L, PROJECT_ID, 1, any())
            }
            coVerify(exactly = 0) { documentDao.clearPrimary(any(), any()) }
        }

    @Test
    fun `setting primary is atomic and does not reorder`() =
        runTest {
            coEvery { documentDao.getById(11L) } returns document(id = 11, order = 4)

            val result = repository.setPrimary(PROJECT_ID, 11L)

            assertEquals(ProjectDocumentMutationResult.PrimaryChanged, result)
            coVerifyOrder {
                documentDao.clearPrimary(PROJECT_ID, any())
                documentDao.setPrimary(11L, PROJECT_ID, any())
            }
            coVerify(exactly = 0) { documentDao.updateSortOrder(any(), any(), any(), any()) }
        }

    @Test
    fun `removing primary promotes lowest ordered document and cleans after commit`() =
        runTest {
            val removed = document(id = 10, order = 0, primary = true, uri = URI_A, key = "local:a")
            val fallback = document(id = 8, order = 2, uri = URI_B, key = "local:b")
            coEvery { documentDao.getById(10L) } returns removed
            coEvery { documentDao.getForProject(PROJECT_ID) } returns listOf(removed, fallback)

            val result = repository.remove(PROJECT_ID, 10L)

            assertTrue(result is ProjectDocumentMutationResult.Removed)
            assertEquals(8L, (result as ProjectDocumentMutationResult.Removed).newPrimary?.id)
            coVerifyOrder {
                documentDao.clearPrimary(PROJECT_ID, any())
                documentDao.setPrimary(8L, PROJECT_ID, any())
                documentDao.delete(10L, PROJECT_ID)
                savedPatternRepository.deleteLocalPatternFileIfUnused(URI_A)
            }
        }

    @Test
    fun `invalid rename and boundary reorder fail without writes`() =
        runTest {
            coEvery { documentDao.getForProject(PROJECT_ID) } returns
                listOf(document(id = 10, order = 0, primary = true))

            assertEquals(ProjectDocumentMutationResult.InvalidLabel, repository.rename(PROJECT_ID, 10L, "  "))
            assertEquals(ProjectDocumentMutationResult.StaleAction, repository.moveEarlier(PROJECT_ID, 10L))

            coVerify(exactly = 0) { documentDao.updateLabel(any(), any(), any(), any()) }
            coVerify(exactly = 0) { documentDao.updateSortOrder(any(), any(), any(), any()) }
        }

    @Test
    fun `rename trims label and allows a label used by another document`() =
        runTest {
            coEvery { documentDao.getById(10L) } returns document(id = 10L)

            val result = repository.rename(PROJECT_ID, 10L, "  Chart  ")

            assertEquals(ProjectDocumentMutationResult.Renamed, result)
            coVerify { documentDao.updateLabel(10L, PROJECT_ID, "Chart", any()) }
        }

    @Test
    fun `move later normalizes tied orders and preserves primary`() =
        runTest {
            coEvery { documentDao.getForProject(PROJECT_ID) } returns
                listOf(
                    document(id = 10L, order = 0, primary = true),
                    document(id = 11L, order = 0),
                    document(id = 12L, order = 4),
                )

            val result = repository.moveLater(PROJECT_ID, 10L)

            assertEquals(ProjectDocumentMutationResult.Reordered, result)
            coVerifyOrder {
                documentDao.updateSortOrder(10L, PROJECT_ID, 1, any())
                documentDao.updateSortOrder(12L, PROJECT_ID, 2, any())
            }
            coVerify(exactly = 0) { documentDao.clearPrimary(any(), any()) }
        }

    @Test
    fun `setting already-primary document is an idempotent no-op`() =
        runTest {
            coEvery { documentDao.getById(10L) } returns document(id = 10L, primary = true)

            assertEquals(ProjectDocumentMutationResult.PrimaryChanged, repository.setPrimary(PROJECT_ID, 10L))

            coVerify(exactly = 0) { documentDao.clearPrimary(any(), any()) }
            coVerify(exactly = 0) { documentDao.setPrimary(any(), any(), any()) }
        }

    @Test
    fun `removing active final document deactivates its layer and leaves no primary`() =
        runTest {
            val only = document(id = 10L, primary = true, key = "local:a")
            coEvery { documentDao.getById(10L) } returns only
            coEvery { documentDao.getForProject(PROJECT_ID) } returns listOf(only)
            coEvery { layerRepository.getActiveProjectLayer(PROJECT_ID) } returns
                PatternAnnotationLayer(
                    id = 2L,
                    owner = PatternAnnotationOwner.Project(PROJECT_ID, "local:a"),
                    isActive = true,
                    createdAt = 1L,
                    updatedAt = 1L,
                )

            val result = repository.remove(PROJECT_ID, 10L)

            assertTrue(result is ProjectDocumentMutationResult.Removed)
            assertNull((result as ProjectDocumentMutationResult.Removed).newPrimary)
            coVerify { layerRepository.deactivateProjectLayersInTransaction(PROJECT_ID) }
            coVerify(exactly = 0) { documentDao.setPrimary(any(), any(), any()) }
        }

    @Test
    fun `removing secondary preserves existing primary and tolerates cleanup failure`() =
        runTest {
            val primary = document(id = 10L, primary = true, key = "local:a")
            val secondary = document(id = 11L, order = 1, uri = URI_B, key = "local:b")
            coEvery { documentDao.getById(11L) } returns secondary
            coEvery { documentDao.getForProject(PROJECT_ID) } returns listOf(primary, secondary)
            coEvery { savedPatternRepository.deleteLocalPatternFileIfUnused(URI_B) } throws
                IllegalStateException("cleanup failed")

            val result = repository.remove(PROJECT_ID, 11L)

            assertTrue(result is ProjectDocumentMutationResult.Removed)
            assertEquals(10L, (result as ProjectDocumentMutationResult.Removed).newPrimary?.id)
            assertTrue(result.newPrimary?.isPrimary == true)
            coVerify(exactly = 0) { documentDao.clearPrimary(any(), any()) }
        }

    @Test
    fun `viewer state update clamps page and guide fractions`() =
        runTest {
            coEvery {
                documentDao.updateViewerState(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns 1

            val result =
                repository.updateViewerState(
                    projectId = PROJECT_ID,
                    documentId = 10L,
                    currentPage = -3,
                    rowMapping = "1:0:0.2",
                    readingLineEnabled = true,
                    readingLineYFraction = -1f,
                    readingLineFollowCurrentRow = false,
                    verticalReadingGuideEnabled = true,
                    verticalReadingGuideXFraction = 3f,
                )

            assertEquals(ProjectDocumentMutationResult.ViewerStateUpdated, result)
            coVerify {
                documentDao.updateViewerState(
                    documentId = 10L,
                    projectId = PROJECT_ID,
                    currentPage = 0,
                    rowMapping = "1:0:0.2",
                    readingLineEnabled = true,
                    readingLineYFraction = 0.05f,
                    readingLineFollowCurrentRow = false,
                    verticalReadingGuideEnabled = true,
                    verticalReadingGuideXFraction = 0.95f,
                    updatedAt = any(),
                )
            }
        }

    @Test
    fun `failed primary write aborts the transaction instead of leaving no primary`() =
        runTest {
            val transaction = RecordingTransactionRunner()
            repository = createRepository(transaction)
            coEvery { documentDao.getById(11L) } returns document(id = 11L)
            coEvery { documentDao.setPrimary(11L, PROJECT_ID, any()) } returns 0

            val result = repository.setPrimary(PROJECT_ID, 11L)

            assertEquals(ProjectDocumentMutationResult.StaleAction, result)
            assertTrue(transaction.rolledBack)
        }

    @Test
    fun `failed removal aborts before cleanup`() =
        runTest {
            val transaction = RecordingTransactionRunner()
            repository = createRepository(transaction)
            val only = document(id = 10L, primary = true)
            coEvery { documentDao.getById(10L) } returns only
            coEvery { documentDao.getForProject(PROJECT_ID) } returns listOf(only)
            coEvery { documentDao.delete(10L, PROJECT_ID) } returns 0

            val result = repository.remove(PROJECT_ID, 10L)

            assertEquals(ProjectDocumentMutationResult.StaleAction, result)
            assertTrue(transaction.rolledBack)
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `selection activates an available secondary and rejects an unavailable file`() =
        runTest {
            val secondary = document(id = 11L, order = 1, uri = URI_B, key = "local:b")
            coEvery { documentDao.getById(11L) } returns secondary
            coEvery { availability.isAvailable(URI_B) } returns false

            assertEquals(ProjectDocumentMutationResult.PdfUnavailable, repository.select(PROJECT_ID, 11L))
            coVerify(exactly = 0) { layerRepository.activateProjectLayerInTransaction(PROJECT_ID, "local:b") }

            coEvery { availability.isAvailable(URI_B) } returns true
            assertEquals(ProjectDocumentMutationResult.Selected, repository.select(PROJECT_ID, 11L))
            coVerify(exactly = 1) { layerRepository.activateProjectLayerInTransaction(PROJECT_ID, "local:b") }
        }

    @Test
    fun `active layer resolves secondary and otherwise falls back to primary`() =
        runTest {
            val secondary = document(id = 11L, order = 1, uri = URI_B, key = "local:b")
            coEvery { documentDao.getByDocumentKey(PROJECT_ID, "local:b") } returns secondary
            coEvery { layerRepository.getActiveProjectLayer(PROJECT_ID) } returns
                PatternAnnotationLayer(
                    id = 3L,
                    owner = PatternAnnotationOwner.Project(PROJECT_ID, "local:b"),
                    isActive = true,
                    createdAt = 1L,
                    updatedAt = 1L,
                )

            assertEquals(11L, repository.getActiveDocument(PROJECT_ID)?.id)

            coEvery { layerRepository.getActiveProjectLayer(PROJECT_ID) } returns null
            coEvery { documentDao.getPrimary(PROJECT_ID) } returns document(id = 10L, primary = true)
            assertEquals(10L, repository.getActiveDocument(PROJECT_ID)?.id)
        }

    @Test
    fun `cancellation propagates instead of becoming persistence failure`() =
        runTest {
            repository =
                createRepository(
                    transactionRunner =
                        object : com.finnvek.knittools.data.local.DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T =
                                throw CancellationException("cancelled")
                        },
                )

            try {
                repository.addImportedPdf(PROJECT_ID, URI_A, "Chart", "local:a")
                fail("CancellationException expected")
            } catch (_: CancellationException) {
                // Peruutus välitetään kutsujalle muuttamatta sitä tavalliseksi virheeksi.
            }
        }

    private fun createRepository(
        transactionRunner: com.finnvek.knittools.data.local.DatabaseTransactionRunner =
            ImmediateDatabaseTransactionRunner,
    ): ProjectDocumentRepository =
        ProjectDocumentRepository(
            documentDao = documentDao,
            projectDao = projectDao,
            savedPatternRepository = savedPatternRepository,
            layerRepository = layerRepository,
            transactionRunner = transactionRunner,
            fileAvailability = availability,
        )

    private class RecordingTransactionRunner : DatabaseTransactionRunner {
        var rolledBack = false

        override suspend fun <T> run(block: suspend () -> T): T =
            try {
                block()
            } catch (failure: Exception) {
                rolledBack = true
                throw failure
            }
    }

    private fun document(
        id: Long,
        order: Int = 0,
        primary: Boolean = false,
        savedPatternId: Long? = null,
        uri: String = URI_A,
        key: String = "local:$id",
    ) = ProjectDocumentEntity(
        id = id,
        projectId = PROJECT_ID,
        savedPatternId = savedPatternId,
        documentKey = key,
        label = "Document $id",
        localPdfUri = uri,
        sortOrder = order,
        isPrimary = primary,
        currentPage = 0,
        rowMapping = null,
        readingLineEnabled = false,
        readingLineYFraction = 0.5f,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = 0.5f,
        createdAt = 1,
        updatedAt = 1,
    )

    private companion object {
        private const val PROJECT_ID = 7L
        private const val URI_A = "file:///patterns/a.pdf"
        private const val URI_B = "file:///patterns/b.pdf"
    }
}
