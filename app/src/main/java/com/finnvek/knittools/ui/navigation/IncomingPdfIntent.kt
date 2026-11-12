package com.finnvek.knittools.ui.navigation

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

private const val MIME_TYPE_PDF = "application/pdf"

/**
 * Ladattu PDF-ohje avattiin tai jaettiin KnitToolsiin (esim. Ravelryn lataus). Palauttaa true,
 * kun intent oli PDF; intent tyhjennetään, jottei sama PDF tule uudelleen näkymän uudelleenluonnissa.
 */
fun IncomingPdfViewModel.receiveFromIntent(
    intent: Intent?,
    contentResolver: ContentResolver,
): Boolean {
    if (intent == null) return false
    val uri =
        when (intent.action) {
            Intent.ACTION_VIEW ->
                intent.data?.takeIf { data ->
                    intent.type == MIME_TYPE_PDF ||
                        runCatching { contentResolver.getType(data) }.getOrNull() == MIME_TYPE_PDF
                }

            Intent.ACTION_SEND ->
                IntentCompat
                    .getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.takeIf { intent.type == MIME_TYPE_PDF }

            else -> null
        } ?: return false
    consumePdfIntent(uri, intent)
    return true
}

internal fun IncomingPdfViewModel.restoreInterruptedReceive(intent: Intent?): Boolean {
    val uri = interruptedSourceUri ?: return false
    if (receiving.value) pdfIntentConsumed = true
    consumePdfIntent(uri, intent)
    return true
}

private fun IncomingPdfViewModel.consumePdfIntent(
    uri: Uri,
    intent: Intent?,
) {
    if (!pdfIntentConsumed) {
        pdfIntentConsumed = true
        receive(uri)
    }
    intent?.apply {
        action = Intent.ACTION_MAIN
        data = null
        type = null
        removeExtra(Intent.EXTRA_STREAM)
    }
}
