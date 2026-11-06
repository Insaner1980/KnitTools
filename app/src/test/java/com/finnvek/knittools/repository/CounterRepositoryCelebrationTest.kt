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
        coEvery {
            projectDao.updateCounterStateWithHistory(
                7L,
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers
            {
                project = project.copy(count = secondArg())
                history +=
                    CounterHistoryEntity(
                        id = history.size + 1L,
                        projectId = 7L,
                        action = arg(3),
                        previousValue = arg(4),
                        newValue = secondArg(),
                    )
            }
        coEvery { projectDao.getLatestHistory(7L) } answers { history.lastOrNull() }
        coEvery { projectDao.deleteHistoryById(any()) } answers { history.removeAll { it.id == firstArg<Long>() } }
        coEvery { projectDao.updateCount(7L, any(), any()) } answers { project = project.copy(count = secondArg()) }
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
    fun `crossing uses committed before after and target with exact and overshooting steps`() =
        runTest {
            listOf(Triple(9, 10, 1), Triple(8, 10, 4), Triple(1, 50, 99)).forEach { (before, target, step) ->
                project = project.copy(count = before, targetRows = target, stepSize = step)
                val result =
                    requireNotNull(repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment))
                assertEquals(before, result.beforeCount)
                assertEquals(before + step, result.afterCount)
                assertEquals(target, result.targetRows)
                assertEquals(project.count, result.afterCount)
                assertTrue(result.crossedTarget)
            }
            assertTrue(repository.applyWidgetCountChange(7L, increment = false))
        }

    @Test
    fun `decrement can cross again but an undo that restores the target never celebrates`() =
        runTest {
            assertTrue(
                requireNotNull(
                    repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment),
                ).crossedTarget,
            )
            assertFalse(
                requireNotNull(
                    repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Decrement),
                ).crossedTarget,
            )
            val undo = requireNotNull(repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Undo))
            assertEquals(9, undo.beforeCount)
            assertEquals(10, undo.afterCount)
            assertFalse(undo.crossedTarget)
            repository.applyMainCounterChange(7L, MainCounterChange.Decrement)
            assertTrue(
                requireNotNull(
                    repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment),
                ).crossedTarget,
            )
            project = project.copy(targetRows = null)
            assertFalse(
                requireNotNull(
                    repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment),
                ).crossedTarget,
            )
        }

    @Test
    fun `missing completed unchanged and history free changes produce no committed result`() =
        runTest {
            assertNull(repository.applyMainCounterChangeWithResult(99L, MainCounterChange.Increment))
            assertNull(repository.applyMainCounterChangeWithResult(9L, MainCounterChange.Increment))
            assertNull(repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Undo))
            project = project.copy(count = 0)
            assertNull(repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Decrement))
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Reset))
            project = project.copy(count = Int.MAX_VALUE)
            assertNull(repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment))
            assertTrue(history.isEmpty())
        }

    @Test
    fun `failed counter commit publishes no result and rolls back count and history`() =
        runTest {
            failCommit = true
            val result = runCatching { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) }
            assertTrue(result.isFailure)
            assertEquals(9, project.count)
            assertTrue(history.isEmpty())
        }

    @Test
    fun `empty stop does not consume the first actual saved session`() =
        runTest {
            val empty = repository.startSession(7L) as StartSessionResult.Started
            assertEquals(StopSessionResult.Saved(null, false), stopReviewedSession(empty.session.sessionToken))
            assertTrue(completed.isEmpty())
            val first = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            assertEquals(StopSessionResult.Saved(1L, true), stopReviewedSession(first.session.sessionToken))
            val second = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            assertEquals(StopSessionResult.Saved(2L, false), stopReviewedSession(second.session.sessionToken))
        }

    @Test
    fun `a saved session in another project prevents global firstness`() =
        runTest {
            completed += savedSession(projectId = 8L, minutes = 1)
            val current = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(60L)
            assertEquals(StopSessionResult.Saved(2L, false), stopReviewedSession(current.session.sessionToken))
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
