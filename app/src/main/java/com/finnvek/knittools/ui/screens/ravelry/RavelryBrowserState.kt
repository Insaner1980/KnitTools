package com.finnvek.knittools.ui.screens.ravelry

import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebBackForwardList
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

internal const val BROWSER_STATE_BUDGET_BYTES = 32 * 1024
private const val METADATA_BUDGET_BYTES = 8 * 1024
private const val SNAPSHOT_KEY = "snapshot"
private const val REQUEST_KEY = "request"
private const val FALLBACK_KEY = "fallback"
private const val HISTORY_KEY = "history"
private const val RENDERER_LOST_KEY = "rendererLost"

/**
 * Owned only by the screen. The Saver captures the live view during Android state saving,
 * before AndroidView release; only the bounded Bundle crosses recreation/process death.
 */
internal class RavelryBrowserState(
    private val requestId: String,
    private var restored: Bundle? = null,
) {
    var webView: WebView? = null
        private set
    var rendererLost by mutableStateOf(
        restored?.getString(REQUEST_KEY) == requestId && restored?.getBoolean(RENDERER_LOST_KEY) == true,
    )
        private set
    var progress by mutableIntStateOf(0)
        private set
    var canGoBack by mutableStateOf(false)
        private set

    /** Viimeisin kassan ulkopuolinen Ravelry-sivu; lomakkeella aloitettu osto jatkuu siitä selaimessa. */
    var fallbackUrl =
        restored
            ?.takeIf { it.getString(REQUEST_KEY) == requestId }
            ?.getString(FALLBACK_KEY)
            ?.let(::restorableRavelryPageOrNull) ?: RAVELRY_PATTERN_SEARCH_URL
        private set

    fun visited(
        view: WebView,
        url: String?,
    ) {
        if (webView === view) {
            restorableRavelryPageOrNull(url)?.let { fallbackUrl = it }
            updateBack(view)
        }
    }

    fun updateBack(view: WebView) {
        if (webView === view) canGoBack = view.eligibleBackOffset() != null
    }

    fun updateProgress(
        view: WebView,
        value: Int,
    ) {
        if (webView === view) progress = value
    }

    fun goBack(): Boolean {
        val view = webView ?: return false
        val offset = view.eligibleBackOffset() ?: return false
        view.goBackOrForward(offset)
        return true
    }

    fun restoreOrLoad(
        view: WebView,
        startUrl: String?,
        openPurchase: (String) -> Unit,
    ) {
        check(!rendererLost)
        webView?.let(::release)
        webView = view
        val saved = restored
        restored = null
        if (saved != null && saved.getString(REQUEST_KEY) == requestId) {
            fallbackUrl = restorableRavelryPageOrNull(saved.getString(FALLBACK_KEY)) ?: RAVELRY_PATTERN_SEARCH_URL
            val history = saved.getStringArrayList(HISTORY_KEY)
            val snapshot = saved.getBundle(SNAPSHOT_KEY)
            val eligible =
                history?.isNotEmpty() == true &&
                    history.all { eligibleRavelryPageOrNull(it) != null } &&
                    saved.serializedSize() <= BROWSER_STATE_BUDGET_BYTES
            if (eligible && snapshot != null && runCatching { view.restoreState(snapshot) }.getOrNull() != null) {
                return
            }
            view.loadUrl(fallbackUrl)
            return
        }

        val startPage = startUrl?.let(::ravelryPageUrlOrNull)
        if (startPage != null && isRavelryPurchasePage(startPage)) {
            openPurchase(startPage)
        }
        fallbackUrl = restorableRavelryPageOrNull(startPage) ?: RAVELRY_PATTERN_SEARCH_URL
        view.loadUrl(startPage?.takeUnless(::isRavelryPurchasePage) ?: RAVELRY_PATTERN_SEARCH_URL)
    }

    fun save(): Bundle {
        val view = webView
        if (view == null) return restored ?: metadata()
        visited(view, view.url)
        return saveBrowserState(view, metadata())
    }

    fun release(view: WebView) {
        if (webView !== view) return
        restored = save()
        CookieManager.getInstance().flush()
        dispose(view)
    }

    fun rendererGone(view: WebView) {
        if (webView !== view) return
        // Only metadata captured while the renderer was alive is safe to use here.
        rendererLost = true
        restored = metadata()
        dispose(view)
    }

    fun retry() {
        if (!rendererLost) return
        rendererLost = false
        restored = metadata()
    }

    private fun dispose(view: WebView) {
        webView = null
        canGoBack = false
        progress = 0
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }

    private fun metadata() =
        Bundle().apply {
            putString(REQUEST_KEY, requestId)
            putString(FALLBACK_KEY, fallbackUrl)
            putBoolean(RENDERER_LOST_KEY, rendererLost)
        }

    companion object {
        fun saver(requestId: String): Saver<RavelryBrowserState, Bundle> =
            Saver(
                save = { it.save() },
                restore = { RavelryBrowserState(requestId, it.takeIf { it.getString(REQUEST_KEY) == requestId }) },
            )
    }
}

internal fun saveBrowserState(
    view: WebView,
    metadata: Bundle,
    useBoundedSave: Boolean = WebViewFeature.isFeatureSupported(WebViewFeature.SAVE_STATE),
): Bundle {
    val fallback = Bundle(metadata)
    return runCatching {
        val history = view.copyBackForwardList()
        val lastIndex = if (useBoundedSave) history.currentIndex else history.size - 1
        if (history.currentIndex !in 0 until history.size) return fallback
        val destinations = history.eligibleDestinations(lastIndex) ?: return fallback
        val result = Bundle(metadata).apply { putStringArrayList(HISTORY_KEY, destinations) }
        if (result.serializedSize() > METADATA_BUDGET_BYTES) return fallback
        val snapshot = Bundle()
        // Reserve the nested Bundle framing too; the final Parcel measurement is authoritative.
        result.putBundle(SNAPSHOT_KEY, snapshot)
        val snapshotBudget = BROWSER_STATE_BUDGET_BYTES - result.serializedSize()
        if (useBoundedSave && WebViewFeature.isFeatureSupported(WebViewFeature.SAVE_STATE)) {
            WebViewCompat.saveState(view, snapshot, snapshotBudget, false)
        } else if (view.saveState(snapshot) == null) {
            return fallback
        }
        result.takeIf { !snapshot.isEmpty && it.serializedSize() <= BROWSER_STATE_BUDGET_BYTES } ?: fallback
    }.getOrDefault(fallback)
}

private fun WebBackForwardList.eligibleDestinations(lastIndex: Int): ArrayList<String>? {
    val destinations = arrayListOf<String>()
    for (index in 0..lastIndex) {
        val item = getItemAtIndex(index)
        val url = eligibleRavelryPageOrNull(item?.url) ?: return null
        val originalUrl = eligibleRavelryPageOrNull(item?.originalUrl) ?: return null
        destinations += url
        destinations += originalUrl
    }
    return destinations
}

internal fun Bundle.serializedSize(): Int {
    val parcel = Parcel.obtain()
    return try {
        parcel.writeBundle(this)
        parcel.dataSize()
    } finally {
        parcel.recycle()
    }
}

/** Skip ineligible entries rather than getting stuck on the same blocked Back target. */
internal fun WebView.eligibleBackOffset(): Int? {
    val history = copyBackForwardList()
    for (index in history.currentIndex - 1 downTo 0) {
        val item = history.getItemAtIndex(index) ?: continue
        if (
            eligibleRavelryPageOrNull(item.url) != null &&
            eligibleRavelryPageOrNull(item.originalUrl) != null
        ) {
            return index - history.currentIndex
        }
    }
    return null
}
