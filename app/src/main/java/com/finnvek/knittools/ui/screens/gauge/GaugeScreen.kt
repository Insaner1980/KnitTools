package com.finnvek.knittools.ui.screens.gauge

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.PasteInstructionButton
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.rememberCurrentLocale

@Composable
fun GaugeScreen(
    onBack: () -> Unit,
    viewModelProvider: @Composable () -> GaugeViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = rememberCurrentLocale()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    CollectWithLifecycleEffect({ viewModel.events }) { event ->
        when (event) {
            is GaugeUiEvent.Copy -> {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                    ClipData.newPlainText(resources.getString(R.string.measurement_title), event.text),
                )
                snackbarHostState.showSnackbar(resources.getString(R.string.measurement_copied))
            }
            is GaugeUiEvent.Message -> snackbarHostState.showSnackbar(resources.getString(event.resourceId))
        }
    }
    GaugeContent(
        state = state,
        onAction = { viewModel.onAction(it, locale) },
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

@Composable
fun GaugeContent(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = rememberCurrentLocale()
    val presentation = remember(state, context, locale) { gaugePresentation(state, context, locale) }
    ToolScreenScaffold(
        title = stringResource(R.string.measurement_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = snackbarHost,
        wrapTitle = true,
    ) { padding ->
        if (!state.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .testTag("measurement_form"),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ProjectContext(state)
                GaugeSelector(
                    label = stringResource(R.string.measurement_task),
                    selectedValue = state.task,
                    choices = GaugeTask.entries.map { it to stringResource(it.titleResource()) },
                    onSelect = { onAction(GaugeAction.Task(it)) },
                    tag = "measurement_task",
                )
                if (state.task == GaugeTask.CONVERT) {
                    ConversionFields(state, onAction)
                } else {
                    GaugeBasisSelector(state, onAction)
                    when (state.task) {
                        GaugeTask.MEASURE -> SwatchFields(state, onAction)
                        GaugeTask.CALCULATE -> CalculateFields(state, onAction)
                        GaugeTask.ADJUST -> AdjustmentFields(state, onAction)
                        GaugeTask.CONVERT -> Unit
                    }
                    Text(
                        text = stringResource(state.task.warningResource()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().testTag("measurement_warning"),
                    )
                }
                state.resultError?.let { resultError ->
                    val message = stringResource(resultError.messageResource())
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics {
                                    error(
                                        message,
                                    )
                                }.testTag("measurement_result_error"),
                    )
                }
                GaugeResults(presentation, onCopy = { onAction(GaugeAction.Copy(it)) })
            }
        }
    }
}

@Composable
private fun ProjectContext(state: GaugeUiState) {
    state.projectName?.let { name ->
        Text(
            text = stringResource(R.string.measurement_project_context, name),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().testTag("measurement_project"),
        )
    }
    if (state.projectUnavailable) {
        Text(
            text = stringResource(R.string.measurement_project_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().testTag("measurement_project_unavailable"),
        )
    }
}

@Composable
private fun ConversionFields(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
) {
    val units = MeasurementUnit.entries.map { it to stringResource(it.titleResource()) }
    GaugeSelector(
        label = stringResource(R.string.measurement_from_unit),
        selectedValue = state.fromUnit,
        choices = units,
        onSelect = { onAction(GaugeAction.FromUnit(it)) },
        tag = "measurement_from",
    )
    GaugeSelector(
        label = stringResource(R.string.measurement_to_unit),
        selectedValue = state.toUnit,
        choices = units,
        onSelect = { onAction(GaugeAction.ToUnit(it)) },
        tag = "measurement_to",
    )
    GaugeNumericField(state, GaugeField.CONVERSION, onAction)
}

@Composable
private fun GaugeBasisSelector(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
) {
    GaugeSelector(
        label = stringResource(R.string.measurement_units),
        selectedValue = state.basis,
        choices = GaugeBasis.entries.map { it to stringResource(it.titleResource()) },
        onSelect = { onAction(GaugeAction.Basis(it)) },
        tag = "measurement_basis",
    )
}

@Composable
private fun SwatchFields(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
    axis: GaugeAxis? = null,
) {
    if (axis != GaugeAxis.ROWS) {
        GaugeHeading(stringResource(R.string.measurement_stitches_width))
        GaugeNumericField(state, GaugeField.SWATCH_WIDTH, onAction)
        GaugeNumericField(state, GaugeField.SWATCH_STITCHES, onAction)
    }
    if (axis != GaugeAxis.STITCHES) {
        GaugeHeading(stringResource(R.string.measurement_rows_height))
        GaugeNumericField(state, GaugeField.SWATCH_HEIGHT, onAction)
        GaugeNumericField(state, GaugeField.SWATCH_ROWS, onAction)
    }
}

@Composable
private fun ActualGaugeFields(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
    axis: GaugeAxis? = null,
) {
    GaugeSelector(
        label = stringResource(R.string.your_gauge_section),
        selectedValue = state.useSwatch,
        choices =
            listOf(
                true to stringResource(R.string.measure_swatch),
                false to stringResource(R.string.enter_directly),
            ),
        onSelect = { onAction(GaugeAction.SwatchInput(it)) },
        tag = "measurement_source",
    )
    if (state.useSwatch) {
        SwatchFields(state, onAction, axis)
    } else {
        if (axis != GaugeAxis.ROWS) GaugeNumericField(state, GaugeField.ACTUAL_STITCHES, onAction)
        if (axis != GaugeAxis.STITCHES) GaugeNumericField(state, GaugeField.ACTUAL_ROWS, onAction)
    }
}

@Composable
private fun CalculateFields(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
) {
    GaugeSelector(
        label = stringResource(R.string.measurement_operation),
        selectedValue = state.operation,
        choices =
            listOf(
                GaugeOperation.COUNT_FOR_SIZE to stringResource(R.string.measurement_count_for_size),
                GaugeOperation.SIZE_FROM_COUNT to stringResource(R.string.measurement_size_from_count),
            ),
        onSelect = { onAction(GaugeAction.Operation(it)) },
        tag = "measurement_operation",
    )
    GaugeSelector(
        label = stringResource(R.string.measurement_axis),
        selectedValue = state.axis,
        choices = GaugeAxis.entries.map { it to stringResource(it.titleResource()) },
        onSelect = { onAction(GaugeAction.Axis(it)) },
        tag = "measurement_axis",
    )
    ActualGaugeFields(state, onAction, state.axis)
    val field =
        when {
            state.operation == GaugeOperation.SIZE_FROM_COUNT && state.axis == GaugeAxis.STITCHES -> {
                GaugeField.STITCH_COUNT
            }
            state.operation == GaugeOperation.SIZE_FROM_COUNT -> GaugeField.ROW_COUNT
            state.axis == GaugeAxis.STITCHES -> GaugeField.TARGET_WIDTH
            else -> GaugeField.TARGET_HEIGHT
        }
    GaugeNumericField(state, field, onAction)
}

@Composable
private fun AdjustmentFields(
    state: GaugeUiState,
    onAction: (GaugeAction) -> Unit,
) {
    PasteInstructionButton(
        isPro = state.isPro,
        hintText = stringResource(R.string.instruction_hint_gauge),
        onResult = { parsed ->
            if (parsed is ParsedInstruction.Gauge || parsed is ParsedInstruction.GaugeSwatch) {
                onAction(GaugeAction.Paste(parsed))
                true
            } else {
                false
            }
        },
    )
    ActualGaugeFields(state, onAction)
    GaugeHeading(stringResource(R.string.pattern_gauge))
    GaugeNumericField(state, GaugeField.PATTERN_STITCHES, onAction)
    GaugeNumericField(state, GaugeField.PATTERN_ROWS, onAction)
    GaugeHeading(stringResource(R.string.pattern_instructions))
    GaugeNumericField(state, GaugeField.PATTERN_STITCH_COUNT, onAction)
    GaugeNumericField(state, GaugeField.PATTERN_ROW_COUNT, onAction)
}
