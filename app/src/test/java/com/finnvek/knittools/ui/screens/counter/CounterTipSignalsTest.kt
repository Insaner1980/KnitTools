package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CommittedMainCounterChange
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.tips.TipSignal
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterTipSignalsTest : CounterViewModelFixture() {
    @Test
    fun `increment signals only after commit and retains the tap time`() =
        runTest {
            val result = pendingIncrement(count = 1)
            val signals = mutableListOf<TipSignal>()
            every { tipSession.onSignal(capture(signals)) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()

            val beforeTap = System.nanoTime() / 1_000_000
            vm.increment()
            val afterTap = System.nanoTime() / 1_000_000
            runCurrent()
            assertTrue(signals.isEmpty())
            result.complete(CommittedMainCounterChange(0, 1, null, MainCounterChange.Increment))
            advanceUntilIdle()

            val signal = signals.single() as TipSignal.Increment
            assertEquals(7L, signal.projectId)
            assertTrue(signal.monotonicMillis in beforeTap..afterTap)
            assertEquals(1, vm.uiState.value.counter.count)
        }

    @Test
    fun `failed and unchanged writes never signal an increment`() =
        runTest {
            coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) } returns null
            coEvery { repository.getProject(7L) } returns observedProject.value
            val vm = viewModel()
            advanceUntilIdle()
            vm.increment()
            advanceUntilIdle()
            coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) } throws
                IOException("write")
            vm.increment()
            advanceUntilIdle()
            verify(exactly = 0) { tipSession.onSignal(any()) }
        }

    @Test
    fun `failed authoritative reload still signals the committed increment`() =
        runTest {
            val signals = mutableListOf<TipSignal>()
            every { tipSession.onSignal(capture(signals)) } returns Unit
            coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) } returns
                CommittedMainCounterChange(0, 1, null, MainCounterChange.Increment)
            coEvery { repository.getProject(7L) } throws IOException("reload")
            val vm = viewModel()
            advanceUntilIdle()
            vm.increment()
            advanceUntilIdle()
            assertEquals(7L, (signals.single() as TipSignal.Increment).projectId)
        }

    @Test
    fun `late successful write cannot signal for a newly selected project`() =
        runTest {
            val result = pendingIncrement(count = 1)
            val signals = mutableListOf<TipSignal>()
            every { tipSession.onSignal(capture(signals)) } returns Unit
            val other = observeOtherProject(count = 37)
            val vm = viewModel()
            advanceUntilIdle()
            vm.increment()
            runCurrent()
            vm.selectProject(other)
            advanceUntilIdle()
            result.complete(CommittedMainCounterChange(0, 1, null, MainCounterChange.Increment))
            advanceUntilIdle()
            assertEquals(7L, (signals.single() as TipSignal.Increment).projectId)
            assertEquals(8L, vm.uiState.value.projectId)
            assertEquals(37, vm.uiState.value.counter.count)
        }

    @Test
    fun `decrement and undo do not contribute to increment tips`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 2)
            coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Decrement) } returns
                CommittedMainCounterChange(2, 1, null, MainCounterChange.Decrement)
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Undo) } returns true
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 1)
            val vm = viewModel()
            advanceUntilIdle()
            vm.decrement()
            advanceUntilIdle()
            coEvery { repository.getProject(7L) } returns observedProject.value
            vm.undo()
            advanceUntilIdle()
            verify(exactly = 0) { tipSession.onSignal(any()) }
        }
}
