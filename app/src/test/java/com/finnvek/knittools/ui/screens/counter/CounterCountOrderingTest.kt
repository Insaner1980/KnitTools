package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.MainCounterChange
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterCountOrderingTest : CounterViewModelFixture() {
    @Test
    fun `late count read cannot replace a newer observation for the same project`() = verifyObservationWins(false)

    @Test
    fun `late undo read cannot replace a newer observation for the same project`() = verifyObservationWins(true)

    private fun verifyObservationWins(undo: Boolean) =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            val read = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val published = captureWidgetUpdates()
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } returns true
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Undo) } returns true
            coEvery { repository.getProject(7L) } coAnswers {
                val old = observedProject.value.copy(count = 1)
                read.complete(Unit)
                release.await()
                old
            }

            if (undo) model.undo() else model.increment()
            runCurrent()
            assertTrue(read.isCompleted)
            observedProject.value = observedProject.value.copy(count = 2)
            runCurrent()
            assertEquals(2, model.uiState.value.counter.count)
            release.complete(Unit)
            advanceUntilIdle()

            assertEquals(2, model.uiState.value.counter.count)
            assertTrue(published.none { it.count == 1 })
            assertEquals(2, published.lastOrNull()?.count)
            coVerify(exactly = 1) { reviewManager.recordAction() }
        }

    @Test
    fun `late first action read cannot replace the second committed action`() =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            val release = CompletableDeferred<Unit>()
            var reads = 0
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } returns true
            coEvery { repository.getProject(7L) } coAnswers {
                val count = ++reads
                if (count == 1) release.await()
                observedProject.value.copy(count = count)
            }
            model.increment()
            runCurrent()
            model.increment()
            runCurrent()
            assertEquals(2, model.uiState.value.counter.count)
            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(2, reads)
            assertEquals(2, model.uiState.value.counter.count)
        }

    @Test
    fun `first write completing cannot rewind a newer pending tap`() =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            val first = CompletableDeferred<Unit>()
            val second = CompletableDeferred<Unit>()
            var writes = 0
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Increment) } coAnswers {
                val count = ++writes
                if (count == 1) first.await() else second.await()
                true
            }
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 1)
            model.increment()
            runCurrent()
            model.increment()
            runCurrent()
            first.complete(Unit)
            runCurrent()
            assertEquals(2, model.uiState.value.counter.count)
            coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = 2)
            second.complete(Unit)
            advanceUntilIdle()
            assertEquals(2, writes)
            assertEquals(2, model.uiState.value.counter.count)
        }
}
