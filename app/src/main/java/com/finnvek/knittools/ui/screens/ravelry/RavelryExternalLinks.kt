package com.finnvek.knittools.ui.screens.ravelry

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import java.net.URI
import java.util.Locale

private const val RAVELRY_EXTERNAL_SCHEME = "https"
private val RAVELRY_EXTERNAL_HOSTS = setOf("ravelry.com", "www.ravelry.com")

internal fun openRavelryUrl(
    context: Context,
    url: String,
    failureMessage: String,
) {
    val uri = ravelryExternalUriOrNull(url)
    val opened =
        if (uri == null) {
            false
        } else {
            runCatching {
                context.startActivity(createRavelryExternalIntent(uri))
            }.fold(
                onSuccess = { true },
                onFailure = { error ->
                    if (error is ActivityNotFoundException) {
                        false
                    } else {
                        throw error
                    }
                },
            )
        }
    if (!opened) {
        Toast
            .makeText(
                context,
                failureMessage,
                Toast.LENGTH_SHORT,
            ).show()
    }
}

internal fun ravelryExternalUrlOrNull(url: String): String? {
    val parsedUri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!parsedUri.scheme.equals(RAVELRY_EXTERNAL_SCHEME, ignoreCase = true)) return null

    val host = parsedUri.host?.lowercase(Locale.US) ?: return null
    if (host !in RAVELRY_EXTERNAL_HOSTS) return null

    return parsedUri.toString()
}

internal fun ravelryExternalUriOrNull(url: String): Uri? = ravelryExternalUrlOrNull(url)?.toUri()

internal fun createRavelryExternalIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
