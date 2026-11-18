package com.finnvek.knittools.domain.calculator

import java.text.DecimalFormatSymbols
import java.util.Locale

enum class MeasurementNumberError {
    INVALID_NUMBER,
    MUST_BE_POSITIVE,
    TOO_LARGE,
}

data class MeasurementNumberResult(
    val value: Double? = null,
    val error: MeasurementNumberError? = null,
    val incomplete: Boolean = false,
)

object MeasurementNumberParser {
    private val integerPattern = Regex("[0-9]+")
    private val decimalPattern = Regex("[0-9]*(\\.[0-9]*)?")

    fun parse(
        text: String,
        locale: Locale,
        integer: Boolean = false,
        allowZero: Boolean = false,
        allowNegative: Boolean = false,
    ): MeasurementNumberResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return MeasurementNumberResult(incomplete = true)
        val negative = trimmed.startsWith('-')
        val magnitude = if (negative) trimmed.drop(1) else trimmed
        if (magnitude.isEmpty() && allowNegative) return MeasurementNumberResult(incomplete = true)
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val normalized = magnitude.replace(separator, '.')
        val pattern = if (integer) integerPattern else decimalPattern
        return when {
            !pattern.matches(normalized) -> MeasurementNumberResult(error = MeasurementNumberError.INVALID_NUMBER)
            negative && !allowNegative -> MeasurementNumberResult(error = MeasurementNumberError.MUST_BE_POSITIVE)
            normalized.isEmpty() || normalized.endsWith('.') -> MeasurementNumberResult(incomplete = true)
            else -> validateValue(normalized, negative, integer, allowZero)
        }
    }

    private fun validateValue(
        normalized: String,
        negative: Boolean,
        integer: Boolean,
        allowZero: Boolean,
    ): MeasurementNumberResult {
        val signed = if (negative) "-$normalized" else normalized
        val value =
            signed.toDoubleOrNull() ?: return MeasurementNumberResult(error = MeasurementNumberError.INVALID_NUMBER)
        return when {
            value == 0.0 && normalized.any { it in '1'..'9' } ->
                MeasurementNumberResult(error = MeasurementNumberError.INVALID_NUMBER)
            !value.isFinite() -> MeasurementNumberResult(error = MeasurementNumberError.TOO_LARGE)
            value == 0.0 && !allowZero -> MeasurementNumberResult(error = MeasurementNumberError.MUST_BE_POSITIVE)
            integer && (value < Int.MIN_VALUE.toDouble() || value > Int.MAX_VALUE.toDouble()) ->
                MeasurementNumberResult(error = MeasurementNumberError.TOO_LARGE)
            else -> MeasurementNumberResult(value = value)
        }
    }
}
