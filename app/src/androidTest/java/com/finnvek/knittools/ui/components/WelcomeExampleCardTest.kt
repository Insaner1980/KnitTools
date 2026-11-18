package com.finnvek.knittools.ui.components

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeExampleCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun plusChangesCountAndCaptionAndUndoStopsAtStartingCount() {
        var demoUses = 0
        compose.setContent {
            KnitToolsTheme { WelcomeExampleCard(onDemoUsed = { demoUses++ }) }
        }
        compose.onNodeWithContentDescription(description(23)).assertExists()
        compose
            .onNodeWithText(context.getString(R.string.welcome_demo_try))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithText(context.getString(R.string.welcome_demo_undo)).assertDoesNotExist()

        plus().performClick()
        compose.onNodeWithContentDescription(description(24)).assertExists()
        compose.onNodeWithText(context.getString(R.string.welcome_demo_after_first, 24)).assertExists()
        plus().performClick()
        compose.onNodeWithContentDescription(description(25)).assertExists()
        compose.onNodeWithText(context.getString(R.string.welcome_demo_after_second)).assertExists()

        undo().assertIsEnabled().performClick()
        compose.onNodeWithContentDescription(description(24)).assertExists()
        undo().performClick()
        compose.onNodeWithContentDescription(description(23)).assertExists()
        undo().assertIsNotEnabled()
        plus().performClick()
        compose.runOnIdle { assertEquals(1, demoUses) }
    }

    @Test
    fun countAndCaptionSurviveSavedStateRestoration() {
        var demoUses = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            KnitToolsTheme { WelcomeExampleCard(onDemoUsed = { demoUses++ }) }
        }
        plus().performClick()
        plus().performClick()
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithContentDescription(description(25)).assertExists()
        compose.onNodeWithText(context.getString(R.string.welcome_demo_after_second)).assertExists()
        undo().performClick()
        compose.onNodeWithContentDescription(description(24)).assertExists()
        plus().performClick()
        compose.runOnIdle { assertEquals(1, demoUses) }
    }

    @Test
    fun swatchGrowsToSevenRowsAndUndoMovesItBack() {
        compose.setContent {
            KnitToolsTheme { WelcomeExampleCard(onDemoUsed = {}) }
        }
        val initialHeight = swatchHeight()
        repeat(3) { plus().performClick() }
        val maximumHeight = swatchHeight()
        assertEquals(initialHeight * 7f / 4f, maximumHeight, 1f)
        plus().performClick()
        assertEquals(maximumHeight, swatchHeight(), 1f)
        repeat(4) { undo().performClick() }
        assertEquals(initialHeight, swatchHeight(), 1f)
    }

    private fun swatchHeight(): Float {
        compose.waitForIdle()
        return compose
            .onNodeWithTag("welcomeFabricSwatch", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot.height
    }

    private fun plus() = compose.onNodeWithContentDescription(context.getString(R.string.welcome_plus_a11y))

    private fun undo() = compose.onNodeWithText(context.getString(R.string.welcome_demo_undo))

    private fun description(count: Int) = context.getString(R.string.welcome_demo_a11y, count)
}
