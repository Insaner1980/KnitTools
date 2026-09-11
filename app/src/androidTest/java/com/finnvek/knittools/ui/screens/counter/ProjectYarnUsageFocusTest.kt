package com.finnvek.knittools.ui.screens.counter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ProjectYarnUsageFocusTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sheetHeightChangesDoNotStealInputFocus() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope
        composeRule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            KnitToolsTheme(isDarkTheme = false) {
                ProjectYarnUsageSheet(
                    state =
                        YarnUsageEditorState(
                            draft = YarnUsageDraft(1L, YarnUsageSource(yarnCardId = 2L), "Test yarn"),
                        ),
                    sheetState = sheetState,
                    actions =
                        YarnUsageEditorActions(
                            onEdit = { _, _, _ -> },
                            onUnit = {},
                            onConversion = {},
                            onSave = {},
                            onDelete = {},
                            onDismiss = {},
                        ),
                )
            }
        }
        composeRule.onNodeWithTag("yarn_usage_heading").assertIsFocused()
        composeRule.runOnIdle {
            assertTrue(sheetState.hasPartiallyExpandedState)
            scope.launch { sheetState.expand() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("yarn_usage_input_PLANNED").performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("yarn_usage_input_PLANNED").assertIsFocused()
        composeRule.runOnIdle { scope.launch { sheetState.partialExpand() } }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(SheetValue.PartiallyExpanded, sheetState.currentValue) }
        composeRule.onNodeWithTag("yarn_usage_input_PLANNED").assertIsFocused()
        composeRule.runOnIdle { scope.launch { sheetState.expand() } }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(SheetValue.Expanded, sheetState.currentValue) }
        composeRule.onNodeWithTag("yarn_usage_input_PLANNED").assertIsFocused()
    }
}
