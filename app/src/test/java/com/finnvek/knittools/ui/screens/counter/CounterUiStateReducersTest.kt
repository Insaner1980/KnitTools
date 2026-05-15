package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterUiStateReducersTest {
    @Test
    fun `started project copies target rows immediately`() {
        val result =
            CounterUiState(targetRows = 12).withStartedProject(
                CounterProject(
                    id = 2L,
                    name = "Sukat",
                    count = 8,
                    stepSize = 2,
                    targetRows = 40,
                    totalRows = 60,
                ),
            )

        assertEquals(40, result.targetRows)
        assertEquals(60, result.totalRows)
        assertEquals(CounterState(count = 8, stepSize = 2), result.counter)
        assertNull(result.activeAlert)
    }

    @Test
    fun `started project clears counters from previous project`() {
        val previousCounter = ProjectCounter(id = 10L, projectId = 1L, name = "Old sleeve")
        val result =
            CounterUiState(projectCounters = listOf(previousCounter)).withStartedProject(
                CounterProject(id = 2L, name = "New project", count = 1),
            )

        assertTrue(result.projectCounters.isEmpty())
    }

    @Test
    fun `started project clears linked pattern from previous project`() {
        val stalePattern = SavedPattern(id = 4L, ravelryId = 4, name = "Old pattern", designerName = "Designer")
        val result =
            CounterUiState(linkedPattern = stalePattern).withStartedProject(
                CounterProject(id = 2L, name = "New project", count = 1),
            )

        assertNull(result.linkedPattern)
    }

    @Test
    fun `observed external row change clears stale undo value`() {
        val result =
            CounterUiState(
                counter = CounterState(count = 10, previousCount = 8, stepSize = 2),
            ).withObservedProject(
                CounterProject(id = 2L, name = "Sukat", count = 11, stepSize = 1),
            )

        assertEquals(CounterState(count = 11, stepSize = 1), result.counter)
    }

    @Test
    fun `observed self echo keeps local undo value`() {
        val result =
            CounterUiState(
                counter = CounterState(count = 10, previousCount = 8, stepSize = 2),
            ).withObservedProject(
                CounterProject(id = 2L, name = "Sukat", count = 10, stepSize = 2),
            )

        assertEquals(8, result.counter.previousCount)
    }

    @Test
    fun `observed project ignores reminders from previous project`() {
        val previousProjectReminder = RowReminder(projectId = 1L, targetRow = 10, message = "Old")
        val result =
            CounterUiState(
                projectId = 2L,
                counter = CounterState(count = 10),
                reminders = listOf(previousProjectReminder),
                activeAlert = null,
            ).withObservedProject(
                CounterProject(id = 2L, name = "Sukat", count = 10),
            )

        assertNull(result.activeAlert)
    }

    @Test
    fun `observed project clears linked pattern when project link is removed`() {
        val stalePattern = SavedPattern(id = 4L, ravelryId = 4, name = "Old pattern", designerName = "Designer")
        val result =
            CounterUiState(linkedPattern = stalePattern).withObservedProject(
                CounterProject(id = 2L, name = "Sukat", count = 10, linkedPatternId = null),
            )

        assertNull(result.linkedPattern)
    }

    @Test
    fun `counter change recomputes active alert from new row`() {
        val staleReminder = RowReminder(projectId = 2L, targetRow = 5, message = "Marker")
        val result =
            CounterUiState(
                counter = CounterState(count = 5, previousCount = 4),
                reminders = listOf(staleReminder),
                activeAlert = staleReminder,
            ).withCounterChange(CounterState(count = 0, previousCount = 5), resetStitch = false)

        assertEquals(0, result.counter.count)
        assertNull(result.activeAlert)
    }

    @Test
    fun `dismissed repeating reminder stays hidden when reminder list refreshes on same row`() {
        val reminder = RowReminder(id = 7L, projectId = 2L, targetRow = 8, repeatInterval = 8, message = "Cable")
        val dismissed =
            CounterUiState(
                projectId = 2L,
                counter = CounterState(count = 8),
                reminders = listOf(reminder),
                activeAlert = reminder,
            ).withDismissedReminder(7L)

        val refreshed = dismissed.withReminderList(listOf(reminder))

        assertNull(refreshed.activeAlert)
    }

    @Test
    fun `dismissed repeating reminder can trigger again on a later matching row`() {
        val reminder = RowReminder(id = 7L, projectId = 2L, targetRow = 8, repeatInterval = 8, message = "Cable")
        val dismissed =
            CounterUiState(
                projectId = 2L,
                counter = CounterState(count = 8),
                reminders = listOf(reminder),
                activeAlert = reminder,
            ).withDismissedReminder(7L)

        val nextRepeat = dismissed.withCounterChange(CounterState(count = 16, previousCount = 8), resetStitch = false)

        assertEquals(reminder, nextRepeat.activeAlert)
    }
}
