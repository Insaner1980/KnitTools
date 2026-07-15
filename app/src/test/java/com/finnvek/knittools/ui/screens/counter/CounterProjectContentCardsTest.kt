package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterProjectContentCardsTest {
    @Test
    fun `empty project content cards expose the fixed square card set`() {
        val cards = projectContentCards()

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
        assertEquals(R.string.project_content_pattern, cards[0].titleRes)
        assertEquals(R.string.project_content_yarn, cards[1].titleRes)
        assertEquals(R.string.project_content_notes, cards[2].titleRes)
        assertEquals(R.string.project_content_photos, cards[3].titleRes)
        assertEquals(R.string.reminders, cards[4].titleRes)
    }

    @Test
    fun `project content pattern card always uses a noun title`() {
        val cards = projectContentCards()

        assertEquals(R.string.project_content_pattern, cards[0].titleRes)
        assertEquals(ProjectContentCardKind.PATTERN, cards[0].kind)
    }

    @Test
    fun `project content card source centers reminder tile and maps accents to theme tokens`() {
        val source = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)

        assertTrue(source.contains("take(4).chunked(2)"))
        assertFalse(source.contains("ProjectContentIconWell("))
        assertTrue(source.contains("titleRes = R.string.project_content_pattern"))
        assertTrue(source.contains("ProjectContentCardKind.PATTERN -> MaterialTheme.colorScheme.primary"))
        assertTrue(source.contains("ProjectContentCardKind.YARN -> MaterialTheme.colorScheme.secondary"))
        assertTrue(source.contains("ProjectContentCardKind.NOTES -> MaterialTheme.knitToolsColors.brandWine"))
        assertTrue(source.contains("ProjectContentCardKind.PHOTOS -> MaterialTheme.colorScheme.tertiary"))
        assertTrue(source.contains("ProjectContentCardKind.REMINDER -> MaterialTheme.knitToolsColors.tealAccent"))
        assertTrue(source.contains("horizontalArrangement = Arrangement.Center"))
        assertTrue(source.contains("ProjectCardIconTitleSpacing"))
        assertTrue(dimens.contains("ProjectCardIconSize = 56.dp"))
        assertFalse(source.contains("aspectRatio(1f),\n                    )\n                }"))
    }

    private companion object {
        const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        const val COUNTER_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt"
    }
}
