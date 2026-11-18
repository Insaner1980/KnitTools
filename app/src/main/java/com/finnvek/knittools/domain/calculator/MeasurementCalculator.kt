package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementAdjustmentResult
import com.finnvek.knittools.domain.model.MeasurementCountResult
import com.finnvek.knittools.domain.model.MeasurementUnit
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

object MeasurementCalculator {
    fun convert(
        value: Double,
        from: MeasurementUnit,
        to: MeasurementUnit,
    ): Double? {
        if (!value.isFinite() || value < 0.0) return null
        if (value == 0.0 || from == to) return value
        val millimeters = from.toMillimeters(value)
        if (!millimeters.isFinite()) return null
        return to.fromMillimeters(millimeters).takeIf(::isPositiveFinite)
    }

    fun density(
        count: Double,
        lengthMm: Double,
    ): Double? {
        if (!isPositiveFinite(count) || !isPositiveFinite(lengthMm)) return null
        return (count / lengthMm).takeIf(::isPositiveFinite)
    }

    fun gaugeForBasis(
        density: Double,
        basis: GaugeBasis,
    ): Double? = exactCountForSize(density, basis.lengthMm)

    fun countForSize(
        density: Double,
        lengthMm: Double,
    ): MeasurementCountResult? {
        val exactCount = exactCountForSize(density, lengthMm) ?: return null
        return countResult(exactCount, density)
    }

    internal fun countForMeasuredSize(
        density: Double,
        lengthMm: Double,
    ): MeasurementCountResult? {
        val exactCount = exactCountForSize(density, lengthMm) ?: return null
        // Converting lengths and densities to millimeters can put a mathematical half a few bits off.
        val half = floor(exactCount) + 0.5
        val count = if (abs(exactCount - half) <= 2 * Math.ulp(half)) half else exactCount
        return countResult(count, density)
    }

    private fun countResult(
        exactCount: Double,
        density: Double,
    ): MeasurementCountResult? {
        val roundedCount = roundedWholeCount(exactCount, allowZero = true) ?: return null
        val roundedLength =
            if (roundedCount == 0) {
                0.0
            } else {
                sizeFromCount(roundedCount.toDouble(), density) ?: return null
            }
        return MeasurementCountResult(exactCount, roundedCount, roundedLength)
    }

    fun sizeFromCount(
        count: Double,
        density: Double,
    ): Double? {
        if (!isPositiveFinite(count) || !isPositiveFinite(density)) return null
        return (count / density).takeIf(::isPositiveFinite)
    }

    fun adjust(
        patternCount: Int,
        patternDensity: Double,
        actualDensity: Double,
    ): MeasurementAdjustmentResult? {
        val originalLength = sizeFromCount(patternCount.toDouble(), patternDensity) ?: return null
        val unchangedLength = sizeFromCount(patternCount.toDouble(), actualDensity) ?: return null
        val adjusted = countForMeasuredSize(actualDensity, originalLength) ?: return null
        val differencePercent = (actualDensity - patternDensity) / patternDensity * 100.0
        if (!differencePercent.isFinite()) return null
        return MeasurementAdjustmentResult(
            originalLengthMm = originalLength,
            unchangedLengthMm = unchangedLength,
            exactCount = adjusted.exactCount,
            roundedCount = adjusted.roundedCount,
            roundedLengthMm = adjusted.roundedLengthMm,
            differencePercent = differencePercent,
        )
    }

    internal fun exactCountForSize(
        density: Double,
        lengthMm: Double,
    ): Double? {
        if (!isPositiveFinite(density) || !isPositiveFinite(lengthMm)) return null
        return (density * lengthMm).takeIf(::isPositiveFinite)
    }

    internal fun roundedWholeCount(
        exactCount: Double,
        allowZero: Boolean = false,
    ): Int? {
        if (!exactCount.isFinite() || exactCount < 0.0 || exactCount >= Int.MAX_VALUE.toDouble() + 0.5) return null
        val rounded = exactCount.roundToInt()
        return rounded.takeIf { it > 0 || allowZero }
    }

    private fun isPositiveFinite(value: Double): Boolean = value.isFinite() && value > 0.0
}
