package com.finnvek.knittools.ui.screens.gauge

import com.finnvek.knittools.domain.calculator.MeasurementCalculator
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementAdjustmentResult
import com.finnvek.knittools.domain.model.MeasurementCountResult
import com.finnvek.knittools.domain.model.MeasurementUnit

enum class GaugeTask { CONVERT, MEASURE, CALCULATE, ADJUST }

enum class GaugeOperation { COUNT_FOR_SIZE, SIZE_FROM_COUNT }

enum class GaugeAxis { STITCHES, ROWS }

enum class GaugeField {
    CONVERSION,
    SWATCH_WIDTH,
    SWATCH_STITCHES,
    SWATCH_HEIGHT,
    SWATCH_ROWS,
    ACTUAL_STITCHES,
    ACTUAL_ROWS,
    PATTERN_STITCHES,
    PATTERN_ROWS,
    PATTERN_STITCH_COUNT,
    PATTERN_ROW_COUNT,
    TARGET_WIDTH,
    TARGET_HEIGHT,
    STITCH_COUNT,
    ROW_COUNT,
    ;

    val isLength: Boolean get() = this in setOf(SWATCH_WIDTH, SWATCH_HEIGHT, TARGET_WIDTH, TARGET_HEIGHT)
    val isGauge: Boolean get() = this in setOf(ACTUAL_STITCHES, ACTUAL_ROWS, PATTERN_STITCHES, PATTERN_ROWS)
    val isCount: Boolean get() = this != CONVERSION && !isLength && !isGauge
}

data class GaugeInput(
    val text: String = "",
    val canonicalValue: Double? = null,
    val error: MeasurementNumberError? = null,
    val touched: Boolean = false,
    val incomplete: Boolean = true,
)

data class GaugeUiState(
    val ready: Boolean = false,
    val task: GaugeTask = GaugeTask.ADJUST,
    val operation: GaugeOperation = GaugeOperation.COUNT_FOR_SIZE,
    val axis: GaugeAxis = GaugeAxis.STITCHES,
    val basis: GaugeBasis = GaugeBasis.PER_10_CM,
    val fromUnit: MeasurementUnit = MeasurementUnit.CM,
    val toUnit: MeasurementUnit = MeasurementUnit.INCH,
    val useSwatch: Boolean = true,
    val fields: Map<GaugeField, GaugeInput> = emptyMap(),
    val manualStitchGauge: Boolean = false,
    val manualRowGauge: Boolean = false,
    val projectName: String? = null,
    val projectUnavailable: Boolean = false,
    val isPro: Boolean = false,
) {
    val lengthUnit: MeasurementUnit
        get() = if (basis == GaugeBasis.PER_10_CM) MeasurementUnit.CM else MeasurementUnit.INCH

    fun input(field: GaugeField): GaugeInput {
        val derived =
            when {
                field == GaugeField.ACTUAL_STITCHES && !manualStitchGauge -> stitchSwatchDensity
                field == GaugeField.ACTUAL_ROWS && !manualRowGauge -> rowSwatchDensity
                else -> return fields[field] ?: GaugeInput()
            }
        val normalized = derived?.let { MeasurementCalculator.gaugeForBasis(it, basis) }
        return GaugeInput(
            text = normalized?.let { MeasurementNumberFormatter.formatEditing(it) }.orEmpty(),
            canonicalValue = derived?.takeIf { normalized != null },
            error = MeasurementNumberError.TOO_LARGE.takeIf { derived != null && normalized == null },
            incomplete = derived == null,
        )
    }

    private fun value(field: GaugeField): Double? = input(field).canonicalValue

    private fun measuredDensity(
        count: GaugeField,
        length: GaugeField,
    ): Double? {
        val countValue = fields[count]?.canonicalValue ?: return null
        val lengthValue = fields[length]?.canonicalValue ?: return null
        return MeasurementCalculator.density(countValue, lengthValue)
    }

    private fun failedSwatch(
        count: GaugeField,
        length: GaugeField,
    ): Boolean {
        if (fields[count]?.canonicalValue == null || fields[length]?.canonicalValue == null) return false
        val density = measuredDensity(count, length) ?: return true
        return MeasurementCalculator.gaugeForBasis(density, basis) == null
    }

    val stitchSwatchDensity: Double? get() = measuredDensity(GaugeField.SWATCH_STITCHES, GaugeField.SWATCH_WIDTH)
    val rowSwatchDensity: Double? get() = measuredDensity(GaugeField.SWATCH_ROWS, GaugeField.SWATCH_HEIGHT)
    val stitchDensity: Double? get() = value(GaugeField.ACTUAL_STITCHES)
    val rowDensity: Double? get() = value(GaugeField.ACTUAL_ROWS)
    val selectedDensity: Double? get() = if (axis == GaugeAxis.STITCHES) stitchDensity else rowDensity
    val selectedTarget: Double?
        get() = value(if (axis == GaugeAxis.STITCHES) GaugeField.TARGET_WIDTH else GaugeField.TARGET_HEIGHT)
    val selectedCount: Double?
        get() = value(if (axis == GaugeAxis.STITCHES) GaugeField.STITCH_COUNT else GaugeField.ROW_COUNT)

    val convertedLength: Double?
        get() =
            value(GaugeField.CONVERSION)?.let { value ->
                toUnit.fromMillimeters(value).takeIf { it.isFinite() && (it > 0.0 || value == 0.0) }
            }

    val countResult: MeasurementCountResult?
        get() {
            val density = selectedDensity ?: return null
            val target = selectedTarget ?: return null
            return MeasurementCalculator.countForSize(density, target)
        }

    val sizeResultMm: Double?
        get() {
            val density = selectedDensity ?: return null
            val count = selectedCount ?: return null
            return MeasurementCalculator.sizeFromCount(count, density)
        }

    private fun adjustment(axis: GaugeAxis): MeasurementAdjustmentResult? {
        val stitches = axis == GaugeAxis.STITCHES
        val pattern = value(if (stitches) GaugeField.PATTERN_STITCHES else GaugeField.PATTERN_ROWS) ?: return null
        val actual = (if (stitches) stitchDensity else rowDensity) ?: return null
        val count =
            value(if (stitches) GaugeField.PATTERN_STITCH_COUNT else GaugeField.PATTERN_ROW_COUNT) ?: return null
        return MeasurementCalculator.adjust(count.toInt(), pattern, actual)
    }

    val stitchAdjustment: MeasurementAdjustmentResult? get() = adjustment(GaugeAxis.STITCHES)
    val rowAdjustment: MeasurementAdjustmentResult? get() = adjustment(GaugeAxis.ROWS)

    val hasResult: Boolean
        get() =
            when (task) {
                GaugeTask.CONVERT -> convertedLength != null
                GaugeTask.MEASURE ->
                    listOfNotNull(stitchSwatchDensity, rowSwatchDensity).any {
                        MeasurementCalculator.gaugeForBasis(it, basis) !=
                            null
                    }
                GaugeTask.CALCULATE ->
                    if (operation ==
                        GaugeOperation.COUNT_FOR_SIZE
                    ) {
                        countResult != null
                    } else {
                        sizeResultMm != null
                    }
                GaugeTask.ADJUST -> stitchAdjustment != null || rowAdjustment != null
            }

    val resultError: MeasurementNumberError?
        get() {
            val failed =
                when (task) {
                    GaugeTask.CONVERT -> value(GaugeField.CONVERSION) != null && convertedLength == null
                    GaugeTask.MEASURE -> false
                    GaugeTask.CALCULATE ->
                        selectedDensity != null &&
                            when (operation) {
                                GaugeOperation.COUNT_FOR_SIZE -> selectedTarget != null && countResult == null
                                GaugeOperation.SIZE_FROM_COUNT -> selectedCount != null && sizeResultMm == null
                            }
                    GaugeTask.ADJUST -> failedAdjustment(GaugeAxis.STITCHES) || failedAdjustment(GaugeAxis.ROWS)
                }
            val swatchFailed =
                when (task) {
                    GaugeTask.CONVERT -> false
                    GaugeTask.MEASURE ->
                        failedSwatch(GaugeField.SWATCH_STITCHES, GaugeField.SWATCH_WIDTH) ||
                            failedSwatch(GaugeField.SWATCH_ROWS, GaugeField.SWATCH_HEIGHT)
                    GaugeTask.CALCULATE ->
                        if (axis == GaugeAxis.STITCHES) {
                            !manualStitchGauge && failedSwatch(GaugeField.SWATCH_STITCHES, GaugeField.SWATCH_WIDTH)
                        } else {
                            !manualRowGauge && failedSwatch(GaugeField.SWATCH_ROWS, GaugeField.SWATCH_HEIGHT)
                        }
                    GaugeTask.ADJUST ->
                        (
                            !manualStitchGauge &&
                                failedSwatch(GaugeField.SWATCH_STITCHES, GaugeField.SWATCH_WIDTH)
                        ) ||
                            (!manualRowGauge && failedSwatch(GaugeField.SWATCH_ROWS, GaugeField.SWATCH_HEIGHT))
                }
            return if (failed || swatchFailed) MeasurementNumberError.TOO_LARGE else null
        }

    private fun failedAdjustment(axis: GaugeAxis): Boolean {
        val stitches = axis == GaugeAxis.STITCHES
        return value(if (stitches) GaugeField.PATTERN_STITCHES else GaugeField.PATTERN_ROWS) != null &&
            (if (stitches) stitchDensity else rowDensity) != null &&
            value(if (stitches) GaugeField.PATTERN_STITCH_COUNT else GaugeField.PATTERN_ROW_COUNT) != null &&
            adjustment(axis) == null
    }
}

sealed interface GaugeAction {
    data class Edit(
        val field: GaugeField,
        val text: String,
    ) : GaugeAction

    data class Blur(
        val field: GaugeField,
    ) : GaugeAction

    data class Task(
        val task: GaugeTask,
    ) : GaugeAction

    data class Operation(
        val operation: GaugeOperation,
    ) : GaugeAction

    data class Axis(
        val axis: GaugeAxis,
    ) : GaugeAction

    data class Basis(
        val basis: GaugeBasis,
    ) : GaugeAction

    data class FromUnit(
        val unit: MeasurementUnit,
    ) : GaugeAction

    data class ToUnit(
        val unit: MeasurementUnit,
    ) : GaugeAction

    data class SwatchInput(
        val enabled: Boolean,
    ) : GaugeAction

    data class Paste(
        val instruction: ParsedInstruction,
    ) : GaugeAction

    data class Copy(
        val text: String,
    ) : GaugeAction
}

sealed interface GaugeUiEvent {
    data class Copy(
        val text: String,
    ) : GaugeUiEvent

    data class Message(
        val resourceId: Int,
    ) : GaugeUiEvent
}
