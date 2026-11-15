package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.screens.counter.TargetRowsDialog
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectDetailsSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultCreateDraftWorksAtZeroWithoutAdvancedFields() {
        var submitted: ProjectDetailsValues? = null
        showSheet(ProjectDetailsMode.Create) { submitted = it }
        compose.onNodeWithText(label(R.string.main_counter_label)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.project_name_label)).performTextReplacement("Socks")
        compose.onNodeWithContentDescription(label(R.string.project_name_label)).performImeAction()
        compose
            .onNodeWithContentDescription(label(R.string.project_start_count_label_rows))
            .assertIsFocused()
            .performImeAction()
        compose.onNodeWithText(label(R.string.create_project)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, submitted?.initialCount) }
    }

    @Test
    fun startingCountAndTargetUpdateHelperPreviewAndSubmittedValues() {
        var submitted: ProjectDetailsValues? = null
        showSheet(ProjectDetailsMode.Create, showPreview = true) { submitted = it }
        compose
            .onNodeWithContentDescription(label(R.string.project_name_label))
            .assertIsFocused()
            .performTextReplacement("Striped socks")
        compose
            .onNodeWithContentDescription(label(R.string.project_start_count_label_rows))
            .performScrollTo()
            .performTextReplacement("37")
        compose.onNodeWithText(context.getString(R.string.project_start_count_next_rows, 38)).assertExists()
        compose.onNodeWithText(label(R.string.project_create_more_options)).performScrollTo().performClick()
        compose
            .onNodeWithContentDescription(label(R.string.project_target_label))
            .performScrollTo()
            .performTextReplacement("120")
        val countText = context.getString(R.string.counter_value_of_target_format, 37, 120)
        val preview = context.getString(R.string.project_create_preview_a11y, "Striped socks, $countText")
        compose
            .onNodeWithContentDescription(preview)
            .performScrollTo()
            .assertExists()
        compose
            .onNodeWithText(label(R.string.create_project))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.runOnIdle {
            assertEquals("Striped socks", submitted?.name)
            assertEquals(37, submitted?.initialCount)
            assertEquals(120, submitted?.targetRows)
        }
    }

    @Test
    fun invalidStartingCountAndTargetDisableCreate() {
        showSheet(ProjectDetailsMode.Create)
        compose.onNodeWithContentDescription(label(R.string.project_name_label)).performTextReplacement("Cardigan")
        val startField = compose.onNodeWithContentDescription(label(R.string.project_start_count_label_rows))
        val targetField = compose.onNodeWithContentDescription(label(R.string.project_target_label))
        val createButton = compose.onNodeWithText(label(R.string.create_project))
        for (invalid in listOf("100000", " 1 ")) {
            startField.performScrollTo().performTextReplacement(invalid)
            createButton.performScrollTo().assertIsNotEnabled()
        }
        startField.performScrollTo().performTextReplacement("0")
        compose.onNodeWithText(label(R.string.project_create_more_options)).performScrollTo().performClick()
        for (invalid in listOf("0", " 1 ")) {
            targetField.performScrollTo().performTextReplacement(invalid)
            createButton.performScrollTo().assertIsNotEnabled()
        }
        targetField.performScrollTo().performTextReplacement("")
        createButton.performScrollTo().assertIsEnabled()
    }

    @Test
    fun targetEditorRejectsMalformedPasteAndAllowsClearing() {
        var target: Int? = 12
        compose.setContent {
            KnitToolsTheme {
                TargetRowsDialog(
                    currentTarget = 12,
                    counterLabel = "Rows",
                    onDismiss = {},
                    onConfirm = { target = it },
                    onRemove = { target = null },
                )
            }
        }
        val targetField = compose.onNode(hasSetTextAction())
        val confirm = compose.onNodeWithText(label(android.R.string.ok))
        for (invalid in listOf("100000", "1.5")) {
            targetField.performTextReplacement(invalid)
            confirm.assertIsNotEnabled()
        }
        targetField.performTextReplacement("99999")
        confirm.performClick()
        compose.runOnIdle { assertEquals(99_999, target) }
        targetField.performTextReplacement("")
        confirm.performClick()
        compose.runOnIdle { assertEquals(null, target) }
    }

    @Test
    fun editModeKeepsStartingCountAndTargetOutOfTheForm() {
        showSheet(ProjectDetailsMode.Edit)
        compose.onNodeWithContentDescription(label(R.string.project_start_count_label_rows)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertDoesNotExist()
    }

    @Test
    fun restoredAdvancedDraftRetainsValuesAcrossCollapseAndCraftChange() {
        var submitted: ProjectDetailsValues? = null
        showSheet(
            ProjectDetailsMode.Create,
            initialValues =
                ProjectDetailsValues(
                    "Scarf",
                    CraftType.KNITTING,
                    MainCounterLabelType.CUSTOM,
                    "Sections",
                    7,
                    40,
                ),
        ) { submitted = it }
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertExists()
        compose.onNodeWithText(label(R.string.project_create_fewer_options)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.craft_type_crochet)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.project_create_more_options)).performScrollTo().performClick()
        compose.onNodeWithText("Sections").assertExists()
        compose.onNodeWithText("40").assertExists()
        compose.onNodeWithText(label(R.string.create_project)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Sections", submitted?.mainCounterCustomLabel)
            assertEquals(40, submitted?.targetRows)
        }
    }

    @Test
    fun emptyCustomLabelExplainsWhyCreateIsDisabled() {
        showSheet(
            ProjectDetailsMode.Create,
            initialValues = ProjectDetailsValues("Scarf", CraftType.KNITTING, MainCounterLabelType.CUSTOM, null),
        )
        compose.onNodeWithText(label(R.string.main_counter_custom_label_error)).assertExists()
        compose.onNodeWithText(label(R.string.create_project)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun invalidAdvancedTargetCannotBeHiddenAndReceivesFocus() {
        showSheet(
            ProjectDetailsMode.Create,
            initialValues =
                ProjectDetailsValues(
                    "Scarf",
                    CraftType.KNITTING,
                    MainCounterLabelType.ROWS,
                    null,
                    0,
                    100000,
                ),
        )
        compose.onNodeWithText(label(R.string.create_project)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.project_create_fewer_options)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertIsFocused()
    }

    @Test
    fun expandedStartingCountKeyboardMovesToVisibleTarget() {
        showSheet(ProjectDetailsMode.Create)
        compose.onNodeWithText(label(R.string.project_create_more_options)).performScrollTo().performClick()
        compose
            .onNodeWithContentDescription(label(R.string.project_start_count_label_rows))
            .performScrollTo()
            .performClick()
            .performImeAction()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertIsFocused()
    }

    @Test
    fun expandedCustomStartingCountKeyboardMovesToCustomLabel() {
        showSheet(
            ProjectDetailsMode.Create,
            initialValues =
                ProjectDetailsValues("Scarf", CraftType.KNITTING, MainCounterLabelType.CUSTOM, "Sections"),
        )
        compose
            .onNodeWithContentDescription(label(R.string.project_start_count_label_custom))
            .performScrollTo()
            .performClick()
            .performImeAction()
        compose
            .onNodeWithContentDescription(label(R.string.main_counter_custom_label))
            .assertIsFocused()
            .performImeAction()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertIsFocused()
    }

    @Test
    fun restoredCollapsedAdvancedDraftReopensWithoutLosingValues() {
        val restoration = StateRestorationTester(compose)
        showSheet(
            ProjectDetailsMode.Create,
            initialValues =
                ProjectDetailsValues(
                    "Scarf",
                    CraftType.KNITTING,
                    MainCounterLabelType.CUSTOM,
                    "Sections",
                    7,
                    40,
                ),
            restorationTester = restoration,
        )
        compose.onNodeWithText("Sections").performScrollTo().performTextReplacement("Panels")
        compose.onNodeWithText("40").performScrollTo().performTextReplacement("55")
        compose.onNodeWithText(label(R.string.project_create_fewer_options)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription(label(R.string.project_target_label)).assertExists()
        compose.onNodeWithText("Panels").assertExists()
        compose.onNodeWithText("55").assertExists()
    }

    private fun showSheet(
        mode: ProjectDetailsMode,
        showPreview: Boolean = false,
        initialValues: ProjectDetailsValues =
            ProjectDetailsValues("", CraftType.KNITTING, MainCounterLabelType.ROWS, null),
        restorationTester: StateRestorationTester? = null,
        onConfirm: (ProjectDetailsValues) -> Unit = {},
    ) {
        val content: @Composable () -> Unit = {
            KnitToolsTheme {
                ProjectDetailsSheet(
                    title = label(R.string.new_project_details_title),
                    confirmText = label(R.string.create_project),
                    initialValues = initialValues,
                    mode = mode,
                    showPreview = showPreview,
                    onConfirm = onConfirm,
                    onDismiss = {},
                )
            }
        }
        if (restorationTester == null) compose.setContent(content) else restorationTester.setContent(content)
    }

    private fun label(resource: Int): String = context.getString(resource)
}
