package com.finnvek.knittools.ui.screens.pro

import android.content.res.Configuration
import android.icu.text.ListFormatter
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyFacts
import com.finnvek.knittools.pro.TrialJourneyStep
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TrialJourneyPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pendingWaitsForReadinessAndActiveStateWithoutBeingConsumed() {
        compose.mainClock.autoAdvance = false
        val presentation = mutableStateOf(TrialJourneyPresentation(pending = true))
        var consumed = 0
        compose.setContent {
            val visible =
                rememberTrialJourneyVisible(
                    presentation.value,
                    eligible = presentation.value.ready,
                    onConsume = { consumed++ },
                )
            Text(if (visible) "Visible" else "Hidden")
        }
        advance(2_500)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle {
            assertEquals(0, consumed)
            presentation.value = presentation.value.copy(ready = true)
        }
        advance(2_500)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle {
            assertEquals(0, consumed)
            presentation.value = presentation.value.copy(status = ProStatus.TRIAL_ACTIVE)
        }
        advance(1_000)
        compose.onNodeWithText("Hidden").assertExists()
        advance(1_200)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle { assertEquals(0, consumed) }
    }

    @Test
    fun blockerRestartsTheWholeSettlingDelay() {
        compose.mainClock.autoAdvance = false
        val eligible = mutableStateOf(true)
        compose.setContent {
            val visible = rememberTrialJourneyVisible(activePresentation(), eligible.value, onConsume = {})
            Text(if (visible) "Visible" else "Hidden")
        }
        advance(1_000)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle { eligible.value = false }
        advance(2_500)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle { eligible.value = true }
        advance(1_500)
        compose.onNodeWithText("Hidden").assertExists()
        advance(700)
        compose.onNodeWithText("Visible").assertExists()
    }

    @Test
    fun actualFormSheetWindowFocusDefersJourneyUntilTheSheetCloses() {
        val presentation = mutableStateOf(activePresentation().copy(pending = false))
        val formOpen = mutableStateOf(true)
        val rootFocused = AtomicBoolean(true)
        var consumed = 0
        compose.setContent {
            KnitToolsTheme {
                val focused = LocalWindowInfo.current.isWindowFocused
                SideEffect { rootFocused.set(focused) }
                val visible =
                    rememberTrialJourneyVisible(
                        presentation.value,
                        eligible = presentation.value.ready && focused,
                        onConsume = { consumed++ },
                    )
                Text(if (visible) "Visible" else "Hidden")
                CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
                    if (formOpen.value) {
                        FormSheet(
                            title = "Existing form",
                            confirm = FormSheetConfirm("Finish form", { formOpen.value = false }),
                            onDismiss = { formOpen.value = false },
                        ) { Text("Form contents") }
                    }
                }
            }
        }
        compose.onNodeWithText("Existing form").assertExists()
        compose.waitUntil(5_000) { !rootFocused.get() }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { presentation.value = presentation.value.copy(pending = true) }
        advance(2_500)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle { assertEquals(0, consumed) }

        compose.onNodeWithText("Finish form").performClick()
        compose.waitUntil(5_000) {
            compose.mainClock.advanceTimeByFrame()
            rootFocused.get()
        }
        compose.onNodeWithText("Existing form").assertDoesNotExist()
        advance(1_500)
        compose.onNodeWithText("Hidden").assertExists()
        advance(700)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle { assertEquals(0, consumed) }
    }

    @Test
    fun visibleJourneySurvivesOwnSheetFocusLossAndSavedStateButExpiryConsumesIt() {
        compose.mainClock.autoAdvance = false
        val presentation = mutableStateOf(activePresentation())
        val eligible = mutableStateOf(true)
        var consumed = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val visible =
                rememberTrialJourneyVisible(presentation.value, eligible.value) {
                    consumed++
                    presentation.value = presentation.value.copy(pending = false)
                }
            Text(if (visible) "Visible" else "Hidden")
        }
        advance(2_500)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle { eligible.value = false }
        advance(100)
        compose.onNodeWithText("Visible").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        advance(100)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle {
            assertEquals(0, consumed)
            presentation.value = presentation.value.copy(status = ProStatus.TRIAL_EXPIRED)
        }
        advance(100)
        compose.onNodeWithText("Hidden").assertExists()
        advance(2_500)
        compose.runOnIdle { assertEquals(1, consumed) }
    }

    @Test
    fun chartStaysUnobstructedAcrossRestorationAndJourneyReturnsAfterLeaving() {
        compose.mainClock.autoAdvance = false
        val entryId = mutableStateOf<String?>("projects")
        val actionVersion = mutableIntStateOf(0)
        lateinit var deferredEntry: MutableState<String?>
        var consumed = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            deferredEntry = rememberTrialJourneyDeferredEntry(entryId.value)
            val visible =
                rememberTrialJourneyVisible(
                    activePresentation(),
                    eligible = entryId.value != null && deferredEntry.value == null,
                    actionVersion = actionVersion.intValue,
                    onConsume = { consumed++ },
                )
            Text(if (visible) "Visible" else "Hidden")
        }
        advance(2_500)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle {
            actionVersion.intValue++
            deferredEntry.value = "insights"
        }
        advance(5_000)
        compose.onNodeWithText("Hidden").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        advance(100)
        compose.runOnIdle {
            assertEquals("insights", deferredEntry.value)
            entryId.value = "insights"
        }
        advance(5_000)
        compose.onNodeWithText("Hidden").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        advance(5_000)
        compose.onNodeWithText("Hidden").assertExists()
        compose.runOnIdle { entryId.value = null }
        advance(100)
        compose.runOnIdle { assertEquals("insights", deferredEntry.value) }
        compose.runOnIdle { entryId.value = "projects" }
        advance(2_500)
        compose.onNodeWithText("Visible").assertExists()
        compose.runOnIdle {
            assertEquals(null, deferredEntry.value)
            assertEquals(0, consumed)
            entryId.value = "insights"
        }
        advance(2_500)
        compose.onNodeWithText("Visible").assertExists()
    }

    @Test
    fun newDeferredEntryWaitsForItsOwnArrivalBeforeClearing() {
        val entryId = mutableStateOf("first-chart")
        lateinit var deferredEntry: MutableState<String?>
        compose.setContent { deferredEntry = rememberTrialJourneyDeferredEntry(entryId.value) }
        compose.runOnIdle { deferredEntry.value = "first-chart" }
        compose.waitForIdle()
        compose.runOnIdle { deferredEntry.value = "second-chart" }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("second-chart", deferredEntry.value)
            entryId.value = "second-chart"
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("second-chart", deferredEntry.value)
            entryId.value = "projects"
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(null, deferredEntry.value) }
    }

    @Test
    fun actionsReturnToJourneyAfterTheirFormClosesUntilExplicitDismissal() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val presentation = mutableStateOf(activePresentation())
        val actionVersion = mutableIntStateOf(0)
        val form = mutableStateOf<TrialJourneyAction?>(null)
        val actions = mutableListOf<TrialJourneyAction>()
        var consumed = 0
        compose.setContent {
            KnitToolsTheme {
                val visible =
                    rememberTrialJourneyVisible(
                        presentation.value,
                        eligible = form.value == null && LocalWindowInfo.current.isWindowFocused,
                        actionVersion = actionVersion.intValue,
                        onConsume = {
                            consumed++
                            presentation.value = presentation.value.copy(pending = false)
                        },
                    )
                if (visible) {
                    TrialJourneySheet(
                        steps = TrialJourneyAction.entries.map { TrialJourneyStep(it, false) },
                        onAction = {
                            actions += it
                            actionVersion.intValue++
                            form.value = it
                        },
                        onDismiss = {
                            consumed++
                            presentation.value = presentation.value.copy(pending = false)
                        },
                    )
                }
                if (form.value != null) {
                    FormSheet(
                        title = "Journey action form",
                        confirm = FormSheetConfirm("Finish action", { form.value = null }),
                        onDismiss = { form.value = null },
                    ) { Text("Action contents") }
                }
            }
        }
        val title = resources.getString(R.string.trial_started_screen_title)
        val actionRows =
            listOf(
                R.string.trial_action_widget to TrialJourneyAction.Widget,
                R.string.trial_action_counter to TrialJourneyAction.RepeatCounter,
                R.string.trial_action_project to TrialJourneyAction.AnotherProject,
            )
        actionRows.forEach { (label, action) ->
            compose.waitUntil(5_000) { compose.onAllNodes(hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(resources.getString(label)).performScrollTo().performClick()
            compose.onNodeWithText("Journey action form").assertExists()
            compose.mainClock.autoAdvance = false
            advance(2_500)
            compose.onNodeWithText(title).assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(action, actions.last())
                assertEquals(0, consumed)
            }
            compose.mainClock.autoAdvance = true
            compose.onNodeWithText("Finish action").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(title)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(resources.getString(R.string.completed_close)).performScrollTo().performClick()
        compose.mainClock.autoAdvance = false
        advance(2_500)
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(actionRows.map { it.second }, actions)
            assertEquals(1, consumed)
        }
    }

    @Test
    fun summaryUsesActualLocaleCountsAndOmitsZeroCategories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locale = Locale.forLanguageTag("de-DE")
        val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val resources = context.createConfigurationContext(configuration).resources
        val currentFacts = mutableStateOf<TrialJourneyFacts?>(facts(projects = 2, counters = 1, photos = 3))
        val afterTrial = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalResources provides resources) {
                Text(trialSummaryText(currentFacts.value, afterTrial.value) ?: "No summary")
            }
        }
        val parts =
            listOf(
                resources.getQuantityString(R.plurals.trial_summary_projects, 2, 2),
                resources.getQuantityString(R.plurals.trial_summary_counters, 1, 1),
                resources.getQuantityString(R.plurals.trial_summary_photos, 3, 3),
            )
        val localizedList = ListFormatter.getInstance(locale).format(parts)
        compose.onNodeWithText(resources.getString(R.string.trial_summary_so_far, localizedList)).assertExists()
        compose.runOnIdle { afterTrial.value = true }
        compose.onNodeWithText(resources.getString(R.string.trial_summary_during, localizedList)).assertExists()
        compose.runOnIdle { currentFacts.value = facts() }
        compose.onNodeWithText("No summary").assertExists()
        compose.runOnIdle { currentFacts.value = null }
        compose.onNodeWithText("No summary").assertExists()
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun activePresentation() =
        TrialJourneyPresentation(
            status = ProStatus.TRIAL_ACTIVE,
            ready = true,
            pending = true,
        )

    private fun facts(
        projects: Int = 0,
        counters: Int = 0,
        photos: Int = 0,
    ) = TrialJourneyFacts(
        trialStartTimestamp = 1L,
        activeProjectCount = 1,
        latestActiveProject = null,
        projectsAdded = projects,
        countersAdded = counters,
        yarnCardsAdded = 0,
        photosAdded = photos,
        repeatCounterAdded = false,
        chartSeen = false,
        photoPdfCreated = false,
    )
}
