package com.finnvek.knittools.ui.screens.counter

internal fun resolvePatternAttachmentUri(
    sourceUriString: String,
    copiedUriString: String?,
    isSourceAppOwned: Boolean,
): String? =
    if (isSourceAppOwned) {
        sourceUriString
    } else {
        copiedUriString
    }
