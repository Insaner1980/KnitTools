package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.RowReminder
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderAuditBoundaryTest : CounterViewModelFixture() {
    @Test
    fun `delete retains a different active alert while its original delivery is delayed`() =
        runTest { assertDelayedIdentity(edit = false, switchProject = false) }

    @Test
    fun `late reminder edit does not mutate the newly selected project`() =
        runTest { assertDelayedIdentity(edit = true, switchProject = true) }

    @Test
    fun `late reminder deletion does not mutate the newly selected project`() =
        runTest { assertDelayedIdentity(edit = false, switchProject = true) }

    private suspend fun TestScope.assertDelayedIdentity(
        edit: Boolean,
        switchProject: Boolean,
    ) {
        val first = RowReminder(id = 1, projectId = 7, targetRow = 10, message = "First")
        val second = RowReminder(id = 2, projectId = 7, targetRow = 10, message = "Second")
        val other = RowReminder(id = 3, projectId = 8, targetRow = 10, message = "Other project")
        val rows = MutableStateFlow(listOf(first, second))
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(7) } returns rows
        every { reminders.getRemindersForProject(8) } returns flowOf(listOf(other))
        val gate = CompletableDeferred<Unit>()
        coEvery { reminders.delete(7, 1) } coAnswers {
            gate.await()
            rows.value = rows.value.filterNot { it.id == 1L }
            ReminderMutationResult.Success(1)
        }
        coEvery { reminders.update(7, any(), any()) } coAnswers {
            gate.await()
            val edited = arg<RowReminder>(1)
            rows.value = rows.value.map { if (it.id == edited.id) edited else it }
            ReminderMutationResult.Success(1)
        }
        observedProject.value = observedProject.value.copy(count = 10)
        val model = viewModel(reminderRepositoryOverride = reminders)
        advanceUntilIdle()
        assertEquals(7L, model.uiState.value.projectId)
        assertEquals(
            1L,
            model.uiState.value.activeAlert
                ?.id,
        )
        if (edit) model.updateReminder(1, 20, null, "Edited") else model.deleteReminder(1)
        runCurrent()
        if (switchProject) {
            val next = CounterProject(id = 8, name = "Other project", count = 10)
            every { repository.observeProject(8) } returns flowOf(next)
            model.selectProject(next)
        } else {
            rows.value = listOf(first.copy(isCompleted = true), second)
        }
        advanceUntilIdle()
        assertEquals(
            if (switchProject) 3L else 2L,
            model.uiState.value.activeAlert
                ?.id,
        )
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(if (switchProject) 8L else 7L, model.uiState.value.projectId)
        assertEquals(
            if (switchProject) 3L else 2L,
            model.uiState.value.activeAlert
                ?.id,
        )
        assertEquals(second, rows.value.single { it.id == 2L })
        assertCommittedMutation(reminders, first, rows.value, edit)
    }

    private fun assertCommittedMutation(
        reminders: ReminderRepository,
        first: RowReminder,
        rows: List<RowReminder>,
        edit: Boolean,
    ) {
        if (edit) {
            coVerify(exactly = 1) {
                reminders.update(
                    7,
                    match {
                        it.id == 1L &&
                            it.targetRow == 20 &&
                            it.message == "Edited"
                    },
                    first,
                )
            }
            assertEquals(20, rows.single { it.id == 1L }.targetRow)
        } else {
            coVerify(exactly = 1) { reminders.delete(7, 1) }
            assertEquals(false, rows.any { it.id == 1L })
        }
        coVerify(exactly = 0) { reminders.delete(8, any()) }
        coVerify(exactly = 0) { reminders.update(8, any(), any()) }
    }
}
