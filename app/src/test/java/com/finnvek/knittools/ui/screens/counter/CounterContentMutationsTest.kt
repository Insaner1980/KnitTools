package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProjectCounterMutationResult
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderMutationResult
import com.finnvek.knittools.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterContentMutationsTest : CounterViewModelFixture() {
    @Test
    fun `delayed reminder edit preserves a newer stored reminder`() =
        runTest {
            val original = RowReminder(id = 19L, projectId = 7L, targetRow = 4, message = "Original")
            var stored = original.toEntity()
            val reminderDao = mockk<RowReminderDao>()
            every { reminderDao.getRemindersForProject(7L) } returns flowOf(listOf(stored))
            coEvery { reminderDao.getReminder(19L) } answers { stored }
            coEvery { reminderDao.update(any()) } answers {
                stored = firstArg()
                1
            }
            val projectDao = mockk<CounterProjectDao>()
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, name = "Project")
            val entered = CompletableDeferred<Unit>()
            val resume = CompletableDeferred<Unit>()
            val transaction =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        entered.complete(Unit)
                        resume.await()
                        return block()
                    }
                }
            val reminders = ReminderRepository(reminderDao, projectDao, purchasedPro(), transaction)
            val model = viewModel(reminderRepositoryOverride = reminders)
            val errors = collectErrors(model.projectContentErrors)
            advanceUntilIdle()

            model.updateReminder(19L, 8, null, "Older pending edit")
            entered.await()
            val newer = original.copy(targetRow = 24, message = "Newer edit").toEntity()
            stored = newer
            resume.complete(Unit)
            advanceUntilIdle()

            assertEquals(newer, stored)
            assertEquals(1, errors.size)
            coVerify(exactly = 0) { reminderDao.update(any()) }
        }

    @Test
    fun `every extra counter action exposes rejected persistence and creation can be retried`() =
        runTest {
            val counter = ProjectCounter(id = 19L, projectId = 7L, name = "Sleeve")
            val counters = mockk<ProjectCounterRepository>()
            every { counters.getCountersForProject(7L) } returns flowOf(listOf(counter))
            coEvery { counters.addCounter(any()) } returns ProjectCounterMutationResult.PersistenceFailure
            coEvery { counters.incrementCounter(counter) } returns ProjectCounterMutationResult.PersistenceFailure
            coEvery { counters.decrementCounter(counter) } returns ProjectCounterMutationResult.StaleAction
            coEvery { counters.resetCounter(7L, 19L) } returns ProjectCounterMutationResult.CounterUnavailable
            coEvery { counters.renameCounter(7L, 19L, "Body") } returns ProjectCounterMutationResult.InvalidCounter
            coEvery { counters.deleteCounter(7L, 19L) } returns ProjectCounterMutationResult.PersistenceFailure
            val viewModel = viewModel(countersOverride = counters, proManagerOverride = purchasedPro())
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.projectContentErrors.collect { errors.add(it) }
            }
            advanceUntilIdle()
            val draft = ProjectCounterDraft("New", null, 1)
            assertTrue(viewModel.addProjectCounter(draft))
            viewModel.incrementProjectCounter(counter)
            viewModel.decrementProjectCounter(counter)
            viewModel.resetProjectCounter(19L)
            viewModel.renameProjectCounter(19L, "Body")
            viewModel.deleteProjectCounter(19L)
            advanceUntilIdle()

            assertEquals(6, errors.size)
            assertEquals(listOf(counter), viewModel.uiState.value.projectCounters)
            coEvery { counters.addCounter(any()) } returns ProjectCounterMutationResult.Success(20L)
            assertTrue(viewModel.addProjectCounter(draft))
            advanceUntilIdle()
            assertEquals(6, errors.size)
            coVerify(exactly = 2) { counters.addCounter(match { it.projectId == 7L && it.name == "New" }) }
        }

    @Test
    fun `reminder rejection stays visible and failed dismissal does not hide reminder`() =
        runTest {
            observedProject.value = observedProject.value.copy(count = 4)
            val reminder = RowReminder(id = 19L, projectId = 7L, targetRow = 4, message = "Change yarn")
            val reminders = mockk<ReminderRepository>()
            every { reminders.getRemindersForProject(7L) } returns flowOf(listOf(reminder))
            coEvery { reminders.insert(any()) } returns ReminderMutationResult.PersistenceFailure
            coEvery { reminders.update(7L, any()) } returns ReminderMutationResult.StaleAction
            coEvery {
                reminders.update(7L, any(), expectedReminder = reminder)
            } returns ReminderMutationResult.StaleAction
            coEvery { reminders.delete(7L, 19L) } returns ReminderMutationResult.PersistenceFailure
            val viewModel = viewModel(reminderRepositoryOverride = reminders, proManagerOverride = purchasedPro())
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.projectContentErrors.collect { errors.add(it) }
            }
            advanceUntilIdle()
            val before = viewModel.uiState.value
            assertNotNull(before.activeAlert)
            assertTrue(viewModel.addReminder(4, null, "New"))
            viewModel.updateReminder(19L, 2, null, "Edited")
            viewModel.dismissReminder(19L)
            viewModel.deleteReminder(19L)
            advanceUntilIdle()

            assertEquals(4, errors.size)
            assertEquals(before.reminders, viewModel.uiState.value.reminders)
            assertEquals(before.activeAlert, viewModel.uiState.value.activeAlert)
            assertEquals(before.dismissedReminderTrigger, viewModel.uiState.value.dismissedReminderTrigger)
            coVerify(exactly = 1) {
                reminders.update(7L, reminder.copy(isCompleted = true), expectedReminder = reminder)
            }
            coEvery { reminders.insert(any()) } returns ReminderMutationResult.Success(20L)
            assertTrue(viewModel.addReminder(4, null, "New"))
            advanceUntilIdle()
            assertEquals(4, errors.size)
            coVerify(exactly = 2) { reminders.insert(match { it.projectId == 7L && it.message == "New" }) }
        }

    @Test
    fun `project yarn note failures and rejected conversion report errors and permit retry`() =
        runTest {
            val notes = mockk<ProjectYarnNoteRepository>()
            coEvery { notes.save(any()) } throws IOException("synthetic save failure")
            coEvery { notes.delete(19L) } throws IOException("synthetic delete failure")
            coEvery { notes.saveToMyYarn(19L) } returns null
            val viewModel = viewModel(yarnNoteRepositoryOverride = notes, proManagerOverride = purchasedPro())
            val errors = mutableListOf<Unit>()
            val savedProjects = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.projectContentErrors.collect { errors.add(it) }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.projectYarnSaved.collect { savedProjects.add(it) }
            }
            advanceUntilIdle()
            viewModel.saveProjectYarnNote("Wool", "", 1, "")
            viewModel.deleteProjectYarnNote(19L)
            assertTrue(viewModel.saveProjectYarnNoteToMyYarn(19L))
            advanceUntilIdle()

            assertEquals(3, errors.size)
            assertTrue(savedProjects.isEmpty())
            val persistedCard = CompletableDeferred<Long>()
            coEvery { notes.saveToMyYarn(19L) } coAnswers { persistedCard.await() }
            assertTrue(viewModel.saveProjectYarnNoteToMyYarn(19L))
            assertTrue(viewModel.saveProjectYarnNoteToMyYarn(19L))
            advanceUntilIdle()
            assertTrue(savedProjects.isEmpty())

            persistedCard.complete(29L)
            advanceUntilIdle()
            assertEquals(3, errors.size)
            assertEquals(listOf(7L), savedProjects)
            coVerify(exactly = 2) { notes.saveToMyYarn(19L) }
        }

    private fun purchasedPro(): ProManager =
        mockk<ProManager>().also {
            every { it.proState } returns MutableStateFlow(ProState(status = ProStatus.PRO_PURCHASED))
            every { it.hasFeature(any()) } returns true
        }
}
