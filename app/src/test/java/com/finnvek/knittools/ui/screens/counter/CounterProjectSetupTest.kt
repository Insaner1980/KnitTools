package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.ui.screens.project.ProjectSetupVisit
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterProjectSetupTest : CounterViewModelFixture() {
    @Test
    fun `celebration records its visit before persistence and writes once`() =
        runTest {
            val persisted = CompletableDeferred<Unit>()
            coEvery { onboardingStateRepository.markProjectSetupCelebrated() } coAnswers { persisted.await() }
            val vm = viewModel()
            advanceUntilIdle()

            vm.celebrateProjectSetup(7L, 4L)
            assertEquals(ProjectSetupVisit(7L, 4L), vm.projectSetupCelebration.value)
            vm.celebrateProjectSetup(7L, 4L)
            runCurrent()
            coVerify(exactly = 1) { onboardingStateRepository.markProjectSetupCelebrated() }
            persisted.complete(Unit)
            runCurrent()
        }

    @Test
    fun `stale project cannot claim the visible setup celebration`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.celebrateProjectSetup(8L, 4L)
            assertNull(vm.projectSetupCelebration.value)
            coVerify(exactly = 0) { onboardingStateRepository.markProjectSetupCelebrated() }
        }

    @Test
    fun `hiding setup survives clearing the project view model`() =
        runTest {
            val persisted = CompletableDeferred<Unit>()
            coEvery { onboardingStateRepository.hideProjectSetup() } coAnswers {
                persisted.await()
                onboardingState.value = onboardingState.value.copy(projectSetupHidden = true)
            }
            val vm = viewModel()
            advanceUntilIdle()
            val store = ViewModelStore().apply { put("counter", vm) }
            vm.hideProjectSetup()
            runCurrent()
            store.clear()
            persisted.complete(Unit)
            runCurrent()
            assertEquals(true, onboardingState.value.projectSetupHidden)
            coVerify(exactly = 1) { onboardingStateRepository.hideProjectSetup() }
        }
}
