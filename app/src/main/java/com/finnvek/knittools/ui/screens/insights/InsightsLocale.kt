package com.finnvek.knittools.ui.screens.insights

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

internal fun currentInsightsLocale(): Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()

// Android plural selection requires Int. Preserve the last six digits and million
// multiples for the supported locales; formatting still receives the original Long.
internal fun insightsRowPluralQuantity(rows: Long): Int =
    if (rows <= Int.MAX_VALUE) rows.toInt() else (1_000_000L + rows % 1_000_000L).toInt()
