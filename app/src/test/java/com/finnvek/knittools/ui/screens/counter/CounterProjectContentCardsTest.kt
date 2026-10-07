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
        val cards = projectContentCards(hasPattern = false)

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
        assertEquals(R.string.project_content_add_pattern, cards[0].titleRes)
        assertEquals(R.string.project_content_yarn, cards[1].titleRes)
        assertEquals(R.string.project_content_notes, cards[2].titleRes)
        assertEquals(R.string.project_content_photos, cards[3].titleRes)
        assertEquals(R.string.reminders, cards[4].titleRes)
    }

    @Test
    fun `project content pattern card uses open title when pattern exists`() {
        val cards = projectContentCards(hasPattern = true)

        assertEquals(R.string.saved_pattern_detail_open_pattern, cards[0].titleRes)
        assertEquals(ProjectContentCardKind.PATTERN, cards[0].kind)
    }

    @Test
    fun `project content card source centers reminder tile and maps accents to theme tokens`() {
        val source = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val glyphs = ProjectSourceFiles.read(CROSS_STITCH_ICON)

        assertTrue(source.contains("take(4).chunked(2)"))
        assertFalse(source.contains("ProjectContentIconWell("))
        assertTrue(source.contains("titleRes = patternContentTitleRes(hasPattern)"))
        assertTrue(glyphs.contains("CrossStitchGlyph.PATTERN -> MaterialTheme.colorScheme.primary"))
        assertTrue(glyphs.contains("CrossStitchGlyph.YARN -> MaterialTheme.colorScheme.secondary"))
        assertTrue(glyphs.contains("CrossStitchGlyph.NOTES -> MaterialTheme.knitToolsColors.brandWine"))
        assertTrue(glyphs.contains("CrossStitchGlyph.PHOTOS -> MaterialTheme.colorScheme.tertiary"))
        assertTrue(glyphs.contains("CrossStitchGlyph.REMINDER -> MaterialTheme.knitToolsColors.tealAccent"))
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
        const val CROSS_STITCH_ICON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CrossStitchIcon.kt"
    }
}
