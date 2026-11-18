package com.finnvek.knittools.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import android.text.format.DateFormat as AndroidDateFormat

@Composable
internal fun rememberLocaleDateFormat(
    dateSkeleton: String,
    includeTime: Boolean = false,
    eventZoneId: String? = null,
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
    val deviceZone = ZoneId.systemDefault()

    return remember(pattern, locale, eventZoneId, deviceZone) {
        val zone =
            try {
                eventZoneId?.let(ZoneId::of) ?: deviceZone
            } catch (_: DateTimeException) {
                deviceZone
            }
        SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone(zone) }
    }
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

/**
 * Julkaisutarjouksen päättymishetki: päivä, minuutin tarkkuudella kellonaika ja aikavyöhyke, koska kampanja
 * päättyy Helsingin aikaan. Sekunnit tekivät rivistä muotoa "8:25:44 PM GMT+02:00".
 */
@Composable
internal fun rememberLaunchEndDateFormat(): SimpleDateFormat = rememberLocaleDateFormat("yMMMdz", includeTime = true)
