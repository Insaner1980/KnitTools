package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.GaugeSwatchResult

object GaugeSwatchCalculator {
    /**
     * Laskee silmukka- ja kerrosmittauksen mittatilkusta.
     * @param gaugeBase 10.0 senttimetreille, 4.0 tuumille
     */
    fun calculate(
        measuredWidth: Double,
        stitchCount: Int,
        measuredHeight: Double,
        rowCount: Int,
        gaugeBase: Double = 10.0,
    ): GaugeSwatchResult? {
        val stitchDensity = MeasurementCalculator.density(stitchCount.toDouble(), measuredWidth) ?: return null
        val rowDensity = MeasurementCalculator.density(rowCount.toDouble(), measuredHeight) ?: return null
        val stitches = MeasurementCalculator.exactCountForSize(stitchDensity, gaugeBase) ?: return null
        val rows = MeasurementCalculator.exactCountForSize(rowDensity, gaugeBase) ?: return null
        return GaugeSwatchResult(
            stitchesPerGaugeUnit = stitches,
            rowsPerGaugeUnit = rows,
        )
    }
}
