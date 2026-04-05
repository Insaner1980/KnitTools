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
        if (measuredWidth <= 0 || measuredHeight <= 0 || stitchCount <= 0 || rowCount <= 0) return null
        return GaugeSwatchResult(
            stitchesPerGaugeUnit = (stitchCount.toDouble() / measuredWidth) * gaugeBase,
            rowsPerGaugeUnit = (rowCount.toDouble() / measuredHeight) * gaugeBase,
        )
    }
}
