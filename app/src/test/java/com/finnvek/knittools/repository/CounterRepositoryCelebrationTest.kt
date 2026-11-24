package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCompletionEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CounterRepositoryCelebrationTest : CounterRepositoryActiveSessionFixture() {
    private var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 9, targetRows = 10, createdAt = 100L)
    private val history = mutableListOf<CounterHistoryEntity>()
    private val completionEvents = mutableListOf<ProjectCompletionEntity>()
    private var failCommit = false
    private var insideTransaction = false

    @Before
    fun prepareCelebrationRows() {
        coEvery { projectDao.getProject(7L) } answers { project }
        projectDao.recordCounterHistory(7L, history) { project = project.copy(count = it) }
        coEvery { projectDao.archiveProject(7L, any(), any(), any()) } answers {
            project =
                project.copy(isCompleted = true, totalRows = secondArg(), completedAt = thirdArg(), updatedAt = arg(3))
        }
        coEvery { projectDao.insertCompletion(any()) } answers {
            val id = completionEvents.size + 1L
            completionEvents += firstArg<ProjectCompletionEntity>().copy(id = id)
            id
        }
        coEvery { projectDao.reactivateProject(7L, any()) } answers {
            project = project.copy(isCompleted = false, completedAt = null, totalRows = null)
        }
        coEvery { sessionDao.countCompletedSessions() } answers {
            check(insideTransaction)
            completed.size.toLong()
        }
        coEvery { sessionDao.getTotalMinutes(7L) } answers {
            check(insideTransaction)
            completed.filter { it.projectId == 7L }.sumOf { it.durationMinutes }
        }
        repository = buildRepository(repositoryProjectCounterDao(), CelebrationTransactionRunner())
    }

    @Test
    fun `missing completed unchanged and history free changes produce no committed result`() =
        runTest {
            assertFalse(repository.applyMainCounterChange(99L, MainCounterChange.Increment))
            assertFalse(repository.applyMainCounterChange(9L, MainCounterChange.Increment))
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Undo))
            project = project.copy(count = 0)
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Decrement))
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Reset))
            project = project.copy(count = Int.MAX_VALUE)
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Increment))
            assertTrue(history.isEmpty())
        }

    @Test
    fun `failed counter commit publishes no result and rolls back count and history`() =
        runTest {
            failCommit = true
            val result = runCatching { repository.applyMainCounterChange(7L, MainCounterChange.Increment) }
            assertTrue(result.isFailure)
            assertEquals(9, project.count)
            assertTrue(history.isEmpty())
        }

    @Test
    fun `empty stop does not insert a completed session`() =
        runTest {
            val empty = repository.startSession(7L) as StartSessionResult.Started
            assertEquals(StopSessionResult.Saved(null), stopReviewedSession(empty.session.sessionToken))
            assertTrue(completed.isEmpty())
            val first = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            assertEquals(StopSessionResult.Saved(1L), stopReviewedSession(first.session.sessionToken))
            val second = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            assertEquals(StopSessionResult.Saved(2L), stopReviewedSession(second.session.sessionToken))
        }

    @Test
    fun `new completion returns immutable final facts including the session saved in its transaction`() =
        runTest {
            completed += savedSession(projectId = 7L, minutes = 2)
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(65L)
            val result =
                repository.completeProjectWithSessionChoice(
                    7L,
                    ActiveSessionCompletionChoice.SAVE,
                    expectedSession = started.session,
                ) as ProjectCompletionResult.CompletedNow
            val snapshot = result.snapshot
            assertEquals(completionEvents.single().id, snapshot.eventId)
            assertEquals(4, snapshot.totalSessionMinutes)
            assertEquals(9, snapshot.project.count)
            assertEquals(9, snapshot.project.totalRows)
            assertEquals(100L, snapshot.project.createdAt)
            assertEquals(66_000L, snapshot.project.completedAt)
            assertTrue(snapshot.project.isCompleted)
            assertNull(active)
            assertEquals(ProjectCompletionResult.Completed, repository.completeProjectWithSessionChoice(7L, null))
            assertEquals(1, completionEvents.size)
            repository.reactivateProject(7L, snapshot.project.completedAt)
            project = project.copy(count = 15)
            val again = repository.completeProjectWithSessionChoice(7L, null) as ProjectCompletionResult.CompletedNow
            assertNotEquals(snapshot.eventId, again.snapshot.eventId)
            assertEquals(15, again.snapshot.project.count)
            assertEquals(9, snapshot.project.count)
        }

    @Test
    fun `failed completion rolls back its event project and saved active session`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            failCommit = true
            val result =
                repository.completeProjectWithSessionChoice(
                    7L,
                    ActiveSessionCompletionChoice.SAVE,
                    expectedSession = started.session,
                )
            assertEquals(ProjectCompletionResult.PersistenceFailure, result)
            assertFalse(project.isCompleted)
            assertNull(project.completedAt)
            assertTrue(completionEvents.isEmpty())
            assertTrue(completed.isEmpty())
            assertEquals(started.session.sessionToken, active?.sessionToken)
        }

    private fun savedSession(
        projectId: Long,
        minutes: Int,
    ) = SessionEntity(
        projectId = projectId,
        startedAt = 0L,
        endedAt = minutes * 60_000L,
        durationMinutes = minutes,
        startRow = 0,
        endRow = 0,
    )

    private inner class CelebrationTransactionRunner : DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val projectBefore = project
            val historyBefore = history.toList()
            val eventsBefore = completionEvents.toList()
            insideTransaction = true
            return try {
                SnapshotTransactionRunner().run {
                    val result = block()
                    check(!failCommit) { "forced commit failure" }
                    result
                }
            } catch (failure: Exception) {
                project = projectBefore
                history.clear()
                history.addAll(historyBefore)
                completionEvents.clear()
                completionEvents.addAll(eventsBefore)
                throw failure
            } finally {
                insideTransaction = false
            }
        }
    }
}
