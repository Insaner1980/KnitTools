package com.finnvek.knittools.ui.navigation

import android.graphics.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.ui.TestContentSplashRule
import org.junit.Rule

abstract class AdaptiveRouteTestFixture {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val splash = TestContentSplashRule()

    @get:Rule(order = 2)
    val rule = createAndroidComposeRule<MainActivity>()

    protected val app: App = ApplicationProvider.getApplicationContext()
    protected val db get() = app.database.get()
    protected var originalSize: String? = null
    protected var initialized = false

    protected fun prepareResize() {
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        check(InstrumentationRegistry.getArguments().getString("adaptiveResizeAllowed") == "true")
        originalSize =
            shell("wm size")
                .lineSequence()
                .firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(":")
                ?.trim()
        initialized = true
        ProPolicyTestControl.setPurchased(true)
    }

    protected fun text(value: String) = rule.findText(value)

    protected suspend fun setAdaptivePreferences() {
        app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
        app.preferencesManager.get().setThemeMode(ThemeMode.LIGHT)
    }

    protected fun showFold(
        bounds: Rect,
        orientation: FoldingFeature.Orientation,
    ): FoldingFeature {
        val fold = FoldingFeature(bounds, size = 24, orientation = orientation)
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
        rule.waitForIdle()
        return fold
    }
}
