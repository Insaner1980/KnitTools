package com.finnvek.knittools.domain.model

data class GaugeConversionResult(
    val adjustedStitches: Int,
    val adjustedRows: Int,
    val adjustedStitchesExact: Double,
    val adjustedRowsExact: Double,
    val stitchPercentDifference: Double,
    val rowPercentDifference: Double,
)
