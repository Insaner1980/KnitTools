package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterUiStateReducersTest {
    @Test
    fun `counter route waits for initial project load before leaving`() {
        assertFalse(CounterUiState().shouldLeaveCounter)
    }

    @Test
    fun `counter route leaves after an empty project list is loaded`() {
        assertTrue(CounterUiState(projectsLoaded = true).shouldLeaveCounter)
    }

    @Test
    fun `counter route stays when a project exists`() {
        val state =
            CounterUiState(
                projectsLoaded = true,
                projects = listOf(CounterProject(id = 2L, name = "Sukat")),
            )

        assertFalse(state.shouldLeaveCounter)
    }

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
    fun `started and observed project preserve completed state`() {
        val started =
            CounterUiState().withStartedProject(
                CounterProject(id = 2L, name = "Completed", isCompleted = true),
            )
        val reopened = started.withObservedProject(CounterProject(id = 2L, name = "Active"))

        assertTrue(started.isCompleted)
        assertFalse(reopened.isCompleted)
    }

    @Test
    fun `timer update does not change counter hero state`() {
        val state =
            CounterUiState(
                projectName = "Sukat",
                counter = CounterState(count = 12, stepSize = 2),
                sessionSeconds = 5L,
                stitchCount = 48,
                stitchTrackingEnabled = true,
                currentStitch = 7,
                targetRows = 40,
            )

        val nextSecond = state.copy(sessionSeconds = 6L)
        val nextRow = state.copy(counter = state.counter.copy(count = 13))

        assertEquals(state.toCounterHeroState(), nextSecond.toCounterHeroState())
        assertNotEquals(state.toCounterHeroState(), nextRow.toCounterHeroState())
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
    fun `started project clears yarn and reminder state from previous project`() {
        val previousReminder = RowReminder(id = 20L, projectId = 1L, targetRow = 4, message = "Old reminder")
        val previousYarnNote = ProjectYarnNote(id = 30L, projectId = 1L, name = "Old yarn")
        val result =
            CounterUiState(
                linkedYarns = listOf(40L to "Old linked yarn"),
                projectYarnNotes = listOf(previousYarnNote),
                reminders = listOf(previousReminder),
                activeAlert = previousReminder,
            ).withStartedProject(
                CounterProject(id = 2L, name = "New project", count = 1),
            )

        assertTrue(result.linkedYarns.isEmpty())
        assertTrue(result.projectYarnNotes.isEmpty())
        assertTrue(result.reminders.isEmpty())
        assertNull(result.activeAlert)
    }

    @Test
    fun `started project clears linked pattern from previous project`() {
        val stalePattern =
            SavedPattern(
                id = 4L,
                source = SavedPatternSource.Ravelry,
                ravelryPatternId = 4,
                name = "Old pattern",
                designerName = "Designer",
            )
        val result =
            CounterUiState(linkedPattern = stalePattern).withStartedProject(
                CounterProject(id = 2L, name = "New project", count = 1),
            )

        assertNull(result.linkedPattern)
    }

    @Test
    fun `started project waits for canonical document reading state`() {
        val result =
            CounterUiState().withStartedProject(
                CounterProject(
                    id = 2L,
                    name = "Pattern project",
                    readingLineEnabled = true,
                    readingLineYFraction = 0.42f,
                ),
            )

        assertFalse(result.readingLineEnabled)
        assertEquals(0.5f, result.readingLineYFraction, 0.0f)
    }

    @Test
    fun `started project keeps previously used pro content available`() {
        val result =
            CounterUiState().withStartedProject(
                CounterProject(
                    id = 2L,
                    name = "Existing project",
                    secondaryCounterUsed = true,
                    notesCreated = true,
                ),
            )

        assertTrue(result.secondaryCounterUsed)
        assertTrue(result.notesCreated)
        assertTrue(result.canUseSecondaryCounter)
        assertTrue(result.canUseNotes)
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
        val stalePattern =
            SavedPattern(
                id = 4L,
                source = SavedPatternSource.Ravelry,
                ravelryPatternId = 4,
                name = "Old pattern",
                designerName = "Designer",
            )
        val result =
            CounterUiState(linkedPattern = stalePattern).withObservedProject(
                CounterProject(id = 2L, name = "Sukat", count = 10, linkedPatternId = null),
            )

        assertNull(result.linkedPattern)
    }

    @Test
    fun `observed project preserves canonical document reading state`() {
        val result =
            CounterUiState(
                readingLineEnabled = false,
                readingLineYFraction = 0.20f,
            ).withObservedProject(
                CounterProject(
                    id = 2L,
                    name = "Sukat",
                    count = 10,
                    readingLineEnabled = true,
                    readingLineYFraction = 0.75f,
                ),
            )

        assertFalse(result.readingLineEnabled)
        assertEquals(0.20f, result.readingLineYFraction, 0.0f)
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

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
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
        // CPD-ON

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

    @Test
    fun `dismissed repeating reminder reveals another active reminder on same row`() {
        val dismissedReminder =
            RowReminder(id = 7L, projectId = 2L, targetRow = 8, repeatInterval = 8, message = "Cable")
        val nextReminder =
            RowReminder(id = 8L, projectId = 2L, targetRow = 16, repeatInterval = 8, message = "Sleeve")

        val result =
            CounterUiState(
                projectId = 2L,
                counter = CounterState(count = 16),
                reminders = listOf(dismissedReminder, nextReminder),
                activeAlert = dismissedReminder,
            ).withDismissedReminder(7L)

        assertEquals(nextReminder, result.activeAlert)
    }
}
