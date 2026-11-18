package com.finnvek.knittools.ui.screens.counter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.deleteTestActiveSession
import com.finnvek.knittools.repository.isIsolatedRoomTestPackage
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class SessionRecoveryBackgroundTest : CounterActivityTestFixture() {
    private lateinit var model: CounterViewModel
    private var projectId = 0L
    private val pauses = AtomicInteger()
    private val resumes = AtomicInteger()
    private val lifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                pauses.incrementAndGet()
            }

            override fun onResume(owner: LifecycleOwner) {
                resumes.incrementAndGet()
            }
        }

    @Before
    fun setUp() {
        check(app.isIsolatedRoomTestPackage)
        rule.runOnUiThread { rule.activity.setContent { } }
        database = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            check(database.sessionDao().getActiveSession() == null)
            projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "SR-F01 ${UUID.randomUUID()}"))
            check(repository.startSession(projectId) is StartSessionResult.Started)
            val active = requireNotNull(database.sessionDao().getActiveSession())
            database.sessionDao().updateActiveSession(
                active.copy(
                    checkpointedDurationSeconds = 60L,
                    recoveryReason = ActiveSessionRecoveryReason.REBOOTED.name,
                    recoveryIntervalToken = UUID.randomUUID().toString(),
                    recoverySuggestedDurationSeconds = 120L,
                    recoveryPromptShown = true,
                ),
            )
        }
        rule.runOnUiThread {
            val extras =
                MutableCreationExtras(rule.activity.defaultViewModelCreationExtras).apply {
                    set(DEFAULT_ARGS_KEY, Bundle().apply { putLong("counter.selected_project_id", projectId) })
                }
            model =
                ViewModelProvider(rule.activity.viewModelStore, rule.activity.defaultViewModelProviderFactory, extras)
                    .get("sr-f01", CounterViewModel::class.java)
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            rule.activity.setContent {
                KnitToolsTheme {
                    ProjectContentHost(
                        actions = CounterScreenActions(),
                        viewModelProvider = { model },
                        overview = false,
                    )
                }
            }
        }
        rule.waitUntil(10_000) {
            model.uiState.value.let {
                it.projectId == projectId && it.initialSessionRefreshFinished && it.activeSessionLoaded
            }
        }
    }

    @After
    fun tearDown() {
        rule.runOnUiThread {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
            rule.activity.setContent { }
            rule.activity.viewModelStore.clear()
        }
        rule.waitForIdle()
        if (projectId > 0L) {
            runBlocking(Dispatchers.IO) {
                database.deleteTestActiveSession(projectId)
                repository.deleteProject(projectId)
                assertEquals(null, database.counterProjectDao().getProject(projectId))
                assertEquals(null, database.sessionDao().getActiveSession())
            }
        }
    }

    @Test
    fun reviewedStopSurvivesHomeCheckpointAndSavesExactlyOnce() {
        runBlocking(Dispatchers.IO) {
            val recovery = requireNotNull(database.sessionDao().getActiveSession())
            repository.addRecoveryInterval(recovery.sessionToken, requireNotNull(recovery.recoveryIntervalToken), 0L)
        }
        rule.waitUntil(10_000) {
            model.uiState.value.activeSession
                ?.needsRecoveryReview == false
        }
        rule.onNodeWithText(text(R.string.work_session_stop)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.work_session_stop_summary_title)).assertIsDisplayed()
        val summary = requireNotNull(model.uiState.value.sessionStopSummary)
        val activityBefore = rule.activity
        val pidBefore = Process.myPid()
        val pausesBefore = pauses.get()
        val resumesBefore = resumes.get()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitUntil(5_000) {
            runBlocking(Dispatchers.IO) {
                repository.activeSessionDurationSeconds(requireNotNull(repository.refreshActiveSession())) >
                    summary.durationSeconds
            }
        }

        assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME))
        rule.waitUntil(10_000) { pauses.get() > pausesBefore }
        rule.waitUntil(10_000) {
            runBlocking(Dispatchers.IO) {
                val checkpoint = requireNotNull(database.sessionDao().getActiveSession())
                checkpoint.checkpointedDurationSeconds > summary.durationSeconds
            }
        }
        app.startActivity(
            Intent(activityBefore.intent).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
        rule.waitUntil(10_000) { resumes.get() > resumesBefore }
        rule.waitForIdle()
        assertSame(activityBefore, rule.activity)
        assertEquals(pidBefore, Process.myPid())
        assertEquals(summary, model.uiState.value.sessionStopSummary)
        rule.onNodeWithText(text(R.string.work_session_save)).performClick()
        rule.waitUntil(10_000) {
            runBlocking(Dispatchers.IO) { database.sessionDao().getActiveSession() == null }
        }
        rule.runOnIdle { model.saveStoppedWorkSession() }
        runBlocking(Dispatchers.IO) {
            val saved = repository.getSessionsForProject(projectId).first().single()
            assertEquals(summary.durationSeconds, saved.durationSeconds)
            assertEquals(summary.rowsWorked, saved.rowsWorked)
            assertEquals(summary.endRow, saved.endRow)
        }
        reportEvidence(
            "Stop review: Home/return in same Activity and process, later checkpoint, reviewed duration saved once",
        )
    }

    @Test
    fun manuallyReopenedEditorSurvivesHomeAndReturnWithoutResolvingSession() {
        assertFalse(model.uiState.value.showSessionRecoveryPrompt)
        rule.onNodeWithText(text(R.string.work_session_resolve)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.work_session_edit_duration)).performClick()
        rule.onNodeWithText(text(R.string.work_session_minutes)).performTextReplacement("37")
        rule.onNodeWithText("37").assertIsDisplayed()
        val before = runBlocking(Dispatchers.IO) { database.sessionDao().getActiveSession() }
        val completedBefore = runBlocking(Dispatchers.IO) { database.sessionDao().countCompletedSessions() }
        val activityBefore = rule.activity
        val pidBefore = Process.myPid()
        val pausesBefore = pauses.get()
        val resumesBefore = resumes.get()
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME))
        rule.waitUntil(10_000) { pauses.get() > pausesBefore }
        app.startActivity(
            Intent(activityBefore.intent).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
        rule.waitUntil(10_000) { resumes.get() > resumesBefore }
        rule.waitForIdle()

        assertEquals(Lifecycle.State.RESUMED, rule.activityRule.scenario.state)
        assertSame(activityBefore, rule.activity)
        assertEquals(pidBefore, Process.myPid())
        assertTrue(model.uiState.value.showSessionRecoveryPrompt)
        rule.onNodeWithText(text(R.string.work_session_recovery_title)).assertIsDisplayed()
        rule.onNodeWithText("37").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.work_session_save_edited_and_stop)).assertIsDisplayed()
        runBlocking(Dispatchers.IO) {
            assertEquals(before, database.sessionDao().getActiveSession())
            assertEquals(completedBefore, database.sessionDao().countCompletedSessions())
        }
        reportEvidence(
            "SR-F01: Home/return, same Activity and PID=$pidBefore, draft=37, " +
                "active row unchanged, completed count unchanged",
        )

        // A different session must not inherit the open editor, even if its interval token matches.
        val replacement =
            requireNotNull(before).copy(sessionToken = UUID.randomUUID().toString(), recoveryPromptShown = false)
        runBlocking(Dispatchers.IO) { database.sessionDao().updateActiveSession(replacement) }
        rule.waitUntil(10_000) {
            model.uiState.value.activeSession
                ?.sessionToken == replacement.sessionToken
        }
        rule.onNodeWithText(text(R.string.work_session_edit_duration)).assertIsDisplayed().performClick()
        rule.onNodeWithText("37").assertDoesNotExist()
        rule.onNodeWithText("3").assertIsDisplayed()
        reportEvidence("SR-F01: replacement session opens a fresh editor with suggested minutes=3")
    }

    private fun text(id: Int): String = app.getString(id)

    private fun reportEvidence(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putString("stream", "$message\n") })
    }
}
