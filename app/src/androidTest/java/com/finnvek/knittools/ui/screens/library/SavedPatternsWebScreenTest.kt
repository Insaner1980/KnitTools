package com.finnvek.knittools.ui.screens.library

import androidx.compose.ui.semantics.SemanticsProperties
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
