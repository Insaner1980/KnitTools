package com.finnvek.knittools.ui.navigation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Real Activity reporter, real splash and local Room; only the disposable policy package is cleared. */
class StartupReadinessTest {
    private val app = ApplicationProvider.getApplicationContext<App>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val completions = AtomicInteger()
    private val launchedActivity = AtomicReference<MainActivity?>()
    private val listener =
        ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity && stage == Stage.CREATED) {
                launchedActivity.set(activity)
                activity.fullyDrawnReporter.addOnReportDrawnListener { completions.incrementAndGet() }
            }
        }

    @Before
    fun prepare() {
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        runBlocking(Dispatchers.IO) {
            app.database.get().clearAllTables()
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            app.preferencesManager.get().setThemeMode(ThemeMode.LIGHT)
        }
        ProPolicyTestControl.setPurchased(false)
        ProPolicyTestControl.setTrialState(TrialState())
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(listener)
    }

    @After
    fun cleanup() {
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(listener)
        runBlocking(Dispatchers.IO) {
            app.database.get().clearAllTables()
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            app.preferencesManager.get().setThemeMode(ThemeMode.LIGHT)
        }
    }

    @Test
    fun emptyLauncherReportsAndLaterNavigationDoesNotReportAgain() {
        withLaunch { activity ->
            awaitReady(activity)
            text(R.string.no_active_projects)
            listOf("Tools", "Library", "Settings", "Projects").forEach {
                clickText(it)
            }
            text(R.string.no_active_projects)
            assertEquals(1, completions.get())
            seedProject()
            waitText("Startup cardigan")
            assertEquals(1, completions.get())
        }
    }

    @Test
    fun populatedLauncherAndCounterLaunchPreserveExistingSession() {
        seedProject()
        val repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking { repository.startSession(1) }
        val before =
            runBlocking {
                app.database
                    .get()
                    .sessionDao()
                    .getActiveSession()
            }
        assertNotNull(before)
        withLaunch { activity ->
            awaitReady(activity)
            waitText("Startup cardigan")
        }
        completions.set(0)
        val intent =
            MainActivity
                .createCounterLaunchIntent(app, 1L)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        withLaunch(intent) { activity ->
            awaitReady(activity)
            text(R.string.counter_add_row)
            waitText("Startup cardigan")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            waitText(app.getString(R.string.project_continue_content_description, "Startup cardigan"))
            assertEquals(1, completions.get())
        }
        val after =
            runBlocking {
                app.database
                    .get()
                    .sessionDao()
                    .getActiveSession()
            }
        assertEquals(before?.sessionToken, after?.sessionToken)
        assertEquals(before?.startedAtWallMillis, after?.startedAtWallMillis)
        assertEquals(before?.startRow, after?.startRow)
        assertEquals(
            24,
            runBlocking {
                app.database
                    .get()
                    .counterProjectDao()
                    .getProject(1)
                    ?.count
            },
        )
        assertEquals(
            0L,
            runBlocking {
                app.database
                    .get()
                    .sessionDao()
                    .countCompletedSessions()
            },
        )
    }

    @Test
    fun missingWidgetProjectFallsBackAndLockedWidgetReportsItsPrompt() {
        val missing = MainActivity.createCounterLaunchIntent(app, 999L).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        withLaunch(missing) { activity ->
            awaitReady(activity)
            text(R.string.no_active_projects)
        }
        completions.set(0)
        val locked = MainActivity.createWidgetProPromptLaunchIntent(app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        withLaunch(locked) { activity ->
            awaitReady(activity)
            text(R.string.pro_prompt_widget_pin_title)
        }
    }

    @Test
    fun storedFinnishAndDarkThemeAreAppliedAtCompletion() {
        runBlocking {
            app.preferencesManager.get().setAppLanguage(AppLanguage.FINNISH)
            app.preferencesManager.get().setThemeMode(ThemeMode.DARK)
        }
        withLaunch { activity ->
            awaitReady(activity)
            waitText("Projektit")
            instrumentation.runOnMainSync {
                assertEquals(
                    "fi",
                    activity.resources.configuration.locales[0]
                        .language,
                )
                assertFalse(
                    WindowInsetsControllerCompat(
                        activity.window,
                        activity.window.decorView,
                    ).isAppearanceLightStatusBars,
                )
            }
            val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(checkNotNull(app.getExternalFilesDir(null)), "startup-finnish-dark.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
        }
    }

    @Test
    fun sharedWebLinkAndTerminalInvalidLinkReportTheEditorSurface() {
        listOf("https://example.com/pattern", "not a web pattern").forEach { value ->
            completions.set(0)
            withLaunch(
                launcherIntent().setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value),
            ) { activity ->
                awaitReady(activity)
                waitNode { it.className?.toString() == "android.widget.EditText" }
                if (value == "not a web pattern") text(R.string.web_pattern_error_url_invalid)
                assertEquals(1, completions.get())
            }
        }
    }

    @Test
    fun sharedRavelryReportsLocalBrowserWithoutWaitingForNetwork() {
        withLaunch(
            launcherIntent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://www.ravelry.com/patterns/library/startup-fixture"),
        ) { activity ->
            awaitReady(activity)
            waitNode { it.className?.toString() == "android.webkit.WebView" }
        }
    }

    @Test
    fun pdfViewAndShareWaitForTheCopiedDocumentAndProjectChoices() {
        seedProject()
        val source = File(app.cacheDir, "startup-test.pdf")
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(300, 400, 1).create())
            pdf.finishPage(page)
            source.outputStream().use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        try {
            listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND).forEach { action ->
                completions.set(0)
                val intent = launcherIntent().setAction(action)
                if (action == Intent.ACTION_VIEW) {
                    intent.setDataAndType(Uri.fromFile(source), "application/pdf")
                } else {
                    intent.setType("application/pdf").putExtra(Intent.EXTRA_STREAM, Uri.fromFile(source))
                }
                withLaunch(intent) { activity ->
                    awaitReady(activity)
                    text(R.string.incoming_pdf_title)
                    waitText("Startup cardigan")
                }
            }
        } finally {
            source.delete()
        }
    }

    private fun seedProject() =
        runBlocking(Dispatchers.IO) {
            app.database.get().counterProjectDao().insert(
                CounterProjectEntity(id = 1, name = "Startup cardigan", count = 24),
            )
        }

    private fun launcherIntent() =
        Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(
                Intent.CATEGORY_LAUNCHER,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    private fun withLaunch(
        intent: Intent = launcherIntent(),
        check: (MainActivity) -> Unit,
    ) {
        // Observe lifecycle directly: consumed PDF/share intents change identity during onCreate.
        launchedActivity.set(null)
        app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (launchedActivity.get() == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20)
        val activity = checkNotNull(launchedActivity.get())
        try {
            check(activity)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            val closeDeadline = SystemClock.uptimeMillis() + TIMEOUT
            while (!activity.isDestroyed && SystemClock.uptimeMillis() < closeDeadline) SystemClock.sleep(20)
            assertTrue(activity.isDestroyed)
        }
    }

    private fun awaitReady(activity: MainActivity) {
        // Dialog destinations have their own window; observe the actual Activity reporter.
        val deadline = android.os.SystemClock.uptimeMillis() + TIMEOUT
        while (completions.get() == 0 && android.os.SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(20)
        }
        assertEquals(1, completions.get())
        instrumentation.runOnMainSync { assertTrue(activity.fullyDrawnReporter.isFullyDrawnReported) }
    }

    private fun text(id: Int) = waitText(app.getString(id))

    private fun waitText(value: String) =
        waitNode { it.text?.toString().equals(value, true) || it.contentDescription?.toString() == value }

    private fun clickText(value: String) {
        var node = waitText(value)
        while (!node.isClickable) node = checkNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun waitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root?.packageName?.toString() == app.packageName) {
                findNode(root, predicate)?.let { return it }
            }
            SystemClock.sleep(50)
        }
        error("Startup destination did not expose the expected content")
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && predicate(node)) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { findNode(it, predicate)?.let { found -> return found } }
        }
        return null
    }

    private companion object {
        const val TIMEOUT = 30_000L
    }
}
