package com.finnvek.knittools.domain.model

data class SizeMeasurement(
    val cm: Double,
    val inches: Double,
)

data class SizeChartEntry(
    val sizeLabel: String,
    val measurements: List<SizeMeasurement>,
)
