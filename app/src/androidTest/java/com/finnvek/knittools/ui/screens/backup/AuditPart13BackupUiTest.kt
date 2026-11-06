package com.finnvek.knittools.ui.screens.backup

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class AuditPart13BackupUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var originalAccessibilityFlags = 0

    @Before fun showOnlyTheTestActivityAboveTheLockScreen() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        originalAccessibilityFlags = automation.serviceInfo.flags
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            }
        compose.runOnUiThread {
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @After fun restoreAutomationAccessibilityFlags() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = originalAccessibilityFlags }
    }

    @Test fun normalWidthAndFontAllLanguagesBothThemes() = matrix(400, 1f)

    @Test fun narrowWidthAllLanguagesBothThemes() = matrix(320, 1f)

    @Test fun largeFontAllLanguagesBothThemes() = matrix(400, 2f)

    @Test fun narrowWidthAndLargeFontAllLanguagesBothThemes() = matrix(320, 2f)

    private fun matrix(
        width: Int,
        fontScale: Float,
    ) {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val language = mutableStateOf("en")
        val dark = mutableStateOf(false)
        val state = mutableStateOf(BackupUiState())
        var confirmed = 0
        var continued = 0
        compose.setContent {
            val config =
                Configuration(app.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(language.value))
                    this.fontScale = fontScale
                }
            val localized = app.createConfigurationContext(config)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                key(language.value, dark.value) {
                    KnitToolsTheme(isDarkTheme = dark.value) {
                        Box(Modifier.width(width.dp)) {
                            BackupContent(state.value, {}, {}, {}, { confirmed++ }, {}, { continued++ })
                        }
                    }
                }
            }
        }
        for (locale in listOf("en", "fi", "sv", "de", "fr", "es", "pt", "it", "nb", "da", "nl")) {
            for (night in listOf(false, true)) {
                val config =
                    Configuration(
                        app.resources.configuration,
                    ).apply { setLocale(Locale.forLanguageTag(locale)) }
                val context = app.createConfigurationContext(config)

                fun label(id: Int) = context.getString(id)
                compose.runOnIdle {
                    language.value = locale
                    dark.value = night
                    state.value =
                        BackupUiState(
                            BackupPhase.PREVIEW,
                            BackupPreview(1_700_000_000_000, "1.0", 123, 456, 789, 2_000),
                        )
                }
                val before = confirmed
                compose
                    .onNode(hasText(label(R.string.backup_restore)) and isEnabled())
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp)
                    .performClick()
                assertEquals(before, confirmed)
                compose.onNodeWithText(label(R.string.backup_confirm_message)).assertIsDisplayed()
                compose.onNodeWithText(label(R.string.backup_replace)).assertIsDisplayed().performClick()
                compose.runOnIdle { assertEquals(before + 1, confirmed) }
                compose.runOnIdle { state.value = BackupUiState(error = BackupError.CORRUPT) }
                compose.onNodeWithText(label(R.string.backup_error_corrupt)).performScrollTo().assertIsDisplayed()
                compose.runOnIdle { state.value = BackupUiState(BackupPhase.RESTORED) }
                val previousContinued = continued
                compose
                    .onNodeWithText(label(R.string.backup_continue))
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp)
                val button = accessibilityButton(label(R.string.backup_continue))
                button.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                compose.waitUntil(5_000) { button.refresh() && button.isAccessibilityFocused }
                assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                compose.runOnIdle { assertEquals(previousContinued + 1, continued) }
            }
        }
    }

    private fun accessibilityButton(label: String): AccessibilityNodeInfo {
        var result: AccessibilityNodeInfo? = null
        compose.waitUntil(5_000) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                automation.clearCache()
            }
            val root = automation.rootInActiveWindow

            fun visit(node: AccessibilityNodeInfo?) {
                if (node == null) return
                if ((node.text?.toString() == label || node.contentDescription?.toString() == label) &&
                    node.isVisibleToUser
                ) {
                    var action: AccessibilityNodeInfo = node
                    while (!action.isClickable) action = action.parent ?: break
                    if (action.isClickable && action.isVisibleToUser) result = action
                }
                repeat(node.childCount) { visit(node.getChild(it)) }
            }
            visit(root)
            result != null
        }
        return checkNotNull(result)
    }
}
