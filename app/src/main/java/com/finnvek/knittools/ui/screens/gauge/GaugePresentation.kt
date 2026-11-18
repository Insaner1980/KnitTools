package com.finnvek.knittools.ui.screens.gauge

import android.content.Context
import androidx.annotation.StringRes
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementCalculator
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementAdjustmentResult
import com.finnvek.knittools.domain.model.MeasurementUnit
import java.util.Locale

internal data class GaugeResultLine(
    val id: String,
    val label: String,
    val value: String,
    val spokenValue: String = value,
)

internal data class GaugeResultSection(
    val id: String,
    val title: String,
    val inputs: List<GaugeResultLine>,
    val results: List<GaugeResultLine>,
)

internal data class GaugePresentation(
    val sections: List<GaugeResultSection>,
    val copyText: String,
)

internal fun gaugePresentation(
    state: GaugeUiState,
    context: Context,
    locale: Locale,
): GaugePresentation {
    val sections = GaugeResultPresenter(state, context, locale).sections()
    val lines =
        buildList {
            if (sections.isEmpty()) return@buildList
            add(context.getString(state.task.titleResource()))
            state.projectName?.let { add(context.getString(R.string.measurement_project_context, it)) }
            sections.forEach { section ->
                add("")
                add(section.title)
                (section.inputs + section.results).forEach { line ->
                    add(context.getString(R.string.measurement_result_line, line.label, line.value))
                }
            }
            if (state.task != GaugeTask.CONVERT) {
                add("")
                add(context.getString(state.task.warningResource()))
            }
        }
    return GaugePresentation(sections, lines.joinToString("\n"))
}

private class GaugeResultPresenter(
    private val state: GaugeUiState,
    private val context: Context,
    private val locale: Locale,
) {
    fun sections(): List<GaugeResultSection> =
        when (state.task) {
            GaugeTask.CONVERT -> listOfNotNull(conversion())
            GaugeTask.MEASURE -> GaugeAxis.entries.mapNotNull(::measured)
            GaugeTask.CALCULATE -> listOfNotNull(calculated())
            GaugeTask.ADJUST -> GaugeAxis.entries.mapNotNull(::adjusted)
        }

    private fun conversion(): GaugeResultSection? {
        val source = state.input(GaugeField.CONVERSION).canonicalValue ?: return null
        val converted = state.convertedLength ?: return null
        return GaugeResultSection(
            id = "conversion",
            title = text(R.string.measurement_result),
            inputs = listOf(length("input", R.string.measurement_value, source, state.fromUnit)),
            results =
                listOf(
                    valueWithUnit(
                        "converted",
                        R.string.measurement_result,
                        converted,
                        state.toUnit,
                    ),
                ),
        )
    }

    private fun measured(axis: GaugeAxis): GaugeResultSection? {
        val stitches = axis == GaugeAxis.STITCHES
        val density = (if (stitches) state.stitchSwatchDensity else state.rowSwatchDensity) ?: return null
        val count =
            state.input(if (stitches) GaugeField.SWATCH_STITCHES else GaugeField.SWATCH_ROWS).canonicalValue
                ?: return null
        val size =
            state.input(if (stitches) GaugeField.SWATCH_WIDTH else GaugeField.SWATCH_HEIGHT).canonicalValue
                ?: return null
        val gauge = gauge("gauge", axis.actualGaugeResource(), density) ?: return null
        return GaugeResultSection(
            id = axis.resultId(),
            title = text(axis.titleResource()),
            inputs =
                listOf(
                    wholeCount("measured_count", axis.countResource(), count.toInt(), axis),
                    length("measured_size", if (stitches) R.string.measured_width else R.string.measured_height, size),
                ),
            results = listOf(gauge),
        )
    }

    private fun calculated(): GaugeResultSection? {
        val density = state.selectedDensity ?: return null
        val gauge = gauge("gauge", state.axis.actualGaugeResource(), density) ?: return null
        return if (state.operation == GaugeOperation.COUNT_FOR_SIZE) {
            val target = state.selectedTarget
            val countResult = state.countResult
            if (target == null || countResult == null) {
                null
            } else {
                GaugeResultSection(
                    id = state.axis.resultId(),
                    title = text(state.axis.titleResource()),
                    inputs = listOf(gauge, length("target", state.axis.targetResource(), target)),
                    results =
                        countLines(
                            countResult.exactCount,
                            countResult.roundedCount,
                            countResult.roundedLengthMm,
                            state.axis,
                        ),
                )
            }
        } else {
            val count = state.selectedCount
            val size = state.sizeResultMm
            if (count == null || size == null) {
                null
            } else {
                GaugeResultSection(
                    id = state.axis.resultId(),
                    title = text(state.axis.titleResource()),
                    inputs = listOf(wholeCount("count", state.axis.countResource(), count.toInt(), state.axis), gauge),
                    results = listOf(length("result_size", R.string.measurement_result_size, size)),
                )
            }
        }
    }

    private fun adjusted(axis: GaugeAxis): GaugeResultSection? {
        val stitches = axis == GaugeAxis.STITCHES
        val result = (if (stitches) state.stitchAdjustment else state.rowAdjustment) ?: return null
        val actual = if (stitches) state.stitchDensity else state.rowDensity
        val pattern =
            state.input(if (stitches) GaugeField.PATTERN_STITCHES else GaugeField.PATTERN_ROWS).canonicalValue
        val count =
            state.input(if (stitches) GaugeField.PATTERN_STITCH_COUNT else GaugeField.PATTERN_ROW_COUNT).canonicalValue
        if (actual == null || pattern == null || count == null) return null
        val actualGauge = gauge("actual_gauge", axis.actualGaugeResource(), actual)
        val patternGauge = gauge("pattern_gauge", axis.patternGaugeResource(), pattern)
        if (actualGauge == null || patternGauge == null) return null
        return GaugeResultSection(
            id = axis.resultId(),
            title = text(axis.titleResource()),
            inputs =
                listOf(
                    wholeCount("pattern_count", axis.patternCountResource(), count.toInt(), axis),
                    patternGauge,
                    actualGauge,
                ),
            results = adjustmentLines(result, axis),
        )
    }

    private fun adjustmentLines(
        result: MeasurementAdjustmentResult,
        axis: GaugeAxis,
    ): List<GaugeResultLine> =
        listOf(
            length("original_size", R.string.measurement_original_size, result.originalLengthMm),
            length("unchanged_size", R.string.measurement_unchanged_size, result.unchangedLengthMm),
        ) +
            countLines(result.exactCount, result.roundedCount, result.roundedLengthMm, axis) +
            GaugeResultLine(
                id = "difference",
                label = text(R.string.measurement_gauge_difference),
                value = MeasurementNumberFormatter.percent(result.differencePercent, locale),
            )

    private fun countLines(
        exactCount: Double,
        roundedCount: Int,
        roundedSize: Double,
        axis: GaugeAxis,
    ): List<GaugeResultLine> =
        listOf(
            GaugeResultLine(
                "calculated_count",
                text(R.string.measurement_calculated_count),
                MeasurementNumberFormatter.format(exactCount, locale),
            ),
            wholeCount("nearest_count", R.string.measurement_nearest_count, roundedCount, axis),
            length("rounded_size", R.string.measurement_rounded_size, roundedSize),
        )

    private fun wholeCount(
        id: String,
        @StringRes label: Int,
        count: Int,
        axis: GaugeAxis,
    ): GaugeResultLine =
        GaugeResultLine(
            id = id,
            label = text(label),
            value =
                context.resources.getQuantityString(
                    if (axis == GaugeAxis.STITCHES) R.plurals.measurement_stitches else R.plurals.measurement_rows,
                    count,
                    count,
                ),
        )

    private fun gauge(
        id: String,
        @StringRes label: Int,
        density: Double,
    ): GaugeResultLine? {
        val value = MeasurementCalculator.gaugeForBasis(density, state.basis) ?: return null
        return GaugeResultLine(
            id = id,
            label = text(label),
            value =
                context.getString(
                    R.string.measurement_gauge_format,
                    MeasurementNumberFormatter.format(value, locale),
                    text(state.basis.titleResource()),
                ),
        )
    }

    private fun length(
        id: String,
        @StringRes label: Int,
        millimeters: Double,
        unit: MeasurementUnit = state.lengthUnit,
    ): GaugeResultLine = valueWithUnit(id, label, unit.fromMillimeters(millimeters), unit)

    private fun valueWithUnit(
        id: String,
        @StringRes label: Int,
        value: Double,
        unit: MeasurementUnit,
    ): GaugeResultLine {
        val number = MeasurementNumberFormatter.format(value, locale)
        return GaugeResultLine(
            id = id,
            label = text(label),
            value = context.getString(R.string.measurement_value_unit_format, number, text(unit.shortResource())),
            spokenValue = context.getString(R.string.measurement_result_line, text(unit.titleResource()), number),
        )
    }

    private fun text(
        @StringRes resource: Int,
    ): String = context.getString(resource)
}

@StringRes
internal fun GaugeTask.titleResource(): Int =
    when (this) {
        GaugeTask.CONVERT -> R.string.measurement_convert
        GaugeTask.MEASURE -> R.string.measurement_measure_gauge
        GaugeTask.CALCULATE -> R.string.measurement_calculate
        GaugeTask.ADJUST -> R.string.measurement_adjust
    }

@StringRes
internal fun GaugeTask.warningResource(): Int =
    when (this) {
        GaugeTask.ADJUST -> R.string.measurement_adjust_warning
        GaugeTask.MEASURE -> R.string.measurement_swatch_hint
        else -> R.string.measurement_estimate
    }

@StringRes
internal fun GaugeBasis.titleResource(): Int =
    if (this == GaugeBasis.PER_10_CM) R.string.measurement_per_10cm else R.string.measurement_per_4in

@StringRes
internal fun MeasurementUnit.titleResource(): Int =
    when (this) {
        MeasurementUnit.CM -> R.string.measurement_unit_cm
        MeasurementUnit.INCH -> R.string.measurement_unit_inch
        MeasurementUnit.METER -> R.string.measurement_unit_meter
        MeasurementUnit.YARD -> R.string.measurement_unit_yard
    }

@StringRes
internal fun MeasurementUnit.shortResource(): Int =
    when (this) {
        MeasurementUnit.CM -> R.string.unit_cm
        MeasurementUnit.INCH -> R.string.measurement_unit_inch_short
        MeasurementUnit.METER -> R.string.measurement_unit_meter_short
        MeasurementUnit.YARD -> R.string.measurement_unit_yard_short
    }

@StringRes
internal fun MeasurementNumberError.messageResource(): Int =
    when (this) {
        MeasurementNumberError.INVALID_NUMBER -> R.string.measurement_invalid_number
        MeasurementNumberError.MUST_BE_POSITIVE -> R.string.measurement_positive_required
        MeasurementNumberError.TOO_LARGE -> R.string.measurement_too_large
    }

@StringRes
internal fun GaugeAxis.titleResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.measurement_stitches_width else R.string.measurement_rows_height

@StringRes
private fun GaugeAxis.actualGaugeResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.measurement_actual_stitches else R.string.measurement_actual_rows

@StringRes
private fun GaugeAxis.patternGaugeResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.measurement_pattern_stitches else R.string.measurement_pattern_rows

@StringRes
private fun GaugeAxis.patternCountResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.stitches_in_pattern else R.string.rows_in_pattern

@StringRes
private fun GaugeAxis.countResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.measurement_stitch_count else R.string.measurement_row_count

@StringRes
private fun GaugeAxis.targetResource(): Int =
    if (this == GaugeAxis.STITCHES) R.string.measurement_target_width else R.string.measurement_target_height

private fun GaugeAxis.resultId(): String = if (this == GaugeAxis.STITCHES) "stitches" else "rows"
