package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternViewerSourceTest {
    @Test
    fun `renderer errors are not displayed from raw exception messages`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertFalse(source.contains("rendererError = error.message"))
        assertTrue(source.contains("rendererError = patternOpenFailed"))
    }

    @Test
    fun `project pattern viewer uses persisted reading line state`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("readingLineEnabled = counterState.readingLineEnabled"))
        assertTrue(source.contains("readingLineYFraction = counterState.readingLineYFraction"))
        assertTrue(source.contains("onReadingLineToggle = counterViewModel::setReadingLineEnabled"))
        assertTrue(source.contains("onReadingLineYFractionChange = counterViewModel::updateReadingLineYFraction"))
    }

    @Test
    fun `library pattern viewer keeps reading line state session local`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("var readingLineEnabled by rememberSaveable(patternUri)"))
        assertTrue(source.contains("var readingLineYFraction by rememberSaveable(patternUri)"))
        assertTrue(source.contains("readingLineEnabled = readingLineEnabled"))
        assertTrue(source.contains("onReadingLineToggle = { readingLineEnabled = it }"))
    }

    @Test
    fun `reading line is toggled from pattern viewer overflow and drawn in transformed pdf layer`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("R.string.pattern_show_reading_line"))
        assertTrue(source.contains("R.string.pattern_hide_reading_line"))
        assertTrue(source.contains(".transformable(state = transformableState)"))
        assertTrue(source.contains("ReadingLineOverlay("))
        assertTrue(source.contains("dragAmount / scale"))
        assertTrue(source.contains("READING_LINE_MIN_Y_FRACTION"))
        assertTrue(source.contains("READING_LINE_MAX_Y_FRACTION"))
    }

    @Test
    fun `reading line controls stay out of counter screen`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertFalse(counterScreen.contains("pattern_show_reading_line"))
        assertFalse(counterScreen.contains("pattern_hide_reading_line"))
        assertFalse(counterScreen.contains("ReadingLineOverlay"))
    }

    private companion object {
        const val PATTERN_VIEWER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
    }
}
