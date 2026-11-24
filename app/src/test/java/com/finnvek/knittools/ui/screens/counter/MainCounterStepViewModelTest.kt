package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.MainCounterChange
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class MainCounterStepViewModelTest : CounterViewModelFixture() {
    @Test
    fun `overlapping preset custom and repeated saves serialize without suppressing counting taps`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Boolean>()
            val writes = mutableListOf<Int>()
            coEvery { repository.updateProjectStepSize(7L, any()) } coAnswers {
                val size = secondArg<Int>()
                writes += size
                if (writes.size == 1) {
                    entered.complete(Unit)
                    release.await()
                } else {
                    true
                }
            }
            coEvery { repository.applyMainCounterChange(7L, any()) } returns true
            val model = viewModel()
            advanceUntilIdle()
            val preset = async { model.awaitStepSize(5) }
            entered.await()
            val custom = async { model.awaitStepSize(12) }
            val repeated = async { model.awaitStepSize(12) }
            runCurrent()
            assertEquals(listOf(5), writes)
            assertFalse(custom.isCompleted)
            assertFalse(repeated.isCompleted)
            model.increment()
            model.increment()
            runCurrent()
            coVerify(exactly = 2) {
                repository.applyMainCounterChange(7L, MainCounterChange.Increment)
            }
            release.complete(true)
            assertTrue(preset.await())
            assertTrue(custom.await())
            assertTrue(repeated.await())
            assertEquals(listOf(5, 12, 12), writes)
            assertEquals(12, model.uiState.value.counter.stepSize)
        }

    @Test
    fun `queued save cannot write after the same project selection is reopened`() =
        runTest {
            val release = deferStepWrite()
            val model = viewModel()
            advanceUntilIdle()
            val first = async { model.awaitStepSize(5) }
            runCurrent()
            val queued = async { model.awaitStepSize(12) }
            runCurrent()
            model.selectProject(observedProject.value)
            runCurrent()
            release.complete(true)
            assertFalse(first.await())
            assertFalse(queued.await())
            coVerify(exactly = 0) { repository.updateProjectStepSize(7L, 12) }
            assertEquals(1, model.uiState.value.counter.stepSize)
        }

    @Test
    fun `feedback reflects the actual clamped change and is absent for step one or no change`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 2, stepSize = 5)
            coEvery { repository.getProject(7L) } answers { observedProject.value }
            val model = viewModel()
            advanceUntilIdle()
            val feedback = mutableListOf<MainCounterStepFeedback>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { model.stepFeedback.collect { feedback += it } }
            model.decrement()
            model.decrement()
            assertEquals(listOf(MainCounterStepFeedback(7L, -2)), feedback)
            observedProject.value = observedProject.value.copy(count = 10, stepSize = 1)
            runCurrent()
            model.increment()
            assertEquals(1, feedback.size)
        }

    @Test
    fun `failed counts and failed readbacks restore the last observed count`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 10)
            val model = viewModel()
            advanceUntilIdle()
            coEvery { repository.applyMainCounterChange(7L, any()) } throws
                IllegalStateException("write failed")
            coEvery { repository.getProject(7L) } throws IllegalStateException("read failed")

            model.increment()
            model.increment()
            assertEquals(12, model.uiState.value.counter.count)
            runCurrent()

            assertEquals(10, model.uiState.value.counter.count)
        }

    @Test
    fun `failed write keeps the saved step and permits retry`() =
        runTest {
            coEvery { repository.updateProjectStepSize(7L, 5) } throws IllegalStateException("write failed")
            val model = viewModel()
            advanceUntilIdle()
            assertFalse(model.awaitStepSize(5))
            assertEquals(1, model.uiState.value.counter.stepSize)
            coEvery { repository.updateProjectStepSize(7L, 5) } returns true
            assertTrue(model.awaitStepSize(5))
            assertEquals(5, model.uiState.value.counter.stepSize)
        }

    @Test
    fun `late step write keeps the newly observed count and applies saved step`() =
        runTest {
            val release = deferStepWrite()
            val model = viewModel()
            advanceUntilIdle()
            val save = async { model.awaitStepSize(5) }
            runCurrent()
            assertEquals(1, model.uiState.value.counter.stepSize)
            observedProject.value = observedProject.value.copy(count = 42)
            runCurrent()
            release.complete(true)
            assertTrue(save.await())
            assertEquals(42, model.uiState.value.counter.count)
            assertEquals(5, model.uiState.value.counter.stepSize)
        }

    @Test
    fun `late result cannot change another project or a reopened selection`() =
        runTest {
            val release = deferStepWrite()
            val other = CounterProject(id = 8L, name = "Other", count = 24, stepSize = 3)
            every { repository.observeProject(8L) } returns flowOf(other)
            val model = viewModel()
            advanceUntilIdle()
            val save = async { model.awaitStepSize(5) }
            runCurrent()
            model.selectProject(other)
            advanceUntilIdle()
            assertEquals(3, model.uiState.value.counter.stepSize)
            model.selectProject(observedProject.value)
            advanceUntilIdle()
            release.complete(true)
            assertFalse(save.await())
            assertEquals(1, model.uiState.value.counter.stepSize)
            coVerify(exactly = 0) { repository.updateProjectStepSize(8L, any()) }
        }

    @Test
    fun `cancelled write leaves step unchanged and releases save lock`() =
        runTest {
            deferStepWrite()
            val model = viewModel()
            advanceUntilIdle()
            val save = async { model.awaitStepSize(5) }
            runCurrent()
            save.cancel()
            runCurrent()
            assertEquals(1, model.uiState.value.counter.stepSize)
            coEvery { repository.updateProjectStepSize(7L, 2) } returns true
            assertTrue(model.awaitStepSize(2))
        }

    @Test
    fun `selection change before save starts prevents stale write`() =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            val result = CompletableDeferred<Boolean>()
            model.setStepSize(5) { result.complete(it) }
            model.selectProject(observedProject.value)
            runCurrent()
            assertFalse(result.await())
            coVerify(exactly = 0) { repository.updateProjectStepSize(any(), any()) }
        }

    @Test
    fun `invalid input and stale sheet do not write`() =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            for (size in listOf(0, -1, 100, Int.MAX_VALUE)) assertFalse(model.awaitStepSize(size))
            assertFalse(model.awaitStepSize(5, expectedProjectId = 8L))
            coVerify(exactly = 0) { repository.updateProjectStepSize(any(), any()) }
        }

    private suspend fun CounterViewModel.awaitStepSize(
        size: Int,
        expectedProjectId: Long? = uiState.value.projectId,
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            val job = setStepSize(size, expectedProjectId) { continuation.resume(it) }
            continuation.invokeOnCancellation { job.cancel() }
        }

    private fun deferStepWrite(): CompletableDeferred<Boolean> {
        val release = CompletableDeferred<Boolean>()
        coEvery { repository.updateProjectStepSize(7L, 5) } coAnswers { release.await() }
        return release
    }
}
