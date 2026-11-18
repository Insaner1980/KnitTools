package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CommittedMainCounterChange
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.tips.TipSignal
import com.finnvek.knittools.repository.StopSessionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterCelebrationSignalsTest : CounterViewModelFixture() {
    @Test
    fun `target uses committed crossing with step overshoot instead of stale visible count`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 0, targetRows = 100, stepSize = 5)
            val result = pendingIncrement(count = 104)
            val signals = mutableListOf<TipSignal>()
            every { tipSession.onSignal(capture(signals)) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()

            vm.increment()
            runCurrent()
            assertTrue(signals.isEmpty())
            result.complete(CommittedMainCounterChange(99, 104, 100, MainCounterChange.Increment))
            advanceUntilIdle()

            assertEquals(listOf(TipSignal.TargetReached(7L)), signals.filterIsInstance<TipSignal.TargetReached>())
            assertEquals(1, signals.filterIsInstance<TipSignal.Increment>().size)
            assertEquals(104, vm.uiState.value.counter.count)
        }

    @Test
    fun `already reached committed target does not celebrate an apparent visible crossing`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 9, targetRows = 10)
            coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) } returns
                CommittedMainCounterChange(12, 13, 10, MainCounterChange.Increment)
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 13)
            val vm = viewModel()
            advanceUntilIdle()

            vm.increment()
            advanceUntilIdle()

            verify(exactly = 0) { tipSession.onSignal(any<TipSignal.TargetReached>()) }
            verify(exactly = 1) { tipSession.onSignal(any<TipSignal.Increment>()) }
        }

    @Test
    fun `late crossing retains original project identity after selection changes`() =
        runTest {
            val result = pendingIncrement(count = 10)
            val other = observeOtherProject(count = 37)
            val vm = viewModel()
            advanceUntilIdle()

            vm.increment()
            runCurrent()
            vm.selectProject(other)
            advanceUntilIdle()
            result.complete(CommittedMainCounterChange(9, 10, 10, MainCounterChange.Increment))
            advanceUntilIdle()

            verify(exactly = 1) { tipSession.onSignal(TipSignal.TargetReached(7L)) }
            verify(exactly = 0) { tipSession.onSignal(TipSignal.TargetReached(8L)) }
            assertEquals(37, vm.uiState.value.counter.count)
        }

    @Test
    fun `undo restoring target never triggers a target celebration`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 9, targetRows = 10)
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Undo) } returns true
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 10)
            val vm = viewModel()
            advanceUntilIdle()

            vm.undo()
            advanceUntilIdle()

            assertEquals(10, vm.uiState.value.counter.count)
            verify(exactly = 0) { tipSession.onSignal(any()) }
        }

    @Test
    fun `successful saved session forwards firstness but a duplicate save has no signal`() =
        runTest {
            val session = activeSession().copy(projectId = 7L)
            every { repository.observeActiveSession() } returns flowOf(session)
            coEvery { repository.refreshActiveSession() } returns session
            coEvery { repository.activeSessionDurationSeconds(any()) } returns 60L
            val signals = mutableListOf<TipSignal>()
            every { tipSession.onSignal(capture(signals)) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()

            listOf(
                StopSessionResult.Saved(12L, wasFirstCompletedSession = true),
                StopSessionResult.Saved(13L, wasFirstCompletedSession = false),
                StopSessionResult.Saved(null, wasFirstCompletedSession = true),
            ).forEach { result ->
                coEvery { repository.stopSession(session.sessionToken, 60L, 0, 0) } returns result
                vm.stopWorkSession()
                advanceUntilIdle()
                vm.saveStoppedWorkSession()
                advanceUntilIdle()
            }

            assertEquals(listOf(TipSignal.SessionSaved(true), TipSignal.SessionSaved(false)), signals)
        }
}
