package com.finnvek.knittools.ui.screens.session

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
import com.finnvek.knittools.domain.model.KnitSession
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

class SessionHistoryNavigationTest {
    private lateinit var app: App
    private lateinit var repository: CounterRepository
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var id = 0L
    private val name = "Session context " + UUID.randomUUID().toString().take(8)
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun label(resource: Int) = app.getString(resource)

    @Before fun seed() {
        app = ApplicationProvider.getApplicationContext()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            val created = repository.createProject(name, canCreateAdditionalProjects = true)
            check(created is ProjectCreationResult.Created)
            id = created.projectId
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

    @Test fun emptyHistoryIdentifiesProjectFromProjectsAndInsights() = verifyEntrances(false)

    @Test fun populatedHistoryIdentifiesProjectFromProjectsAndInsights() = verifyEntrances(true)

    private fun verifyEntrances(populated: Boolean) {
        if (populated) {
            runBlocking(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                repository.insertSession(
                    KnitSession(
                        projectId = id,
                        startedAt = now - 60_000,
                        endedAt = now,
                        startRow = 2,
                        endRow = 5,
                        durationMinutes = 1,
                    ),
                )
            }
        }
        val before = runBlocking(Dispatchers.IO) { repository.getSessionsForProject(id).first() }
        clickText(name)
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.session_history_title))
        verifyHistory(populated, "projects")
        clickDescription(R.string.back)
        awaitDescription(R.string.project_actions_title)
        clickDescription(R.string.back)
        clickText(label(R.string.tab_insights))
        clickText(label(R.string.all_projects))
        clickText(name)
        clickText(label(R.string.session_history_title))
        verifyHistory(populated, "insights")
        clickDescription(R.string.back)
        awaitText(label(R.string.session_history_title))
        await("returned from history to Insights") {
            find { it.contentDescription?.toString() == label(R.string.back) } == null
        }
        awaitSelectedTab(R.string.tab_insights)
        runBlocking(Dispatchers.IO) { assertEquals(before, repository.getSessionsForProject(id).first()) }
    }

    private fun verifyHistory(
        populated: Boolean,
        entrance: String,
    ) {
        awaitText(label(R.string.session_history_title))
        awaitDescription(R.string.back)
        awaitText(name.uppercase())
        awaitSelectedTab(if (entrance == "projects") R.string.tab_projects else R.string.tab_insights)
        if (populated) {
            awaitText(app.getString(R.string.session_row_range, 2, 5))
        } else {
            awaitText(label(R.string.no_sessions))
        }
        File(app.cacheDir, "session-$entrance-$populated.png").outputStream().use {
            check(requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun awaitSelectedTab(resource: Int) {
        await("selected tab ${label(resource)}") {
            var node = find { hasText(it, label(resource)) }
            while (node != null && !node.isSelected) node = node.parent
            node?.isSelected == true
        }
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
