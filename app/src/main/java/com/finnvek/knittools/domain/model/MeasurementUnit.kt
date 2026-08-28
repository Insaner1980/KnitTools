package com.finnvek.knittools.domain.model

enum class MeasurementUnit(
    private val millimetersPerUnit: Double,
) {
    CM(10.0),
    INCH(25.4),
    METER(1000.0),
    YARD(914.4),
    ;

    fun toMillimeters(value: Double): Double = value * millimetersPerUnit

    fun fromMillimeters(value: Double): Double = value / millimetersPerUnit
}

enum class GaugeBasis(
    val lengthMm: Double,
) {
    PER_10_CM(MeasurementUnit.CM.toMillimeters(10.0)),
    PER_4_INCHES(MeasurementUnit.INCH.toMillimeters(4.0)),
}
