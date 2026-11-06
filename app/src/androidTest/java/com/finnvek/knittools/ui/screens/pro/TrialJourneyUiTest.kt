package com.finnvek.knittools.ui.screens.pro

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyStep
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class TrialJourneyUiTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<App>()
    private val manager get() = app.proManager.get()
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        check(ProPolicyTestControl.isActive)
        setTestProStatus(ProStatus.TRIAL_NOT_STARTED)
    }

    @After
    fun tearDown() {
        store.clear()
        setTestProStatus(ProStatus.TRIAL_NOT_STARTED)
    }

    @Test
    fun promptQueuesStartedJourneyBeforeResumingItsPendingActionOnce() {
        val coordinator = TrialJourneyCoordinator()
        val viewModel =
            ProPromptViewModel(manager, TrialManager(app, Dispatchers.IO), coordinator, app.billingManager.get())
        store.put("prompt", viewModel)
        val visible = mutableStateOf(true)
        var resumed = 0
        ProPolicyTestControl.setTrialStartResult(TrialStartResult.Started)
        compose.waitUntil(5_000) {
            manager.initialStateReady.value &&
                manager.proState.value.status == ProStatus.TRIAL_NOT_STARTED
        }
        compose.setContent {
            KnitToolsTheme {
                if (visible.value) {
                    ProPromptSheet(
                        request = ProPromptRequest(ProPromptSource.Notes),
                        onDismiss = { visible.value = false },
                        onTrialStarted = {
                            assertTrue(coordinator.pending.value)
                            resumed++
                            visible.value = false
                        },
                        onSeePro = {},
                        viewModelProvider = { viewModel },
                    )
                }
            }
        }
        compose.onNodeWithText(app.getString(R.string.onboarding_pro_notes_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.onboarding_pro_retention_notes)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.pro_start_free_trial)).performScrollTo().performClick()
        compose.waitUntil(5_000) { resumed == 1 }
        compose.onNodeWithText(app.getString(R.string.onboarding_pro_notes_title)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, resumed) }
    }

    @Test
    fun sharedJourneyShowsProgressAndAllowsMultipleActionsUntilDismissedInBothThemes() {
        val dark = mutableStateOf(false)
        val visible = mutableStateOf(true)
        val events = mutableListOf<String>()
        val steps = TrialJourneyAction.entries.map { TrialJourneyStep(it, it == TrialJourneyAction.Widget) }
        setTestProStatus(ProStatus.TRIAL_ACTIVE)
        compose.waitUntil(5_000) {
            manager.initialStateReady.value &&
                manager.proState.value.status == ProStatus.TRIAL_ACTIVE
        }
        compose.setContent {
            KnitToolsTheme(isDarkTheme = dark.value) {
                if (visible.value) {
                    TrialJourneySheet(
                        steps = steps,
                        onDismiss = {
                            events += "dismiss"
                            visible.value = false
                        },
                        onAction = { events += it.name },
                    )
                }
            }
        }
        compose.onNodeWithText(app.getString(R.string.trial_started_screen_title)).assertIsDisplayed()
        compose
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(1f, 0f..5f),
                ),
            ).assertExists()
        compose.onNodeWithText(app.getString(R.string.trial_action_widget)).assertHasNoClickAction()
        captureIfRequested("phase9-journey-light", dialog = true)
        compose.runOnIdle { dark.value = true }
        captureIfRequested("phase9-journey-dark", dialog = true)
        compose.onNodeWithText(app.getString(R.string.trial_action_counter)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("RepeatCounter"), events) }
        compose.onNodeWithText(app.getString(R.string.trial_started_screen_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.trial_action_project)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("RepeatCounter", "AnotherProject"), events) }
        compose.onNodeWithText(app.getString(R.string.completed_close)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("RepeatCounter", "AnotherProject", "dismiss"), events) }
        compose.onNodeWithText(app.getString(R.string.trial_started_screen_title)).assertDoesNotExist()
    }

    @Test
    fun editorialProPageSharesJourneyAndKeepsTrialPurchaseSectionInBothThemes() {
        val dark = mutableStateOf(false)
        val viewModel = ProUpgradeViewModel(manager, app.billingManager.get(), TrialJourneyCoordinator())
        store.put("upgrade", viewModel)
        val steps = mutableStateOf(emptyList<TrialJourneyStep>())
        val actions = mutableListOf<TrialJourneyAction>()
        compose.waitUntil(5_000) {
            manager.initialStateReady.value &&
                manager.proState.value.status == ProStatus.TRIAL_NOT_STARTED
        }
        compose.setContent {
            KnitToolsTheme(isDarkTheme = dark.value) {
                ProUpgradeScreen(
                    onBack = {},
                    onPurchase = {},
                    viewModelProvider = { viewModel },
                    personalLine = "You have 3 projects in progress.",
                    journeySteps = steps.value,
                    onJourneyAction = { actions += it },
                )
            }
        }
        compose.onNodeWithText(app.getString(R.string.onboarding_pro_intro)).assertIsDisplayed()
        compose.onNodeWithText("You have 3 projects in progress.").assertIsDisplayed()
        captureIfRequested("phase9-pro-light")
        compose.runOnIdle { dark.value = true }
        captureIfRequested("phase9-pro-dark")
        compose.onNodeWithText(app.getString(R.string.onboarding_pro_free_backup)).performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                app.getString(R.string.onboarding_pro_group_projects_title),
            ).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.pro_start_14_day_trial)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.one_time_purchase)).performScrollTo().assertIsDisplayed()
        setTestProStatus(ProStatus.TRIAL_ACTIVE)
        compose.waitUntil(5_000) { viewModel.proState.value.status == ProStatus.TRIAL_ACTIVE }
        compose.runOnIdle { steps.value = TrialJourneyAction.entries.map { TrialJourneyStep(it, false) } }
        compose.onNodeWithText(app.getString(R.string.trial_action_project)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(TrialJourneyAction.AnotherProject), actions) }
        captureIfRequested("phase9-pro-dark-journey")
        compose.runOnIdle { dark.value = false }
        captureIfRequested("phase9-pro-light-journey")
        setTestProStatus(ProStatus.TRIAL_NOT_STARTED)
        compose.waitUntil(5_000) { viewModel.proState.value.status == ProStatus.TRIAL_NOT_STARTED }
        compose.runOnIdle { steps.value = emptyList() }
        compose.onNodeWithText(app.getString(R.string.pro_start_14_day_trial)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.one_time_purchase)).performScrollTo().assertIsDisplayed()
    }

    private fun captureIfRequested(
        name: String,
        dialog: Boolean = false,
    ) {
        if (InstrumentationRegistry.getArguments().getString("onboardingScreenshots") != "true") return
        compose.waitForIdle()
        val directory = File(app.filesDir, "onboarding-evidence")
        assertTrue(directory.exists() || directory.mkdirs())
        val target = if (dialog) compose.onNode(isDialog()) else compose.onRoot()
        val screenshot = target.captureToImage().asAndroidBitmap()
        try {
            File(directory, "$name.png").outputStream().use { output ->
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }
}
