package com.finnvek.knittools.ui.screens.library

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.navigation.PatternShareCoordinatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PatternShareQueueRuntimeTest {
    @get:Rule(order = 0)
    val timeout = Timeout.seconds(90)

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val app: App = ApplicationProvider.getApplicationContext()
    private lateinit var activity: MainActivity

    @After
    fun closeActivity() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
    }

    @Test
    fun queuedLinksWaitForEachChoiceAndPreserveDraftWithoutSaving() {
        assertEquals("com.finnvek.knittools.activepolicytest", app.packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, app.checkSelfPermission(Manifest.permission.INTERNET))
        assertEquals(PackageManager.PERMISSION_DENIED, app.checkSelfPermission("com.android.vending.BILLING"))
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        launchActivity()
        val patternsBefore = savedPatterns()

        for (useFirst in listOf(false, true)) {
            deliver("original", "Original draft")
            assertDraft("Original draft", "original")
            deliver("first", "First link")
            assertChoice()
            instrumentation.runOnMainSync {
                deliverOnMain("second", "Second link")
                deliverOnMain("third", "Third link")
            }
            assertChoice()
            assertDraft("Original draft", "original")
            evidence("$useFirst-pending")

            choose(useFirst)
            assertDraft(if (useFirst) "First link" else "Original draft", if (useFirst) "first" else "original")
            assertChoice()

            choose(!useFirst)
            assertDraft(if (useFirst) "First link" else "Second link", if (useFirst) "first" else "second")
            assertChoice()

            choose(useFirst)
            assertDraft(if (useFirst) "Third link" else "Second link", if (useFirst) "third" else "second")
            waitUntil {
                visibleText(R.string.web_pattern_keep_current_draft) == null &&
                    visibleText(R.string.web_pattern_use_shared_link) == null
            }
            evidence("$useFirst-resolved")
            instrumentation.runOnMainSync {
                assertEquals(Intent.ACTION_MAIN, activity.intent.action)
                assertNull(activity.intent.type)
                assertNull(activity.intent.getStringExtra(Intent.EXTRA_TEXT))
                assertNull(activity.intent.getStringExtra(Intent.EXTRA_SUBJECT))
            }
            rule
                .onNodeWithText(app.getString(R.string.cancel))
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            waitUntil { editorTexts().isEmpty() }
        }

        assertEquals(patternsBefore, savedPatterns())
    }

    private fun savedPatterns() =
        runBlocking(Dispatchers.IO) {
            app.database
                .get()
                .savedPatternDao()
                .getAllOnce()
        }

    private fun launchActivity() {
        val intent =
            Intent(app, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            )
        instrumentation.runOnMainSync { app.startActivity(intent) }
        waitUntil {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .singleOrNull()
                    ?.let { activity = it }
            }
            ::activity.isInitialized
        }
    }

    private fun deliver(
        path: String,
        title: String,
    ) {
        instrumentation.runOnMainSync { deliverOnMain(path, title) }
    }

    private fun deliverOnMain(
        path: String,
        title: String,
    ) {
        app.startActivity(
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://example.invalid/$path")
                .putExtra(Intent.EXTRA_SUBJECT, title),
        )
    }

    private fun assertDraft(
        title: String,
        path: String,
    ) {
        val expected = listOf(title, "https://example.invalid/$path").sorted()
        waitUntil { editorTexts().sorted() == expected }
        assertEquals(expected, editorTexts().sorted())
    }

    private fun editorTexts(): List<String> =
        listOfNotNull(fieldText(WEB_PATTERN_TITLE_FIELD_TAG), fieldText(WEB_PATTERN_URL_FIELD_TAG))
            .filter(String::isNotEmpty)

    private fun fieldText(tag: String): String? =
        rule
            .onAllNodesWithTag(tag)
            .fetchSemanticsNodes()
            .singleOrNull()
            ?.config
            ?.getOrNull(SemanticsProperties.EditableText)
            ?.text

    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        fun visit(node: AccessibilityNodeInfo) {
            if (node.packageName?.toString() == app.packageName) result += node
            repeat(node.childCount) { index -> node.getChild(index)?.let(::visit) }
        }
        root?.let(::visit)
        return result
    }

    private fun visibleText(resource: Int): AccessibilityNodeInfo? =
        automation.windows
            .flatMap { nodes(it.root) }
            .firstOrNull { it.isVisibleToUser && it.text?.toString() == app.getString(resource) }

    private fun assertChoice() {
        waitUntil {
            visibleText(R.string.web_pattern_keep_current_draft) != null &&
                visibleText(R.string.web_pattern_use_shared_link) != null
        }
    }

    private fun choose(useIncoming: Boolean) {
        click(if (useIncoming) R.string.web_pattern_use_shared_link else R.string.web_pattern_keep_current_draft)
    }

    private fun click(resource: Int) {
        var target: AccessibilityNodeInfo? = null
        waitUntil {
            target = visibleText(resource)
            target != null
        }
        val bounds = Rect().also { checkNotNull(target).getBoundsInScreen(it) }
        assertTrue("Empty bounds for ${app.getString(resource)}", bounds.width() > 0 && bounds.height() > 0)
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event =
                MotionEvent.obtain(
                    down,
                    SystemClock.uptimeMillis(),
                    action,
                    bounds.exactCenterX(),
                    bounds.exactCenterY(),
                    0,
                )
            try {
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                assertTrue(automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000L
        while (true) {
            rule.waitForIdle()
            if (condition()) return
            if (SystemClock.uptimeMillis() >= deadline) {
                evidence("failure")
                error("Share queue UI condition did not become true within 10 seconds")
            }
            SystemClock.sleep(50L)
        }
    }

    private fun evidence(name: String) {
        val configuration = app.resources.configuration
        var pendingTitle: String? = null
        instrumentation.runOnMainSync {
            if (::activity.isInitialized) {
                val coordinator = ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java]
                pendingTitle =
                    coordinator.pending.value
                        ?.payload
                        ?.titleSuggestion
            }
        }
        val directory = File(app.filesDir, "share-queue-evidence").apply { mkdirs() }
        val prefix = "${configuration.screenWidthDp}dp-font${configuration.fontScale}-$name"
        File(directory, "$prefix.json").writeText(
            JSONObject()
                .put("widthDp", configuration.screenWidthDp)
                .put("fontScale", configuration.fontScale)
                .put("texts", editorTexts().joinToString("\n"))
                .put("coordinatorPendingTitle", pendingTitle ?: JSONObject.NULL)
                .toString(),
        )
        automation.takeScreenshot()?.let { bitmap ->
            try {
                File(directory, "$prefix.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
