package com.finnvek.knittools.ui.screens.pattern

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

        composeRule.onNodeWithText("1 image selected").assertIsDisplayed()
        composeRule.onNodeWithText("Page 1 of 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Move page 1 earlier").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move page 1 later").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Remove page 1").assertIsEnabled().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Create pattern PDF").assertIsEnabled().assertHeightIsAtLeast(48.dp)
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

        composeRule.onNodeWithText("Creating page 1 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("Add more").assertIsNotEnabled()
        composeRule.onNodeWithText("Create pattern PDF").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel conversion").assertIsEnabled().assertHeightIsAtLeast(48.dp)
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
            .onNodeWithText("Add more")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText("Create pattern PDF")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(72.dp)
        composeRule
            .onNodeWithText("Cancel")
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
