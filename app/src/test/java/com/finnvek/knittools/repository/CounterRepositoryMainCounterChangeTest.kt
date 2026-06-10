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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun linkedCounterDao(
        projectId: Long,
        linkedCount: Int,
        unlinkedCount: Int,
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
                            linkedToMainCounter = false,
                        ),
                    ),
                )
        }

    private fun buildRepository(
        projectDao: CounterProjectDao,
        projectCounterDao: ProjectCounterDao,
        transactionRunner: DatabaseTransactionRunner = ImmediateRecordingTransactionRunner,
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            projectCounterDao = projectCounterDao,
            sessionDao = mockk<SessionDao>(relaxed = true),
            photoStorage = mockk<ProgressPhotoStorage>(relaxed = true),
            patternDocumentStorage = mockk<PatternDocumentStorage>(relaxed = true),
            context = mockk<Context>(relaxed = true),
            yarnCardRepository = mockk(relaxed = true),
            savedPatternRepository = mockk(relaxed = true),
            patternAnnotationRepository = mockk(relaxed = true),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
        )

    private object ImmediateRecordingTransactionRunner : DatabaseTransactionRunner {
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
