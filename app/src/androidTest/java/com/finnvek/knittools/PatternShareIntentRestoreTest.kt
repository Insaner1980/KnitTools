package com.finnvek.knittools

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.ui.navigation.PatternShareCoordinatorViewModel
import com.finnvek.knittools.ui.navigation.PatternShareOfferResult
import com.finnvek.knittools.ui.navigation.PatternSharePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternShareIntentRestoreTest {
    @Test
    fun consumedShareDoesNotReplayWhenRecreationReceivesOriginalSystemIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://example.org/first")
                putExtra(Intent.EXTRA_SUBJECT, "Synthetic pattern")
            }
        val launch = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            scenario.onActivity {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, Intent(original))
            }
            awaitConsumed(scenario)
            var originalIntentRestored = false
            val restoreIntent =
                ActivityLifecycleCallback { activity, stage ->
                    if (activity is MainActivity && stage == Stage.PRE_ON_CREATE) {
                        activity.intent = Intent(original)
                        originalIntentRestored = true
                    }
                }
            val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
            lifecycle.addLifecycleCallback(restoreIntent)
            try {
                scenario.recreate()
            } finally {
                lifecycle.removeLifecycleCallback(restoreIntent)
            }
            assertTrue("Original system intent must be restored before onCreate", originalIntentRestored)
            awaitConsumed(scenario)
            scenario.onActivity { activity ->
                val coordinator = ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java]
                val offered = coordinator.offer(PatternSharePayload.WebLink("https://example.org/second", "Second"))
                assertTrue(offered is PatternShareOfferResult.Accepted)
                assertEquals(2L, (offered as PatternShareOfferResult.Accepted).request.requestId)
                coordinator.acknowledge(offered.request.requestId)
            }
        }
    }

    private fun awaitConsumed(scenario: ActivityScenario<MainActivity>) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var consumed = false
            scenario.onActivity { activity ->
                consumed =
                    activity.intent.action == Intent.ACTION_MAIN &&
                    !activity.intent.hasExtra(Intent.EXTRA_TEXT) &&
                    ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java].pending.value == null
            }
            if (consumed) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Share was not acknowledged and cleared")
    }
}
