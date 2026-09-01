package com.finnvek.knittools.ui.screens.gauge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(restore())
        val state: StateFlow<GaugeUiState> = mutableState.asStateFlow()
        private val eventChannel = Channel<GaugeUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

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
            }
            viewModelScope.launch {
                proManager.proState.collect { pro -> mutableState.update { it.copy(isPro = pro.isPro) } }
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
        ) {
            if (!state.value.ready) return
            when (action) {
                is GaugeAction.Edit -> edit(action.field, action.text, locale)
                is GaugeAction.Blur -> blur(action.field)
                is GaugeAction.Task -> mutableState.update { it.copy(task = action.task) }
                is GaugeAction.Operation -> mutableState.update { it.copy(operation = action.operation) }
                is GaugeAction.Axis -> mutableState.update { it.copy(axis = action.axis) }
                is GaugeAction.Basis -> switchBasis(action.basis)
                is GaugeAction.FromUnit -> switchFromUnit(action.unit)
                is GaugeAction.ToUnit -> mutableState.update { it.copy(toUnit = action.unit) }
                is GaugeAction.SwatchInput ->
                    mutableState.update {
                        it.copy(
                            useSwatch = action.enabled,
                            manualStitchGauge = !action.enabled && it.manualStitchGauge,
                            manualRowGauge = !action.enabled && it.manualRowGauge,
                        )
                    }
                is GaugeAction.Paste -> paste(action.instruction)
                is GaugeAction.Copy -> copyResult(action.text)
            }
            save()
        }

        private fun copyResult(text: String) {
            if (state.value.hasResult && text.isNotBlank()) {
                eventChannel.trySend(GaugeUiEvent.Copy(text))
            }
        }

        private fun edit(
            field: GaugeField,
            text: String,
            locale: Locale,
        ) {
            val parsed =
                MeasurementNumberParser.parse(
                    text,
                    locale,
                    integer = field.isCount,
                    allowZero =
                        field == GaugeField.CONVERSION,
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
            val input =
                GaugeInput(
                    text = text,
                    canonicalValue = canonical?.takeUnless { rangeError },
                    error = if (rangeError) MeasurementNumberError.TOO_LARGE else parsed.error,
                    touched = current.fields[field]?.touched ?: false,
                    incomplete = parsed.incomplete,
                )
            mutableState.update {
                it.copy(
                    fields = it.fields + (field to input),
                    manualStitchGauge = it.manualStitchGauge || field == GaugeField.ACTUAL_STITCHES,
                    manualRowGauge = it.manualRowGauge || field == GaugeField.ACTUAL_ROWS,
                )
            }
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
            mutableState.update { current ->
                current.copy(
                    fromUnit = unit,
                    fields =
                        current.fields + (
                            GaugeField.CONVERSION to
                                input.copy(
                                    text =
                                        converted?.let {
                                            MeasurementNumberFormatter.formatEditing(
                                                it,
                                            )
                                        } ?: input.text,
                                )
                        ),
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

        private fun paste(instruction: ParsedInstruction) {
            if (!state.value.isPro) return
            when (instruction) {
                is ParsedInstruction.Gauge -> {
                    val basisMm =
                        if (instruction.unit ==
                            ParsedInstruction.GaugeUnit.PER_10_CM
                        ) {
                            GaugeBasis.PER_10_CM.lengthMm
                        } else {
                            GaugeBasis.PER_4_INCHES.lengthMm
                        }
                    pasteValue(
                        GaugeField.ACTUAL_STITCHES,
                        instruction.stitchesPer10cm / basisMm * state.value.basis.lengthMm,
                    )
                    pasteValue(GaugeField.ACTUAL_ROWS, instruction.rowsPer10cm / basisMm * state.value.basis.lengthMm)
                    mutableState.update { it.copy(useSwatch = false) }
                }
                is ParsedInstruction.GaugeSwatch -> pasteSwatch(instruction)
                else -> Unit
            }
        }

        private fun pasteSwatch(instruction: ParsedInstruction.GaugeSwatch) {
            val sourceUnit =
                when (instruction.lengthUnit) {
                    ParsedInstruction.LengthUnit.CM -> MeasurementUnit.CM
                    ParsedInstruction.LengthUnit.INCHES -> MeasurementUnit.INCH
                    null -> state.value.lengthUnit
                }
            listOf(
                GaugeField.SWATCH_WIDTH to instruction.width,
                GaugeField.SWATCH_HEIGHT to instruction.height,
            ).forEach { (field, value) ->
                value?.let { pasteValue(field, state.value.lengthUnit.fromMillimeters(sourceUnit.toMillimeters(it))) }
            }
            listOf(
                GaugeField.SWATCH_STITCHES to instruction.stitches,
                GaugeField.SWATCH_ROWS to instruction.rows,
            ).forEach { (field, value) ->
                value?.let { edit(field, it.toString(), Locale.US) }
            }
            mutableState.update {
                it.copy(
                    useSwatch = true,
                    manualStitchGauge =
                        if (instruction.width != null ||
                            instruction.stitches != null
                        ) {
                            false
                        } else {
                            it.manualStitchGauge
                        },
                    manualRowGauge =
                        if (instruction.height != null ||
                            instruction.rows != null
                        ) {
                            false
                        } else {
                            it.manualRowGauge
                        },
                )
            }
        }

        private fun pasteValue(
            field: GaugeField,
            value: Double,
        ) {
            edit(
                field,
                if (value.isFinite()) MeasurementNumberFormatter.formatEditing(value) else value.toString(),
                Locale.US,
            )
        }

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
