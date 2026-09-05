package com.finnvek.knittools.ui.screens.pattern

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import org.junit.Rule
import org.junit.Test

class PatternImageImportSurfaceTest {
    @get:Rule
    val composeRule = createComposeRule()

    // CPD-OFF: Compose-testin pintafixture pidetaan skenaarion yhteydessa.
    @Test
    fun onePageShowsAccessibleOrderControlsAndCreateAction() {
        composeRule.setContent {
            MaterialTheme {
                PatternImageImportSurface(
                    state = state(listOf(page("one"))),
                    onAddMore = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onRemove = {},
                    onCreate = {},
                    onCancel = {},
                    onPreviewFailed = {},
                )
            }
        }

        composeRule.onNodeWithText(plural(R.plurals.pattern_images_selected, 1, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.pattern_image_page_position, 1, 1)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.pattern_image_move_earlier, 1)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(string(R.string.pattern_image_move_later, 1)).assertIsNotEnabled()
        composeRule
            .onNodeWithContentDescription(string(R.string.pattern_image_remove_page, 1))
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText(string(R.string.pattern_image_create_pdf))
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
    }

    // CPD-ON

    @Test
    fun conversionProgressIsVisibleAndControlsAreDisabled() {
        composeRule.setContent {
            MaterialTheme {
                PatternImageImportSurface(
                    state =
                        state(listOf(page("one"), page("two"))).copy(
                            phase = PatternImageImportPhase.CONVERTING,
                            progress = PatternImageProgress(1, 2),
                        ),
                    onAddMore = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onRemove = {},
                    onCreate = {},
                    onCancel = {},
                    onPreviewFailed = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.pattern_image_conversion_progress, 1, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.pattern_image_add_more)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.pattern_image_create_pdf)).assertIsNotEnabled()
        composeRule
            .onNodeWithText(string(R.string.pattern_image_cancel_conversion))
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun narrowLayoutLetsLargeFontActionsGrowWithoutClipping() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        PatternImageImportSurface(
                            state = state(listOf(page("one"))),
                            onAddMore = {},
                            onMoveEarlier = {},
                            onMoveLater = {},
                            onRemove = {},
                            onCreate = {},
                            onCancel = {},
                            onPreviewFailed = {},
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithText(string(R.string.pattern_image_add_more))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText(string(R.string.pattern_image_create_pdf))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(72.dp)
        composeRule
            .onNodeWithText(string(R.string.cancel))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    private fun state(pages: List<StagedPatternPage>) =
        PatternImageImportUiState(
            projectId = 7L,
            sessionId = "session-a",
            selection = PatternImageSelection(pages),
            phase = PatternImageImportPhase.READY,
        )

    private fun string(
        @StringRes id: Int,
        vararg arguments: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *arguments)

    private fun plural(
        @PluralsRes id: Int,
        quantity: Int,
        vararg arguments: Any,
    ): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .resources
            .getQuantityString(id, quantity, *arguments)

    private fun page(id: String) =
        StagedPatternPage(
            id = id,
            sourceUri = "content://$id",
            stagedPath = "/missing/$id.img",
            byteCount = 10L,
            width = 100,
            height = 200,
        )
}
