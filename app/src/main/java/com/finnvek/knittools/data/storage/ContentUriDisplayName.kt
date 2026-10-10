package com.finnvek.knittools.data.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/** Tiedoston näyttönimi content URI:sta, esim. "STEP_BY_STEP_SWEATER_V3.pdf"; null jos sitä ei ole. */
internal fun ContentResolver.displayNameOrNull(uri: Uri): String? =
    runCatching {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(columnIndex)?.takeIf(String::isNotBlank)
            } else {
                null
            }
        }
    }.getOrNull()
