package com.finnvek.knittools.ui.screens.ravelry

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

internal fun openRavelryUrl(
    context: Context,
    url: String,
    failureMessage: String,
) {
    val opened =
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
    if (!opened) {
        Toast
            .makeText(
                context,
                failureMessage,
                Toast.LENGTH_SHORT,
            ).show()
    }
}
