package com.finnvek.knittools.ui.screens.yarncard

import android.net.Uri

internal fun handleYarnLabelCaptureResult(
    success: Boolean,
    pendingPhotoUri: Uri?,
    onCaptured: (Uri) -> Unit,
    onDeleteUnusedPhoto: (String) -> Unit,
    clearPendingPhoto: () -> Unit,
) {
    val photoUri = pendingPhotoUri ?: return
    clearPendingPhoto()
    if (success) {
        onCaptured(photoUri)
    } else {
        onDeleteUnusedPhoto(photoUri.toString())
    }
}
