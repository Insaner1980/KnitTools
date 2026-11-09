package com.finnvek.knittools.ui.screens.counter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.ui.TestContentSplashRule
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class SessionRecoveryBackgroundTest {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val app: App = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
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
        check(app.packageName == "com.finnvek.knittools.notestest")
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
                database.sessionDao().getActiveSession()?.takeIf { it.projectId == projectId }?.let {
                    database.sessionDao().deleteActiveSession(it.sessionToken)
                }
                repository.deleteProject(projectId)
                assertEquals(null, database.counterProjectDao().getProject(projectId))
                assertEquals(null, database.sessionDao().getActiveSession())
            }
        }
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
