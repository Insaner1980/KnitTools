package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.MainCounterChange
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterRemainingWriteResultTest : CounterViewModelFixture() {
    @Test
    fun `requested stitch activation saves the count and enables tracking through one mutation`() =
        runTest {
            var storedProject = observedProject.value
            coEvery { repository.getProject(7L) } coAnswers { storedProject }
            coEvery { repository.updateProjectStitchCount(7L, 100, true) } coAnswers {
                storedProject = storedProject.copy(stitchCount = 100, stitchTrackingEnabled = true)
                true
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setStitchCount(100, enableTracking = true)
            advanceUntilIdle()

            assertEquals(100, viewModel.uiState.value.stitchCount)
            assertEquals(true, viewModel.uiState.value.stitchTrackingEnabled)
            assertEquals(0, viewModel.uiState.value.currentStitch)
            coVerify(exactly = 1) { repository.updateProjectStitchCount(7L, 100, true) }
            coVerify(exactly = 0) { repository.updateStitchTrackingEnabled(any(), any()) }
        }

    @Test
    fun `ordinary stitch count editing keeps disabled tracking and clearing disables active tracking`() =
        runTest {
            var storedProject = observedProject.value.copy(stitchCount = 12)
            observedProject.value = storedProject
            every { repository.getActiveProjects() } returns flowOf(listOf(storedProject))
            coEvery { repository.getProject(7L) } coAnswers { storedProject }
            coEvery { repository.updateProjectStitchCount(7L, 100, false) } coAnswers {
                storedProject = storedProject.copy(stitchCount = 100)
                true
            }
            coEvery { repository.updateStitchTrackingEnabled(7L, true) } coAnswers {
                storedProject = storedProject.copy(stitchTrackingEnabled = true)
                true
            }
            coEvery { repository.updateProjectStitchCount(7L, null, false) } coAnswers {
                storedProject = storedProject.copy(stitchCount = null, stitchTrackingEnabled = false)
                true
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setStitchCount(100)
            advanceUntilIdle()
            assertEquals(100, viewModel.uiState.value.stitchCount)
            assertEquals(false, viewModel.uiState.value.stitchTrackingEnabled)
            viewModel.setStitchTrackingEnabled(true)
            advanceUntilIdle()
            viewModel.setStitchCount(null)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.stitchCount)
            assertEquals(false, viewModel.uiState.value.stitchTrackingEnabled)
            assertEquals(0, viewModel.uiState.value.currentStitch)
        }

    @Test
    fun `failed stitch activation restores persisted count and disabled state`() =
        runTest {
            val project = observedProject.value
            coEvery { repository.getProject(7L) } returns project
            coEvery { repository.updateProjectStitchCount(7L, 100, true) } throws IOException("stitch save failed")
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()

            viewModel.setStitchCount(100, enableTracking = true)
            assertEquals(true, viewModel.uiState.value.stitchTrackingEnabled)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.stitchCount)
            assertEquals(false, viewModel.uiState.value.stitchTrackingEnabled)
            assertEquals(1, errors.size)
        }

    @Test
    fun `undo and project detail failures are surfaced without changing displayed project`() =
        runTest {
            coEvery { repository.applyMainCounterChange(7L, MainCounterChange.Undo) } throws IOException("undo failed")
            coEvery { repository.updateProjectDetails(any(), any(), any(), any(), any()) } returns null
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()
            val project = observedProject.value

            viewModel.undo()
            advanceUntilIdle()
            viewModel.setProjectDetails("Changed", project.craftType, project.mainCounterLabelType, null)
            advanceUntilIdle()

            assertEquals(project.name, viewModel.uiState.value.projectName)
            assertEquals(project.count, viewModel.uiState.value.counter.count)
            assertEquals(2, errors.size)
        }

    @Test
    fun `rejected stitch changes and tracking toggles restore persisted stitch state`() =
        runTest {
            val project = observedProject.value.copy(stitchCount = 10, currentStitch = 2, stitchTrackingEnabled = true)
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            coEvery { repository.applyStitchChange(7L, false) } returns false
            coEvery { repository.updateStitchTrackingEnabled(7L, false) } returns false
            coEvery { repository.updateProjectStitchCount(7L, null) } returns false
            coEvery { repository.getProject(7L) } returns project
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()

            viewModel.decrementStitch()
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.currentStitch)
            viewModel.setStitchTrackingEnabled(false)
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.stitchTrackingEnabled)
            assertEquals(2, viewModel.uiState.value.currentStitch)
            assertEquals(2, errors.size)
            viewModel.setStitchCount(null)
            advanceUntilIdle()
            assertEquals(10, viewModel.uiState.value.stitchCount)
            assertEquals(true, viewModel.uiState.value.stitchTrackingEnabled)
            assertEquals(2, viewModel.uiState.value.currentStitch)
            assertEquals(3, errors.size)
        }

    @Test
    fun `failed target save restores stored target and reports failure`() =
        runTest {
            val project = observedProject.value.copy(targetRows = 12)
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            coEvery { repository.setTargetRows(7L, 20) } throws IOException("target failed")
            coEvery { repository.getProject(7L) } returns project
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()

            viewModel.setTargetRows(20)
            advanceUntilIdle()

            assertEquals(12, viewModel.uiState.value.targetRows)
            assertEquals(1, errors.size)
        }
}
