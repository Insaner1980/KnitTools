package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectYarnNote
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterProjectContentCardsTest {
    @Test
    fun `empty project content cards expose the fixed square card set`() {
        val cards = projectContentCards(CounterUiState())

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
        assertEquals(R.string.project_content_attach_pattern, cards[0].titleRes)
        assertEquals(R.string.project_content_yarn, cards[1].titleRes)
        assertEquals(R.string.project_content_notes, cards[2].titleRes)
        assertEquals(R.string.project_content_photos, cards[3].titleRes)
        assertEquals(R.string.reminders, cards[4].titleRes)
    }

    @Test
    fun `filled project content cards keep preview data out of the card model`() {
        val cards =
            projectContentCards(
                CounterUiState(
                    counter = CounterState(count = 9),
                    patternUri = "file:///two_sleeves_one_promise.pdf",
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
        assertEquals(R.string.project_content_yarn, cards[1].titleRes)
        assertEquals(R.string.project_content_notes, cards[2].titleRes)
        assertEquals(R.string.project_content_photos, cards[3].titleRes)
        assertEquals(R.string.reminders, cards[4].titleRes)
    }
}
