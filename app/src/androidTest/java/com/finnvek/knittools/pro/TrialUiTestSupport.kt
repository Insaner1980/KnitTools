package com.finnvek.knittools.pro

import android.app.Instrumentation
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertTrue

internal fun Instrumentation.findTrialText(
    value: String,
    includeContentDescription: Boolean = false,
): AccessibilityNodeInfo? {
    val root = uiAutomation.rootInActiveWindow ?: return null
    if (root.packageName?.toString() != targetContext.packageName) return null
    return findTrialText(root, value, includeContentDescription)
}

private fun findTrialText(
    node: AccessibilityNodeInfo,
    value: String,
    includeContentDescription: Boolean,
): AccessibilityNodeInfo? {
    if (node.isVisibleToUser &&
        (node.text?.toString() == value || (includeContentDescription && node.contentDescription?.toString() == value))
    ) {
        return node
    }
    for (index in 0 until node.childCount) {
        node.getChild(index)?.let { child ->
            findTrialText(child, value, includeContentDescription)?.let { return it }
        }
    }
    return null
}

internal fun Instrumentation.clickTrialText(
    value: String,
    includeContentDescription: Boolean = false,
) {
    waitForTrialUi("Visible text: $value") { findTrialText(value, includeContentDescription) != null }
    var node = checkNotNull(findTrialText(value, includeContentDescription))
    while (!node.isClickable) node = checkNotNull(node.parent)
    assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
}

internal fun waitForTrialUi(
    message: String,
    condition: () -> Boolean,
) {
    val deadline = SystemClock.uptimeMillis() + 15_000
    while (!condition()) {
        check(SystemClock.uptimeMillis() < deadline) { message }
        SystemClock.sleep(50)
    }
}
