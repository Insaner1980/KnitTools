package com.finnvek.knittools.ui.screens.ravelry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class RavelryBrowserNavigationTest {
    @get:Rule val timeout = Timeout.seconds(60)

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
        } finally {
            instrumentation.runOnMainSync { activity?.finishAndRemoveTask() }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Browser URL did not update within 10 seconds" }
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

    private fun findBrowser(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findBrowser(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
