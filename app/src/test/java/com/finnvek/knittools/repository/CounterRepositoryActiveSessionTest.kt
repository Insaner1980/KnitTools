package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.calculator.ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CounterRepositoryActiveSessionTest {
    private val projectDao = mockk<CounterProjectDao>(relaxed = true)
    private val sessionDao = mockk<SessionDao>(relaxed = true)
    private val timeSource = FakeSessionTimeSource()
    private var active: ActiveSessionEntity? = null
    private val completed = mutableListOf<SessionEntity>()
    private var failActiveDelete = false
    private lateinit var repository: CounterRepository

    @Before
    fun setUp() {
        active = null
        completed.clear()
        failActiveDelete = false
        coEvery { projectDao.getProject(7L) } returns
            CounterProjectEntity(id = 7L, name = "Cardigan", count = 12)
        coEvery { projectDao.getProject(8L) } returns
            CounterProjectEntity(id = 8L, name = "Socks", count = 3)
        coEvery { projectDao.getProject(9L) } returns
            CounterProjectEntity(id = 9L, name = "Finished", count = 20, isCompleted = true)
        coEvery { projectDao.getProject(99L) } returns null
        coEvery { sessionDao.getActiveSession() } answers { active }
        coEvery { sessionDao.insertActiveSession(any()) } answers { active = firstArg() }
        coEvery { sessionDao.updateActiveSession(any()) } answers {
            active = firstArg()
            1
        }
        coEvery { sessionDao.deleteActiveSession(any()) } answers {
            if (failActiveDelete) error("forced active delete failure")
            if (active?.sessionToken == firstArg<String>()) {
                active = null
                1
            } else {
                0
            }
        }
        coEvery { sessionDao.insert(any()) } answers {
            completed += firstArg<SessionEntity>()
            completed.size.toLong()
        }
        repository = buildRepository(mockk<ProjectCounterDao>(relaxed = true))
    }

    // CPD-OFF: Istuntotestien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `explicit start and stop persist one coherent completed session`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 65L)

            val stopped = stopReviewedSession(started.session.sessionToken)

            assertEquals(StopSessionResult.Saved(1L), stopped)
            assertEquals(null, active)
            assertEquals(65L, completed.single().durationSeconds)
            assertEquals(1_000L, completed.single().startedAt)
            assertEquals(66_000L, completed.single().endedAt)
            coVerify(exactly = 1) { sessionDao.insert(any()) }
            coVerify(exactly = 1) { sessionDao.deleteActiveSession(started.session.sessionToken) }
        }

    @Test
    fun `singleton start is idempotent for same project and conflicts for another`() =
        runTest {
            val first = repository.startSession(7L) as StartSessionResult.Started
            val same = repository.startSession(7L) as StartSessionResult.AlreadyActive
            val other = repository.startSession(8L) as StartSessionResult.ProjectConflict

            assertEquals(first.session.sessionToken, same.session.sessionToken)
            assertEquals(first.session.sessionToken, other.activeSession.sessionToken)
            assertEquals(8L, other.requestedProjectId)
            coVerify(exactly = 1) { sessionDao.insertActiveSession(any()) }
        }

    @Test
    fun `start distinguishes missing and completed projects`() =
        runTest {
            assertEquals(StartSessionResult.ProjectMissing, repository.startSession(99L))
            assertEquals(StartSessionResult.ProjectCompleted, repository.startSession(9L))
            assertEquals(null, active)
        }

    @Test
    fun `repeated completion leaves the original completion state untouched`() =
        runTest {
            assertEquals(
                ProjectCompletionResult.Completed,
                repository.completeProjectWithSessionChoice(
                    projectId = 9L,
                    choice = null,
                    completedAtMillis = 9_999L,
                ),
            )

            coVerify(exactly = 0) { projectDao.archiveProject(any(), any(), any(), any()) }
            coVerify(exactly = 0) { sessionDao.getActiveSession() }
        }

    @Test
    fun `completion requires a choice and then discards or saves the active session`() =
        runTest {
            val discardedSession = repository.startSession(7L) as StartSessionResult.Started

            assertEquals(
                ProjectCompletionResult.NeedsActiveSessionChoice(discardedSession.session),
                repository.completeProjectWithSessionChoice(7L, choice = null),
            )
            assertEquals(
                ProjectCompletionResult.Completed,
                repository.completeProjectWithSessionChoice(
                    7L,
                    choice = ActiveSessionCompletionChoice.DISCARD,
                ),
            )
            assertTrue(completed.isEmpty())

            val savedSession = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 60L)

            assertEquals(
                ProjectCompletionResult.Completed,
                repository.completeProjectWithSessionChoice(
                    7L,
                    choice = ActiveSessionCompletionChoice.SAVE,
                ),
            )
            assertEquals(60L, completed.single().durationSeconds)
            coVerify(exactly = 1) { sessionDao.deleteActiveSession(discardedSession.session.sessionToken) }
            coVerify(exactly = 1) { sessionDao.deleteActiveSession(savedSession.session.sessionToken) }
            coVerify(exactly = 2) { projectDao.archiveProject(7L, 12, any(), any()) }
        }

    @Test
    fun `completion requires recovery review before saving the active session`() =
        runTest {
            repository.startSession(7L)
            timeSource.reboot()
            val recovery = requireNotNull(repository.refreshActiveSession())

            assertEquals(
                ProjectCompletionResult.NeedsRecoveryReview(recovery),
                repository.completeProjectWithSessionChoice(
                    7L,
                    choice = ActiveSessionCompletionChoice.SAVE,
                ),
            )
            coVerify(exactly = 0) { projectDao.archiveProject(any(), any(), any(), any()) }
        }

    @Test
    fun `completion leaves another project's active session untouched`() =
        runTest {
            val otherSession = repository.startSession(8L) as StartSessionResult.Started

            assertEquals(
                ProjectCompletionResult.Completed,
                repository.completeProjectWithSessionChoice(7L, choice = null),
            )
            assertEquals(otherSession.session.sessionToken, active?.sessionToken)
            coVerify(exactly = 1) { projectDao.archiveProject(7L, 12, any(), any()) }
        }

    @Test
    fun `reopening updates only a completed project`() =
        runTest {
            repository.reactivateProject(7L)
            repository.reactivateProject(9L)

            coVerify(exactly = 0) { projectDao.reactivateProject(7L, any()) }
            coVerify(exactly = 1) { projectDao.reactivateProject(9L, any()) }
        }

    @Test
    fun `stop token is idempotent and discard creates no history`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started

            assertEquals(StopSessionResult.StaleAction, stopReviewedSession("stale"))
            assertEquals(StopSessionResult.Discarded, repository.discardActiveSession(started.session.sessionToken))
            assertEquals(
                StopSessionResult.NoActiveSession,
                repository.discardActiveSession(started.session.sessionToken),
            )
            assertTrue(completed.isEmpty())
        }

    @Test
    fun `failed final clear rolls back completed insert and leaves active row recoverable`() =
        runTest {
            repository = buildRepository(repositoryProjectCounterDao(), SnapshotTransactionRunner())
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 60L)
            failActiveDelete = true

            val failed = stopReviewedSession(started.session.sessionToken)

            assertEquals(StopSessionResult.PersistenceFailure, failed)
            assertEquals(started.session.sessionToken, active?.sessionToken)
            assertTrue(completed.isEmpty())
        }

    @Test
    fun `reboot creates one stable recovery interval and discard keeps only trusted checkpoint`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 30L)
            repository.checkpointActiveSession()
            timeSource.advance(seconds = 20L)
            timeSource.reboot()

            val firstRecovery = requireNotNull(repository.refreshActiveSession())
            val secondRecovery = requireNotNull(repository.refreshActiveSession())
            val intervalToken = requireNotNull(firstRecovery.recoveryIntervalToken)

            assertEquals(ActiveSessionRecoveryReason.REBOOTED, firstRecovery.recoveryReason)
            assertEquals(intervalToken, secondRecovery.recoveryIntervalToken)
            assertNotEquals(started.session.sessionToken, intervalToken)

            val result =
                repository.discardRecoveryInterval(
                    sessionToken = started.session.sessionToken,
                    recoveryIntervalToken = intervalToken,
                )

            assertEquals(RecoveryResolutionResult.DiscardedAndStopped(1L), result)
            assertEquals(30L, completed.single().durationSeconds)
            assertEquals(null, active)
        }

    @Test
    fun `adding reviewed recovery time resets anchors and continues without immediate repeat`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 90L)
            timeSource.reboot()
            val recovery = requireNotNull(repository.refreshActiveSession())

            val result =
                repository.addRecoveryInterval(
                    sessionToken = started.session.sessionToken,
                    recoveryIntervalToken = requireNotNull(recovery.recoveryIntervalToken),
                    durationSeconds = 90L,
                ) as RecoveryResolutionResult.Continued

            assertEquals(90L, result.session.timingAnchors.checkpointedDurationSeconds)
            assertEquals(90L, result.session.timingAnchors.reviewedDurationBaselineSeconds)
            assertTrue(!result.session.needsRecoveryReview)
            assertTrue(!requireNotNull(repository.refreshActiveSession()).needsRecoveryReview)
        }

    @Test
    fun `long review spans automatic checkpoints and moves unreviewed rows to pending`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
            }
            repository = buildRepository(repositoryProjectCounterDao())
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 10L * 60L * 60L)
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            timeSource.advance(seconds = 14L * 60L * 60L)

            val recovery = requireNotNull(repository.refreshActiveSession())

            assertEquals(ActiveSessionRecoveryReason.LONG_RUNNING, recovery.recoveryReason)
            assertEquals(0, recovery.trustedRowsWorked)
            assertEquals(1, recovery.pendingRowsWorked)
            assertEquals(0L, recovery.timingAnchors.checkpointedDurationSeconds)
            assertEquals(ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS, recovery.recoverySuggestedDurationSeconds)

            val continued =
                repository.addRecoveryInterval(
                    sessionToken = started.session.sessionToken,
                    recoveryIntervalToken = requireNotNull(recovery.recoveryIntervalToken),
                    durationSeconds = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS,
                ) as RecoveryResolutionResult.Continued
            assertEquals(1, continued.session.trustedRowsWorked)
            assertEquals(0, continued.session.pendingRowsWorked)
            assertEquals(
                ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS,
                continued.session.timingAnchors.checkpointedDurationSeconds,
            )
        }

    @Test
    fun `edited recovery duration finalizes once with pending rows and coherent timestamps`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
            }
            repository = buildRepository(repositoryProjectCounterDao())
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 30L)
            repository.checkpointActiveSession()
            timeSource.reboot()
            val recovery = requireNotNull(repository.refreshActiveSession())
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            val intervalToken = requireNotNull(recovery.recoveryIntervalToken)

            val first =
                repository.editRecoveryDurationAndStop(
                    sessionToken = started.session.sessionToken,
                    recoveryIntervalToken = intervalToken,
                    totalDurationSeconds = 75L,
                )
            val repeated =
                repository.editRecoveryDurationAndStop(
                    sessionToken = started.session.sessionToken,
                    recoveryIntervalToken = intervalToken,
                    totalDurationSeconds = 75L,
                )

            assertEquals(RecoveryResolutionResult.EditedAndStopped(1L), first)
            assertEquals(RecoveryResolutionResult.StaleAction, repeated)
            assertEquals(75L, completed.single().durationSeconds)
            assertEquals(76_000L, completed.single().endedAt)
            assertEquals(13, completed.single().endRow)
            assertEquals(1, completed.single().rowsWorked)
        }

    @Test
    fun `recovery prompt dismissal keeps the interval and is token guarded`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.reboot()
            val recovery = requireNotNull(repository.refreshActiveSession())
            val intervalToken = requireNotNull(recovery.recoveryIntervalToken)

            assertTrue(repository.markRecoveryPromptShown(started.session.sessionToken, intervalToken))
            assertTrue(requireNotNull(active).recoveryPromptShown)
            assertTrue(!repository.markRecoveryPromptShown(started.session.sessionToken, "stale"))
            assertEquals(intervalToken, active?.recoveryIntervalToken)
        }

    @Test
    fun `counter changes checkpoint trusted duration and net row progress atomically`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
            }
            val counterDao = repositoryProjectCounterDao()
            repository = buildRepository(counterDao)
            repository.startSession(7L)
            timeSource.advance(seconds = 10L)

            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            timeSource.advance(seconds = 5L)
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Decrement)

            assertEquals(15L, active?.checkpointedDurationSeconds)
            assertEquals(0, active?.trustedRowsWorked)
            assertEquals(12, active?.lastObservedRow)
        }

    @Test
    fun `half second counter checkpoints preserve elapsed duration`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
            }
            repository = buildRepository(repositoryProjectCounterDao())
            val started = repository.startSession(7L) as StartSessionResult.Started

            repeat(10) {
                timeSource.advanceMillis(500L)
                repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            }
            stopReviewedSession(started.session.sessionToken)

            assertEquals(5L, completed.single().durationSeconds)
            assertEquals(10, completed.single().rowsWorked)
        }

    @Test
    fun `saving a reviewed stop keeps the reviewed duration`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 10L)
            val reviewed = requireNotNull(repository.refreshActiveSession())
            val reviewedDuration = repository.activeSessionDurationSeconds(reviewed)
            timeSource.advance(seconds = 50L)

            val stopped =
                repository.stopSession(
                    sessionToken = started.session.sessionToken,
                    reviewedDurationSeconds = reviewedDuration,
                    reviewedRowsWorked = reviewed.trustedRowsWorked,
                    reviewedEndRow = reviewed.trustedLastObservedRow,
                )

            assertEquals(StopSessionResult.Saved(1L), stopped)
            assertEquals(10L, completed.single().durationSeconds)
            assertEquals(11_000L, completed.single().endedAt)
        }

    @Test
    fun `fractional checkpoint time remains in the wall clock recovery interval`() =
        runTest {
            repository.startSession(7L)
            timeSource.advanceMillis(1_500L)
            repository.checkpointActiveSession()
            timeSource.advanceMillis(500L)
            timeSource.reboot()

            val recovery = requireNotNull(repository.refreshActiveSession())

            assertEquals(1L, recovery.timingAnchors.checkpointedDurationSeconds)
            assertEquals(1L, recovery.recoverySuggestedDurationSeconds)
        }

    @Test
    fun `undo reset restores net active row progress without counting restored counter value`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery { projectDao.updateCount(7L, any(), any()) } answers {
                project = project.copy(count = secondArg())
            }
            val history = mutableListOf<CounterHistoryEntity>()
            coEvery { projectDao.getLatestHistory(7L) } answers { history.lastOrNull() }
            coEvery { projectDao.deleteHistoryById(any()) } answers {
                history.removeAt(history.lastIndex)
            }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
                history +=
                    CounterHistoryEntity(
                        id = history.size.toLong() + 1L,
                        projectId = 7L,
                        action = arg(3),
                        previousValue = arg(4),
                        newValue = arg(5),
                    )
            }
            repository = buildRepository(repositoryProjectCounterDao())
            repository.startSession(7L)

            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Reset)
            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Undo)

            assertEquals(14, project.count)
            assertEquals(2, active?.trustedRowsWorked)
            assertEquals(2, active?.unreviewedRowsWorked)
            assertEquals(14, active?.lastObservedRow)
        }

    @Test
    fun `counter changes during recovery remain pending until the interval is reviewed`() =
        runTest {
            var project = CounterProjectEntity(id = 7L, name = "Cardigan", count = 12, stepSize = 1)
            coEvery { projectDao.getProject(7L) } answers { project }
            coEvery {
                projectDao.updateCounterStateWithHistory(
                    projectId = 7L,
                    count = any(),
                    stepSize = any(),
                    action = any(),
                    previousValue = any(),
                    newValue = any(),
                    updatedAt = any(),
                )
            } answers {
                project = project.copy(count = secondArg())
            }
            repository = buildRepository(repositoryProjectCounterDao())
            repository.startSession(7L)
            timeSource.advance(seconds = 20L)
            timeSource.reboot()
            repository.refreshActiveSession()

            repository.applyMainCounterChange(7L, com.finnvek.knittools.domain.model.MainCounterChange.Increment)

            assertEquals(0, active?.trustedRowsWorked)
            assertEquals(1, active?.pendingRowsWorked)
        }

    // CPD-ON

    private suspend fun stopReviewedSession(sessionToken: String): StopSessionResult {
        val reviewed = requireNotNull(repository.refreshActiveSession())
        return repository.stopSession(
            sessionToken = sessionToken,
            reviewedDurationSeconds = repository.activeSessionDurationSeconds(reviewed),
            reviewedRowsWorked = reviewed.trustedRowsWorked,
            reviewedEndRow = reviewed.trustedLastObservedRow,
        )
    }

    private fun repositoryProjectCounterDao(): ProjectCounterDao =
        mockk<ProjectCounterDao>(relaxed = true) {
            coEvery { getCountersForProject(any()) } returns flowOf(emptyList())
        }

    // CPD-OFF: Testin repository-kooste pidetaan istuntofixturen yhteydessa.
    private fun buildRepository(
        counterDao: ProjectCounterDao,
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            projectCounterDao = counterDao,
            sessionDao = sessionDao,
            photoStorage = mockk<ProgressPhotoStorage>(relaxed = true),
            patternDocumentStorage = mockk<PatternDocumentStorage>(relaxed = true),
            context = mockk<Context>(relaxed = true),
            yarnCardRepository = mockk(relaxed = true),
            savedPatternRepository = mockk(relaxed = true),
            projectDocumentRepository = mockk(relaxed = true),
            projectFolderDao = mockk(relaxed = true),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
            sessionTimeSource = timeSource,
        )
    // CPD-ON

    private inner class SnapshotTransactionRunner : DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val activeBefore = active
            val completedBefore = completed.toList()
            return try {
                block()
            } catch (failure: Exception) {
                active = activeBefore
                completed.clear()
                completed.addAll(completedBefore)
                throw failure
            }
        }
    }

    private class FakeSessionTimeSource : SessionTimeSource {
        private var wallMillis = 1_000L
        private var elapsedMillis = 5_000L
        private var bootCount = 4L

        override fun snapshot() =
            SessionTimeSnapshot(
                wallClockMillis = wallMillis,
                elapsedRealtimeMillis = elapsedMillis,
                bootCount = bootCount,
                zoneId = "Europe/Helsinki",
            )

        fun advance(seconds: Long) = advanceMillis(seconds * 1_000L)

        fun advanceMillis(milliseconds: Long) {
            wallMillis += milliseconds
            elapsedMillis += milliseconds
        }

        fun reboot() {
            bootCount += 1L
            elapsedMillis = 500L
        }
    }
}
