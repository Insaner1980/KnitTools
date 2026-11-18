package com.finnvek.knittools.ui.screens.ravelry

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import com.finnvek.knittools.MainActivity

internal fun browser(scenario: ActivityScenario<MainActivity>): WebView {
    var view: WebView? = null
    scenario.onActivity { view = findBrowser(it.findViewById(android.R.id.content)) }
    return requireNotNull(view)
}

internal fun findBrowser(view: View): WebView? {
    if (view is WebView) return view
    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            findBrowser(view.getChildAt(index))?.let { return it }
        }
    }
    return null
}

internal fun awaitBrowserUrl(
    scenario: ActivityScenario<MainActivity>,
    url: String,
    waitUntil: (() -> Boolean) -> Unit,
) {
    waitUntil {
        var loaded = false
        scenario.onActivity {
            val view = findBrowser(it.findViewById(android.R.id.content))
            loaded = view?.url == url && view.progress == 100
        }
        loaded
    }
}
