package com.finnvek.knittools.ui

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Explicit launches must not be overlaid by the passive expired-trial notice. */
class AuditPart15LaunchTest {
    @Test fun explicitCounterWidgetAndShareLaunchesSuppressPassiveTrialNotice() {
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName.endsWith(".activepolicytest") && ProPolicyTestControl.isActive)
        val trial = TrialManager(app, Dispatchers.IO)
        val id =
            runBlocking(Dispatchers.IO) {
                app.database.get().clearAllTables()
                app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
                trial.startTrial()
                ProPolicyTestControl.setPurchased(false)
                ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
                withTimeout(10_000) {
                    app.proManager
                        .get()
                        .proState
                        .first { it.status == ProStatus.TRIAL_EXPIRED }
                }
                app.database
                    .get()
                    .counterProjectDao()
                    .insert(CounterProjectEntity(name = "Launch target"))
            }
        val intents =
            listOf(
                MainActivity.createCounterLaunchIntent(app, id),
                MainActivity.createWidgetProPromptLaunchIntent(app),
                Intent(app, MainActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://example.com/synthetic-pattern")
                },
            )
        val expectedContent =
            listOf(
                "Launch target",
                app.getString(R.string.pro_prompt_widget_title),
                app.getString(R.string.web_pattern_confirm_details),
            )
        for ((index, intent) in intents.withIndex()) {
            // Give ActivityScenario ownership of a fresh task instead of reusing the instrumentation task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                try {
                    var ready = false
                    val deadline = SystemClock.elapsedRealtime() + 15_000
                    while (!ready && SystemClock.elapsedRealtime() < deadline) {
                        scenario.onActivity { activity ->
                            ready =
                                MainActivity::class.java
                                    .getDeclaredMethod("getLaunchRequestsReady")
                                    .apply { isAccessible = true }
                                    .invoke(activity) as Boolean
                        }
                        SystemClock.sleep(50)
                    }
                    assertTrue("Launch requests loaded", ready)
                    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
                    var root = automation.rootInActiveWindow
                    while (
                        (
                            root?.packageName != app.packageName ||
                                !containsText(
                                    checkNotNull(root),
                                    expectedContent[index],
                                )
                        ) &&
                        SystemClock.elapsedRealtime() < deadline
                    ) {
                        SystemClock.sleep(50)
                        root = automation.rootInActiveWindow
                    }
                    assertTrue("App window rendered", root?.packageName == app.packageName)
                    assertTrue(
                        "Expected launch content: ${expectedContent[index]}",
                        containsText(checkNotNull(root), expectedContent[index]),
                    )
                    assertFalse(containsText(checkNotNull(root), app.getString(R.string.pro_trial_ended_title)))
                    scenario.onActivity { activity ->
                        assertTrue(
                            MainActivity::class.java
                                .getDeclaredMethod("getSuppressPassiveTrialNotice")
                                .apply { isAccessible = true }
                                .invoke(activity) as Boolean,
                        )
                    }
                } finally {
                    // Consuming the request changes its action; Scenario matches teardown events by intent.
                    scenario.onActivity { activity -> activity.intent = intent }
                }
            }
        }
    }

    private fun containsText(
        node: AccessibilityNodeInfo,
        text: String,
    ): Boolean =
        node.text?.toString() == text ||
            (0 until node.childCount).any { index ->
                node.getChild(index)?.let { containsText(it, text) } == true
            }
}
