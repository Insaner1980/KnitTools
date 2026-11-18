package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.CastOnResult
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import java.math.BigDecimal

object CastOnCalculator {
    fun calculate(
        desiredWidth: Double,
        stitchGauge: Double,
        useInches: Boolean = false,
        patternRepeat: Int? = null,
        edgeStitches: Int = 0,
    ): CastOnResult? {
        if (edgeStitches < 0) return null
        val unit = if (useInches) MeasurementUnit.INCH else MeasurementUnit.CM
        val basis = if (useInches) GaugeBasis.PER_4_INCHES else GaugeBasis.PER_10_CM
        val density = MeasurementCalculator.density(stitchGauge, basis.lengthMm) ?: return null
        val exactCount =
            MeasurementCalculator.exactCountForSize(density, unit.toMillimeters(desiredWidth)) ?: return null
        val bodyStitches = MeasurementCalculator.roundedWholeCount(exactCount, allowZero = true) ?: return null

        return if (patternRepeat == null || patternRepeat <= 0) {
            calculateWithoutRepeat(bodyStitches, edgeStitches, density, unit)
        } else {
            val gaugeWidth = if (useInches) 4 else 10
            val scaledBodyCount = desiredWidth.toBigDecimal() * stitchGauge.toBigDecimal()
            calculateWithRepeat(scaledBodyCount, gaugeWidth, patternRepeat, edgeStitches, density, unit)
        }
    }

    private fun calculateWithoutRepeat(
        bodyStitches: Int,
        edgeStitches: Int,
        density: Double,
        unit: MeasurementUnit,
    ): CastOnResult? {
        val total = supportedCount(bodyStitches.toLong() + edgeStitches) ?: return null
        val width = widthForCount(total, density, unit) ?: return null
        return CastOnResult(stitches = total, actualWidth = width)
    }

    private fun calculateWithRepeat(
        scaledBodyCount: BigDecimal,
        gaugeWidth: Int,
        patternRepeat: Int,
        edgeStitches: Int,
        density: Double,
        unit: MeasurementUnit,
    ): CastOnResult? {
        // Compare in the entered decimal units so a true midpoint selects the lower repeat.
        val scaledRepeat = (patternRepeat.toLong() * gaugeWidth).toBigDecimal()
        val nearestDown = scaledBodyCount.divideToIntegralValue(scaledRepeat).longValueExact() * patternRepeat
        val nearestUp = nearestDown + patternRepeat
        val totalDown = nearestDown.takeIf { it > 0 }?.let { supportedCount(it + edgeStitches) }
        val totalUp = supportedCount(nearestUp + edgeStitches)
        val scaledMidpoint = ((nearestDown + nearestUp) * gaugeWidth).toBigDecimal()
        val closerTotal =
            if (totalDown != null && (totalUp == null || scaledBodyCount + scaledBodyCount <= scaledMidpoint)) {
                totalDown
            } else {
                totalUp ?: return null
            }
        val actualWidth = widthForCount(closerTotal, density, unit) ?: return null
        val downWidth = totalDown?.let { widthForCount(it, density, unit) ?: return null }
        val upWidth = totalUp?.let { widthForCount(it, density, unit) ?: return null }
        return CastOnResult(
            stitches = closerTotal,
            actualWidth = actualWidth,
            adjustedDown = totalDown,
            adjustedUp = totalUp,
            adjustedDownWidth = downWidth,
            adjustedUpWidth = upWidth,
        )
    }

    private fun supportedCount(value: Long): Int? = value.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()

    private fun widthForCount(
        count: Int,
        density: Double,
        unit: MeasurementUnit,
    ): Double? {
        if (count == 0) return 0.0
        val millimeters = MeasurementCalculator.sizeFromCount(count.toDouble(), density) ?: return null
        return unit.fromMillimeters(millimeters).takeIf { it.isFinite() && it > 0.0 }
    }
}
