package com.finnvek.knittools.ui.screens.pro

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.clickTrialText
import com.finnvek.knittools.pro.findTrialText
import com.finnvek.knittools.pro.waitForTrialUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in preview walkthrough. Existing projects, trial state and widgets are never reset. */
class TrialJourneyContinuationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun realActionFormsReturnToJourneyUntilClosed() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("trialJourneyPreview") == "true")
        check(context.packageName == "com.finnvek.knittools.onboardingpreview")
        val intent = Intent(context, MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        try {
            lateinit var coordinator: TrialJourneyCoordinator
            scenario.onActivity { coordinator = it.trialJourneyCoordinator }
            waitForTrialUi("An existing active trial and project are required") {
                var ready = false
                scenario.onActivity {
                    ready =
                        it.proManager.proState.value.status == ProStatus.TRIAL_ACTIVE &&
                        it.trialJourneyStateRepository.state.value
                            ?.latestActiveProject != null
                }
                ready
            }
            scenario.onActivity { coordinator.requestStartedJourney(fromOnboarding = true) }
            waitText(R.string.trial_started_screen_title)
            click(R.string.trial_action_counter)
            waitText(R.string.add_counter)
            waitText(R.string.repeating)
            SystemClock.sleep(2_500)
            assertFalse(hasText(R.string.trial_started_screen_title))
            assertTrue(coordinator.pending.value)
            click(R.string.cancel)
            waitText(R.string.trial_started_screen_title)

            click(R.string.trial_action_project)
            waitText(R.string.new_project_details_title)
            SystemClock.sleep(2_500)
            assertFalse(hasText(R.string.trial_started_screen_title))
            assertTrue(coordinator.pending.value)
            click(R.string.cancel)
            waitText(R.string.trial_started_screen_title)
            scenario.recreate()
            waitText(R.string.trial_started_screen_title)

            click(R.string.completed_close)
            waitForTrialUi("Explicit Close consumes the journey") { !coordinator.pending.value }
            scenario.recreate()
            SystemClock.sleep(2_500)
            assertFalse(hasText(R.string.trial_started_screen_title))
        } finally {
            scenario.close()
        }
    }

    private fun hasText(id: Int) = findText(context.getString(id)) != null

    private fun waitText(id: Int) = waitForTrialUi("Visible text: ${context.getString(id)}") { hasText(id) }

    private fun click(id: Int) = instrumentation.clickTrialText(context.getString(id))

    private fun findText(value: String) = instrumentation.findTrialText(value)
}
