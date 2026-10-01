package com.finnvek.knittools.ui

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.util.UUID

abstract class HistoryNavigationTest(
    namePrefix: String,
    private val stepSize: Int? = null,
) {
    protected lateinit var app: App
    protected lateinit var repository: CounterRepository
    protected lateinit var scenario: ActivityScenario<MainActivity>
    protected var id = 0L
    protected val name = namePrefix + UUID.randomUUID().toString().take(8)
    protected val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    protected fun label(resource: Int) = app.getString(resource)

    @Before fun seed() {
        app = ApplicationProvider.getApplicationContext()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            val created = repository.createProject(name, canCreateAdditionalProjects = true)
            check(created is ProjectCreationResult.Created)
            id = created.projectId
            stepSize?.let { repository.updateProjectStepSize(id, it) }
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

    protected fun awaitText(text: String) {
        await(text) { find { hasText(it, text) } != null }
    }

    protected fun awaitDescription(resource: Int) {
        await(label(resource)) { find { it.contentDescription?.toString() == label(resource) } != null }
    }

    protected fun clickText(text: String) = click(text) { hasText(it, text) }

    protected fun clickDescription(resource: Int) =
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

    protected fun hasText(
        node: AccessibilityNodeInfo,
        text: String,
    ): Boolean =
        node.text
            ?.toString()
            ?.lineSequence()
            ?.any { it.equals(text, ignoreCase = true) } == true

    protected fun find(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
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

    protected fun await(
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
