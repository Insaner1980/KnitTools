package com.finnvek.knittools.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.widget.CounterWidgetPinHost
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.deleteTestActiveSession
import com.finnvek.knittools.ui.TestContentSplashRule
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Drive the pin confirmation, Home-screen increment, and widget title tap on a disposable emulator. */
class WidgetLauncherSessionTest {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Test
    fun launcherIncrementUpdatesActiveSessionAndReopenedCounter() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("widgetLauncherSession") == "true")
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val manager = AppWidgetManager.getInstance(app)
        val provider = ComponentName(app, CounterWidgetReceiver::class.java)
        check(manager.getAppWidgetIds(provider).isEmpty())
        val database = app.database.get()
        val entryPoint = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java)
        val repository = entryPoint.counterRepository()
        val projectName = "Launcher session"
        val projectId =
            runBlocking(Dispatchers.IO) {
                check(database.sessionDao().getActiveSession() == null)
                app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
                app.onboardingStateRepository.get().dismissWelcome()
                database.counterProjectDao().insert(CounterProjectEntity(name = projectName, count = 10, stepSize = 2))
            }

        ProPolicyTestControl.setPurchased(true)
        try {
            val started =
                runBlocking(Dispatchers.IO) {
                    withTimeout(15_000) { entryPoint.proManager().proState.first { it.isPro } }
                    CounterWidgetState.save(
                        app,
                        WidgetData(projectName = projectName, count = 10, projectId = projectId),
                    )
                    repository.startSession(projectId) as StartSessionResult.Started
                }
            val activity =
                instrumentation.startActivitySync(
                    Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            try {
                instrumentation.runOnMainSync {
                    assertTrue("Launcher rejected the widget pin request", CounterWidgetPinHost(activity).requestPin())
                }
                runBlocking {
                    withTimeout(60_000) {
                        while (manager.getAppWidgetIds(provider).isEmpty()) delay(100)
                    }
                }
                runBlocking(Dispatchers.IO) {
                    val active =
                        withTimeout(60_000) {
                            repository.observeActiveSession().first { it?.lastObservedRow == 12 }
                        }
                    assertEquals(started.session.sessionToken, active?.sessionToken)
                    assertEquals(2, active?.trustedRowsWorked)
                    assertEquals(12, repository.getProject(projectId)?.count)
                    assertTrue(repository.getSessionsForProject(projectId).first().isEmpty())
                }
                instrumentation.sendStatus(
                    0,
                    Bundle().apply {
                        putString("stream", "Persisted counter 12 and two rows in the same active session.\n")
                    },
                )
                val row = app.getString(R.string.current_row_short, 12)
                compose.waitUntil(60_000) {
                    compose
                        .onAllNodes(hasContentDescription(row) or hasText(row))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
                compose.onNode(hasContentDescription(row) or hasText(row)).performScrollTo().assertIsDisplayed()
                compose
                    .onNode(hasText(app.getString(R.string.work_session_active)))
                    .performScrollTo()
                    .assertIsDisplayed()
                compose
                    .onNode(hasText(app.getString(R.string.counter_progress_format, 2)))
                    .performScrollTo()
                    .assertIsDisplayed()
            } finally {
                instrumentation.runOnMainSync {
                    ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<MainActivity>()
                        .forEach { it.finish() }
                    activity.finish()
                }
            }
        } finally {
            runBlocking(Dispatchers.IO) {
                database.deleteTestActiveSession(projectId)
                repository.deleteProject(projectId)
            }
            ProPolicyTestControl.setPurchased(false)
        }
    }
}
