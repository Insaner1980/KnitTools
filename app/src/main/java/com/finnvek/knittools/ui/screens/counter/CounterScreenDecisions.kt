package com.finnvek.knittools.ui.screens.counter

internal fun requestCounterFeature(
    hasAccess: Boolean,
    onOpenFeature: () -> Unit,
    onOpenUpgrade: () -> Unit,
) {
    if (hasAccess) {
        onOpenFeature()
    } else {
        onOpenUpgrade()
    }
}

internal fun handleStitchTrackingToggle(
    enabled: Boolean,
    stitchCount: Int?,
    onRequestStitchCount: () -> Unit,
    onSetStitchTrackingEnabled: (Boolean) -> Unit,
) {
    if (enabled && (stitchCount ?: 0) <= 0) {
        onRequestStitchCount()
    } else {
        onSetStitchTrackingEnabled(enabled)
    }
}
