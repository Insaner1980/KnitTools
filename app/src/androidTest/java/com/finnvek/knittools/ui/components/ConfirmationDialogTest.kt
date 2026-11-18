package com.finnvek.knittools.ui.components

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.res.Configuration
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class ConfirmationDialogTest(
    private val width: Int,
    private val fontScale: Float,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longProjectDeletionMessageCanScrollWithoutConfirming() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val automation =
            if (InstrumentationRegistry.getArguments().getString("confirmationTalkBack") == "true") {
                instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).also {
                    val manager = base.getSystemService(AccessibilityManager::class.java)
                    compose.waitUntil(10_000) {
                        manager.isTouchExplorationEnabled &&
                            manager
                                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
                                .any { service ->
                                    service.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback"
                                }
                    }
                }
            } else {
                null
            }
        val context =
            base.createConfigurationContext(
                Configuration(base.resources.configuration).apply { setLocale(Locale.GERMAN) },
            )
        // Project names accept arbitrary text; use only synthetic content in this component test.
        val name = "Lange Strickjacke mit Zopfmuster und verschiedenen Farben ".repeat(20)
        val message = context.getString(R.string.delete_project_message, name)
        val confirm = context.getString(R.string.delete)
        val height = 600.dp
        var confirmations = 0
        var dismissals = 0
        compose.setContent {
            KnitToolsTheme(isDarkTheme = dark) {
                ConfirmationDialog(
                    title = context.getString(R.string.delete_project),
                    message = message,
                    modifier = Modifier.width(width.dp).heightIn(max = height).testTag("confirmation-dialog"),
                    confirmText = confirm,
                    isDestructive = true,
                    onConfirm = { confirmations++ },
                    onDismiss = { dismissals++ },
                )
            }
        }
        val text = compose.onNodeWithText(message)
        val textNode = text.fetchSemanticsNode()
        val range = textNode.config[SemanticsProperties.VerticalScrollAxisRange]
        val dialog = compose.onNodeWithTag("confirmation-dialog").fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        val layout = layouts.single()
        val density = layout.layoutInput.density
        val actualWidth = dialog.size.width / density.density
        val actualHeight = dialog.size.height / density.density
        val viewportHeight = textNode.boundsInRoot.height
        Log.i(
            "ConfirmationDialogTest",
            "dark=$dark dialog=${actualWidth}x$actualHeight dp font=${density.fontScale} " +
                "text=${layout.size.height}px viewport=${viewportHeight}px extent=${range.maxValue()}px",
        )
        assertEquals("Dialog width in dp", width.toFloat(), actualWidth, 1f)
        assertEquals("Dialog height in dp", height.value, actualHeight, 1f)
        assertEquals("Effective message font scale", fontScale, density.fontScale, 0.01f)
        assertTrue("The entire message must be laid out", !layout.hasVisualOverflow)
        assertTrue("Long message must expose a reachable scroll extent", range.maxValue() > 0f)
        assertEquals(
            "Scroll range must cover all off-screen text",
            layout.size.height - viewportHeight,
            range.maxValue(),
            1f,
        )
        assertEquals(0f, range.value(), 1f)
        text.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        assertEquals(range.maxValue(), range.value(), 1f)
        assertEquals(0, confirmations)
        assertEquals(0, dismissals)
        val confirmButton = compose.onNode(hasText(confirm) and hasClickAction()).assertIsDisplayed()
        if (automation == null) {
            confirmButton.performClick()
        } else {
            var target: AccessibilityNodeInfo? = null
            compose.waitUntil(5_000) {
                target = automation.rootInActiveWindow?.let { findButton(it, confirm, base.packageName) }
                target != null
            }
            val button = requireNotNull(target)
            button.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            compose.waitUntil(5_000) { button.refresh() && button.isAccessibilityFocused }
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            compose.waitForIdle()
        }
        assertEquals(1, confirmations)
        assertEquals(0, dismissals)
    }

    private fun findButton(
        node: AccessibilityNodeInfo,
        label: String,
        packageName: String,
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.text?.toString() == label) {
            generateSequence(node) { it.parent }
                .firstOrNull {
                    it.isClickable && it.isVisibleToUser && it.packageName == packageName
                }?.let { return it }
        }
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> findButton(child, label, packageName)?.let { return it } }
        }
        return null
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0} font={1} dark={2}")
        fun configurations(): List<Array<Any>> {
            // The dialog uses the Android window's font scale. Run the matrix once per system setting.
            val deviceConfiguration = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
            val font =
                InstrumentationRegistry.getArguments().getString("confirmationFontScale")?.toFloat()
                    ?: deviceConfiguration.fontScale
            require(font == 1f || font == 2f) { "Set the device font scale to 1.0 or 2.0 before running this test" }
            val widths = if (font == 1f) listOf(320) else listOf(412, 320)
            return widths.flatMap { width -> listOf(arrayOf(width, font, false), arrayOf(width, font, true)) }
        }
    }
}
