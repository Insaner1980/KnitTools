package com.finnvek.knittools.ui.screens.pattern

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
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
    fun selectedBookmarkSemanticsSurviveErrorPresentation() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent(content = content(error = PatternBookmarkError.SAVE_FAILURE))
        }
        composeRule.onNodeWithText("First").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
        )
        composeRule.onNodeWithText("Second").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, false),
        )
        composeRule.onNodeWithText(context.getString(R.string.dismiss)).assertExists()
    }

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
        composeRule.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Cancelled add")
        click(R.string.cancel)
        click(R.string.pattern_bookmark_add_here)
        composeRule.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
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
        composeRule
            .onNodeWithContentDescription(options)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        val action =
            context.getString(
                R.string.pattern_bookmark_action_accessibility_description,
                context.getString(R.string.pattern_bookmark_rename),
                name,
            )
        composeRule.onNodeWithContentDescription(action).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    // Semantic actions keep dialog-window transition timing out of these draft-state assertions.
    private fun click(id: Int) =
        composeRule.onNodeWithText(context.getString(id)).performSemanticsAction(SemanticsActions.OnClick) { it() }

    private fun recreate(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.recreate()
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        composeRule.waitForIdle()
    }

    private fun content(
        error: PatternBookmarkError? = null,
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
                            selectedBookmarkId = 1L,
                            error = error,
                            bookmarks =
                                listOf("First", "Second").mapIndexed { index, name ->
                                    PatternBookmark(index + 1L, 1L, "document", name, index, 0.5f, 1L)
                                },
                        ),
                    totalPages = 2,
                    actions =
                        PatternBookmarkSheetActions(
                            onDismiss = {},
                            onAdd = { name, saved ->
                                onAdd(name)
                                saved()
                            },
                            onJump = {},
                            onPrevious = {},
                            onNext = {},
                            onRename = { id, name, saved ->
                                onRename(id, name)
                                saved()
                            },
                            onDelete = {},
                            onClearError = {},
                        ),
                )
            }
        }
}
