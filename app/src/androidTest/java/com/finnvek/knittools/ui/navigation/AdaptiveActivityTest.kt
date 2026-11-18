package com.finnvek.knittools.ui.navigation

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.WindowMetricsCalculator
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.ui.TestContentSplashRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.FileInputStream
import kotlin.math.roundToInt

/** Opt-in emulator display resizing; restores the existing override even on failure. */
class AdaptiveActivityTest {
    // Compose test synchronization cannot advance a frame behind the production splash pre-draw gate.
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun platformTransitionsKeepActiveCounterSessionAndUndo() {
        assumeTrue(
            "Requires the host-driven platform suite",
            InstrumentationRegistry.getArguments().getString("adaptivePlatformAllowed") == "true",
        )
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        val db = app.database.get()
        runBlocking(Dispatchers.IO) {
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            db.clearAllTables()
            db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Platform cardigan", count = 20))
        }

        fun count() = runBlocking(Dispatchers.IO) { db.counterProjectDao().getProject(1)?.count }

        fun session() = runBlocking(Dispatchers.IO) { db.sessionDao().getActiveSession() }

        fun action(
            id: Int,
            vararg args: Any,
        ) = rule
            .onAllNodes(
                hasText(rule.activity.getString(id, *args)) or
                    hasContentDescription(rule.activity.getString(id, *args)),
            ).onLast()
        tab(TopLevelDestination.Projects).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Platform cardigan")).fetchSemanticsNodes().isNotEmpty() }
        action(R.string.project_continue_content_description, "Platform cardigan").performClick()
        action(R.string.project_actions_title).performClick()
        action(R.string.work_session_start).performScrollTo().performClick()
        rule.waitUntil(10_000) { session() != null }
        val token = checkNotNull(session()).sessionToken
        action(R.string.counter_add_row).performScrollTo().performClick()
        rule.waitUntil(10_000) { count() == 21 }
        val history = runBlocking { db.counterProjectDao().observeCounterHistory(1).first() }
        for (step in 1..4) {
            awaitAdaptivePlatformTransition(step)
            rule.waitForIdle()
            recordWindow("counter-platform-$step multiWindow=${rule.activity.isInMultiWindowMode}")
            assertEquals(21, count())
            assertEquals(token, session()?.sessionToken)
            assertEquals(history, runBlocking { db.counterProjectDao().observeCounterHistory(1).first() })
            assertEquals(0L, runBlocking { db.sessionDao().countCompletedSessions() })
            action(R.string.counter_add_row).performScrollTo().assertIsDisplayed()
        }
        action(R.string.counter_undo_last_change).performScrollTo().performClick()
        rule.waitUntil(10_000) { count() == 20 }
        assertEquals(token, session()?.sessionToken)
        assertEquals(0L, runBlocking { db.sessionDao().countCompletedSessions() })
    }

    @Test
    fun platformTransitionsPreserveKeyboardDraftAndTabBackStack() {
        assumeTrue(
            "Requires the host-driven platform suite",
            InstrumentationRegistry.getArguments().getString("adaptivePlatformAllowed") == "true",
        )
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        runBlocking { app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH) }
        selectTools()
        rule.onNodeWithText(rule.activity.getString(R.string.tool_cast_on_calculator)).performClick()
        rule.onAllNodes(hasSetTextAction())[0].performClick().performTextReplacement("43")
        rule.onAllNodes(hasSetTextAction())[0].assertTextContains("43")
        for (step in 1..4) {
            rule.waitUntil(10_000) {
                ViewCompat
                    .getRootWindowInsets(rule.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            awaitAdaptivePlatformTransition(step)
            rule.waitForIdle()
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("43")
            recordWindow("platform-$step multiWindow=${rule.activity.isInMultiWindowMode}")
            hidePlatformKeyboard()
            tab(TopLevelDestination.Library).performClick()
            selectTools()
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("43")
            if (step < 4) rule.onAllNodes(hasSetTextAction())[0].performClick()
        }
        hidePlatformKeyboard()
        Espresso.pressBack()
        rule.onNodeWithText(rule.activity.getString(R.string.tool_gauge_converter)).assertIsDisplayed()
    }

    @Test
    fun startupAndAllFiveTabsKeepCalculatorBackStack() {
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        runBlocking(Dispatchers.Main) { app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH) }
        visitAllTabs()
        selectTools()
        rule.onNodeWithText(rule.activity.getString(R.string.tool_cast_on_calculator)).performClick()
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("29")
        Espresso.closeSoftKeyboard()
        tab(TopLevelDestination.Library).performClick()
        selectTools()
        rule.onAllNodes(hasSetTextAction())[0].assertTextContains("29")
        rule.activityRule.scenario.recreate()
        rule.onAllNodes(hasSetTextAction())[0].assertTextContains("29")
        Espresso.pressBack()
        rule.onNodeWithText(rule.activity.getString(R.string.tool_gauge_converter)).assertIsDisplayed()
        recordWindow("startup-navigation-api-${android.os.Build.VERSION.SDK_INT}")
    }

    @Test
    fun realActivityResizeAndRotationPreserveToolDraftAndTabRestoration() {
        assumeTrue(
            "Requires the display-resize suite",
            InstrumentationRegistry.getArguments().getString("adaptiveResizeAllowed") == "true",
        )
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        ProPolicyTestControl.setPurchased(false)
        ProPolicyTestControl.setTrialState(TrialState())
        runBlocking { app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH) }
        val originalSize =
            shell("wm size")
                .lineSequence()
                .firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(":")
                ?.trim()
        try {
            resize(360, 800, rail = false)
            visitAllTabs()
            selectTools()
            rule.onNodeWithText(rule.activity.getString(R.string.tool_cast_on_calculator)).performClick()
            rule.onAllNodes(hasSetTextAction())[0].performClick().performTextReplacement("27")
            assertKeyboardAvoidance()
            Espresso.closeSoftKeyboard()
            val portraitActivity = rule.activity
            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(15_000) {
                rule.activity.resources.configuration
                    .let { it.screenWidthDp > it.screenHeightDp }
            }
            rule.onNodeWithTag("navigation-bottom-bar").assertIsDisplayed()
            assertNotSame(portraitActivity, rule.activity)
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("27")
            recordWindow("landscape")

            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            for (width in listOf(600, 840, 411)) {
                resize(width, 800, rail = width >= 600)
                rule.onAllNodes(hasSetTextAction())[0].assertTextContains("27")
                tab(TopLevelDestination.Library).performClick()
                selectTools()
                rule.onAllNodes(hasSetTextAction())[0].assertTextContains("27")
            }
            rule.activityRule.scenario.recreate()
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("27")
            Espresso.closeSoftKeyboard()
            Espresso.pressBack()
            rule.onNodeWithText(rule.activity.getString(R.string.tool_gauge_converter)).assertIsDisplayed()
        } finally {
            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            shell("wm size ${originalSize ?: "reset"}")
        }
    }

    private fun visitAllTabs() {
        TopLevelDestination.entries.forEach { destination -> tab(destination).assertIsDisplayed().performClick() }
    }

    private fun resize(
        widthDp: Int,
        heightDp: Int,
        rail: Boolean,
    ) {
        val scale = rule.activity.resources.displayMetrics.density
        shell("wm size ${(widthDp * scale).roundToInt()}x${(heightDp * scale).roundToInt()}")
        rule.waitUntil(15_000) {
            val config = rule.activity.resources.configuration
            config.screenWidthDp == widthDp &&
                rule
                    .onAllNodesWithTag(if (rail) "navigation-rail" else "navigation-bottom-bar")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        rule.onNodeWithTag(if (rail) "navigation-rail" else "navigation-bottom-bar").assertIsDisplayed()
        assertEquals(widthDp, rule.activity.resources.configuration.screenWidthDp)
        recordWindow("resize-$widthDp")
    }

    private fun recordWindow(label: String) {
        val config = rule.activity.resources.configuration
        val content = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
        assertTrue(content.width > 0 && content.height > 0)
        println(
            "ADAPTIVE $label windowDp=${config.screenWidthDp}x${config.screenHeightDp} " +
                "densityDpi=${config.densityDpi} contentPx=$content fontScale=${config.fontScale}",
        )
    }

    private fun assertKeyboardAvoidance() {
        // Android's inset animation is not a Compose idling resource.
        rule.waitUntil(15_000) {
            val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView) ?: return@waitUntil false
            val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
            val keyboardTop = bounds.height() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bar = rule.onAllNodesWithTag("navigation-bottom-bar").fetchSemanticsNodes().singleOrNull()
            insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                bar != null &&
                bar.boundsInWindow.bottom <= keyboardTop + 1
        }
        val keyboardTop =
            rule.runOnIdle {
                val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
                val insets = checkNotNull(ViewCompat.getRootWindowInsets(rule.activity.window.decorView))
                bounds.height() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            }
        val bar = rule.onNodeWithTag("navigation-bottom-bar").fetchSemanticsNode().boundsInWindow
        assertTrue("Navigation must stay above the IME: $bar, keyboard top $keyboardTop", bar.bottom <= keyboardTop + 1)
        recordWindow("ime-visible")
    }

    private fun selectTools() {
        tab(TopLevelDestination.Tools).performClick()
    }

    private fun tab(destination: TopLevelDestination): androidx.compose.ui.test.SemanticsNodeInteraction {
        val matcher =
            isSelectable() and hasAnyDescendant(hasText(rule.activity.getString(destination.labelRes).uppercase()))
        rule.waitUntil(15_000) { rule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        return rule.onNode(matcher, useUnmergedTree = true)
    }

    private fun hidePlatformKeyboard() {
        rule.runOnUiThread {
            ViewCompat
                .getWindowInsetsController(rule.activity.window.decorView)
                ?.hide(WindowInsetsCompat.Type.ime())
        }
        rule.waitUntil(10_000) {
            ViewCompat
                .getRootWindowInsets(rule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
}
