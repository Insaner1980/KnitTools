package com.finnvek.knittools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.ui.theme.ProMarkerDimens
import com.finnvek.knittools.ui.theme.ProjectListDimens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProMarkerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allowedAndUnknownAccessRenderNoMarker() {
        val access = mutableStateOf(ProActionAccess.Allowed)
        compose.setContent { KnitToolsTheme { ProMarker(access.value) } }
        assertNoMarker()
        compose.runOnIdle { access.value = ProActionAccess.Unknown }
        assertNoMarker()
    }

    @Test
    fun lockedAndTrialMarkersExposeTheirDistinctMeaningAndSharedHeight() {
        val access = mutableStateOf(ProActionAccess.Locked)
        compose.setContent { KnitToolsTheme { ProMarker(access.value) } }
        compose
            .onNodeWithContentDescription(text(R.string.pro_badge_locked_description))
            .assertIsDisplayed()
            .assertHeightIsEqualTo(ProMarkerDimens.Height)
        compose.runOnIdle { access.value = ProActionAccess.Trial }
        compose
            .onNodeWithContentDescription(text(R.string.pro_badge_trial_description))
            .assertIsDisplayed()
            .assertHeightIsEqualTo(ProMarkerDimens.Height)
        compose.onNodeWithContentDescription(text(R.string.pro_badge_locked_description)).assertDoesNotExist()
    }

    @Test
    fun clickableRowMergesMarkerMeaningIntoItsSingleAction() {
        var clicks = 0
        compose.setContent {
            KnitToolsTheme {
                Row(Modifier.clickable { clicks++ }) {
                    Text("Add counter")
                    ProMarker(ProActionAccess.Locked)
                }
            }
        }
        val description = text(R.string.pro_badge_locked_description)
        compose.onNodeWithText("Add counter").assert(hasContentDescription(description)).performClick()
        compose.onAllNodes(hasContentDescription(description)).assertCountEquals(1)
        assertEquals(1, clicks)
    }

    @Test
    fun clearedPillUsesTheSameAccessForItsVisualAndAccessibleName() {
        val access = mutableStateOf(ProActionAccess.Locked)
        var clicks = 0
        compose.setContent {
            KnitToolsTheme {
                val description = proMarkerDescription(access.value)
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = "New Project",
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = { clicks++ },
                    badge = { ProMarker(access.value) },
                    badgeContentDescription = description,
                )
            }
        }
        compose
            .onNodeWithContentDescription("New Project")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("New Project", text(R.string.pro_badge_locked_description)),
                ),
            ).performClick()
        compose.runOnIdle { access.value = ProActionAccess.Trial }
        compose
            .onNodeWithContentDescription("New Project")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("New Project", text(R.string.pro_badge_trial_description)),
                ),
            ).performClick()
        assertEquals(2, clicks)
    }

    private fun assertNoMarker() {
        compose.onNodeWithContentDescription(text(R.string.pro_badge_locked_description)).assertDoesNotExist()
        compose.onNodeWithContentDescription(text(R.string.pro_badge_trial_description)).assertDoesNotExist()
    }

    private fun text(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
