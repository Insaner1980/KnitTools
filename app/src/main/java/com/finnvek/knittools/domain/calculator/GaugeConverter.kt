package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.GaugeConversionResult
import kotlin.math.roundToInt

object GaugeConverter {

    fun convert(
        patternStitchGauge: Double,
        patternRowGauge: Double,
        yourStitchGauge: Double,
        yourRowGauge: Double,
        patternStitches: Int,
        patternRows: Int,
    ): GaugeConversionResult {
        val stitchRatio = if (patternStitchGauge > 0) yourStitchGauge / patternStitchGauge else 0.0
        val rowRatio = if (patternRowGauge > 0) yourRowGauge / patternRowGauge else 0.0

        val adjustedStitchesExact = patternStitches * stitchRatio
        val adjustedRowsExact = patternRows * rowRatio

        val stitchPercentDiff = if (patternStitchGauge > 0) {
            ((yourStitchGauge - patternStitchGauge) / patternStitchGauge) * 100.0
        } else {
            0.0
        }

        val rowPercentDiff = if (patternRowGauge > 0) {
            ((yourRowGauge - patternRowGauge) / patternRowGauge) * 100.0
        } else {
            0.0
        }

        return GaugeConversionResult(
            adjustedStitches = adjustedStitchesExact.roundToInt(),
            adjustedRows = adjustedRowsExact.roundToInt(),
            adjustedStitchesExact = adjustedStitchesExact,
            adjustedRowsExact = adjustedRowsExact,
            stitchPercentDifference = stitchPercentDiff,
            rowPercentDifference = rowPercentDiff,
        )
    }
}
