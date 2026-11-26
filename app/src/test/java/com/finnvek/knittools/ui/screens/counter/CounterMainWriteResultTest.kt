package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.repository.RecoveryResolutionResult
import com.finnvek.knittools.repository.StopSessionResult
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterMainWriteResultTest : CounterViewModelFixture() {
    @Test
    fun `current stop recovery result adopts its review and closes its summary`() =
        runTest {
            val original = activeSession().copy(projectId = 7L)
            val recovery =
                original.copy(
                    recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                    recoveryIntervalToken = "current-interval",
                )
            every { repository.observeActiveSession() } returns flowOf(original)
            coEvery { repository.refreshActiveSession() } returns original
            coEvery { repository.activeSessionDurationSeconds(any()) } returns 60L
            coEvery { repository.stopSession(original.sessionToken, 60L, 0, 0) } returns
                StopSessionResult.NeedsRecoveryReview(recovery)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.stopWorkSession()
            runCurrent()
            viewModel.saveStoppedWorkSession()
            advanceUntilIdle()

            assertEquals(recovery, viewModel.uiState.value.activeSession)
            assertEquals(null, viewModel.uiState.value.sessionStopSummary)
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
        }

    @Test
    fun `late stop recovery result preserves a newer observed session`() =
        runTest {
            val original = activeSession().copy(projectId = 7L)
            val active = MutableStateFlow(original)
            val newer =
                original.copy(
                    sessionToken = "restored-session",
                    recoveryReason = ActiveSessionRecoveryReason.BOOT_IDENTITY_UNAVAILABLE,
                    recoveryIntervalToken = "new-interval",
                )
            val (viewModel, reply) = beginPendingStop(active)
            active.value = newer
            runCurrent()
            reply.complete(
                StopSessionResult.NeedsRecoveryReview(
                    original.copy(
                        recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                        recoveryIntervalToken = "old-interval",
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(newer, viewModel.uiState.value.activeSession)
            assertEquals(null, viewModel.uiState.value.sessionStopSummary)
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
        }

    @Test
    fun `late stop rejection preserves a newly reviewed summary for the same session`() =
        runTest {
            val summaries = mutableListOf<Int?>()
            val prompts = mutableListOf<Boolean>()
            val original = activeSession().copy(projectId = 7L)
            val outcomes =
                listOf(
                    StopSessionResult.StaleAction,
                    StopSessionResult.NeedsRecoveryReview(
                        original.copy(
                            recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                            recoveryIntervalToken = "old-interval",
                        ),
                    ),
                )
            for (outcome in outcomes) {
                val active = MutableStateFlow(original)
                val (viewModel, reply) = beginPendingStop(active)
                active.value = original.copy(trustedRowsWorked = 1, trustedLastObservedRow = 1)
                runCurrent()
                viewModel.stopWorkSession()
                runCurrent()
                reply.complete(outcome)
                advanceUntilIdle()

                val summary = viewModel.uiState.value.sessionStopSummary
                summaries += summary?.rowsWorked
                prompts += viewModel.uiState.value.showSessionRecoveryPrompt
            }

            assertEquals(listOf(1, 1), summaries)
            assertEquals(listOf(false, false), prompts)
        }

    @Test
    fun `stale stop summary is refreshed after the widget changes the session rows`() =
        runTest {
            val original = activeSession().copy(projectId = 7L)
            val active = MutableStateFlow(original)
            val (viewModel, reply) = beginPendingStop(active)
            active.value = original.copy(trustedRowsWorked = 1, trustedLastObservedRow = 1)
            runCurrent()
            reply.complete(StopSessionResult.StaleAction)
            advanceUntilIdle()

            val summary = viewModel.uiState.value.sessionStopSummary
            assertEquals(original.sessionToken, summary?.sessionToken)
            assertEquals(1, summary?.rowsWorked)
            assertEquals(1, summary?.endRow)
        }

    @Test
    fun `project session total updates when history deletes one of two sessions`() =
        runTest {
            val sessions = MutableSharedFlow<Boolean>(replay = 1)
            sessions.emit(true)
            every { repository.observeHasSessionsForProject(7L) } returns sessions
            var totalMinutes = 50
            coEvery { repository.getTotalMinutesForProject(7L) } answers { totalMinutes }
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(50, viewModel.uiState.value.totalSessionMinutes)

            totalMinutes = 30
            sessions.emit(true)
            advanceUntilIdle()

            assertEquals(30, viewModel.uiState.value.totalSessionMinutes)
        }

    @Test
    fun `editing a linked yarn refreshes its project summary without changing the project`() =
        runTest {
            val project = observedProject.value.copy(yarnCardIds = "31")
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            val yarn = YarnCard(id = 31L, yarnName = "Original", colorName = "Red", linkedProjectId = 7L)
            val cards = MutableStateFlow(listOf(yarn))
            val yarnRepository = mockk<YarnCardRepository>()
            coEvery { yarnRepository.getCards(listOf(31L)) } answers { cards.value }
            val viewModel = viewModel(yarnRepositoryOverride = yarnRepository, savedYarnCardRows = cards)
            advanceUntilIdle()
            assertEquals(listOf(LinkedYarnSummary(31L, "Original", "Red")), viewModel.uiState.value.linkedYarns)

            cards.value = listOf(yarn.copy(yarnName = "Renamed", colorName = "Blue"))
            advanceUntilIdle()

            assertEquals(listOf(LinkedYarnSummary(31L, "Renamed", "Blue")), viewModel.uiState.value.linkedYarns)
        }

    @Test
    fun `recovery retries retain reviewed tokens after the active session is replaced`() =
        runTest {
            val submittedTargets = mutableListOf<List<String>>()
            for (operation in listOf("add", "edit", "discard")) {
                val original =
                    recoverySession()
                val active = MutableStateFlow(original)
                val requests = mutableListOf<String>()
                observeSession(active)
                coEvery { repository.addRecoveryInterval(any(), any(), 60L) } coAnswers {
                    requests += "${firstArg<String>()}/${secondArg<String>()}"
                    RecoveryResolutionResult.PersistenceFailure
                }
                coEvery { repository.editRecoveryDurationAndStop(any(), any(), 60L) } coAnswers {
                    requests += "${firstArg<String>()}/${secondArg<String>()}"
                    RecoveryResolutionResult.PersistenceFailure
                }
                coEvery { repository.discardRecoveryInterval(any(), any()) } coAnswers {
                    requests += "${firstArg<String>()}/${secondArg<String>()}"
                    RecoveryResolutionResult.PersistenceFailure
                }
                val viewModel = viewModel()
                advanceUntilIdle()

                when (operation) {
                    "add" -> viewModel.addRecoveryInterval(60L)
                    "edit" -> viewModel.editRecoveryDurationAndStop(60L)
                    else -> viewModel.discardRecoveryInterval()
                }
                advanceUntilIdle()
                active.value = original.copy(sessionToken = "restored-session", recoveryIntervalToken = "new-interval")
                runCurrent()
                viewModel.retryWorkSessionAction()
                advanceUntilIdle()
                submittedTargets += requests.toList()
            }

            assertEquals(List(3) { listOf("existing/old-interval", "existing/old-interval") }, submittedTargets)
        }

    @Test
    fun `late discarded stop preserves a newer session summary`() =
        runTest {
            val original = activeSession().copy(projectId = 7L)
            val active = MutableStateFlow(original)
            observeSession(active)
            coEvery { repository.activeSessionDurationSeconds(any()) } returns 60L
            val reply = CompletableDeferred<StopSessionResult>()
            coEvery { repository.discardActiveSession(original.sessionToken) } coAnswers { reply.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.stopWorkSession()
            runCurrent()
            viewModel.discardStoppedWorkSession()
            runCurrent()
            // A replacement can be observed before the committed operation's UI continuation resumes.
            active.value = original.copy(sessionToken = "new-session")
            runCurrent()
            viewModel.stopWorkSession()
            runCurrent()
            reply.complete(StopSessionResult.Discarded)
            advanceUntilIdle()

            val summary = viewModel.uiState.value.sessionStopSummary
            assertEquals("new-session", summary?.sessionToken)
        }

    @Test
    fun `late recovery finalization preserves a newer interval prompt`() =
        runTest {
            for (edit in listOf(true, false)) {
                val original =
                    recoverySession()
                val active = MutableStateFlow(original)
                observeSession(active)
                coEvery { repository.getTotalMinutesForProject(7L) } returns 0
                coEvery { repository.editRecoveryDurationAndStop(original.sessionToken, "old-interval", 60L) } returns
                    RecoveryResolutionResult.EditedAndStopped(1L)
                coEvery { repository.discardRecoveryInterval(original.sessionToken, "old-interval") } returns
                    RecoveryResolutionResult.DiscardedAndStopped(1L)
                val viewModel = viewModel()
                advanceUntilIdle()
                val release = CompletableDeferred<Unit>()
                coEvery { repository.getTotalMinutesForProject(7L) } coAnswers {
                    release.await()
                    1
                }

                if (edit) viewModel.editRecoveryDurationAndStop(60L) else viewModel.discardRecoveryInterval()
                runCurrent()
                active.value = original.copy(sessionToken = "new-session", recoveryIntervalToken = "new-interval")
                runCurrent()
                release.complete(Unit)
                advanceUntilIdle()

                assertTrue("edit=$edit", viewModel.uiState.value.showSessionRecoveryPrompt)
                val observed = viewModel.uiState.value.activeSession
                assertEquals("new-interval", observed?.recoveryIntervalToken)
            }
        }

    @Test
    fun `late accepted recovery add preserves a newer interval prompt`() =
        runTest {
            val original =
                recoverySession()
            val active = MutableStateFlow(original)
            observeSession(active)
            val reply = CompletableDeferred<RecoveryResolutionResult>()
            coEvery { repository.addRecoveryInterval(original.sessionToken, "old-interval", 60L) } coAnswers {
                reply.await()
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.addRecoveryInterval(60L)
            runCurrent()
            // Deliver the old committed result after a newer recovery interval has been observed.
            active.value = original.copy(recoveryIntervalToken = "new-interval")
            runCurrent()
            reply.complete(RecoveryResolutionResult.Continued(original.copy(recoveryReason = null)))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            val observed = viewModel.uiState.value.activeSession
            assertEquals("new-interval", observed?.recoveryIntervalToken)
        }

    @Test
    fun `session total refresh failure does not repeat a committed stop`() =
        runTest {
            val active = activeSession().copy(projectId = 7L)
            every { repository.observeActiveSession() } returns flowOf(active)
            coEvery { repository.refreshActiveSession() } returns active
            coEvery { repository.activeSessionDurationSeconds(active) } returns 60L
            coEvery { repository.stopSession(active.sessionToken, 60L, 0, 0) } returns StopSessionResult.Saved(1L)
            val viewModel = viewModel()
            advanceUntilIdle()
            coEvery { repository.getTotalMinutesForProject(7L) } throws IOException("synthetic total read failure")

            viewModel.stopWorkSession()
            runCurrent()
            viewModel.saveStoppedWorkSession()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.sessionStopSummary)
            assertEquals(null, viewModel.uiState.value.workSessionErrorRes)
            viewModel.saveStoppedWorkSession()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.stopSession(active.sessionToken, 60L, 0, 0) }
        }

    @Test
    fun `late initial total read cannot replace the total after a committed stop`() =
        runTest {
            val active = activeSession().copy(projectId = 7L)
            observeSession(MutableStateFlow(active))
            coEvery { repository.activeSessionDurationSeconds(active) } returns 60L
            coEvery { repository.stopSession(active.sessionToken, 60L, 0, 0) } returns StopSessionResult.Saved(1L)
            val initialTotal = CompletableDeferred<Int>()
            var reads = 0
            val viewModel = viewModel()
            coEvery { repository.getTotalMinutesForProject(7L) } coAnswers {
                if (++reads == 1) initialTotal.await() else 11
            }
            runCurrent()
            assertEquals(1, reads)

            viewModel.stopWorkSession()
            runCurrent()
            viewModel.saveStoppedWorkSession()
            runCurrent()
            assertEquals(11, viewModel.uiState.value.totalSessionMinutes)

            initialTotal.complete(10)
            advanceUntilIdle()
            assertEquals(11, viewModel.uiState.value.totalSessionMinutes)
        }

    @Test
    fun `late saved stop preserves a newer session summary`() =
        runTest {
            val original = activeSession().copy(projectId = 7L)
            val active = MutableStateFlow(original)
            observeSession(active)
            coEvery { repository.activeSessionDurationSeconds(any()) } returns 60L
            coEvery { repository.stopSession(original.sessionToken, 60L, 0, 0) } returns StopSessionResult.Saved(1L)
            val viewModel = viewModel()
            advanceUntilIdle()
            val release = CompletableDeferred<Unit>()
            coEvery { repository.getTotalMinutesForProject(7L) } coAnswers {
                release.await()
                1
            }

            viewModel.stopWorkSession()
            runCurrent()
            viewModel.saveStoppedWorkSession()
            runCurrent()
            active.value = original.copy(sessionToken = "new-session")
            runCurrent()
            viewModel.stopWorkSession()
            runCurrent()
            release.complete(Unit)
            advanceUntilIdle()

            val summary = viewModel.uiState.value.sessionStopSummary
            assertEquals("new-session", summary?.sessionToken)
            coVerify(exactly = 1) { repository.stopSession(original.sessionToken, 60L, 0, 0) }
        }

    @Test
    fun `rejected count restores authoritative row instead of publishing optimistic value`() =
        runTest {
            val published = captureWidgetUpdates()
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } returns false
            coEvery { repository.getProject(7L) } returns observedProject.value
            val viewModel = viewModel()
            val errors = incrementAndCollectErrors(viewModel)

            assertEquals(0, viewModel.uiState.value.counter.count)
            assertTrue(published.none { it.count == 1 })
            assertEquals(1, errors.size)
        }

    @Test
    fun `count persistence exception restores visible count and reports failure`() =
        runTest {
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } throws
                IOException("synthetic write failure")
            coEvery { repository.getProject(7L) } returns observedProject.value
            val viewModel = viewModel()
            val errors = incrementAndCollectErrors(viewModel)

            assertEquals(0, viewModel.uiState.value.counter.count)
            assertEquals(1, errors.size)
        }

    @Test
    fun `successful count publishes the committed count after a competing writer`() =
        runTest {
            val published = captureWidgetUpdates()
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } returns true
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 6)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.increment()
            advanceUntilIdle()

            assertEquals(6, viewModel.uiState.value.counter.count)
            assertEquals(6, published.last().count)
        }

    @Test
    fun `widget refresh failure preserves committed count without repeating the change`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } returns true
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 1)
            coEvery { CounterWidgetState.syncAll(any(), any()) } throws IOException("synthetic widget failure")
            val errors = collectErrors(viewModel.projectContentErrors)

            viewModel.increment()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.counter.count)
            assertTrue(errors.isEmpty())
            coVerify(exactly = 1) { repository.applyMainCounterChange(7L, MainCounterChange.Increment) }
        }

    @Test
    fun `late count completion cannot replace newly selected project widget`() =
        runTest {
            val result = pendingIncrement(count = 1)
            val published = captureWidgetUpdates()
            val other = CounterProject(id = 8L, name = "Other", count = 10)
            every { repository.observeProject(8L) } returns flowOf(other)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.increment()
            runCurrent()
            viewModel.selectProject(other)
            advanceUntilIdle()
            val beforeCompletion = published.size
            result.complete(true)
            advanceUntilIdle()

            assertEquals(8L, viewModel.uiState.value.projectId)
            assertEquals(10, viewModel.uiState.value.counter.count)
            assertTrue(published.drop(beforeCompletion).none { it.projectId == 7L })
        }

    private fun observeSession(active: MutableStateFlow<ActiveWorkSession>) {
        every { repository.observeActiveSession() } returns active
        coEvery { repository.refreshActiveSession() } returns active.value
    }

    private fun TestScope.beginPendingStop(
        active: MutableStateFlow<ActiveWorkSession>,
    ): Pair<CounterViewModel, CompletableDeferred<StopSessionResult>> {
        observeSession(active)
        coEvery { repository.activeSessionDurationSeconds(any()) } returns 60L
        val reply = CompletableDeferred<StopSessionResult>()
        val sessionToken = active.value.sessionToken
        coEvery { repository.stopSession(sessionToken, 60L, 0, 0) } coAnswers { reply.await() }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.stopWorkSession()
        runCurrent()
        viewModel.saveStoppedWorkSession()
        runCurrent()
        return viewModel to reply
    }

    private fun recoverySession() =
        activeSession().copy(
            projectId = 7L,
            recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
            recoveryIntervalToken = "old-interval",
        )

    private fun TestScope.incrementAndCollectErrors(viewModel: CounterViewModel): List<Unit> {
        val errors = collectErrors(viewModel.projectContentErrors)
        advanceUntilIdle()
        viewModel.increment()
        advanceUntilIdle()
        return errors
    }
}
