package com.finnvek.knittools.ui.screens.insights

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

internal fun currentInsightsLocale(): Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
