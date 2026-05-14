package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.RowReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
