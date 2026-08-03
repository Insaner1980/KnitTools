package com.finnvek.knittools.domain.calculator

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun formatIntegerForDisplay(
    value: Long,
    locale: Locale,
): String = NumberFormat.getIntegerInstance(locale).format(value)

fun formatDecimalForDisplay(
    value: Double,
    locale: Locale,
    minimumFractionDigits: Int,
    maximumFractionDigits: Int,
): String =
    NumberFormat.getNumberInstance(locale).run {
        this.minimumFractionDigits = minimumFractionDigits
        this.maximumFractionDigits = maximumFractionDigits
        format(value)
    }

fun formatSignedDecimalForDisplay(
    value: Double,
    locale: Locale,
    fractionDigits: Int,
): String =
    (NumberFormat.getNumberInstance(locale) as DecimalFormat).run {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
        positivePrefix = "+"
        format(value)
    }

/**
 * Osuus prosentteina ilman desimaaleja. Prosenttimerkin paikka ja sitä edeltävä
 * väli tulevat lokaalilta, joten merkkiä ei liimata Kotlinissa.
 */
fun formatPercentForDisplay(
    fraction: Double,
    locale: Locale,
): String =
    NumberFormat
        .getPercentInstance(locale)
        .apply { maximumFractionDigits = 0 }
        .format(fraction)
