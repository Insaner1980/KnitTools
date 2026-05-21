package com.finnvek.knittools.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun rememberLocaleDateFormat(pattern: String): SimpleDateFormat {
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { configuration.primaryLocale() }

    return remember(pattern, locale) { SimpleDateFormat(pattern, locale) }
}

private fun Configuration.primaryLocale(): Locale = ConfigurationCompat.getLocales(this)[0] ?: Locale.ROOT
