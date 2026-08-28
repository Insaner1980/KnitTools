package com.finnvek.knittools.util.extensions

import com.finnvek.knittools.domain.calculator.MeasurementCalculator
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import java.util.Locale

fun cmToInches(cm: Double): Double = MeasurementUnit.INCH.fromMillimeters(MeasurementUnit.CM.toMillimeters(cm))

fun inchesToCm(inches: Double): Double = MeasurementUnit.CM.fromMillimeters(MeasurementUnit.INCH.toMillimeters(inches))

fun metersToYards(meters: Double): Double =
    MeasurementUnit.YARD.fromMillimeters(MeasurementUnit.METER.toMillimeters(meters))

fun yardsToMeters(yards: Double): Double =
    MeasurementUnit.METER.fromMillimeters(MeasurementUnit.YARD.toMillimeters(yards))

fun convertFieldValue(
    value: String,
    toImperial: Boolean,
    isLength: Boolean = true,
): String {
    val number = MeasurementNumberParser.parse(value, Locale.ROOT, allowZero = true).value ?: return value
    if (number == 0.0) return value
    val metric = if (isLength) MeasurementUnit.CM else MeasurementUnit.METER
    val imperial = if (isLength) MeasurementUnit.INCH else MeasurementUnit.YARD
    val from = if (toImperial) metric else imperial
    val to = if (toImperial) imperial else metric
    val converted = MeasurementCalculator.convert(number, from, to) ?: return value
    return MeasurementNumberFormatter.formatEditing(converted)
}

fun convertGaugeValue(
    value: String,
    toImperial: Boolean,
): String {
    val number = MeasurementNumberParser.parse(value, Locale.ROOT, allowZero = true).value ?: return value
    if (number == 0.0) return value
    val from = if (toImperial) GaugeBasis.PER_10_CM else GaugeBasis.PER_4_INCHES
    val to = if (toImperial) GaugeBasis.PER_4_INCHES else GaugeBasis.PER_10_CM
    val density = MeasurementCalculator.density(number, from.lengthMm) ?: return value
    val converted = MeasurementCalculator.gaugeForBasis(density, to) ?: return value
    return MeasurementNumberFormatter.formatEditing(converted)
}
