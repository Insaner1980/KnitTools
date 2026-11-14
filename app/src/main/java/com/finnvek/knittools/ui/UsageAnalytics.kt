package com.finnvek.knittools.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent

val LocalUsageAnalytics = staticCompositionLocalOf { UsageAnalytics.NONE }

/** One valid result per tool visit, never the input or calculated value. */
@Composable
internal fun TrackCalculatorResult(
    tool: UsageDetail,
    hasResult: Boolean,
) {
    val analytics = LocalUsageAnalytics.current
    var reported by remember(tool) { mutableStateOf(false) }
    LaunchedEffect(hasResult, tool) {
        if (hasResult && !reported) {
            reported = true
            analytics.track(UsageEvent.CALCULATOR_RESULT_SHOWN, tool)
        }
    }
}
