package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

internal class PatternImagePickerContract :
    ActivityResultContracts.PickMultipleVisualMedia(PatternImageImportLimits.MAX_PAGES) {
    override fun createIntent(
        context: Context,
        input: PickVisualMediaRequest,
    ): Intent {
        val configuration = context.resources.configuration
        // PhotoPickerin toimintopalkki ei mahdu kapeaan ikkunaan suurella fontilla.
        // SAF säilyttää järjestelmän monivalinnan ja saman välittömän kopiointiputken.
        return if (configuration.screenWidthDp < 360 && configuration.fontScale >= 1.5f) {
            ActivityResultContracts
                .OpenMultipleDocuments()
                .createIntent(context, arrayOf("image/*"))
                .addCategory(Intent.CATEGORY_OPENABLE)
        } else {
            super.createIntent(context, input)
        }
    }
}
