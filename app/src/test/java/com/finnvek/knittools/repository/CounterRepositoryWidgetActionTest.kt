package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterRepositoryWidgetActionTest {
    @Test
    fun `widget count change reads current project state inside transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val dao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { dao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Sukat",
                    count = 10,
                    stepSize = 5,
                    stitchTrackingEnabled = true,
                    currentStitch = 12,
                )
            val repository = buildRepository(dao, runner)

            val changed = repository.applyWidgetCountChange(7L, increment = true)

            assertTrue(changed)
            assertEquals(1, runner.runCount)
            coVerify {
                dao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = 15,
                    stepSize = 5,
                    action = "increment",
                    previousValue = 10,
                    newValue = 15,
                    updatedAt = any(),
                )
                dao.updateCurrentStitch(7L, 0, any())
            }
        }

    @Test
    fun `widget count change leaves inactive and unchanged projects untouched`() =
        runTest {
            val dao = mockk<CounterProjectDao>(relaxed = true)
            val repository = buildRepository(dao)

            coEvery { dao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Sukat", count = 0, stepSize = 5)
            assertFalse(repository.applyWidgetCountChange(7L, increment = false))

            coEvery { dao.getProject(8L) } returns
                CounterProjectEntity(id = 8L, name = "Valmis", count = 3, isCompleted = true)
            assertFalse(repository.applyWidgetCountChange(8L, increment = true))

            coVerify(exactly = 0) {
                dao.updateCounterStateWithHistory(any(), any(), any(), any(), any(), any(), any())
                dao.updateCurrentStitch(any(), any(), any())
            }
        }

    private fun buildRepository(
        dao: CounterProjectDao,
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): CounterRepository =
        CounterRepository(
            dao = dao,
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

    private class RecordingTransactionRunner : DatabaseTransactionRunner {
        var runCount: Int = 0

        override suspend fun <T> run(block: suspend () -> T): T {
            runCount += 1
            return block()
        }
    }
}
