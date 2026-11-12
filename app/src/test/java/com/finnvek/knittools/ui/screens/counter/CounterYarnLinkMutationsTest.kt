package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
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
    fun `queued unlink cannot clear a card moved to another project`() =
        runTest {
            val cards = mockk<YarnCardDao>(relaxed = true)
            val projects = mockk<CounterProjectDao>(relaxed = true)
            var card = YarnCardEntity(id = 19L, linkedProjectId = 7L)
            var rows = listOf(CounterProjectEntity(id = 7L, yarnCardIds = "19"), CounterProjectEntity(id = 8L))
            coEvery { cards.getCard(19L) } answers { card }
            coEvery { cards.updateLinkedProjectId(19L, any()) } answers {
                card = card.copy(linkedProjectId = secondArg())
                1
            }
            coEvery { projects.getAllProjectsOnce() } answers { rows }
            coEvery { projects.updateYarnCardIds(any(), any(), any()) } answers {
                val owner = firstArg<Long>()
                val csv = secondArg<String>()
                rows = rows.map { if (it.id == owner) it.copy(yarnCardIds = csv) else it }
            }
            val yarn =
                spyk(
                    YarnCardRepository(
                        cards,
                        projects,
                        mockk<Context>(),
                        ImmediateDatabaseTransactionRunner,
                        dispatcher,
                    ),
                )
            val viewModel = viewModel(yarnRepositoryOverride = yarn)
            advanceUntilIdle()

            viewModel.unlinkYarnCard(19L)
            card = card.copy(linkedProjectId = 8L)
            rows = listOf(CounterProjectEntity(id = 7L), CounterProjectEntity(id = 8L, yarnCardIds = "19"))
            advanceUntilIdle()

            assertEquals(8L, card.linkedProjectId)
            assertEquals(listOf("", "19"), rows.map { it.yarnCardIds })
        }

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
            advanceUntilIdle()
            coEvery {
                yarn.updateLinkedProjectId(19L, null, expectedLinkedProjectId = 7L)
            } throws IOException("synthetic failure")
            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()
            assertEquals(1, errors.size)

            coEvery { yarn.updateLinkedProjectId(19L, null, expectedLinkedProjectId = 7L) } returns true
            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()

            assertEquals(1, errors.size)
            coVerify(exactly = 2) { yarn.updateLinkedProjectId(19L, null, expectedLinkedProjectId = 7L) }
        }

    @Test
    fun `cancelled yarn write remains cancellation without a failure message`() =
        runTest {
            val yarn = mockk<YarnCardRepository>()
            val viewModel = viewModel(yarnRepositoryOverride = yarn)
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()
            coEvery {
                yarn.updateLinkedProjectId(19L, null, expectedLinkedProjectId = 7L)
            } throws CancellationException("cancelled")

            viewModel.unlinkYarnCard(19L)
            advanceUntilIdle()

            assertEquals(0, errors.size)
            coVerify(exactly = 1) { yarn.updateLinkedProjectId(19L, null, expectedLinkedProjectId = 7L) }
        }
}
