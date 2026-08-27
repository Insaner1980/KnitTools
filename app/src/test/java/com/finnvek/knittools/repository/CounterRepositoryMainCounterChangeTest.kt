package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.ProjectDocument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CounterRepositoryMainCounterChangeTest {
    @Test
    fun `applyMainCounterChange increments linked counters by the real main-counter delta`() =
        runTest {
            val transactionRunner = RecordingTransactionRunner()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 5, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Cardigan",
                    count = 10,
                    stepSize = 2,
                    stitchTrackingEnabled = true,
                    currentStitch = 11,
                )
            val repository = buildRepository(projectDao, counterDao, transactionRunner)

            val changed = repository.applyMainCounterChange(7L, MainCounterChange.Increment)

            assertTrue(changed)
            assertEquals(1, transactionRunner.runCount)
            coVerify {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = 12,
                    stepSize = 2,
                    action = "increment",
                    previousValue = 10,
                    newValue = 12,
                    updatedAt = any(),
                )
                projectDao.updateCurrentStitch(7L, 0, any())
                counterDao.updateCount(20L, 7)
            }
            coVerify(exactly = 0) { counterDao.updateCount(21L, any()) }
        }

    @Test
    fun `applyMainCounterChange ignores repeat sections even if their linked flag is true`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao =
                linkedCounterDao(
                    projectId = 7L,
                    linkedCount = 5,
                    unlinkedCount = 9,
                    repeatSectionLinked = true,
                )
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Cardigan", count = 10, stepSize = 2)
            val repository = buildRepository(projectDao, counterDao)

            val changed = repository.applyMainCounterChange(7L, MainCounterChange.Increment)

            assertTrue(changed)
            coVerify { counterDao.updateCount(20L, 7) }
            coVerify(exactly = 0) { counterDao.updateCount(22L, any()) }
        }

    @Test
    fun `applyMainCounterChange decrement uses clamped main delta for linked counters`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 3, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Hat", count = 1, stepSize = 3)
            val repository = buildRepository(projectDao, counterDao)

            val changed = repository.applyMainCounterChange(7L, MainCounterChange.Decrement)

            assertTrue(changed)
            coVerify {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = 0,
                    stepSize = 3,
                    action = "decrement",
                    previousValue = 1,
                    newValue = 0,
                    updatedAt = any(),
                )
                counterDao.updateCount(20L, 2)
            }
        }

    @Test
    fun `applyMainCounterChange reset moves linked counters by reset delta without going below zero`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 4, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Sweater", count = 30, stepSize = 1)
            val repository = buildRepository(projectDao, counterDao)

            val changed = repository.applyMainCounterChange(7L, MainCounterChange.Reset)

            assertTrue(changed)
            coVerify {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = 0,
                    stepSize = 1,
                    action = "reset",
                    previousValue = 30,
                    newValue = 0,
                    updatedAt = any(),
                )
                counterDao.updateCount(20L, 0)
            }
        }

    @Test
    fun `applyMainCounterChange undo applies inverse history delta to linked counters`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 6, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Socks",
                    count = 12,
                    stepSize = 1,
                    stitchTrackingEnabled = true,
                    currentStitch = 8,
                )
            coEvery { projectDao.getLatestHistory(7L) } returns
                CounterHistoryEntity(
                    id = 55L,
                    projectId = 7L,
                    action = "increment",
                    previousValue = 10,
                    newValue = 12,
                )
            val repository = buildRepository(projectDao, counterDao)

            val changed = repository.applyMainCounterChange(7L, MainCounterChange.Undo)

            assertTrue(changed)
            coVerify {
                projectDao.updateCount(7L, 10, any())
                projectDao.deleteHistoryById(55L)
                projectDao.updateCurrentStitch(7L, 0, any())
                counterDao.updateCount(20L, 4)
            }
        }

    @Test
    fun `applyWidgetCountChange uses the same linked-counter delta semantics`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 0, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Widget project", count = 5, stepSize = 1)
            val repository = buildRepository(projectDao, counterDao)

            val changed = repository.applyWidgetCountChange(7L, increment = true)

            assertTrue(changed)
            coVerify {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = 6,
                    stepSize = 1,
                    action = "increment",
                    previousValue = 5,
                    newValue = 6,
                    updatedAt = any(),
                )
                counterDao.updateCount(20L, 1)
            }
        }

    @Test
    fun `applyMainCounterChange leaves completed and unchanged projects untouched`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val counterDao = linkedCounterDao(projectId = 7L, linkedCount = 1, unlinkedCount = 9)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Done", count = 5, isCompleted = true)
            val repository = buildRepository(projectDao, counterDao)

            val completedChanged = repository.applyMainCounterChange(7L, MainCounterChange.Increment)

            coEvery { projectDao.getProject(8L) } returns
                CounterProjectEntity(id = 8L, name = "Zero", count = 0, stepSize = 3)
            val unchanged = repository.applyMainCounterChange(8L, MainCounterChange.Decrement)

            assertFalse(completedChanged)
            assertFalse(unchanged)
            coVerify(exactly = 0) {
                projectDao.updateCounterStateWithHistory(any(), any(), any(), any(), any(), any(), any())
                counterDao.updateCount(any(), any())
            }
        }

    @Test
    fun `follow moves hidden reading line to exact marker in the counter transaction`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Followed",
                    count = 7,
                    stepSize = 1,
                )
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val document =
                projectDocument(
                    rowMapping = "[{\"row\":8,\"page\":2,\"yPosition\":0.7}]",
                    readingLineYFraction = 0.4f,
                )
            coEvery { documentRepository.getActiveDocument(7L) } returns document
            coEvery { documentRepository.updateViewerStateInTransaction(any()) } returns true
            val repository =
                buildRepository(projectDao, linkedCounterDao(7L, 0, 0), projectDocumentRepository = documentRepository)

            assertTrue(repository.applyMainCounterChange(7L, MainCounterChange.Increment))

            coVerify {
                documentRepository.updateViewerStateInTransaction(
                    match { it.id == document.id && it.currentPage == 2 && it.readingLineYFraction == 0.7f },
                )
            }
        }

    @Test
    fun `paused follow leaves persisted page and Y unchanged`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Paused",
                    count = 7,
                )
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            coEvery { documentRepository.getActiveDocument(7L) } returns
                projectDocument(
                    currentPage = 3,
                    readingLineYFraction = 0.6f,
                    readingLineFollowCurrentRow = false,
                )
            val repository =
                buildRepository(projectDao, linkedCounterDao(7L, 0, 0), projectDocumentRepository = documentRepository)

            assertTrue(repository.applyMainCounterChange(7L, MainCounterChange.Increment))

            coVerify(exactly = 0) { documentRepository.updateViewerStateInTransaction(any()) }
        }

    @Test
    fun `calibration re-evaluates current row with zero delta`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Calibrated",
                    count = 12,
                )
            val mapping = "[{\"row\":12,\"page\":4,\"yPosition\":0.35}]"
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val document = projectDocument()
            coEvery { documentRepository.getActiveDocument(7L) } returns document
            coEvery { documentRepository.updateViewerStateInTransaction(any()) } returns true
            val repository =
                buildRepository(projectDao, linkedCounterDao(7L, 0, 0), projectDocumentRepository = documentRepository)

            repository.updatePatternRowMapping(7L, mapping)

            coVerify {
                documentRepository.updateViewerStateInTransaction(
                    match {
                        it.id == document.id &&
                            it.rowMapping == mapping &&
                            it.currentPage == 4 &&
                            it.readingLineYFraction == 0.35f
                    },
                )
            }
        }

    @Test
    fun `manual page and line movement pause follow without creating markers`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Manual",
                )
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val document = projectDocument(currentPage = 1, readingLineYFraction = 0.4f)
            coEvery { documentRepository.getActiveDocument(7L) } returns document
            coEvery { documentRepository.updateViewerStateInTransaction(any()) } returns true
            val repository =
                buildRepository(projectDao, linkedCounterDao(7L, 0, 0), projectDocumentRepository = documentRepository)

            repository.updateCurrentPatternPage(7L, 2)
            repository.commitManualReadingLinePosition(7L, 0.8f)

            coVerify {
                documentRepository.updateViewerStateInTransaction(
                    match { it.currentPage == 2 && it.readingLineYFraction == 0.4f && !it.readingLineFollowCurrentRow },
                )
                documentRepository.updateViewerStateInTransaction(
                    match { it.currentPage == 1 && it.readingLineYFraction == 0.8f && !it.readingLineFollowCurrentRow },
                )
            }
        }

    @Test
    fun `transaction cancellation propagates`() =
        runTest {
            val repository =
                buildRepository(
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    transactionRunner =
                        object : DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T =
                                throw CancellationException("cancelled")
                        },
                )

            try {
                repository.applyMainCounterChange(7L, MainCounterChange.Increment)
                fail("CancellationException expected")
            } catch (_: CancellationException) {
                // Peruutus kuuluu välittää kutsujalle muuttamatta sitä tavalliseksi virheeksi.
            }
        }

    private fun linkedCounterDao(
        projectId: Long,
        linkedCount: Int,
        unlinkedCount: Int,
        repeatSectionLinked: Boolean = false,
    ): ProjectCounterDao =
        mockk<ProjectCounterDao>(relaxed = true).also { dao ->
            every { dao.getCountersForProject(projectId) } returns
                flowOf(
                    listOf(
                        ProjectCounterEntity(
                            id = 20L,
                            projectId = projectId,
                            name = "Linked",
                            count = linkedCount,
                            linkedToMainCounter = true,
                        ),
                        ProjectCounterEntity(
                            id = 21L,
                            projectId = projectId,
                            name = "Manual",
                            count = unlinkedCount,
                            linkedToMainCounter = false,
                        ),
                        ProjectCounterEntity(
                            id = 22L,
                            projectId = projectId,
                            name = "Repeat section",
                            count = 0,
                            counterType = "REPEAT_SECTION",
                            linkedToMainCounter = repeatSectionLinked,
                        ),
                    ),
                )
        }

    private fun buildRepository(
        projectDao: CounterProjectDao,
        projectCounterDao: ProjectCounterDao,
        transactionRunner: DatabaseTransactionRunner = ImmediateRecordingTransactionRunner,
        projectDocumentRepository: ProjectDocumentRepository = emptyProjectDocumentRepository(),
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            projectCounterDao = projectCounterDao,
            sessionDao = mockk<SessionDao>(relaxed = true),
            photoStorage = mockk<ProgressPhotoStorage>(relaxed = true),
            patternDocumentStorage = mockk<PatternDocumentStorage>(relaxed = true),
            context = mockk<Context>(relaxed = true),
            yarnCardRepository = mockk(relaxed = true),
            savedPatternRepository = mockk(relaxed = true),
            projectDocumentRepository = projectDocumentRepository,
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
        )

    private fun emptyProjectDocumentRepository(): ProjectDocumentRepository =
        mockk<ProjectDocumentRepository>(relaxed = true).also { repository ->
            coEvery { repository.getActiveDocument(any()) } returns null
        }

    private fun projectDocument(
        currentPage: Int = 0,
        rowMapping: String? = null,
        readingLineYFraction: Float = 0.5f,
        readingLineFollowCurrentRow: Boolean = true,
    ): ProjectDocument =
        ProjectDocument(
            id = 21L,
            projectId = 7L,
            savedPatternId = null,
            documentKey = "import:21",
            label = "Pattern",
            localPdfUri = "content://pattern",
            sortOrder = 0,
            isPrimary = true,
            currentPage = currentPage,
            rowMapping = rowMapping,
            readingLineEnabled = false,
            readingLineYFraction = readingLineYFraction,
            readingLineFollowCurrentRow = readingLineFollowCurrentRow,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private object ImmediateRecordingTransactionRunner : DatabaseTransactionRunner {
        // CPD-ON
        override suspend fun <T> run(block: suspend () -> T): T = block()
    }

    private class RecordingTransactionRunner : DatabaseTransactionRunner {
        var runCount: Int = 0

        override suspend fun <T> run(block: suspend () -> T): T {
            runCount += 1
            return block()
        }
    }
}
