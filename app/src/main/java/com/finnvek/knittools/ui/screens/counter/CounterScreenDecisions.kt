package com.finnvek.knittools.ui.screens.counter

internal enum class ProjectPatternCardAction {
    OpenPicker,
    OpenMetadataDetail,
    OpenPrimaryDocument,
    OpenDocumentRecovery,
}

internal fun projectPatternCardAction(
    hasMetadataLink: Boolean,
    hasPrimaryDocument: Boolean,
    primaryAvailable: Boolean,
): ProjectPatternCardAction =
    when {
        hasPrimaryDocument && primaryAvailable -> ProjectPatternCardAction.OpenPrimaryDocument
        hasPrimaryDocument -> ProjectPatternCardAction.OpenDocumentRecovery
        hasMetadataLink -> ProjectPatternCardAction.OpenMetadataDetail
        else -> ProjectPatternCardAction.OpenPicker
    }

internal fun hasProjectPatternContent(
    hasMetadataLink: Boolean,
    hasDocuments: Boolean,
): Boolean = hasMetadataLink || hasDocuments

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
