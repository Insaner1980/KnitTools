package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import java.util.Locale

@Composable
internal fun String.localizedUppercase(): String = uppercaseForDisplay(rememberCurrentLocale())

internal fun String.uppercaseForDisplay(locale: Locale): String = uppercase(locale)
