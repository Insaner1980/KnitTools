package com.finnvek.knittools.ui.screens.counterhistory

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class CounterHistoryNavigationTest {
    private lateinit var app: App
    private lateinit var repository: CounterRepository
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var id = 0L
    private val name = "History test " + UUID.randomUUID().toString().take(8)
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun label(resource: Int) = app.getString(resource)

    @Before fun seed() {
        app = ApplicationProvider.getApplicationContext()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            val created = repository.createProject(name, canCreateAdditionalProjects = true)
            check(created is ProjectCreationResult.Created)
            id = created.projectId
            repository.updateProjectStepSize(id, 2)
        }
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
    }

    @After fun cleanup() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            runBlocking(Dispatchers.IO) { if (id > 0) repository.deleteProject(id) }
        }
    }

    @Test fun actionsOpenDistinctHistoryAndLiveChangesUndoAndBackUseTheSameProject() {
        openProject()
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(1)
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(2)
        clickDescription(R.string.counter_decrease_row)
        awaitHistorySize(3)
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.reset_counter))
        awaitText(label(R.string.reset_counter_message))
        clickText(label(R.string.reset))
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        File(app.cacheDir, "counter-history-runtime.png").outputStream().use {
            check(requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        runBlocking(Dispatchers.IO) { repository.applyWidgetCountChange(id, true) }
        awaitHistorySize(5)
        awaitAction(R.string.counter_history_increase)
        back()
        awaitDescription(R.string.project_actions_title)
        clickDescription(R.string.counter_undo_last_change)
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        back()
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.session_history_title))
        awaitText(label(R.string.session_history_title))
        await("counter history closed") { find { hasText(it, label(R.string.counter_history_title)) } == null }
        back()
        awaitDescription(R.string.project_actions_title)
    }

    @Test fun emptyCompletedProjectCanReadHistoryAndDeletionReturnsToProjects() {
        openProject()
        openHistory()
        awaitText(label(R.string.counter_history_empty_title))
        runBlocking(Dispatchers.IO) {
            repository.applyMainCounterChange(id, MainCounterChange.Increment)
            repository.archiveProject(id, 100)
        }
        awaitAction(R.string.counter_history_increase)
        back()
        openHistory()
        awaitAction(R.string.counter_history_increase)
        runBlocking(Dispatchers.IO) { repository.deleteProject(id) }
        awaitText(label(R.string.project_list_title))
        await("deleted project left the navigation stack") {
            find { hasText(it, label(R.string.counter_history_title)) } == null &&
                find { it.contentDescription?.toString() == label(R.string.project_actions_title) } == null
        }
    }

    @Test fun ordinaryOpeningAndRelaunchRetainOldHistoryWhileUndoRemovesOnlyNewChange() {
        val old =
            runBlocking(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                listOf(180L, 7L, 2L).forEach { days ->
                    app.database.get().counterProjectDao().insertHistory(
                        CounterHistoryEntity(
                            projectId = id,
                            action = "reset",
                            previousValue = 2,
                            newValue = 0,
                            timestamp = now - days * 86_400_000L,
                        ),
                    )
                }
                repository.observeCounterHistory(id).first()
            }
        openProject()
        awaitHistorySize(3)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        back()
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_increase)
        runBlocking(Dispatchers.IO) {
            assertEquals(old, repository.observeCounterHistory(id).first().drop(1))
        }
        back()
        clickDescription(R.string.counter_undo_last_change)
        awaitHistorySize(3)
        scenario.close()
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        openProject()
        openHistory()
        awaitAction(R.string.counter_history_reset)
        runBlocking(Dispatchers.IO) { assertEquals(old, repository.observeCounterHistory(id).first()) }
    }

    private fun openProject() {
        clickText(name)
        awaitDescription(R.string.project_actions_title)
    }

    private fun openHistory() {
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.counter_history_title))
        awaitText(name)
        awaitText(label(R.string.counter_history_title))
    }

    private fun back() = clickDescription(R.string.back)

    private fun awaitHistorySize(size: Int) {
        await("history size $size") {
            runBlocking(Dispatchers.IO) { repository.observeCounterHistory(id).first().size == size }
        }
    }

    private fun awaitAction(resource: Int) {
        val prefix = label(resource) + "."
        await("history action") { find { it.contentDescription?.toString()?.startsWith(prefix) == true } != null }
    }

    private fun awaitText(text: String) {
        await(text) { find { hasText(it, text) } != null }
    }

    private fun awaitDescription(resource: Int) {
        await(label(resource)) { find { it.contentDescription?.toString() == label(resource) } != null }
    }

    private fun clickText(text: String) = click(text) { hasText(it, text) }

    private fun clickDescription(resource: Int) =
        click(label(resource)) {
            it.contentDescription?.toString() ==
                label(resource)
        }

    private fun click(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ) {
        automation.waitForIdle(100, 20_000)
        var scrollAction = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        await(description) {
            var target = find(matches)
            if (target == null) {
                find { it.isScrollable }?.let { scrollable ->
                    if (scrollable.actionList.none { it.id == scrollAction }) {
                        scrollAction =
                            if (scrollAction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                            } else {
                                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                            }
                    }
                    scrollable.performAction(scrollAction)
                }
                automation.waitForIdle(100, 20_000)
                false
            } else {
                while (target != null && !target.isClickable) target = target.parent
                target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            }
        }
        automation.waitForIdle(100, 20_000)
    }

    private fun hasText(
        node: AccessibilityNodeInfo,
        text: String,
    ): Boolean =
        node.text
            ?.toString()
            ?.lineSequence()
            ?.any { it.equals(text, ignoreCase = true) } == true

    private fun find(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
        val root = automation.rootInActiveWindow ?: return null
        return find(root, matches)
    }

    private fun find(
        node: AccessibilityNodeInfo,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && matches(node)) return node
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> find(child, matches)?.let { return it } }
        }
        return null
    }

    private fun await(
        description: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for $description")
    }
}
