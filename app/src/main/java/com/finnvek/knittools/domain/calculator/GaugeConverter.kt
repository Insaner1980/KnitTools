package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.GaugeConversionResult

object GaugeConverter {
    fun convert(
        patternStitchGauge: Double,
        patternRowGauge: Double,
        yourStitchGauge: Double,
        yourRowGauge: Double,
        patternStitches: Int,
        patternRows: Int,
    ): GaugeConversionResult? {
        val stitches = MeasurementCalculator.adjust(patternStitches, patternStitchGauge, yourStitchGauge) ?: return null
        val rows = MeasurementCalculator.adjust(patternRows, patternRowGauge, yourRowGauge) ?: return null
        return GaugeConversionResult(
            adjustedStitches = stitches.roundedCount,
            adjustedRows = rows.roundedCount,
            adjustedStitchesExact = stitches.exactCount,
            adjustedRowsExact = rows.exactCount,
            stitchPercentDifference = stitches.differencePercent,
            rowPercentDifference = rows.differencePercent,
        )
    }
}
