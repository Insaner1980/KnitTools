package com.finnvek.knittools.ui.screens.ravelry

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.DownloadListener
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebBackForwardList
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Injected callback coverage: detects any state/history operation after a view becomes unusable. */
class RavelryBrowserStateTest {
    @Test
    fun mainFramePostRequestsAreFilteredWithoutReplayingTheBody() =
        onMain {
            val state = RavelryBrowserState("request")
            val view = GuardedWebView(context())
            var externalActions = 0
            try {
                view.configureRavelryBrowser(state, {}, { _, _ -> externalActions++ }, "checkout")
                state.restoreOrLoad(view, FIRST) {}
                val client = view.webViewClient
                for (address in listOf(
                    "https://www.paypal.com/checkout",
                    CHECKOUT,
                    "https://outside.example/form",
                    "http://www.ravelry.com/login",
                )) {
                    val response =
                        requireNotNull(client.shouldInterceptRequest(view, request(address, method = "POST")))
                    assertEquals(403, response.statusCode)
                    response.data.close()
                }
                assertNull(
                    client.shouldInterceptRequest(
                        view,
                        request("https://www.ravelry.com/account/login", method = "POST"),
                    ),
                )
                assertNull(client.shouldInterceptRequest(view, request(PDF, redirect = true)))
                assertNull(
                    client.shouldInterceptRequest(view, request("https://asset.example/image.png", mainFrame = false)),
                )
                assertEquals(listOf(FIRST), view.loads)
                assertEquals(0, externalActions)
            } finally {
                state.release(view)
            }
        }

    @Test
    fun lossNeverReadsDeadViewAndOldCallbacksCannotAffectReplacement() =
        onMain {
            for (crashed in listOf(true, false)) {
                val state = RavelryBrowserState("request")
                val otherState = RavelryBrowserState("other")
                val old = GuardedWebView(context())
                val replacement = GuardedWebView(context())
                val unaffected = GuardedWebView(context())
                var downloads = 0
                var externalActions = 0
                try {
                    old.configureRavelryBrowser(state, { downloads++ }, { _, _ -> externalActions++ }, "checkout")
                    state.restoreOrLoad(old, FIRST) {}
                    otherState.restoreOrLoad(unaffected, FIRST) {}
                    FrameLayout(context()).addView(old)
                    state.visited(old, SECOND)
                    state.updateProgress(old, 75)
                    val client = old.webViewClient
                    val chrome = requireNotNull(old.webChromeClient)
                    val download = requireNotNull(old.download)
                    download.onDownloadStart(PDF, "fixture", null, "application/pdf", 1)
                    assertEquals(1, downloads)
                    old.unusable = true
                    assertTrue(client.onRenderProcessGone(old, gone(crashed)))
                    assertNull(old.parent)
                    assertEquals(1, old.destroys)
                    assertNull(state.webView)
                    assertTrue(state.rendererLost)
                    assertFalse(state.canGoBack)
                    assertFalse(state.goBack())
                    assertEquals(0, state.progress)
                    assertSame(unaffected, otherState.webView)
                    assertEquals(0, unaffected.destroys)

                    val saved = state.save()
                    assertTrue(saved.serializedSize() <= BROWSER_STATE_BUDGET_BYTES)
                    assertNull(saved.getBundle("snapshot"))
                    assertEquals(SECOND, saved.getString("fallback"))
                    assertTrue(RavelryBrowserState("request", saved).rendererLost)
                    state.release(old)
                    assertEquals(1, old.destroys)
                    state.retry()
                    state.restoreOrLoad(replacement, FIRST) { externalActions++ }
                    assertEquals(listOf(SECOND), replacement.loads)
                    state.visited(replacement, THIRD)
                    state.updateProgress(replacement, 41)

                    // All of these callbacks may already be queued when the old renderer exits.
                    client.onPageStarted(old, FIRST, null)
                    client.doUpdateVisitedHistory(old, FIRST, false)
                    chrome.onProgressChanged(old, 99)
                    download.onDownloadStart(PDF, "fixture", null, "application/pdf", 1)
                    assertTrue(client.shouldOverrideUrlLoading(old, request("https://www.paypal.com/checkout")))
                    assertTrue(client.onRenderProcessGone(old, gone(crashed)))
                    state.release(old)
                    assertSame(replacement, state.webView)
                    assertFalse(state.rendererLost)
                    assertEquals(41, state.progress)
                    assertEquals(1, downloads)
                    assertEquals(0, externalActions)
                    assertEquals(1, old.destroys)
                    assertEquals(0, replacement.destroys)
                    assertEquals(0, old.unusableAccesses)
                    // The old history callback must not replace the next recovery destination either.
                    replacement.unusable = true
                    state.rendererGone(replacement)
                    assertEquals(THIRD, state.save().getString("fallback"))
                    assertEquals(1, replacement.destroys)
                    assertEquals(0, replacement.unusableAccesses)
                } finally {
                    state.release(old)
                    state.release(replacement)
                    otherState.release(unaffected)
                }
            }
        }

    @Test
    fun normalReleaseSavesBeforeDestroyAndStaleReleaseCannotClearNewView() =
        onMain {
            val state = RavelryBrowserState("request")
            val old = GuardedWebView(context())
            val replacement = GuardedWebView(context())
            try {
                state.restoreOrLoad(old, SECOND) {}
                state.release(old)
                assertTrue(old.reads > 0)
                assertEquals(1, old.destroys)
                assertNull(state.webView)
                assertFalse(state.rendererLost)
                state.restoreOrLoad(replacement, FIRST) {}
                state.release(old)
                assertSame(replacement, state.webView)
                assertEquals(listOf(SECOND), replacement.loads)
                assertEquals(0, replacement.destroys)
            } finally {
                state.release(old)
                state.release(replacement)
            }
        }

    @Test
    fun retryAndNewRequestsRevalidateMetadataWithoutRepeatingExternalActions() =
        onMain {
            for (fallback in listOf(SECOND, "http://www.ravelry.com/", "https://www.paypal.com/", CHECKOUT, PDF)) {
                val saved =
                    Bundle().apply {
                        putString("request", "request")
                        putString("fallback", fallback)
                        putBoolean("rendererLost", true)
                    }
                val restored = RavelryBrowserState("request", saved)
                val retryView = GuardedWebView(context())
                val fresh = RavelryBrowserState("new-request", saved)
                val freshView = GuardedWebView(context())
                try {
                    assertTrue(restored.rendererLost)
                    restored.retry()
                    restored.restoreOrLoad(retryView, CHECKOUT) { error("Retry must not repeat checkout") }
                    val expected = if (fallback == SECOND) SECOND else RAVELRY_PATTERN_SEARCH_URL
                    assertEquals(listOf(expected), retryView.loads)
                    assertFalse(fresh.rendererLost)
                    fresh.restoreOrLoad(freshView, FIRST) { error("Unexpected external action") }
                    assertEquals(listOf(FIRST), freshView.loads)
                } finally {
                    restored.release(retryView)
                    fresh.release(freshView)
                }
            }
        }

    private fun onMain(block: () -> Unit) {
        assertEquals("com.finnvek.knittools.activepolicytest", context().packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, context().checkSelfPermission(Manifest.permission.INTERNET))
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        requireNotNull(result).getOrThrow()
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun gone(crashed: Boolean) =
        object : RenderProcessGoneDetail() {
            override fun didCrash() = crashed

            override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
        }

    private fun request(
        address: String,
        method: String = "GET",
        mainFrame: Boolean = true,
        redirect: Boolean = false,
    ) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(address)

        override fun isForMainFrame() = mainFrame

        override fun isRedirect() = redirect

        override fun hasGesture() = true

        override fun getMethod() = method

        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    private class GuardedWebView(
        context: Context,
    ) : WebView(context) {
        var unusable = false
        var destroys = 0
        var reads = 0
        var unusableAccesses = 0
        val loads = mutableListOf<String>()
        var download: DownloadListener? = null

        override fun setDownloadListener(listener: DownloadListener?) {
            download = listener
            super.setDownloadListener(listener)
        }

        override fun loadUrl(url: String) {
            requireUsable()
            loads += url
        }

        override fun getUrl(): String? {
            requireUsable()
            reads++
            return loads.lastOrNull()
        }

        override fun copyBackForwardList(): WebBackForwardList {
            requireUsable()
            reads++
            return super.copyBackForwardList()
        }

        override fun saveState(outState: Bundle): WebBackForwardList? {
            requireUsable()
            reads++
            return super.saveState(outState)
        }

        override fun goBackOrForward(steps: Int) {
            requireUsable()
            super.goBackOrForward(steps)
        }

        override fun destroy() {
            destroys++
            check(destroys == 1)
            super.destroy()
            unusable = true
        }

        private fun requireUsable() {
            if (unusable) unusableAccesses++
            check(!unusable)
        }
    }

    private companion object {
        const val FIRST = "https://www.ravelry.com/patterns/library/state-first"
        const val SECOND = "https://www.ravelry.com/patterns/library/state-second"
        const val THIRD = "https://www.ravelry.com/patterns/library/state-third"
        const val CHECKOUT = "https://www.ravelry.com/carts/1/checkout"
        const val PDF = "https://downloads.example.test/pattern.pdf"
    }
}
