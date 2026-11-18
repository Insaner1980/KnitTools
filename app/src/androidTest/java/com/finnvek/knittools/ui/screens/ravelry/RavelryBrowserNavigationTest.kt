package com.finnvek.knittools.ui.screens.ravelry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebBackForwardList
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.webkit.WebViewFeature
import androidx.window.layout.WindowMetricsCalculator
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.ui.navigation.awaitAdaptivePlatformTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class RavelryBrowserNavigationTest {
    @get:Rule val timeout = Timeout.seconds(120)

    @Test
    fun platformTransitionsPreserveControlledHistoryAndBack() {
        assumeTrue("Requires the host-driven platform suite", InstrumentationRegistry.getArguments().getString("adaptivePlatformAllowed") == "true")
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            for (step in 1..4) {
                awaitAdaptivePlatformTransition(step)
                awaitUrl(scenario, THIRD)
                onMain {
                    val view = browser(scenario)
                    assertPreviousPages(view)
                    assertTrue(view.width > 0 && view.height > 0)
                }
                scenario.onActivity {
                    val config = it.resources.configuration
                    println(
                        "PLATFORM_BROWSER step=$step window=${config.screenWidthDp}x${config.screenHeightDp} " +
                            "density=${config.densityDpi} multiWindow=${it.isInMultiWindowMode}",
                    )
                }
            }
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, SECOND)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, FIRST)
        }
    }

    @Test
    fun adaptiveViewportKeepsControlledHistoryAcrossTheWindowMatrix() {
        assumeTrue("Requires the display-resize suite", InstrumentationRegistry.getArguments().getString("adaptiveResizeAllowed") == "true")

        fun shell(command: String): String =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)
                .use {
                    FileInputStream(it.fileDescriptor).bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
        val original =
            shell("wm size")
                .lineSequence()
                .firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(":")
                ?.trim()
        try {
            launchBrowser().use { scenario ->
                browseThreePages(scenario)
                var density = 1f
                scenario.onActivity { density = it.resources.displayMetrics.density }
                for ((width, height) in listOf(360 to 800, 800 to 400, 800 to 1280, 1280 to 800, 1600 to 900)) {
                    val widthPx = (width * density).roundToInt()
                    val heightPx = (height * density).roundToInt()
                    shell("wm size ${widthPx}x$heightPx")
                    waitUntil("Window did not resize to ${width}x${height}dp (${widthPx}x${heightPx}px)") {
                        var resized = false
                        scenario.onActivity {
                            val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(it).bounds
                            resized = bounds.width() == widthPx && bounds.height() == heightPx
                        }
                        resized
                    }
                    scenario.recreate()
                    awaitUrl(scenario, THIRD)
                    waitUntil { browser(scenario).width > 0 && browser(scenario).height > 0 }
                    onMain {
                        val view = browser(scenario)
                        assertPreviousPages(view)
                        assertTrue(view.height > 100)
                        if (width >= 800) assertTrue("Web page uses available width", view.width > 600)
                        println("ADAPTIVE_BROWSER window=${width}x$height web=${view.width}x${view.height}")
                    }
                }
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                awaitUrl(scenario, SECOND)
            }
        } finally {
            shell("wm size ${original ?: "reset"}")
        }
    }

    @Test
    fun historySurvivesActivityRecreationAndSystemBack() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            val before = browser(scenario)
            scenario.recreate()
            awaitUrl(scenario, THIRD)
            assertNotSame(before, browser(scenario))
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, SECOND)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, FIRST)
        }
    }

    @Test
    fun themeAndOrientationChangesKeepThePageAndHistory() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            val beforeTheme = browser(scenario)
            scenario.onActivity {
                val nightMode =
                    it.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                it.delegate.localNightMode =
                    if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                        AppCompatDelegate.MODE_NIGHT_NO
                    } else {
                        AppCompatDelegate.MODE_NIGHT_YES
                    }
            }
            waitUntil { browser(scenario) !== beforeTheme }
            awaitUrl(scenario, THIRD)
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            waitUntil {
                var landscape = false
                scenario.onActivity {
                    landscape =
                        it.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                }
                landscape
            }
            awaitUrl(scenario, THIRD)
            onMain { assertEquals(-1, browser(scenario).eligibleBackOffset()) }
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, SECOND)
        }
    }

    @Test
    fun sameSharedUrlStartsANewRequestEvenAfterRecreation() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            scenario.recreate()
            awaitUrl(scenario, THIRD)
            val previous = browser(scenario)
            scenario.onActivity {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, shareIntent(it, FIRST))
                it.intent = launcherIntent()
            }
            awaitUrl(scenario, FIRST)
            onMain {
                assertNotSame(previous, browser(scenario))
                assertNull(browser(scenario).eligibleBackOffset())
            }
            scenario.recreate()
            awaitUrl(scenario, FIRST)
            onMain { assertNull(browser(scenario).eligibleBackOffset()) }
        }
    }

    @Test
    fun frameworkSaveRestoresRealHistoryWhenBoundedFeatureIsUnavailable() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            onMain {
                val saved = saveBrowserState(browser(scenario), metadata(THIRD), useBoundedSave = false)
                assertTrue(saved.serializedSize() <= BROWSER_STATE_BUDGET_BYTES)
                assertTrue(saved.containsKey("snapshot"))
                val restoredView = WebView(context())
                try {
                    RavelryBrowserState("test", saved).restoreOrLoad(restoredView, FIRST) {
                        error("Unexpected checkout")
                    }
                    val history = restoredView.copyBackForwardList()
                    assertEquals(THIRD, history.currentItem?.url)
                    assertEquals(SECOND, history.getItemAtIndex(history.currentIndex - 1)?.url)
                    assertEquals(FIRST, history.getItemAtIndex(history.currentIndex - 2)?.url)
                } finally {
                    restoredView.destroy()
                }
            }
        }
    }

    @Test
    fun nativeBoundedSaveOmitsForwardHistory() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, SECOND)
            onMain {
                assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SAVE_STATE))
                val source = browser(scenario)
                assertTrue(source.canGoForward())
                val saved = saveBrowserState(source, metadata(SECOND))
                assertTrue(saved.serializedSize() <= BROWSER_STATE_BUDGET_BYTES)
                assertTrue(saved.containsKey("snapshot"))
                assertTrue(source.canGoForward())
                val restoredView = WebView(context())
                try {
                    RavelryBrowserState("test", saved).restoreOrLoad(restoredView, FIRST) {}
                    assertEquals(SECOND, restoredView.copyBackForwardList().currentItem?.url)
                    assertTrue(restoredView.canGoBack())
                    assertTrue(!restoredView.canGoForward())
                } finally {
                    restoredView.destroy()
                }
            }
        }
    }

    @Test
    fun oversizedFrameworkStateFallsBackWithoutClearingLiveHistory() {
        launchBrowser().use { scenario ->
            browseThreePages(scenario)
            var scriptFinished = false
            onMain {
                browser(scenario).evaluateJavascript("history.replaceState('x'.repeat(70000), '')") {
                    scriptFinished = true
                }
            }
            waitUntil {
                var finished = false
                onMain { finished = scriptFinished }
                finished
            }
            onMain {
                val view = browser(scenario)
                val before = view.copyBackForwardList()
                val raw = Bundle()
                assertTrue(view.saveState(raw) != null)
                assertTrue("Fixture must exceed the budget", raw.serializedSize() > BROWSER_STATE_BUDGET_BYTES)
                val saved = saveBrowserState(view, metadata(THIRD), useBoundedSave = false)
                assertTrue(saved.serializedSize() <= BROWSER_STATE_BUDGET_BYTES)
                assertNull(saved.getBundle("snapshot"))
                assertEquals(THIRD, saved.getString("fallback"))
                val bounded = saveBrowserState(view, metadata(THIRD))
                assertTrue(bounded.serializedSize() <= BROWSER_STATE_BUDGET_BYTES)
                assertEquals(before.size, view.copyBackForwardList().size)
                assertEquals(before.currentIndex, view.copyBackForwardList().currentIndex)
            }
        }
    }

    @Test
    fun missingFailedAndRejectedSnapshotsLoadOneSafeFallback() {
        onMain {
            val invalid =
                listOf(
                    "http://www.ravelry.com/",
                    "https://www.paypal.com/",
                    "https://www.ravelry.com/carts/1/checkout",
                )
            val fixtures =
                listOf(
                    metadata(SECOND),
                    metadata(SECOND).apply {
                        putStringArrayList("history", arrayListOf(FIRST, SECOND))
                        putBundle("snapshot", Bundle())
                    },
                ) +
                    invalid.map { url ->
                        metadata(url).apply {
                            putStringArrayList("history", arrayListOf(url))
                            putBundle("snapshot", Bundle())
                        }
                    }
            fixtures.forEachIndexed { index, saved ->
                val view = FailedRestoreWebView(context())
                try {
                    RavelryBrowserState("test", saved).restoreOrLoad(view, FIRST) {
                        error("Restore must not open checkout")
                    }
                    assertEquals(listOf(if (index < 2) SECOND else RAVELRY_PATTERN_SEARCH_URL), view.loads)
                    assertEquals(if (index == 1) 1 else 0, view.restoreCalls)
                } finally {
                    view.destroy()
                }
            }
            val stale = FailedRestoreWebView(context())
            try {
                RavelryBrowserState("new-request", metadata(THIRD)).restoreOrLoad(stale, FIRST) {}
                assertEquals(listOf(FIRST), stale.loads)
                assertEquals(0, stale.restoreCalls)
            } finally {
                stale.destroy()
            }
        }
    }

    @Test
    fun blockedHistoryCannotBeSavedAndBackSkipsItWithoutLooping() {
        launchBrowser().use { scenario ->
            loadPage(scenario, "https://www.ravelry.com/carts/1/checkout")
            loadPage(scenario, THIRD)
            onMain {
                val view = browser(scenario)
                assertEquals(-2, view.eligibleBackOffset())
                val saved = saveBrowserState(view, metadata(THIRD))
                assertNull(saved.getBundle("snapshot"))
            }
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitUrl(scenario, FIRST)
            onMain { assertNull(browser(scenario).eligibleBackOffset()) }
        }
    }

    @Test
    fun lastEligiblePageSurvivesATransientExternalDestination() {
        launchBrowser().use { scenario ->
            loadPage(scenario, SECOND)
            val state = RavelryBrowserState("test")
            onMain {
                state.restoreOrLoad(browser(scenario), SECOND) {}
            }
            awaitUrl(scenario, SECOND)
            loadPage(scenario, "https://downloads.example.test/pattern.pdf")
            onMain {
                state.visited(browser(scenario), browser(scenario).url)
                val saved = state.save()
                assertEquals(SECOND, saved.getString("fallback"))
                assertNull(saved.getBundle("snapshot"))
            }
        }
    }

    @Test
    fun leavingBrowserReleasesItsViewAndActivity() {
        val scenario = launchBrowser()
        var viewReference: WeakReference<WebView>? = null
        var activityReference: WeakReference<MainActivity>? = null
        scenario.onActivity {
            viewReference = WeakReference(findBrowser(it.findViewById(android.R.id.content)))
            activityReference = WeakReference(it)
        }
        scenario.close()
        waitUntil {
            Runtime.getRuntime().gc()
            System.runFinalization()
            viewReference?.get() == null && activityReference?.get() == null
        }
    }

    private fun launchBrowser(): ActivityScenario<MainActivity> {
        assertEquals("com.finnvek.knittools.activepolicytest", context().packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, context().checkSelfPermission(Manifest.permission.INTERNET))
        return ActivityScenario.launch<MainActivity>(launcherIntent()).also { scenario ->
            scenario.onActivity {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, shareIntent(it, FIRST))
                // Consuming ACTION_SEND changes the Activity intent; keep Scenario's matching identity stable.
                it.intent = launcherIntent()
            }
            awaitUrl(scenario, FIRST)
            loadPage(scenario, FIRST)
            onMain { browser(scenario).clearHistory() }
        }
    }

    private fun launcherIntent() =
        Intent(context(), MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

    private fun browseThreePages(scenario: ActivityScenario<MainActivity>) {
        loadPage(scenario, SECOND)
        loadPage(scenario, THIRD)
    }

    private fun loadPage(
        scenario: ActivityScenario<MainActivity>,
        url: String,
    ) {
        onMain {
            val view = browser(scenario)
            val productionClient = view.webViewClient
            view.webViewClient =
                object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: RenderProcessGoneDetail,
                    ): Boolean = productionClient.onRenderProcessGone(view, detail)

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse =
                        WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            ByteArrayInputStream("<html><body>Restoration fixture</body></html>".toByteArray()),
                        )

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = productionClient.shouldOverrideUrlLoading(view, request)

                    override fun onPageStarted(
                        view: WebView,
                        url: String?,
                        favicon: Bitmap?,
                    ) = productionClient.onPageStarted(view, url, favicon)

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String?,
                        isReload: Boolean,
                    ) = productionClient.doUpdateVisitedHistory(view, url, isReload)
                }
            view.loadUrl(url)
        }
        awaitUrl(scenario, url)
        onMain { assertEquals(url, browser(scenario).copyBackForwardList().currentItem?.originalUrl) }
    }

    private fun awaitUrl(
        scenario: ActivityScenario<MainActivity>,
        url: String,
    ) = awaitBrowserUrl(scenario, url) { condition -> waitUntil(condition = condition) }

    private fun assertPreviousPages(view: WebView) {
        val history = view.copyBackForwardList()
        assertEquals(SECOND, history.getItemAtIndex(history.currentIndex - 1)?.url)
        assertEquals(FIRST, history.getItemAtIndex(history.currentIndex - 2)?.url)
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun onMain(block: () -> Unit) {
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        requireNotNull(result).getOrThrow()
    }

    private fun metadata(url: String) =
        Bundle().apply {
            putString("request", "test")
            putString("fallback", url)
        }

    private class FailedRestoreWebView(
        context: Context,
    ) : WebView(context) {
        val loads = mutableListOf<String>()
        var restoreCalls = 0

        override fun loadUrl(url: String) {
            loads += url
        }

        override fun restoreState(inState: Bundle): WebBackForwardList? {
            restoreCalls++
            return null
        }
    }

    @Test
    fun newSharedLinkReplacesTheOpenBrowserPage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.finnvek.knittools.activepolicytest", context.packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
        val firstUrl = "https://www.ravelry.com/patterns/library/audit-first"
        val secondUrl = "https://www.ravelry.com/patterns/library/audit-second"
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var activity: MainActivity? = null
        try {
            instrumentation.runOnMainSync {
                context.startActivity(
                    shareIntent(context, firstUrl)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
            }
            var first: WebView? = null
            waitUntil {
                var loaded = false
                instrumentation.runOnMainSync {
                    activity =
                        ActivityLifecycleMonitorRegistry
                            .getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .filterIsInstance<MainActivity>()
                            .singleOrNull()
                    first = activity?.let { findBrowser(it.findViewById(android.R.id.content)) }
                    loaded = first?.url == firstUrl
                }
                loaded
            }

            instrumentation.runOnMainSync {
                requireNotNull(activity).startActivity(
                    shareIntent(context, secondUrl).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }

            var second: WebView? = null
            waitUntil {
                var loaded = false
                instrumentation.runOnMainSync {
                    second = findBrowser(requireNotNull(activity).findViewById(android.R.id.content))
                    loaded = second?.url == secondUrl
                }
                loaded
            }
            assertNotSame(first, second)
            instrumentation.runOnMainSync {
                requireNotNull(activity).startActivity(
                    shareIntent(context, secondUrl).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }
            waitUntil {
                var replaced = false
                instrumentation.runOnMainSync {
                    val third = findBrowser(requireNotNull(activity).findViewById(android.R.id.content))
                    replaced = third != null && third !== second && third.url == secondUrl
                }
                replaced
            }
        } finally {
            instrumentation.runOnMainSync { activity?.finishAndRemoveTask() }
        }
    }

    private fun waitUntil(
        message: String = "Browser URL did not update within 10 seconds",
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { message }
            SystemClock.sleep(50)
        }
    }

    private fun shareIntent(
        context: Context,
        url: String,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }

    private companion object {
        const val FIRST = "https://www.ravelry.com/patterns/library/restoration-first"
        const val SECOND = "https://www.ravelry.com/patterns/library/restoration-second"
        const val THIRD = "https://www.ravelry.com/patterns/library/restoration-third"
    }
}
