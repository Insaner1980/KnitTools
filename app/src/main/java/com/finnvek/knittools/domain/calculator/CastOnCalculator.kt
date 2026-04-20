package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.CastOnResult
import kotlin.math.roundToInt

object CastOnCalculator {
    private const val GAUGE_REFERENCE_CM = 10.0
    private const val GAUGE_REFERENCE_INCHES = 4.0

    fun calculate(
        desiredWidth: Double,
        stitchGauge: Double,
        useInches: Boolean = false,
        patternRepeat: Int? = null,
        edgeStitches: Int = 0,
    ): CastOnResult {
        val gaugeReference = if (useInches) GAUGE_REFERENCE_INCHES else GAUGE_REFERENCE_CM
        val stitchesPerUnit = stitchGauge / gaugeReference
        val rawStitches = (desiredWidth * stitchesPerUnit).roundToInt()

        if (patternRepeat == null || patternRepeat <= 0) {
            val total = rawStitches + edgeStitches
            val actualWidth = total / stitchesPerUnit
            return CastOnResult(
                stitches = total,
                actualWidth = actualWidth,
            )
        }

        // desiredWidth is already translated into the body stitch target.
        // Edge stitches are added on top after snapping the body to the nearest repeat.
        val bodyStitches = rawStitches
        val nearestDown = (bodyStitches / patternRepeat) * patternRepeat
        val nearestUp = nearestDown + patternRepeat

        val totalDown = nearestDown + edgeStitches
        val totalUp = nearestUp + edgeStitches

        val closerTotal =
            if ((bodyStitches - nearestDown) <= (nearestUp - bodyStitches)) {
                totalDown
            } else {
                totalUp
            }

        return CastOnResult(
            stitches = closerTotal,
            actualWidth = closerTotal / stitchesPerUnit,
            adjustedDown = totalDown,
            adjustedUp = totalUp,
            adjustedDownWidth = totalDown / stitchesPerUnit,
            adjustedUpWidth = totalUp / stitchesPerUnit,
        )
    }
}
