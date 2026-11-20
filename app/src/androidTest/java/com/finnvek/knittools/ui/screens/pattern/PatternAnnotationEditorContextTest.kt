package com.finnvek.knittools.ui.screens.pattern

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ChartCounterType
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatternAnnotationEditorContextTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val state =
        mutableStateOf(
            PatternAnnotationUiState(
                owner = PatternAnnotationOwner.Project(1L, "saved:1:v1"),
                editableLayerId = 2L,
            ),
        )
    private var writes = 0
    private var acknowledgeWrites = true
    private var submittedDraftId: String? = null

    @Test
    fun creationDraftsRemainUntilSuccessfulSave() {
        acknowledgeWrites = false
        showToolbar()
        for (tool in listOf(R.string.pattern_annotation_tool_text, R.string.pattern_annotation_tool_callout)) {
            click(tool)
            composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Retained draft")
            click(R.string.save)
            composeRule.runOnIdle { state.value = state.value.copy(isSaving = true) }
            composeRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
            composeRule.runOnIdle {
                state.value = state.value.copy(isSaving = false, writeError = PatternAnnotationWriteError.WRITE_FAILED)
            }
            composeRule.onAllNodes(hasSetTextAction())[0].assertTextContains("Retained draft")
            composeRule.onNodeWithText(context.getString(R.string.pattern_annotation_write_failed)).assertExists()
            click(R.string.save)
            composeRule.runOnIdle {
                state.value =
                    state.value.copy(
                        savedDraftId = submittedDraftId,
                        writeError = PatternAnnotationWriteError.NONE,
                    )
            }
            composeRule.onNodeWithText(context.getString(R.string.save)).assertDoesNotExist()
        }
        composeRule.runOnIdle { assertEquals(4, writes) }
    }

    private fun submitDraft(draftId: String) {
        writes++
        submittedDraftId = draftId
        if (acknowledgeWrites) state.value = state.value.copy(savedDraftId = draftId)
    }

    @Test
    fun textAndCalloutCannotConfirmAfterPageOrDocumentChanges() {
        showToolbar()
        for (tool in listOf(R.string.pattern_annotation_tool_text, R.string.pattern_annotation_tool_callout)) {
            for (switchDocument in listOf(false, true)) {
                click(tool)
                composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Draft")
                composeRule.runOnIdle {
                    state.value =
                        if (switchDocument) {
                            state.value.copy(
                                owner = PatternAnnotationOwner.Project(1L, "saved:2:v1"),
                                editableLayerId = checkNotNull(state.value.editableLayerId) + 1L,
                            )
                        } else {
                            state.value.copy(currentPage = state.value.currentPage + 1)
                        }
                }
                composeRule.onNodeWithText(context.getString(R.string.save)).assertDoesNotExist()
                composeRule.runOnIdle { assertEquals(0, writes) }
            }
        }
        click(R.string.pattern_annotation_tool_text)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Valid")
        click(R.string.save)
        composeRule.runOnIdle { assertEquals(1, writes) }
        click(R.string.pattern_annotation_tool_callout)
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Cancelled")
        click(R.string.cancel)
        composeRule.runOnIdle { assertEquals(1, writes) }
    }

    @Test
    fun chartDialogClosesWhenCounterIdentityOrSelectedAnnotationChanges() {
        state.value =
            state.value.copy(
                selectedAnnotationId = 10L,
                selectedAnnotationSupportsChartTracker = true,
                chartCounterOptions =
                    listOf(
                        PatternChartCounterOption(ChartCounterType.MAIN, name = "Main", value = 0),
                        PatternChartCounterOption(ChartCounterType.EXTRA, 3L, "Extra", 0),
                    ),
            )
        showToolbar()
        click(R.string.pattern_annotation_link_chart)
        composeRule.runOnIdle {
            state.value =
                state.value.copy(chartCounterOptions = state.value.chartCounterOptions.map { it.copy(value = 1) })
        }
        composeRule.onNodeWithText(context.getString(R.string.save)).assertExists()
        composeRule.runOnIdle {
            state.value = state.value.copy(chartCounterOptions = state.value.chartCounterOptions.dropLast(1))
        }
        composeRule.onNodeWithText(context.getString(R.string.save)).assertDoesNotExist()
        click(R.string.pattern_annotation_link_chart)
        composeRule.runOnIdle { state.value = state.value.copy(selectedAnnotationId = 11L) }
        composeRule.onNodeWithText(context.getString(R.string.save)).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, writes) }
    }

    private fun click(id: Int) = composeRule.onNodeWithText(context.getString(id)).performScrollTo().performClick()

    private fun showToolbar() {
        composeRule.setContent {
            KnitToolsTheme {
                PatternAnnotationToolbar(
                    state = state.value,
                    actions =
                        PatternAnnotationToolbarActions(
                            onToolSelected = {},
                            onPenArgbChange = {},
                            onHighlighterArgbChange = {},
                            onPenStrokeWidthChange = {},
                            onHighlighterStrokeWidthChange = {},
                            onPressureEnabledChange = {},
                            onHighlighterAxisLockChange = {},
                            onMoveSelected = { _, _ -> },
                            onResizeSelected = {},
                            onDuplicateSelected = {},
                            onDeleteSelected = {},
                            onBringSelectedForward = {},
                            onSendSelectedBackward = {},
                            onAddTextBox = { _, draftId -> submitDraft(draftId) },
                            onAddCallout = { _, _, _, draftId -> submitDraft(draftId) },
                            onAddChartTracker = { _, draftId -> submitDraft(draftId) },
                            onEditText = {},
                            onCancelTextEdit = {},
                            onSaveTextEdit = { _, _ -> writes++ },
                        ),
                )
            }
        }
    }
}
