package com.finnvek.knittools.ui.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.domain.model.validateWebPatternUrl

enum class ExternalWebLinkOpenResult {
    Opened,
    InvalidUrl,
    NoBrowser,
    Failed,
}

fun openExternalWebLink(
    url: String,
    launch: (String) -> Unit,
): ExternalWebLinkOpenResult {
    val validation = validateWebPatternUrl(url)
    val originalUrl =
        (validation as? WebPatternUrlValidation.Valid)?.value?.originalUrl
            ?: return ExternalWebLinkOpenResult.InvalidUrl
    return try {
        launch(originalUrl)
        ExternalWebLinkOpenResult.Opened
    } catch (_: ActivityNotFoundException) {
        ExternalWebLinkOpenResult.NoBrowser
    } catch (_: SecurityException) {
        ExternalWebLinkOpenResult.Failed
    } catch (_: RuntimeException) {
        ExternalWebLinkOpenResult.Failed
    }
}

fun openExternalWebLink(
    context: Context,
    url: String,
): ExternalWebLinkOpenResult =
    openExternalWebLink(url) { validatedUrl ->
        context.startActivity(createExternalWebLinkIntent(validatedUrl))
    }

internal fun createExternalWebLinkIntent(url: String): Intent =
    Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
