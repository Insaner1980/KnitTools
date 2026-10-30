package com.finnvek.knittools.ui.navigation

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import com.finnvek.knittools.MainActivity
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt

internal fun shell(command: String): String =
    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
        FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
    }

internal fun captureScreenshot(
    context: Context,
    directoryName: String,
    name: String,
) {
    val directory = File(context.getExternalFilesDir(null), directoryName).apply { mkdirs() }
    val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
}

internal fun AndroidComposeTestRule<*, MainActivity>.resizeWindow(
    width: Int,
    height: Int,
    density: Float = activity.resources.displayMetrics.density,
) {
    shell("wm size ${(width * density).roundToInt()}x${(height * density).roundToInt()}")
    waitUntil(15_000) {
        activity.resources.configuration.let { it.screenWidthDp == width && it.screenHeightDp == height }
    }
    waitForIdle()
}

internal fun focusedWindowOutside(
    fold: FoldingFeature,
    onMeasured: (View, IntArray) -> Unit = { _, _ -> },
): Boolean {
    val view =
        WindowInspector.getGlobalWindowViews().lastOrNull { it.isAttachedToWindow && it.hasWindowFocus() }
            ?: return false
    val position = IntArray(2).also(view::getLocationOnScreen)
    onMeasured(view, position)
    return if (fold.orientation == FoldingFeature.Orientation.VERTICAL) {
        position[0] + view.width <= fold.bounds.left || position[0] >= fold.bounds.right
    } else {
        position[1] + view.height <= fold.bounds.top || position[1] >= fold.bounds.bottom
    }
}

internal fun ComposeTestRule.findText(value: String) =
    onAllNodes(hasText(value) or hasContentDescription(value)).onLast()

internal fun ComposeTestRule.scrollToText(
    value: String,
    list: () -> SemanticsNodeInteraction,
) = findText(value).also {
    if (onAllNodes(hasText(value) or hasContentDescription(value)).fetchSemanticsNodes().isEmpty()) {
        list().performScrollToNode(hasText(value) or hasContentDescription(value))
    } else {
        runCatching { it.performScrollTo() }
    }
    it.assertIsDisplayed()
}

internal fun ComposeTestRule.insightsHeading() =
    onAllNodes(
        hasAnyAncestor(
            hasTestTag("insights-chart-heading"),
        ) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().joinToString { it.config.getOrNull(SemanticsProperties.Text).toString() }

internal fun ComposeTestRule.awaitKeyboard(visible: Boolean) {
    var lastBottom = -1
    var lastChange = SystemClock.uptimeMillis()
    waitUntil(10_000) {
        val insets =
            WindowInspector
                .getGlobalWindowViews()
                .filter { it.isAttachedToWindow && it.hasWindowFocus() }
                .mapNotNull { ViewCompat.getRootWindowInsets(it) }
        val bottom = insets.maxOfOrNull { it.getInsets(WindowInsetsCompat.Type.ime()).bottom } ?: 0
        if (bottom != lastBottom) {
            lastBottom = bottom
            lastChange = SystemClock.uptimeMillis()
        }
        // A tabletop pane can sit entirely above a visible IME and receive zero overlap.
        insets.any { it.isVisible(WindowInsetsCompat.Type.ime()) } == visible &&
            SystemClock.uptimeMillis() - lastChange > 350
    }
}
