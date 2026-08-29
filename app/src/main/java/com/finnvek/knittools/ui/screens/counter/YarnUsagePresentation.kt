package com.finnvek.knittools.ui.screens.counter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.YarnUsageCalculator
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import kotlin.math.abs

internal fun YarnUsageUnit.titleResource(): Int =
    when (this) {
        YarnUsageUnit.METERS -> R.string.measurement_unit_meter
        YarnUsageUnit.YARDS -> R.string.measurement_unit_yard
        YarnUsageUnit.GRAMS -> R.string.yarn_usage_grams
        YarnUsageUnit.SKEINS -> R.string.yarn_usage_skeins
    }

@Composable
internal fun yarnUsageAmount(
    meters: Double,
    unit: YarnUsageUnit,
    amounts: YarnUsageAmounts,
): String? {
    val value =
        YarnUsageCalculator.fromMeters(abs(meters), unit, amounts.metersPerSkein, amounts.gramsPerSkein) ?: return null
    val locale = rememberCurrentLocale()
    val formatted = MeasurementNumberFormatter.format(value, locale)
    return when (unit) {
        YarnUsageUnit.METERS -> stringResource(R.string.yarn_usage_meters_format, formatted)
        YarnUsageUnit.YARDS -> stringResource(R.string.yarn_usage_yards_format, formatted)
        YarnUsageUnit.GRAMS -> stringResource(R.string.yarn_usage_grams_format, formatted)
        YarnUsageUnit.SKEINS ->
            pluralStringResource(
                R.plurals.yarn_usage_skeins_format,
                if (value == 1.0 || (locale.language in listOf("fr", "pt") && value < 2.0)) 1 else 2,
                formatted,
            )
    }
}

@Composable
internal fun yarnUsageRemaining(
    amounts: YarnUsageAmounts,
    unit: YarnUsageUnit,
): String {
    val remaining = YarnUsageCalculator.remaining(amounts.allocatedMeters, amounts.usedMeters)
    val formatted = remaining?.let { yarnUsageAmount(it, unit, amounts) }
    return if (formatted == null) {
        stringResource(R.string.yarn_usage_remaining_unknown)
    } else {
        stringResource(
            if (remaining < 0.0) R.string.yarn_usage_over_format else R.string.yarn_usage_remaining_format,
            formatted,
        )
    }
}
