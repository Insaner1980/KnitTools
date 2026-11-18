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
    isNewIntent: Boolean = false,
): Boolean {
    if (intent == null) return false
    val uri = intent.pdfUri(contentResolver) ?: return false
    if (isNewIntent) pdfIntentConsumed = false
    consumePdfIntent(uri, intent)
    return true
}

internal fun IncomingPdfViewModel.restoreInterruptedReceive(
    intent: Intent?,
    contentResolver: ContentResolver,
): Boolean {
    val uri = interruptedSourceUri ?: return false
    if (receiving.value) pdfIntentConsumed = true
    consumePdfIntent(uri, intent?.takeIf { it.pdfUri(contentResolver) == uri })
    return true
}

private fun Intent.pdfUri(contentResolver: ContentResolver): Uri? =
    when (action) {
        Intent.ACTION_VIEW ->
            data?.takeIf { source ->
                type == MIME_TYPE_PDF || runCatching { contentResolver.getType(source) }.getOrNull() == MIME_TYPE_PDF
            }

        Intent.ACTION_SEND ->
            if (type == MIME_TYPE_PDF) {
                IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                null
            }

        else -> null
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
