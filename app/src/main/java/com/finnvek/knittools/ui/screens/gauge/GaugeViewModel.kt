package com.finnvek.knittools.ui.screens.gauge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GaugeViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        preferencesManager: PreferencesManager,
        repository: CounterRepository,
        proManager: ProManager,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(restore())
        val state: StateFlow<GaugeUiState> = mutableState.asStateFlow()
        private val eventChannel = Channel<GaugeUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()
        private val reportedResults = mutableSetOf<GaugeTask>()

        init {
            viewModelScope.launch {
                if (!state.value.ready) {
                    val imperial = preferencesManager.preferences.first().useImperial
                    mutableState.update {
                        it.copy(
                            ready = true,
                            basis = if (imperial) GaugeBasis.PER_4_INCHES else GaugeBasis.PER_10_CM,
                            fromUnit = if (imperial) MeasurementUnit.INCH else MeasurementUnit.CM,
                            toUnit = if (imperial) MeasurementUnit.CM else MeasurementUnit.INCH,
                        )
                    }
                    save()
                }
                trackResultIfNeeded()
            }
            viewModelScope.launch {
                combine(proManager.proState, proManager.initialStateReady) { pro, ready -> pro to ready }
                    .collect { (pro, ready) -> mutableState.update { it.copy(proState = pro, proAccessReady = ready) } }
            }
            val argument = savedStateHandle.get<Any?>("projectId")
            val projectId = optionalGaugeProjectId(argument)
            if (projectId == null) {
                mutableState.update { it.copy(projectUnavailable = argument != null) }
            } else {
                viewModelScope.launch {
                    repository.observeProject(projectId).collect { project ->
                        mutableState.update {
                            it.copy(
                                projectName = project?.name,
                                projectUnavailable = project == null,
                            )
                        }
                    }
                }
            }
        }

        fun onAction(
            action: GaugeAction,
            locale: Locale = Locale.getDefault(),
        ): Boolean {
            if (!state.value.ready) return false
            when (action) {
                is GaugeAction.Edit -> edit(action.field, action.text, locale)
                is GaugeAction.Blur -> blur(action.field)
                is GaugeAction.Task -> mutableState.update { it.copy(task = action.task) }
                is GaugeAction.Operation -> mutableState.update { it.copy(operation = action.operation) }
                is GaugeAction.Axis -> mutableState.update { it.copy(axis = action.axis) }
                is GaugeAction.Basis -> switchBasis(action.basis)
                is GaugeAction.FromUnit -> switchFromUnit(action.unit)
                is GaugeAction.ToUnit -> mutableState.update { it.copy(toUnit = action.unit) }
                is GaugeAction.SwatchInput -> switchSwatchInput(action.enabled)
                is GaugeAction.Paste -> if (!paste(action.instruction)) return false
                is GaugeAction.Copy -> copyResult(action)
            }
            trackResultIfNeeded()
            save()
            return true
        }

        private fun switchSwatchInput(enabled: Boolean) {
            mutableState.update {
                it.copy(
                    useSwatch = enabled,
                    manualStitchGauge = !enabled && it.manualStitchGauge,
                    manualRowGauge = !enabled && it.manualRowGauge,
                )
            }
        }

        private fun trackResultIfNeeded() {
            if (state.value.hasResult && reportedResults.add(state.value.task)) {
                analytics.track(UsageEvent.CALCULATOR_RESULT_SHOWN, toolDetail(state.value.task))
            }
        }

        private fun copyResult(action: GaugeAction.Copy) {
            val current = state.value
            if (action.resultState == current && current.hasResult && action.text.isNotBlank()) {
                eventChannel.trySend(GaugeUiEvent.Copy(action.text, current.task))
            }
        }

        fun resultCopied(task: GaugeTask) {
            analytics.track(UsageEvent.CALCULATOR_RESULT_COPIED, toolDetail(task))
        }

        private fun edit(
            field: GaugeField,
            text: String,
            locale: Locale,
        ) {
            val input = fieldInput(field, text, locale)
            mutableState.update {
                it.copy(
                    fields = it.fields + (field to input),
                    manualStitchGauge = it.manualStitchGauge || field == GaugeField.ACTUAL_STITCHES,
                    manualRowGauge = it.manualRowGauge || field == GaugeField.ACTUAL_ROWS,
                )
            }
        }

        private fun fieldInput(
            field: GaugeField,
            text: String,
            locale: Locale,
        ): GaugeInput {
            val parsed =
                MeasurementNumberParser.parse(
                    text,
                    locale,
                    integer = field.isCount,
                    allowZero = field == GaugeField.CONVERSION,
                )
            val current = state.value
            val canonical =
                parsed.value?.let {
                    when {
                        field == GaugeField.CONVERSION -> current.fromUnit.toMillimeters(it)
                        field.isLength -> current.lengthUnit.toMillimeters(it)
                        field.isGauge -> it / current.basis.lengthMm
                        else -> it
                    }
                }
            val rangeError =
                canonical != null && (!canonical.isFinite() || (canonical <= 0 && field != GaugeField.CONVERSION))
            return GaugeInput(
                text = text,
                canonicalValue = canonical?.takeUnless { rangeError },
                error = if (rangeError) MeasurementNumberError.TOO_LARGE else parsed.error,
                touched = current.fields[field]?.touched ?: false,
                incomplete = parsed.incomplete,
            )
        }

        private fun blur(field: GaugeField) {
            val input = state.value.input(field)
            val error =
                input.error
                    ?: MeasurementNumberError.INVALID_NUMBER.takeIf { input.text.isNotBlank() && input.incomplete }
            mutableState.update { it.copy(fields = it.fields + (field to input.copy(touched = true, error = error))) }
        }

        private fun switchBasis(basis: GaugeBasis) {
            if (basis == state.value.basis) return
            val fields =
                GaugeField.entries.filter {
                    (it.isLength || it.isGauge) &&
                        (it != GaugeField.ACTUAL_STITCHES || state.value.manualStitchGauge) &&
                        (it != GaugeField.ACTUAL_ROWS || state.value.manualRowGauge)
                }
            if (!canConvert(fields)) return
            val conversionFailed =
                fields.any { field ->
                    state.value.fields[field]?.canonicalValue?.let {
                        val converted = inputValueForBasis(field, it, basis)
                        !converted.isFinite() || converted <= 0.0
                    } == true
                }
            if (conversionFailed) {
                eventChannel.trySend(GaugeUiEvent.Message(R.string.measurement_too_large))
                return
            }
            mutableState.update { current ->
                current.copy(
                    basis = basis,
                    fields =
                        current.fields.mapValues { (field, input) ->
                            val canonical = input.canonicalValue
                            if (canonical == null || field !in fields) {
                                input
                            } else {
                                val value = inputValueForBasis(field, canonical, basis)
                                input.copy(text = MeasurementNumberFormatter.formatEditing(value))
                            }
                        },
                )
            }
        }

        private fun inputValueForBasis(
            field: GaugeField,
            canonical: Double,
            basis: GaugeBasis,
        ): Double {
            if (field.isGauge) return canonical * basis.lengthMm
            val unit = if (basis == GaugeBasis.PER_10_CM) MeasurementUnit.CM else MeasurementUnit.INCH
            return unit.fromMillimeters(canonical)
        }

        private fun switchFromUnit(unit: MeasurementUnit) {
            if (unit == state.value.fromUnit || !canConvert(listOf(GaugeField.CONVERSION))) return
            val input = state.value.input(GaugeField.CONVERSION)
            val converted = input.canonicalValue?.let { unit.fromMillimeters(it) }
            if (converted != null && (!converted.isFinite() || (converted == 0.0 && input.canonicalValue != 0.0))) {
                eventChannel.trySend(GaugeUiEvent.Message(R.string.measurement_too_large))
                return
            }
            val convertedText = converted?.let { MeasurementNumberFormatter.formatEditing(it) } ?: input.text
            mutableState.update { current ->
                current.copy(
                    fromUnit = unit,
                    fields = current.fields + (GaugeField.CONVERSION to input.copy(text = convertedText)),
                )
            }
        }

        private fun canConvert(fields: List<GaugeField>): Boolean {
            val invalid =
                fields.filter { field ->
                    val input = state.value.fields[field] ?: GaugeInput()
                    input.text.isNotBlank() && input.canonicalValue == null
                }
            invalid.forEach(::blur)
            if (invalid.isNotEmpty()) eventChannel.trySend(GaugeUiEvent.Message(R.string.measurement_complete_number))
            return invalid.isEmpty()
        }

        private fun paste(instruction: ParsedInstruction): Boolean {
            val current = state.value
            if (current.task != GaugeTask.ADJUST || !current.proState.hasFeature(ProFeature.INSTRUCTION_PASTE)) {
                return false
            }
            return when (instruction) {
                is ParsedInstruction.Gauge -> pasteGauge(instruction)
                is ParsedInstruction.GaugeSwatch -> pasteSwatch(instruction)
                else -> false
            }
        }

        private fun pasteGauge(instruction: ParsedInstruction.Gauge): Boolean {
            val current = state.value
            val basisMm =
                if (instruction.unit == ParsedInstruction.GaugeUnit.PER_10_CM) {
                    GaugeBasis.PER_10_CM.lengthMm
                } else {
                    GaugeBasis.PER_4_INCHES.lengthMm
                }
            val stitchInput =
                pasteInput(GaugeField.ACTUAL_STITCHES, instruction.stitchesPer10cm / basisMm * current.basis.lengthMm)
            val rowInput =
                pasteInput(GaugeField.ACTUAL_ROWS, instruction.rowsPer10cm / basisMm * current.basis.lengthMm)
            if (stitchInput.canonicalValue == null || rowInput.canonicalValue == null) return false
            mutableState.update {
                it.copy(
                    fields =
                        it.fields +
                            mapOf(
                                GaugeField.ACTUAL_STITCHES to stitchInput,
                                GaugeField.ACTUAL_ROWS to rowInput,
                            ),
                    useSwatch = false,
                    manualStitchGauge = true,
                    manualRowGauge = true,
                )
            }
            return true
        }

        private fun pasteSwatch(instruction: ParsedInstruction.GaugeSwatch): Boolean {
            val current = state.value
            val width = instruction.width
            val stitches = instruction.stitches
            val height = instruction.height
            val rows = instruction.rows
            val hasStitchAxis = width != null && stitches != null
            val hasRowAxis = height != null && rows != null
            if (!hasStitchAxis && !hasRowAxis) return false
            val sourceUnit = swatchSourceUnit(instruction.lengthUnit, current.lengthUnit)
            val inputs = mutableMapOf<GaugeField, GaugeInput>()
            if (hasStitchAxis) {
                val displayWidth = current.lengthUnit.fromMillimeters(sourceUnit.toMillimeters(width))
                inputs[GaugeField.SWATCH_WIDTH] = pasteInput(GaugeField.SWATCH_WIDTH, displayWidth)
                inputs[GaugeField.SWATCH_STITCHES] =
                    fieldInput(GaugeField.SWATCH_STITCHES, stitches.toString(), Locale.US)
            }
            if (hasRowAxis) {
                val displayHeight = current.lengthUnit.fromMillimeters(sourceUnit.toMillimeters(height))
                inputs[GaugeField.SWATCH_HEIGHT] = pasteInput(GaugeField.SWATCH_HEIGHT, displayHeight)
                inputs[GaugeField.SWATCH_ROWS] = fieldInput(GaugeField.SWATCH_ROWS, rows.toString(), Locale.US)
            }
            if (inputs.values.any { it.canonicalValue == null }) return false
            val updated =
                current.copy(
                    fields = current.fields + inputs,
                    useSwatch = true,
                    manualStitchGauge = current.manualStitchGauge && !hasStitchAxis,
                    manualRowGauge = current.manualRowGauge && !hasRowAxis,
                )
            if ((hasStitchAxis && updated.stitchDensity == null) ||
                (hasRowAxis && updated.rowDensity == null)
            ) {
                return false
            }
            mutableState.value = updated
            return true
        }

        private fun swatchSourceUnit(
            unit: ParsedInstruction.LengthUnit?,
            fallback: MeasurementUnit,
        ): MeasurementUnit =
            when (unit) {
                ParsedInstruction.LengthUnit.CM -> MeasurementUnit.CM
                ParsedInstruction.LengthUnit.INCHES -> MeasurementUnit.INCH
                null -> fallback
            }

        private fun pasteInput(
            field: GaugeField,
            value: Double,
        ): GaugeInput =
            fieldInput(
                field,
                if (value.isFinite()) MeasurementNumberFormatter.formatEditing(value) else value.toString(),
                Locale.US,
            )

        private fun save() {
            val current = state.value
            savedStateHandle["measurement.ready"] = current.ready
            savedStateHandle["measurement.task"] = current.task.name
            savedStateHandle["measurement.operation"] = current.operation.name
            savedStateHandle["measurement.axis"] = current.axis.name
            savedStateHandle["measurement.basis"] = current.basis.name
            savedStateHandle["measurement.from"] = current.fromUnit.name
            savedStateHandle["measurement.to"] = current.toUnit.name
            savedStateHandle["measurement.swatch"] = current.useSwatch
            savedStateHandle["measurement.manualStitches"] = current.manualStitchGauge
            savedStateHandle["measurement.manualRows"] = current.manualRowGauge
            current.fields.forEach { (field, input) ->
                val key = "measurement.${field.name}"
                savedStateHandle["$key.text"] = input.text
                savedStateHandle["$key.value"] = input.canonicalValue
                savedStateHandle["$key.error"] = input.error?.name
                savedStateHandle["$key.touched"] = input.touched
                savedStateHandle["$key.incomplete"] = input.incomplete
            }
        }

        private fun restore(): GaugeUiState =
            GaugeUiState(
                ready = savedStateHandle["measurement.ready"] ?: false,
                task = restoredEnum("task", GaugeTask.ADJUST),
                operation = restoredEnum("operation", GaugeOperation.COUNT_FOR_SIZE),
                axis = restoredEnum("axis", GaugeAxis.STITCHES),
                basis = restoredEnum("basis", GaugeBasis.PER_10_CM),
                fromUnit = restoredEnum("from", MeasurementUnit.CM),
                toUnit = restoredEnum("to", MeasurementUnit.INCH),
                useSwatch = savedStateHandle["measurement.swatch"] ?: true,
                manualStitchGauge = savedStateHandle["measurement.manualStitches"] ?: false,
                manualRowGauge = savedStateHandle["measurement.manualRows"] ?: false,
                fields =
                    GaugeField.entries
                        .mapNotNull { field ->
                            val key = "measurement.${field.name}"
                            savedStateHandle.get<String>("$key.text")?.let { text ->
                                field to
                                    GaugeInput(
                                        text = text,
                                        canonicalValue = savedStateHandle["$key.value"],
                                        error =
                                            MeasurementNumberError.entries.firstOrNull {
                                                it.name ==
                                                    savedStateHandle.get<String>("$key.error")
                                            },
                                        touched = savedStateHandle["$key.touched"] ?: false,
                                        incomplete = savedStateHandle["$key.incomplete"] ?: false,
                                    )
                            }
                        }.toMap(),
            )

        private inline fun <reified T : Enum<T>> restoredEnum(
            key: String,
            default: T,
        ): T = enumValues<T>().firstOrNull { it.name == savedStateHandle.get<String>("measurement.$key") } ?: default
    }

internal fun optionalGaugeProjectId(value: Any?): Long? =
    when (value) {
        is Long -> value.takeIf { it > 0 }
        is Int -> value.toLong().takeIf { it > 0 }
        is String -> value.toLongOrNull()?.takeIf { it > 0 }
        else -> null
    }

private fun toolDetail(task: GaugeTask): UsageDetail =
    when (task) {
        GaugeTask.CONVERT -> UsageDetail.TOOL_CONVERT
        GaugeTask.MEASURE -> UsageDetail.TOOL_MEASURE
        GaugeTask.CALCULATE -> UsageDetail.TOOL_CALCULATE
        GaugeTask.ADJUST -> UsageDetail.TOOL_ADJUST
    }
