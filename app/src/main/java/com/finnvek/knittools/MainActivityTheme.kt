package com.finnvek.knittools

import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.ThemeMode

internal fun AppPreferences?.resolveStartupDarkTheme(systemInDarkTheme: Boolean): Boolean? =
    this?.themeMode?.resolveDarkTheme(systemInDarkTheme)

private fun ThemeMode.resolveDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
