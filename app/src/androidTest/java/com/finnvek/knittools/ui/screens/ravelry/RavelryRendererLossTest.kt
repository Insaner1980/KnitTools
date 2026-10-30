package com.finnvek.knittools.ui.screens.ravelry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicReference

/** Real provider exits, deliberately separate from injected callback/state-owner tests. */
class RavelryRendererLossTest {
    @get:Rule val timeout = Timeout.seconds(120)

    @Test
    fun realCrashWaitsForRetrySurvivesRecreationAndAllowsBack() = verifyRendererExit(crash = true)

    @Test
    fun realTerminationWaitsForRetrySurvivesRecreationAndAllowsLeaving() = verifyRendererExit(crash = false)

    private fun verifyRendererExit(crash: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.finnvek.knittools.activepolicytest", context.packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
        val appPid = Process.myPid()
        val launcher =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        ActivityScenario.launch<MainActivity>(launcher).use { scenario ->
            println("RENDERER_FIXTURE launched crash=$crash")
            scenario.onActivity {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
                    it,
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, FIRST)
                    },
                )
                it.intent = launcher
            }
            awaitPage(scenario, FIRST)
            loadFixture(scenario, FIRST)
            loadFixture(scenario, SECOND)
            println("RENDERER_FIXTURE loaded crash=$crash")
            repeat(2) { iteration ->
                val old = browser(scenario)
                val exit = AtomicReference<Boolean?>()
                scenario.onActivity {
                    val delegate = old.webViewClient
                    old.webViewClient = FixtureClient(delegate, exit)
                    assertTrue(old.parent != null)
                    assertTrue(old.webViewRenderProcess != null)
                    if (crash) {
                        // Official WebView test hook; never reachable through production URL policy.
                        old.loadUrl("chrome://crash")
                    } else {
                        assertTrue(requireNotNull(old.webViewRenderProcess).terminate())
                    }
                }
                waitUntil { exit.get() != null }
                assertEquals(crash, exit.get())
                assertEquals(appPid, Process.myPid())
                assertFailureUi(scenario, context)
                scenario.onActivity { assertNull(old.parent) }
                println("RENDERER_EXIT crash=$crash iteration=$iteration appPid=$appPid detached=true")

                if (iteration == 0) {
                    // Saving/releasing a failed screen must not touch the destroyed WebView or reload it.
                    if (crash) scenario.recreate()
                    assertFailureUi(scenario, context)
                    clickNode(context.getString(R.string.retry))
                    awaitPage(scenario, SECOND)
                    val replacement = browser(scenario)
                    assertNotSame(old, replacement)
                    loadFixture(scenario, SECOND)
                    loadFixture(scenario, THIRD)
                    scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                    awaitPage(scenario, SECOND)
                    scenario.recreate()
                    awaitPage(scenario, SECOND)
                    loadFixture(scenario, SECOND)
                }
            }
            if (!crash) {
                scenario.recreate()
                assertFailureUi(scenario, context)
            }
            if (crash) {
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            } else {
                clickNode(context.getString(R.string.back))
            }
            waitUntil {
                node(context.getString(R.string.retry)) == null &&
                    node(context.getString(R.string.ravelry_browser_hint)) == null
            }
            assertEquals(appPid, Process.myPid())
        }
    }

    private fun assertFailureUi(
        scenario: ActivityScenario<MainActivity>,
        context: Context,
    ) {
        waitUntil {
            node(context.getString(R.string.generic_error_unknown)) != null &&
                node(context.getString(R.string.retry)) != null
        }
        // Observe a quiet interval to catch automatic reload/crash loops.
        repeat(10) {
            scenario.onActivity { assertNull(findBrowser(it.findViewById(android.R.id.content))) }
            SystemClock.sleep(100)
        }
    }

    private fun clickNode(label: String) {
        waitUntil { node(label) != null }
        var target = node(label)
        while (target != null && !target.isClickable) target = target.parent
        assertTrue(requireNotNull(target).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun node(label: String): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (root.isVisibleToUser &&
                (root.text?.toString() == label || root.contentDescription?.toString() == label)
            ) {
                return root
            }
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { find(it) }?.let { return it }
            }
            return null
        }
        return InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation.rootInActiveWindow
            ?.let(::find)
    }

    private fun loadFixture(
        scenario: ActivityScenario<MainActivity>,
        url: String,
    ) {
        scenario.onActivity {
            val view = requireNotNull(findBrowser(it.findViewById(android.R.id.content)))
            view.webViewClient = FixtureClient(view.webViewClient)
            view.loadUrl(url)
        }
        awaitPage(scenario, url)
    }

    private fun awaitPage(
        scenario: ActivityScenario<MainActivity>,
        url: String,
    ) = awaitBrowserUrl(scenario, url, ::waitUntil)

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Renderer fixture timed out" }
            SystemClock.sleep(50)
        }
    }

    private class FixtureClient(
        val delegate: WebViewClient,
        val exit: AtomicReference<Boolean?>? = null,
    ) : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ) = WebResourceResponse(
            "text/html",
            "UTF-8",
            ByteArrayInputStream("<html><body>Renderer recovery fixture</body></html>".toByteArray()),
        )

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean = delegate.shouldOverrideUrlLoading(view, request)

        override fun onPageStarted(
            view: WebView,
            url: String?,
            favicon: Bitmap?,
        ) = delegate.onPageStarted(view, url, favicon)

        override fun doUpdateVisitedHistory(
            view: WebView,
            url: String?,
            isReload: Boolean,
        ) = delegate.doUpdateVisitedHistory(view, url, isReload)

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail,
        ): Boolean {
            val handled = delegate.onRenderProcessGone(view, detail)
            assertTrue(handled)
            assertFalse(view.isAttachedToWindow)
            exit?.set(detail.didCrash())
            return handled
        }
    }

    private companion object {
        const val FIRST = "https://www.ravelry.com/patterns/library/renderer-first"
        const val SECOND = "https://www.ravelry.com/patterns/library/renderer-second"
        const val THIRD = "https://www.ravelry.com/patterns/library/renderer-third"
    }
}
