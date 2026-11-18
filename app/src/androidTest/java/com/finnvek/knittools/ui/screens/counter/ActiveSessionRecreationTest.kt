package com.finnvek.knittools.ui.screens.counter

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.repository.deleteTestActiveSession
import com.finnvek.knittools.repository.isIsolatedRoomTestPackage
import com.finnvek.knittools.ui.TestContentSplashRule
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.math.abs

class ActiveSessionRecreationTest {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val app: App = ApplicationProvider.getApplicationContext()
    private val database get() = app.database.get()
    private val projectName = "Session recreation ${UUID.randomUUID()}"
    private var projectId = 0L
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        check(app.isIsolatedRoomTestPackage)
        runBlocking(Dispatchers.IO) {
            check(database.sessionDao().getActiveSession() == null)
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            app.onboardingStateRepository.get().dismissWelcome()
            projectId = database.counterProjectDao().insert(CounterProjectEntity(name = projectName, count = 10))
        }
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        val continueLabel = text(R.string.project_continue_content_description, projectName)
        rule.waitUntil(15_000) {
            rule.onAllNodes(hasContentDescription(continueLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        node(continueLabel).performScrollTo().performClick()
    }

    @After
    fun tearDown() {
        try {
            scenario?.close()
        } finally {
            if (projectId > 0L) {
                runBlocking(Dispatchers.IO) {
                    database.deleteTestActiveSession(projectId)
                    EntryPointAccessors
                        .fromApplication(app, WidgetEntryPoint::class.java)
                        .counterRepository()
                        .deleteProject(projectId)
                    assertNull(database.counterProjectDao().getProject(projectId))
                    assertTrue(
                        database
                            .sessionDao()
                            .getSessionsForProject(projectId)
                            .first()
                            .isEmpty(),
                    )
                    assertNull(database.sessionDao().getActiveSession())
                }
                evidence("cleanup: owned project, active session and completed session removed")
            }
        }
    }

    @Test
    fun recreationPreservesActiveSessionAndExplicitStopSavesExactlyOnce() {
        node(text(R.string.project_actions_title)).performClick()
        node(text(R.string.work_session_start)).performScrollTo().performClick()
        rule.waitUntil(10_000) { active() != null }
        val started = requireNotNull(active())
        assertNotNull(started.bootCount)
        incrementTo(11)
        incrementTo(12)
        assertUi(12, 2)
        val before = requireNotNull(active())
        val projectBefore = project()
        val historyBefore = history()
        val completedBefore = completedCount()
        lateinit var oldActivity: MainActivity
        val currentScenario = requireNotNull(scenario)
        currentScenario.onActivity { oldActivity = it }
        val pidBefore = Process.myPid()
        evidence(
            "before: pid=$pidBefore, activity=${System.identityHashCode(oldActivity)}, " +
                "active=$before, completed=$completedBefore",
        )

        currentScenario.recreate()

        currentScenario.onActivity {
            assertNotSame(oldActivity, it)
            assertSame(app, it.application)
            evidence(
                "after: pid=${Process.myPid()}, activity=${System.identityHashCode(it)}, " +
                    "oldDestroyed=${oldActivity.isDestroyed}",
            )
        }
        assertTrue(oldActivity.isDestroyed)
        assertEquals(pidBefore, Process.myPid())
        assertEquals(Lifecycle.State.RESUMED, currentScenario.state)
        assertUi(12, 2)
        val after = requireNotNull(active())
        assertEquals(projectBefore, project())
        assertEquals(historyBefore, history())
        assertEquals(completedBefore, completedCount())
        // Lifecycle checkpointing may advance time anchors without changing session identity or progress.
        assertEquals(
            before,
            after.copy(
                checkpointedDurationSeconds = before.checkpointedDurationSeconds,
                segmentStartedAtWallMillis = before.segmentStartedAtWallMillis,
                segmentStartedElapsedRealtimeMillis = before.segmentStartedElapsedRealtimeMillis,
                updatedAtWallMillis = before.updatedAtWallMillis,
            ),
        )
        val now = SystemClock.elapsedRealtime()
        assertTrue(after.checkpointedDurationSeconds >= before.checkpointedDurationSeconds)
        assertTrue(abs(durationAt(before, now) - durationAt(after, now)) <= 1L)
        assertNull(after.recoveryReason)
        assertNull(after.recoveryIntervalToken)
        assertEquals(2, after.trustedRowsWorked)
        assertEquals(0, after.pendingRowsWorked)
        evidence(
            "restored: active=$after, count=${project().count}, " +
                "history=${history().size}, completed=${completedCount()}",
        )

        incrementTo(13)
        assertUi(13, 3)
        assertEquals(started.sessionToken, active()?.sessionToken)
        assertEquals(3, active()?.trustedRowsWorked)
        val stopEarliest = SystemClock.elapsedRealtime()
        node(text(R.string.work_session_stop)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.work_session_stop_summary_title)).assertIsDisplayed()
        val stopLatest = SystemClock.elapsedRealtime()
        assertEquals(completedBefore, completedCount())
        assertEquals(started.sessionToken, active()?.sessionToken)
        node(text(R.string.work_session_save)).performClick()
        rule.waitUntil(10_000) { active() == null }
        rule.waitForIdle()
        val completed = runBlocking(Dispatchers.IO) { database.sessionDao().getSessionsForProject(projectId).first() }
        assertEquals(1, completed.size)
        assertEquals(completedBefore + 1, completedCount())
        val saved = completed.single()
        assertEquals(started.startedAtWallMillis, saved.startedAt)
        assertEquals(started.startZoneId, saved.zoneId)
        assertEquals(10, saved.startRow)
        assertEquals(13, saved.endRow)
        assertEquals(3, saved.rowsWorked)
        assertEquals(13, project().count)
        val durationRange = (durationAt(started, stopEarliest) - 2)..(durationAt(started, stopLatest) + 2)
        assertTrue("Saved duration ${saved.durationSeconds} in $durationRange", saved.durationSeconds in durationRange)
        currentScenario.close()
        scenario = null
        assertNull(active())
        assertEquals(completedBefore + 1, completedCount())
        evidence(
            "stopped: saved=$saved, expectedDurationSeconds=$durationRange, " +
                "completed=${completedCount()}, active=null; no extra completion on close",
        )
    }

    private fun assertUi(
        count: Int,
        progress: Int,
    ) {
        node(text(R.string.current_row_short, count)).performScrollTo().assertIsDisplayed()
        node(text(R.string.work_session_active)).performScrollTo().assertIsDisplayed()
        node(text(R.string.counter_progress_format, progress)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.work_session_recovery_title)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.work_session_recovery_needed)).assertDoesNotExist()
    }

    private fun incrementTo(count: Int) {
        node(text(R.string.counter_add_row)).performScrollTo().performClick()
        rule.waitUntil(10_000) { project().count == count }
    }

    private fun node(value: String) = rule.onNode(hasText(value) or hasContentDescription(value))

    private fun text(
        id: Int,
        vararg args: Any,
    ) = app.getString(id, *args)

    private fun active() = runBlocking(Dispatchers.IO) { database.sessionDao().getActiveSession() }

    private fun project() =
        runBlocking(Dispatchers.IO) { requireNotNull(database.counterProjectDao().getProject(projectId)) }

    private fun history() =
        runBlocking(Dispatchers.IO) { database.counterProjectDao().observeCounterHistory(projectId).first() }

    private fun completedCount() = runBlocking(Dispatchers.IO) { database.sessionDao().countCompletedSessions() }

    private fun durationAt(
        active: ActiveSessionEntity,
        elapsedMillis: Long,
    ) = active.checkpointedDurationSeconds + (elapsedMillis - active.segmentStartedElapsedRealtimeMillis) / 1_000L

    private fun evidence(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putString("stream", "$message\n") })
    }
}
