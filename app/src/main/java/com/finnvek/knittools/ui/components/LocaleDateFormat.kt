package com.finnvek.knittools.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat

@Composable
internal fun rememberLocaleDateFormat(
    dateSkeleton: String,
    includeTime: Boolean = false,
): SimpleDateFormat {
    val context = LocalContext.current
    val locale = rememberCurrentLocale()
    val use24HourTime = includeTime && AndroidDateFormat.is24HourFormat(context)
    val skeleton =
        dateSkeleton +
            when {
                !includeTime -> ""
                use24HourTime -> "Hm"
                else -> "hm"
            }
    val pattern = remember(locale, skeleton) { localizedDateTimePattern(locale, skeleton) }

    return remember(pattern, locale) { SimpleDateFormat(pattern, locale) }
}

@Composable
internal fun rememberCurrentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.primaryLocale() }
}

internal fun localizedDateTimePattern(
    locale: Locale,
    skeleton: String,
): String = AndroidDateFormat.getBestDateTimePattern(locale, skeleton)

private fun Configuration.primaryLocale(): Locale = ConfigurationCompat.getLocales(this)[0] ?: Locale.ROOT
