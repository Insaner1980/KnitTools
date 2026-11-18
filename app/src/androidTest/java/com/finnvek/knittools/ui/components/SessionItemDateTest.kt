package com.finnvek.knittools.ui.components

import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SessionItemDateTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun storedZoneAndAppLanguageIndependentlyDetermineTheDisplayedDate() {
        val start = Instant.parse("2026-01-02T12:00:00Z")
        val deviceZone = ZoneId.systemDefault()
        val nextDayZone = ZoneId.of("Pacific/Kiritimati")
        val sessionZone =
            if (start.atZone(deviceZone).toLocalDate() == start.atZone(nextDayZone).toLocalDate()) {
                ZoneId.of("UTC")
            } else {
                nextDayZone
            }
        val zoneId = mutableStateOf<String?>(sessionZone.id)
        val spanish = rule.activity.localizedTestContext("es")
        val finnish = rule.activity.localizedTestContext("fi")
        val appContext = mutableStateOf(spanish)
        rule.setContent {
            val context = appContext.value
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                KnitToolsTheme {
                    SessionItem(start.toEpochMilli(), 12, 1, 4, zoneId = zoneId.value)
                }
            }
        }

        fun expectDate(
            language: String,
            zone: ZoneId,
        ) {
            val context = appContext.value
            val locale = Locale.forLanguageTag(language)
            val skeleton = if (DateFormat.is24HourFormat(context)) "MMMdHm" else "MMMdhm"
            val expected = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
            expected.timeZone = TimeZone.getTimeZone(zone)
            rule.onNodeWithText(expected.format(Date.from(start))).assertIsDisplayed()
        }

        expectDate("es", sessionZone)
        rule.runOnIdle { appContext.value = finnish }
        expectDate("fi", sessionZone)
        rule.runOnIdle { zoneId.value = null }
        expectDate("fi", deviceZone)
        rule.runOnIdle { zoneId.value = "not/a/zone" }
        expectDate("fi", deviceZone)
    }
}
