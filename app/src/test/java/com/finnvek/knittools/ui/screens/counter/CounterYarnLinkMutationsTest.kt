package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterYarnLinkMutationsTest : CounterViewModelFixture() {
    @Test
    fun `rejected link reports failure and successful retry uses the selected project`() =
        runTest {
            val yarn = mockk<YarnCardRepository>()
            val viewModel = viewModel(yarnRepositoryOverride = yarn)
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()
            coEvery { yarn.updateLinkedProjectId(19L, 7L) } returns false
            viewModel.linkYarnCard(19L)
            advanceUntilIdle()
            assertEquals(1, errors.size)

            coEvery { yarn.updateLinkedProjectId(19L, 7L) } returns true
            viewModel.linkYarnCard(19L)
            advanceUntilIdle()

            assertEquals(1, errors.size)
            coVerify(exactly = 2) { yarn.updateLinkedProjectId(19L, 7L) }
        }

    @Test
    fun `unlink exception reports failure and leaves subsequent unlink usable`() =
        runTest {
            val yarn = mockk<YarnCardRepository>()
            val viewModel = viewModel(yarnRepositoryOverride = yarn)
            val errors = collectErrors(viewModel.projectContentErrors)
            coEvery { yarn.updateLinkedProjectId(19L, null) } throws IOException("synthetic failure")
            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()
            assertEquals(1, errors.size)

            coEvery { yarn.updateLinkedProjectId(19L, null) } returns true
            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()

            assertEquals(1, errors.size)
            coVerify(exactly = 2) { yarn.updateLinkedProjectId(19L, null) }
        }

    @Test
    fun `cancelled yarn write remains cancellation without a failure message`() =
        runTest {
            val yarn = mockk<YarnCardRepository>()
            val viewModel = viewModel(yarnRepositoryOverride = yarn)
            val errors = collectErrors(viewModel.projectContentErrors)
            coEvery { yarn.updateLinkedProjectId(19L, null) } throws CancellationException("cancelled")

            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()

            assertEquals(0, errors.size)
            coVerify(exactly = 1) { yarn.updateLinkedProjectId(19L, null) }
        }
}
