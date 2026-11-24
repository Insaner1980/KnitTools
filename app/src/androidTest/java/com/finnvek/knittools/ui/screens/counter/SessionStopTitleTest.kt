package com.finnvek.knittools.ui.screens.counter

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.linesEndingInsideWords
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class SessionStopTitleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun finnishTitleWrapsAtWordBoundariesAtDoubleFontScale() {
        val resources =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .localizedTestContext("fi")
                .resources
        val title = resources.getString(R.string.work_session_stop_summary_title)
        assertEquals("Lopetetaanko istunto?", title)
        compose.setContent {
            CompositionLocalProvider(
                LocalResources provides resources,
                LocalDensity provides Density(LocalDensity.current.density, 2f),
            ) {
                KnitToolsTheme {
                    SessionStopSummaryDialog(
                        projectName = "Koeprojekti",
                        durationSeconds = 120L,
                        rowsWorked = 8,
                        onSave = {},
                        onDiscard = {},
                        onCancel = {},
                    )
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }

        val breaks = layouts.single().linesEndingInsideWords(Locale.forLanguageTag("fi"))
        assertTrue("Title wraps inside a word: $breaks", breaks.isEmpty())
    }
}
