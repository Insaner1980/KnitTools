package com.finnvek.knittools.ui.navigation

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
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
            app.onboardingStateRepository.get().dismissWelcome()
            app.preferencesManager.get().setThemeMode(ThemeMode.LIGHT)
        }
        ProPolicyTestControl.setPurchased(false)
        ProPolicyTestControl.setTrialState(TrialState())
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(listener)
    }

    @After
    fun cleanup() {
        if (app.packageName != "com.finnvek.knittools.activepolicytest") return
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
            waitEmptyProjects()
            listOf("Tools", "Library", "Settings", "Projects").forEach {
                clickText(it)
            }
            waitEmptyProjects()
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
            waitEmptyProjects()
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
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "startup-${SystemClock.elapsedRealtime()}.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val resolver = app.contentResolver
        val source = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            val pdf = PdfDocument()
            try {
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(300, 400, 1).create())
                pdf.finishPage(page)
                checkNotNull(resolver.openOutputStream(source)).use(pdf::writeTo)
            } finally {
                pdf.close()
            }
            listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND).forEach { action ->
                completions.set(0)
                val intent = launcherIntent().setAction(action).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (action == Intent.ACTION_VIEW) {
                    intent.setDataAndType(source, "application/pdf")
                } else {
                    intent.setType("application/pdf").putExtra(Intent.EXTRA_STREAM, source)
                }
                withLaunch(intent) { activity ->
                    awaitReady(activity)
                    text(R.string.incoming_pdf_title)
                    waitText("Startup cardigan")
                }
            }
        } finally {
            resolver.delete(source, null, null)
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

    private fun waitEmptyProjects() {
        val titles =
            listOf(
                R.string.welcome_screen_title,
                R.string.start_compact_title_knitting,
                R.string.start_compact_title_crochet,
            ).map(app::getString)
        waitNode { it.text?.toString() in titles }
    }

    private fun waitText(value: String) = waitNode { it.matchesLabel(value) }

    private fun clickText(value: String) {
        var target: AccessibilityNodeInfo? = null
        waitNode { node ->
            if (!node.matchesLabel(value)) return@waitNode false
            target = generateSequence(node) { it.parent }.firstOrNull { it.isClickable && it.isEnabled }
            target != null
        }
        assertTrue(checkNotNull(target).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun AccessibilityNodeInfo.matchesLabel(value: String): Boolean =
        text?.toString().equals(value, true) || contentDescription?.toString() == value

    private fun waitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val automation = instrumentation.uiAutomation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
            val root = automation.rootInActiveWindow
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
