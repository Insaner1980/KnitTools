package com.finnvek.knittools.ui.screens.pro

import android.content.res.Configuration
import android.icu.text.ListFormatter
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyFacts
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
            val visible = rememberTrialJourneyVisible(activePresentation(), eligible.value, {})
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
