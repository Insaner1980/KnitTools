package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCompletionSnapshot
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.ui.screens.project.ProjectCompletionCelebration
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterCompletionCelebrationTest : CounterViewModelFixture() {
    @Test
    fun `completion snapshot exists before close and survives clearing the selected project`() =
        runTest {
            val snapshot = snapshot(eventId = 41L)
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } returns
                ProjectCompletionResult.CompletedNow(snapshot)
            val vm = viewModel()
            advanceUntilIdle()
            val atClose = mutableListOf<ProjectCompletionCelebration?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.projectClosedEvents.collect { atClose += vm.pendingCompletion.value }
            }

            vm.completeProject()
            advanceUntilIdle()

            val expected = ProjectCompletionCelebration(snapshot, photoUri = null)
            assertEquals(listOf(expected), atClose)
            assertEquals(expected, vm.pendingCompletion.value)
            assertNull(vm.uiState.value.projectId)
            vm.consumeCompletion(40L)
            assertEquals(expected, vm.pendingCompletion.value)
            vm.consumeCompletion(41L)
            assertNull(vm.pendingCompletion.value)
        }

    @Test
    fun `reopened project produces a new celebration for the new completion event`() =
        runTest {
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } returnsMany
                listOf(
                    ProjectCompletionResult.CompletedNow(snapshot(41L)),
                    ProjectCompletionResult.CompletedNow(snapshot(42L)),
                )
            val vm = viewModel()
            advanceUntilIdle()
            vm.completeProject()
            advanceUntilIdle()
            vm.consumeCompletion(41L)
            vm.selectProject(observedProject.value)
            advanceUntilIdle()

            vm.completeProject()
            advanceUntilIdle()

            assertEquals(
                42L,
                vm.pendingCompletion.value
                    ?.snapshot
                    ?.eventId,
            )
            vm.consumeCompletion(41L)
            assertEquals(
                42L,
                vm.pendingCompletion.value
                    ?.snapshot
                    ?.eventId,
            )
        }

    @Test
    fun `idempotent already completed result has no celebration`() =
        runTest {
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } returns ProjectCompletionResult.Completed
            val vm = viewModel()
            advanceUntilIdle()

            vm.completeProject()
            advanceUntilIdle()

            assertNull(vm.pendingCompletion.value)
        }

    @Test
    fun `late completion cannot present a celebration over another project`() =
        runTest {
            val result = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers { result.await() }
            val other = CounterProject(id = 8L, name = "Other")
            every { repository.observeProject(8L) } returns flowOf(other)
            val vm = viewModel()
            advanceUntilIdle()
            vm.completeProject()
            runCurrent()
            vm.selectProject(other)
            advanceUntilIdle()

            result.complete(ProjectCompletionResult.CompletedNow(snapshot(41L)))
            advanceUntilIdle()

            assertNull(vm.pendingCompletion.value)
            assertEquals(8L, vm.uiState.value.projectId)
        }

    private fun snapshot(eventId: Long) =
        ProjectCompletionSnapshot(
            eventId = eventId,
            project = observedProject.value.copy(count = 37, isCompleted = true, completedAt = 2_000L),
            totalSessionMinutes = 90,
        )
}
