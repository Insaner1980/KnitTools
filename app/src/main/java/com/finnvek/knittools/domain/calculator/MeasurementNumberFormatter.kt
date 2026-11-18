package com.finnvek.knittools.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object MeasurementNumberFormatter {
    fun format(
        value: Double,
        locale: Locale,
        maximumFractionDigits: Int = 2,
    ): String {
        require(value.isFinite())
        require(maximumFractionDigits >= 0)
        val magnitude = abs(value)
        val fractionalDigits =
            if (magnitude > 0.0 && magnitude < 0.5 * 10.0.pow(-maximumFractionDigits)) {
                maxOf(maximumFractionDigits, 1 - floor(log10(magnitude)).toInt())
            } else {
                maximumFractionDigits
            }
        return NumberFormat
            .getNumberInstance(locale)
            .apply {
                minimumFractionDigits = 0
                this.maximumFractionDigits = fractionalDigits
                roundingMode = RoundingMode.HALF_UP
            }.format(value)
    }

    fun formatEditing(value: Double): String {
        require(value.isFinite())
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    fun percent(
        valuePercent: Double,
        locale: Locale,
    ): String {
        require(valuePercent.isFinite())
        return NumberFormat
            .getPercentInstance(locale)
            .apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 1
                roundingMode = RoundingMode.HALF_UP
                if (valuePercent > 0.0 && this is DecimalFormat) positivePrefix = "+$positivePrefix"
            }.format(valuePercent / 100.0)
    }
}
