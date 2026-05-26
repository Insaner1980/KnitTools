package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CounterProjectContentCardsTest {
    @Test
    fun `first project note line ignores blank lines`() {
        assertEquals(
            "Sleeve shaping starts here",
            firstProjectNoteLine("\n  \nSleeve shaping starts here\nSecond note"),
        )
        assertNull(firstProjectNoteLine(" \n\t "))
    }

    @Test
    fun `nearest upcoming reminder picks current or next incomplete row`() {
        val reminders =
            listOf(
                reminder(id = 1, row = 4),
                reminder(id = 2, row = 14),
                reminder(id = 3, row = 9),
                reminder(id = 4, row = 12, isCompleted = true),
            )

        assertEquals(3L, nearestUpcomingReminder(reminders, currentRow = 9)?.id)
        assertEquals(2L, nearestUpcomingReminder(reminders, currentRow = 10)?.id)
        assertNull(nearestUpcomingReminder(reminders, currentRow = 15))
    }

    @Test
    fun `empty project content cards expose only core add actions`() {
        val cards = projectContentCards(CounterUiState())

        assertEquals(
            listOf(
                ProjectContentCardKind.PATTERN,
                ProjectContentCardKind.YARN,
                ProjectContentCardKind.NOTES,
                ProjectContentCardKind.PHOTOS,
            ),
            cards.map { it.kind },
        )
        assertEquals(R.string.project_content_attach_pattern, cards[0].titleRes)
        assertEquals(R.string.project_content_attach_pattern_body, cards[0].bodyRes)
        assertEquals(R.string.project_content_add_yarn, cards[1].titleRes)
        assertEquals(R.string.project_content_add_yarn_body, cards[1].bodyRes)
        assertEquals(R.string.project_content_add_note, cards[2].titleRes)
        assertEquals(R.string.project_content_add_note_body, cards[2].bodyRes)
        assertEquals(R.string.project_content_add_photo, cards[3].titleRes)
        assertEquals(R.string.project_content_add_photo_body, cards[3].bodyRes)
        assertFalse(cards.any { it.kind == ProjectContentCardKind.REMINDER })
    }

    @Test
    fun `filled project content cards combine information and action state`() {
        val cards =
            projectContentCards(
                CounterUiState(
                    counter = CounterState(count = 9),
                    patternName = "two_sleeves_one_promise.pdf",
                    linkedYarns = listOf(3L to "Isager Highland Wool"),
                    projectYarnNotes =
                        listOf(
                            ProjectYarnNote(
                                projectId = 10L,
                                name = "Blue sock yarn",
                                quantity = 2,
                            ),
                        ),
                    notes = "\nFirst sleeve is moving again.\nDecrease every 8 rows.",
                    reminders =
                        listOf(
                            reminder(id = 7L, row = 12, message = "Try on before cuff"),
                            reminder(id = 8L, row = 20, message = "Switch needles"),
                        ),
                    latestPhotos =
                        listOf(
                            ProgressPhoto(
                                id = 5L,
                                projectId = 10L,
                                photoUri = "file:///photo.jpg",
                                rowNumber = 9,
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ProjectContentCardKind.PATTERN,
                ProjectContentCardKind.YARN,
                ProjectContentCardKind.NOTES,
                ProjectContentCardKind.PHOTOS,
                ProjectContentCardKind.REMINDER,
            ),
            cards.map { it.kind },
        )

        assertEquals(R.string.project_content_open_pattern, cards[0].titleRes)
        assertEquals("two_sleeves_one_promise.pdf", cards[0].bodyText)
        assertEquals(R.string.project_content_yarn, cards[1].titleRes)
        assertEquals("Isager Highland Wool, Blue sock yarn", cards[1].bodyText)
        assertEquals(R.string.project_content_notes, cards[2].titleRes)
        assertEquals("First sleeve is moving again.", cards[2].bodyText)
        assertEquals(R.string.project_content_photos, cards[3].titleRes)
        assertEquals(1, cards[3].photoCount)
        assertEquals(R.string.project_content_next_reminder, cards[4].titleRes)
        assertEquals(12, cards[4].reminderRow)
        assertEquals("Try on before cuff", cards[4].reminderMessage)
    }

    private fun reminder(
        id: Long,
        row: Int,
        message: String = "Reminder $row",
        isCompleted: Boolean = false,
    ) = RowReminder(
        id = id,
        projectId = 10,
        targetRow = row,
        message = message,
        isCompleted = isCompleted,
    )
}
