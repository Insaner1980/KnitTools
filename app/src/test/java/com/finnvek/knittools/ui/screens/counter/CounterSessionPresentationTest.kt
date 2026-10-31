package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveWorkSession
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterSessionPresentationTest : CounterViewModelFixture() {
    @Test
    fun `new observation cancels an older pending duration read`() =
        runTest {
            val active = MutableStateFlow<ActiveWorkSession?>(null)
            every { repository.observeActiveSession() } returns active
            coEvery { repository.refreshActiveSession() } returns null
            val vm = viewModel()
            runCurrent()
            val original = activeSession()
            val newer = original.copy(sessionToken = "new-session")
            val pending = CompletableDeferred<Long>()
            var cancelled = false
            coEvery { repository.activeSessionDurationSeconds(original) } coAnswers {
                try {
                    pending.await()
                } finally {
                    cancelled = true
                }
            }
            coEvery { repository.activeSessionDurationSeconds(newer) } returns 20L

            active.value = original
            runCurrent()
            active.value = newer
            runCurrent()

            assertTrue(cancelled)
            assertEquals(newer, vm.uiState.value.activeSession)
            assertEquals(20L, vm.uiState.value.sessionSeconds)
            active.value = null
            runCurrent()
            assertNull(vm.uiState.value.activeSession)
            assertEquals(0L, vm.uiState.value.sessionSeconds)
        }

    @Test
    fun `ticks do not pile up or overwrite newer anchors for the same session`() =
        runTest {
            val original = activeSession()
            val active = stubActiveSession(original)
            coEvery { repository.activeSessionNeedsRecovery(any()) } returns false
            val vm = viewModel()
            runCurrent()
            clearMocks(repository, answers = false)
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(original) } coAnswers { pending.await() }

            vm.refreshSessionPresentationTime()
            runCurrent()
            vm.refreshSessionPresentationTime()
            runCurrent()
            coVerify(exactly = 1) { repository.activeSessionNeedsRecovery(original) }
            coVerify(exactly = 1) { repository.activeSessionDurationSeconds(original) }

            val updated =
                original.copy(
                    timingAnchors = original.timingAnchors.copy(checkpointedDurationSeconds = 20L),
                )
            coEvery { repository.activeSessionDurationSeconds(updated) } returns 20L
            active.value = updated
            runCurrent()
            pending.complete(999L)
            runCurrent()

            assertEquals(updated, vm.uiState.value.activeSession)
            assertEquals(20L, vm.uiState.value.sessionSeconds)
        }

    @Test
    fun `cancelled stop read cannot open a summary later`() =
        runTest {
            val session = activeSession()
            every { repository.observeActiveSession() } returns MutableStateFlow(session)
            coEvery { repository.refreshActiveSession() } returns session
            coEvery { repository.activeSessionDurationSeconds(session) } returns 10L
            val vm = viewModel()
            runCurrent()
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(session) } coAnswers { pending.await() }

            vm.stopWorkSession()
            runCurrent()
            vm.cancelSessionStop()
            pending.complete(30L)
            runCurrent()

            assertNull(vm.uiState.value.sessionStopSummary)
            coVerify(exactly = 0) { repository.stopSession(any(), any(), any(), any()) }
        }

    @Test
    fun `stop retries updated anchors and row totals for the same session`() =
        runTest {
            val original = activeSession()
            val active = stubActiveSession(original)
            val vm = viewModel()
            runCurrent()
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(original) } coAnswers { pending.await() }

            vm.stopWorkSession()
            runCurrent()
            val updated =
                original.copy(
                    timingAnchors = original.timingAnchors.copy(checkpointedDurationSeconds = 20L),
                    trustedRowsWorked = 3,
                    trustedLastObservedRow = 8,
                )
            coEvery { repository.activeSessionDurationSeconds(updated) } returns 25L
            active.value = updated
            runCurrent()
            pending.complete(10L)
            runCurrent()

            val summary = vm.uiState.value.sessionStopSummary
            assertEquals(updated.sessionToken, summary?.sessionToken)
            assertEquals(25L, summary?.durationSeconds)
            assertEquals(3, summary?.rowsWorked)
            assertEquals(8, summary?.endRow)
            coVerify(exactly = 0) { repository.stopSession(any(), any(), any(), any()) }
        }

    @Test
    fun `stop retry opens recovery instead of a summary when updated session needs review`() =
        runTest {
            val original = activeSession()
            val active = stubActiveSession(original)
            val vm = viewModel()
            runCurrent()
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(original) } coAnswers { pending.await() }

            vm.stopWorkSession()
            runCurrent()
            active.value =
                original.copy(
                    recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                    recoveryPromptShown = true,
                )
            runCurrent()
            pending.complete(30L)
            runCurrent()

            assertTrue(vm.uiState.value.showSessionRecoveryPrompt)
            assertNull(vm.uiState.value.sessionStopSummary)
        }

    @Test
    fun `stop read cannot publish after changing the selected project`() =
        runTest {
            val session = activeSession()
            stubActiveSession(session)
            val vm = viewModel()
            runCurrent()
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(session) } coAnswers { pending.await() }

            vm.stopWorkSession()
            runCurrent()
            vm.selectProject(observeOtherProject(count = 0))
            runCurrent()
            pending.complete(30L)
            runCurrent()

            assertNull(vm.uiState.value.sessionStopSummary)
        }

    @Test
    fun `stop read cannot publish a summary for a replaced session`() =
        runTest {
            val original = activeSession()
            val active = stubActiveSession(original)
            val vm = viewModel()
            runCurrent()
            val pending = CompletableDeferred<Long>()
            coEvery { repository.activeSessionDurationSeconds(original) } coAnswers { pending.await() }

            vm.stopWorkSession()
            runCurrent()
            val newer = original.copy(sessionToken = "new-session")
            active.value = newer
            runCurrent()
            pending.complete(30L)
            runCurrent()

            assertEquals(newer, vm.uiState.value.activeSession)
            assertNull(vm.uiState.value.sessionStopSummary)
        }

    private fun stubActiveSession(session: ActiveWorkSession): MutableStateFlow<ActiveWorkSession> {
        val active = MutableStateFlow(session)
        every { repository.observeActiveSession() } returns active
        coEvery { repository.refreshActiveSession() } returns session
        coEvery { repository.activeSessionDurationSeconds(any()) } returns 10L
        return active
    }
}
