package com.finnvek.knittools.domain.model

data class MeasurementCountResult(
    val exactCount: Double,
    val roundedCount: Int,
    val roundedLengthMm: Double,
)

data class MeasurementAdjustmentResult(
    val originalLengthMm: Double,
    val unchangedLengthMm: Double,
    val exactCount: Double,
    val roundedCount: Int,
    val roundedLengthMm: Double,
    val differencePercent: Double,
)
