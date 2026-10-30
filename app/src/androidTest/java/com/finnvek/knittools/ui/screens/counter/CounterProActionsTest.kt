package com.finnvek.knittools.ui.screens.counter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.proMarkerDescription
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CounterProActionsTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requirePolicyProfile() {
        assumeTrue(ProPolicyTestControl.isActive)
    }

    @Test
    fun repeatDiscoveryRequestsHelpAndDisappearsWhenTrialAllowsIt() {
        val state = mutableStateOf(sheetState())
        var requests = 0
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectActionsBottomSheet(state.value, callbacks(onSecondary = { requests++ }))
            }
        }
        compose.onNodeWithText(context.getString(R.string.pro_repeat_counter_row)).performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle { state.value = state.value.copy(proState = ProState(status = ProStatus.TRIAL_ACTIVE)) }
        compose.onNodeWithText(context.getString(R.string.pro_repeat_counter_row)).assertDoesNotExist()
    }

    @Test
    fun unknownAccessKeepsCreationAndPinActionsDisabled() {
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectActionsBottomSheet(sheetState().copy(proAccessReady = false), callbacks())
            }
        }
        compose.onNodeWithText(context.getString(R.string.add_counter)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.widget_add_row)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.pro_repeat_counter_row)).assertDoesNotExist()
    }

    @Test
    fun knownTrialKeepsExistingAddActionUsableWhileMarkerStateLoads() {
        var requests = 0
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectActionsBottomSheet(
                    sheetState().copy(proState = ProState(status = ProStatus.TRIAL_ACTIVE), proAccessReady = false),
                    callbacks(onAddCounter = { requests++ }),
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.add_counter)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText(context.getString(R.string.widget_add_row)).assertIsNotEnabled()
    }

    @Test
    fun reminderMarkerIsMergedIntoTheAddAction() {
        var requests = 0
        var description = ""
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                description = requireNotNull(proMarkerDescription(ProActionAccess.Trial))
                RemindersSheet(
                    reminders = emptyList(),
                    currentRow = 0,
                    proState = ProState(status = ProStatus.TRIAL_ACTIVE),
                    proAccessReady = true,
                    onAdd = { requests++ },
                    onEdit = {},
                    onDelete = {},
                    onDismiss = {},
                )
            }
        }
        compose.waitForIdle()
        compose
            .onNode(hasText(context.getString(R.string.project_overview_add)) and hasContentDescription(description))
            .assertExists()
            .performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun lockedShapingAndRepeatSectionRemainDiscoverableAndRequestPro() {
        var requests = 0
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                AddCounterDialog(
                    onSave = {},
                    onDismiss = {},
                    proState = ProState(),
                    proAccessReady = true,
                    onProRequired = { requests++ },
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.shaping)).performClick()
        compose.onNodeWithText(context.getString(R.string.repeat_section)).performClick()
        compose.runOnIdle { assertEquals(2, requests) }
    }

    @Test
    fun trialRepeatActionStartsWithTheRepeatingCounterFields() {
        compose.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                AddCounterDialog(
                    onSave = {},
                    onDismiss = {},
                    proState = ProState(status = ProStatus.TRIAL_ACTIVE),
                    proAccessReady = true,
                    onProRequired = {},
                    initialCounterType = ProjectCounterType.REPEATING,
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.repeat_every)).assertIsDisplayed()
    }

    private fun sheetState() =
        ProjectActionsSheetState(
            projectId = 7L,
            projectName = "Sample",
            craftType = CraftType.KNITTING,
            photoUri = null,
            projectCounterCount = 0,
            stitchTrackingEnabled = false,
            stitchCount = null,
            proState = ProState(),
            proAccessReady = true,
            secondaryCounterUsed = false,
            isWorkSessionActiveForProject = false,
            isCompleted = false,
        )

    private fun callbacks(
        onSecondary: () -> Unit = {},
        onAddCounter: () -> Unit = {},
    ) = ProjectActionsSheetCallbacks(
        onDismiss = {},
        onOpenOverview = {},
        onOpenCountersList = {},
        onOpenAddCounter = onAddCounter,
        onToggleStitchTracking = {},
        onOpenStitchCount = {},
        onStartWorkSession = {},
        onStopWorkSession = {},
        onShowResetDialog = {},
        onOpenSecondary = onSecondary,
    )
}
