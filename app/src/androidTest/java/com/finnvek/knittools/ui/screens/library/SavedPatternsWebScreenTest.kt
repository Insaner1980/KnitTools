package com.finnvek.knittools.ui.screens.library

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SavedPatternsWebScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun largeFontReservesSelectionSpaceAndRestoresNormalInset() {
        val selection = mutableStateOf(false)
        val selected = mutableStateOf(false)
        val title = "Long cable cardigan pattern with detailed instructions"
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                KnitToolsTheme {
                    SavedPatternsScreen(
                        state = state(listOf(webPattern().copy(name = title, designerName = "Pattern designer"))).copy(
                            isSelectMode = selection.value,
                            selectedPatternIds = if (selected.value) setOf(7L) else emptySet(),
                        ),
                        actions = actions(),
                    )
                }
            }
        }
        val density = context.resources.displayMetrics.density
        var normalTitleLeft = 0f
        for (mode in listOf("normal", "unselected", "selected", "normal-again")) {
            composeRule.runOnIdle {
                selection.value = mode == "unselected" || mode == "selected"
                selected.value = mode == "selected"
            }
            val titleBounds = composeRule.onNodeWithText(title, useUnmergedTree = true)
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val cardBounds = composeRule.onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot
            if (mode == "normal") normalTitleLeft = titleBounds.left
            if (selection.value) {
                // Indicator ends at 8 + 2 + 22 + 2 dp from the row's leading edge.
                assertTrue("Text overlaps selection slot: $mode", titleBounds.left - normalTitleLeft >= 48f * density - 1f)
            } else {
                assertTrue("Normal card reserves selection space", titleBounds.left - cardBounds.left <= 24f * density)
            }
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            java.io.File(context.getExternalFilesDir(null), "p2-web-$mode.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun emptyCollectionExposesAddWebPattern() {
        var additions = 0
        composeRule.setContent {
            KnitToolsTheme {
                SavedPatternsScreen(
                    state = state(emptyList()),
                    actions = actions(onAdd = { additions += 1 }),
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_add)).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, additions) }
    }

    @Test
    fun populatedCollectionExposesAddWebPattern() {
        var additions = 0
        composeRule.setContent {
            KnitToolsTheme {
                SavedPatternsScreen(
                    state = state(listOf(webPattern())),
                    actions = actions(onAdd = { additions += 1 }),
                )
            }
        }

        val addAction =
            composeRule.onNodeWithText(
                text = context.getString(R.string.web_pattern_add),
                useUnmergedTree = true,
            )
        addAction.assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, additions) }
    }

    @Test
    fun webRowHasSourceAwareContentAndOneClickSemanticOwner() {
        composeRule.setContent {
            KnitToolsTheme {
                SavedPatternsScreen(
                    state = state(listOf(webPattern())),
                    actions = actions(),
                )
            }
        }

        composeRule.onNodeWithText("Cable cardigan").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_label)).assertIsDisplayed()
        composeRule.onNodeWithText("example.com").assertIsDisplayed()
        composeRule.onAllNodesWithText(context.getString(R.string.availability_unknown)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Cable cardigan") and hasClickAction()).assertCountEquals(1)

        val card =
            composeRule
                .onAllNodes(hasText("Cable cardigan") and hasClickAction())
                .fetchSemanticsNodes()
                .single()
        assertEquals(
            listOf(
                "Cable cardigan",
                context.getString(R.string.web_pattern_label),
                "example.com",
            ),
            card.config[SemanticsProperties.Text].map { it.text },
        )
        val titleBounds =
            composeRule
                .onNodeWithText("Cable cardigan", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val maximumTextInset = 24f * context.resources.displayMetrics.density
        assertTrue(
            "Web row title must not reserve an empty thumbnail slot",
            titleBounds.left - card.boundsInRoot.left <= maximumTextInset,
        )
    }

    private fun state(patterns: List<SavedPattern>) =
        SavedPatternsState(
            patterns = patterns,
            isSelectMode = false,
            selectedPatternIds = emptySet(),
            deleteErrorId = 0L,
        )

    private fun actions(onAdd: () -> Unit = {}) =
        SavedPatternsActions(
            onPatternClick = {},
            onAddWebPattern = onAdd,
            onEnterSelectMode = {},
            onToggleSelection = {},
            onSelectAll = {},
            onDeleteSelected = {},
            onExitSelectMode = {},
            onBack = {},
        )

    private fun webPattern() =
        SavedPattern(
            id = 7L,
            source = SavedPatternSource.WebLink,
            name = "Cable cardigan",
            designerName = "",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
        )
}
