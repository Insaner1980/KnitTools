package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

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
        composeRule.onNodeWithText(string(R.string.pattern_page_indicator, 1, 1)).assertIsDisplayed()
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

    @Test
    fun narrowActionsRemainReadableAndClickable() = verifyActions(320, 1f)

    @Test
    fun largeFontActionsRemainReadableAndClickable() = verifyActions(400, 2f)

    @Test
    fun narrowLargeFontActionsRemainReadableAndClickable() = verifyActions(320, 2f)

    private fun verifyActions(
        width: Int,
        fontScale: Float,
    ) {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(width.dp).height(640.dp)) {
                        PatternImageImportSurface(
                            state = state(listOf(page("one"), page("two"))),
                            onAddMore = { actions += "add" },
                            onMoveEarlier = { actions += "earlier" },
                            onMoveLater = { actions += "later" },
                            onRemove = { actions += "remove" },
                            onCreate = { actions += "create" },
                            onCancel = { actions += "cancel" },
                            onPreviewFailed = {},
                        )
                    }
                }
            }
        }
        listOf(R.string.pattern_image_add_more, R.string.pattern_image_create_pdf, R.string.cancel).forEach { id ->
            val label = string(id)
            composeRule
                .onNodeWithText(label)
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule
                .onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("Text layout must be measured", layouts.isEmpty())
            val snapshot =
                File(
                    InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                    "audit16-ui-$width-$fontScale.png",
                )
            snapshot.outputStream().use {
                composeRule
                    .onRoot()
                    .captureToImage()
                    .asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            assertTrue(
                "Text must not be clipped at $width dp and $fontScale font scale: $label; " +
                    layouts.joinToString {
                        "size=${it.size}, paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}, lines=${it.lineCount}"
                    },
                layouts.all { layout ->
                    val glyphs =
                        layout.layoutInput.text.indices
                            .filterNot { layout.layoutInput.text[it].isWhitespace() }
                            .map(layout::getBoundingBox)
                    !layout.multiParagraph.didExceedMaxLines &&
                        (0 until layout.lineCount).none(layout::isLineEllipsized) &&
                        glyphs.all { bounds ->
                            bounds.left >= -0.5f &&
                                bounds.top >= -0.5f &&
                                bounds.right <= layout.size.width + 0.5f &&
                                bounds.bottom <= layout.size.height + 0.5f
                        }
                },
            )
            composeRule.onNodeWithText(label).performTouchInput { click() }
        }
        composeRule
            .onNodeWithContentDescription(string(R.string.pattern_image_move_later, 1))
            .performScrollTo()
            .assertIsDisplayed()
            .performTouchInput { click() }
        composeRule
            .onNodeWithContentDescription(string(R.string.pattern_image_remove_page, 1))
            .performScrollTo()
            .assertIsDisplayed()
            .performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(listOf("add", "create", "cancel", "later", "remove"), actions) }
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
