package com.finnvek.knittools.ui.screens.pattern

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatternBookmarkDraftTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addDraftSurvivesActivityRecreationAndNewAddStartsEmpty() {
        val added = mutableListOf<String>()
        val content = content(onAdd = { added += it })
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        click(R.string.pattern_bookmark_add_here)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Unfinished add")
        recreate(content)
        composeRule.onNode(hasSetTextAction()).assertTextContains("Unfinished add")
        click(R.string.save)
        composeRule.runOnIdle { assertEquals(listOf("Unfinished add"), added) }
        click(R.string.pattern_bookmark_add_here)
        composeRule.onNode(hasSetTextAction()).assertTextContains("")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Cancelled add")
        click(R.string.cancel)
        click(R.string.pattern_bookmark_add_here)
        composeRule.onNode(hasSetTextAction()).assertTextContains("")
    }

    @Test
    fun renameDraftSurvivesActivityRecreationAndOtherBookmarkStartsWithItsOwnName() {
        val renamed = mutableListOf<Pair<Long, String>>()
        val content = content(onRename = { id, name -> renamed += id to name })
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        rename("First")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Unfinished rename")
        recreate(content)
        composeRule.onNode(hasSetTextAction()).assertTextContains("Unfinished rename")
        click(R.string.save)
        composeRule.runOnIdle { assertEquals(listOf(1L to "Unfinished rename"), renamed) }
        rename("Second")
        composeRule.onNode(hasSetTextAction()).assertTextContains("Second")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Cancelled rename")
        click(R.string.cancel)
        rename("First")
        composeRule.onNode(hasSetTextAction()).assertTextContains("First")
    }

    private fun rename(name: String) {
        val options =
            context.getString(
                R.string.pattern_bookmark_action_accessibility_description,
                context.getString(R.string.more_options),
                name,
            )
        composeRule.onNodeWithContentDescription(options).performScrollTo().performClick()
        val action =
            context.getString(
                R.string.pattern_bookmark_action_accessibility_description,
                context.getString(R.string.pattern_bookmark_rename),
                name,
            )
        composeRule.onNodeWithContentDescription(action).performClick()
    }

    private fun click(id: Int) = composeRule.onNodeWithText(context.getString(id)).performClick()

    private fun recreate(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.recreate()
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        composeRule.waitForIdle()
    }

    private fun content(
        onAdd: (String) -> Unit = {},
        onRename: (Long, String) -> Unit = { _, _ -> },
    ): @Composable () -> Unit =
        {
            KnitToolsTheme {
                PatternBookmarkSheet(
                    state =
                        PatternBookmarkUiState(
                            documentKey = "document",
                            isLoading = false,
                            bookmarks =
                                listOf("First", "Second").mapIndexed { index, name ->
                                    PatternBookmark(index + 1L, 1L, "document", name, index, 0.5f, 1L)
                                },
                        ),
                    totalPages = 2,
                    actions = PatternBookmarkSheetActions({}, onAdd, {}, {}, {}, onRename, {}, {}),
                )
            }
        }
}
