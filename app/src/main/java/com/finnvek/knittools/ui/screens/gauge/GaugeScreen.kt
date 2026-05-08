package com.finnvek.knittools.ui.screens.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.nano.ParsedInstruction
import com.finnvek.knittools.domain.calculator.GaugeConverter
import com.finnvek.knittools.domain.calculator.GaugeSwatchCalculator
import com.finnvek.knittools.domain.model.GaugeConversionResult
import com.finnvek.knittools.domain.model.GaugeSwatchResult
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.InfoTip
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.PasteInstructionButton
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.SectionHeader
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import com.finnvek.knittools.util.extensions.convertFieldValue
import com.finnvek.knittools.util.extensions.convertGaugeValue
import java.util.Locale

@Composable
fun GaugeScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val proState by homeViewModel.proState.collectAsStateWithLifecycle()
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()
    var patternSt by rememberSaveable { mutableStateOf("") }
    var patternRows by rememberSaveable { mutableStateOf("") }
    var yourSt by rememberSaveable { mutableStateOf("") }
    var yourRows by rememberSaveable { mutableStateOf("") }
    var stitchCount by rememberSaveable { mutableStateOf("") }
    var rowCount by rememberSaveable { mutableStateOf("") }

    // Swatch-mittaus
    var swatchWidth by rememberSaveable { mutableStateOf("") }
    var swatchStitches by rememberSaveable { mutableStateOf("") }
    var swatchHeight by rememberSaveable { mutableStateOf("") }
    var swatchRows by rememberSaveable { mutableStateOf("") }
    var lastAutoFilledYourSt by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAutoFilledYourRows by rememberSaveable { mutableStateOf<String?>(null) }

    // Moodi: 0 = swatch-mittaus, 1 = suora syöttö
    var gaugeInputMode by rememberSaveable { mutableIntStateOf(0) }

    val gaugeBase = if (useImperial) 4.0 else 10.0
    val swatchResult by remember(swatchWidth, swatchStitches, swatchHeight, swatchRows, gaugeBase) {
        derivedStateOf { calculateSwatchGauge(swatchWidth, swatchStitches, swatchHeight, swatchRows, gaugeBase) }
    }

    // Automaattitäyttö swatch-tuloksesta "Your gauge" -kenttiin (vain swatch-moodissa)
    LaunchedEffect(swatchResult, gaugeInputMode) {
        if (gaugeInputMode != 0) return@LaunchedEffect
        val result = swatchResult ?: return@LaunchedEffect
        val autoFilledSt = String.format(Locale.US, "%.1f", result.stitchesPerGaugeUnit)
        val autoFilledRows = String.format(Locale.US, "%.1f", result.rowsPerGaugeUnit)

        if (yourSt.isBlank() || yourSt == lastAutoFilledYourSt) {
            yourSt = autoFilledSt
        }
        if (yourRows.isBlank() || yourRows == lastAutoFilledYourRows) {
            yourRows = autoFilledRows
        }

        lastAutoFilledYourSt = autoFilledSt
        lastAutoFilledYourRows = autoFilledRows
    }

    val result by remember(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount) {
        derivedStateOf {
            parseAndConvert(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount)
        }
    }

    val unit = if (useImperial) stringResource(R.string.unit_inches) else stringResource(R.string.unit_cm)
    val gaugeUnit =
        if (useImperial) {
            stringResource(R.string.unit_per_4in)
        } else {
            stringResource(R.string.unit_per_10cm)
        }

    val modeOptions =
        listOf(
            stringResource(R.string.measure_swatch),
            stringResource(R.string.enter_directly),
        )

    ToolScreenScaffold(
        title = stringResource(R.string.tool_gauge_converter),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PasteInstructionButton(
                isPro = proState.isPro,
                hintText = stringResource(R.string.instruction_hint_gauge),
                onResult = applyPaste@{ parsed ->
                    val fields = parseGaugeInstruction(parsed, useImperial) ?: return@applyPaste false
                    fields.yourSt?.let { yourSt = it }
                    fields.yourRows?.let { yourRows = it }
                    fields.swatchWidth?.let { swatchWidth = it }
                    fields.swatchStitches?.let { swatchStitches = it }
                    fields.swatchHeight?.let { swatchHeight = it }
                    fields.swatchRows?.let { swatchRows = it }
                    true
                },
            )

            // Osio 1: Your Gauge
            YourGaugeSection(
                state =
                    YourGaugeSectionState(
                        gaugeInputMode = gaugeInputMode,
                        modeOptions = modeOptions,
                        swatchWidth = swatchWidth,
                        swatchStitches = swatchStitches,
                        swatchHeight = swatchHeight,
                        swatchRows = swatchRows,
                        swatchResult = swatchResult,
                        yourSt = yourSt,
                        yourRows = yourRows,
                        unit = unit,
                        gaugeUnit = gaugeUnit,
                    ),
                actions =
                    YourGaugeSectionActions(
                        onGaugeInputModeChange = { gaugeInputMode = it },
                        onSwatchWidthChange = { swatchWidth = it },
                        onSwatchStitchesChange = { swatchStitches = it },
                        onSwatchHeightChange = { swatchHeight = it },
                        onSwatchRowsChange = { swatchRows = it },
                        onYourStChange = { yourSt = it },
                        onYourRowsChange = { yourRows = it },
                    ),
            )

            // Osio 2: Pattern Gauge
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GaugeSection(
                        title = stringResource(R.string.pattern_gauge),
                        stitches = patternSt,
                        rows = patternRows,
                        gaugeUnit = gaugeUnit,
                        onStitchesChange = { patternSt = it },
                        onRowsChange = { patternRows = it },
                        headerColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Osio 3: Pattern Instructions
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PatternInputSection(
                        stitchCount = stitchCount,
                        rowCount = rowCount,
                        onStitchCountChange = { stitchCount = it },
                        onRowCountChange = { rowCount = it },
                        headerColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            result?.let { r -> GaugeResultCard(r) }
        }
    }
}

// Data-luokka YourGaugeSection-parametrien ryhmittelyyn (S107)
data class YourGaugeSectionState(
    val gaugeInputMode: Int,
    val modeOptions: List<String>,
    val swatchWidth: String,
    val swatchStitches: String,
    val swatchHeight: String,
    val swatchRows: String,
    val swatchResult: GaugeSwatchResult?,
    val yourSt: String,
    val yourRows: String,
    val unit: String,
    val gaugeUnit: String,
)

data class YourGaugeSectionActions(
    val onGaugeInputModeChange: (Int) -> Unit,
    val onSwatchWidthChange: (String) -> Unit,
    val onSwatchStitchesChange: (String) -> Unit,
    val onSwatchHeightChange: (String) -> Unit,
    val onSwatchRowsChange: (String) -> Unit,
    val onYourStChange: (String) -> Unit,
    val onYourRowsChange: (String) -> Unit,
)

@Composable
private fun YourGaugeSection(
    state: YourGaugeSectionState,
    actions: YourGaugeSectionActions,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                SectionHeader(
                    text = stringResource(R.string.your_gauge_section),
                    icon = Icons.Filled.Straighten,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                InfoTip(
                    title = stringResource(R.string.tip_your_gauge_title),
                    description = stringResource(R.string.tip_your_gauge_desc),
                )
            }
            SegmentedToggle(
                options = state.modeOptions,
                selectedIndex = state.gaugeInputMode,
                onSelect = actions.onGaugeInputModeChange,
                fraction = 1f,
            )

            if (state.gaugeInputMode == 0) {
                SwatchMeasurementFields(
                    state =
                        SwatchFieldsState(
                            swatchWidth = state.swatchWidth,
                            swatchStitches = state.swatchStitches,
                            swatchHeight = state.swatchHeight,
                            swatchRows = state.swatchRows,
                            swatchResult = state.swatchResult,
                            unit = state.unit,
                            gaugeUnit = state.gaugeUnit,
                        ),
                    actions =
                        SwatchFieldsActions(
                            onSwatchWidthChange = actions.onSwatchWidthChange,
                            onSwatchStitchesChange = actions.onSwatchStitchesChange,
                            onSwatchHeightChange = actions.onSwatchHeightChange,
                            onSwatchRowsChange = actions.onSwatchRowsChange,
                        ),
                )
            } else {
                NumberInputField(
                    value = state.yourSt,
                    onValueChange = actions.onYourStChange,
                    label = stringResource(R.string.stitches_gauge, state.gaugeUnit),
                    isDecimal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                NumberInputField(
                    value = state.yourRows,
                    onValueChange = actions.onYourRowsChange,
                    label = stringResource(R.string.rows_gauge, state.gaugeUnit),
                    isDecimal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Data-luokat SwatchMeasurementFields-parametrien ryhmittelyyn (S107)
data class SwatchFieldsState(
    val swatchWidth: String,
    val swatchStitches: String,
    val swatchHeight: String,
    val swatchRows: String,
    val swatchResult: GaugeSwatchResult?,
    val unit: String,
    val gaugeUnit: String,
)

data class SwatchFieldsActions(
    val onSwatchWidthChange: (String) -> Unit,
    val onSwatchStitchesChange: (String) -> Unit,
    val onSwatchHeightChange: (String) -> Unit,
    val onSwatchRowsChange: (String) -> Unit,
)

@Composable
private fun SwatchMeasurementFields(
    state: SwatchFieldsState,
    actions: SwatchFieldsActions,
) {
    NumberInputField(
        value = state.swatchWidth,
        onValueChange = actions.onSwatchWidthChange,
        label = stringResource(R.string.measured_width),
        isDecimal = true,
        suffix = state.unit,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = state.swatchStitches,
        onValueChange = actions.onSwatchStitchesChange,
        label = stringResource(R.string.stitch_count_in_swatch),
        suffix = stringResource(R.string.unit_st),
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = state.swatchHeight,
        onValueChange = actions.onSwatchHeightChange,
        label = stringResource(R.string.measured_height),
        isDecimal = true,
        suffix = state.unit,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = state.swatchRows,
        onValueChange = actions.onSwatchRowsChange,
        label = stringResource(R.string.row_count_in_swatch),
        suffix = stringResource(R.string.unit_rows),
        modifier = Modifier.fillMaxWidth(),
    )
    state.swatchResult?.let { sr ->
        Text(
            text =
                stringResource(
                    R.string.calculated_gauge_format,
                    sr.stitchesPerGaugeUnit,
                    sr.rowsPerGaugeUnit,
                    state.gaugeUnit,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun calculateSwatchGauge(
    swatchWidth: String,
    swatchStitches: String,
    swatchHeight: String,
    swatchRows: String,
    gaugeBase: Double,
): GaugeSwatchResult? {
    val w = swatchWidth.toDoubleOrNull() ?: return null
    val sc = swatchStitches.toIntOrNull() ?: return null
    val h = swatchHeight.toDoubleOrNull() ?: return null
    val rc = swatchRows.toIntOrNull() ?: return null
    return GaugeSwatchCalculator.calculate(w, sc, h, rc, gaugeBase)
}

private fun parseAndConvert(
    patternSt: String,
    patternRows: String,
    yourSt: String,
    yourRows: String,
    stitchCount: String,
    rowCount: String,
): GaugeConversionResult? {
    val pSt = patternSt.toDoubleOrNull() ?: return null
    val pR = patternRows.toDoubleOrNull() ?: return null
    val ySt = yourSt.toDoubleOrNull() ?: return null
    val yR = yourRows.toDoubleOrNull() ?: return null
    val sc = stitchCount.toIntOrNull() ?: return null
    val rc = rowCount.toIntOrNull() ?: return null
    if (pSt <= 0 || pR <= 0) return null
    return GaugeConverter.convert(pSt, pR, ySt, yR, sc, rc)
}

@Composable
private fun GaugeSection(
    title: String,
    stitches: String,
    rows: String,
    gaugeUnit: String,
    onStitchesChange: (String) -> Unit,
    onRowsChange: (String) -> Unit,
    headerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    SectionHeader(text = title, icon = Icons.Filled.Straighten, color = headerColor)
    NumberInputField(
        value = stitches,
        onValueChange = onStitchesChange,
        label = stringResource(R.string.stitches_gauge, gaugeUnit),
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = rows,
        onValueChange = onRowsChange,
        label = stringResource(R.string.rows_gauge, gaugeUnit),
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PatternInputSection(
    stitchCount: String,
    rowCount: String,
    onStitchCountChange: (String) -> Unit,
    onRowCountChange: (String) -> Unit,
    headerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    SectionHeader(
        text = stringResource(R.string.pattern_instructions),
        icon = Icons.Filled.Description,
        color = headerColor,
    )
    NumberInputField(
        value = stitchCount,
        onValueChange = onStitchCountChange,
        label = stringResource(R.string.stitches_in_pattern),
        suffix = stringResource(R.string.unit_st),
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = rowCount,
        onValueChange = onRowCountChange,
        label = stringResource(R.string.rows_in_pattern),
        suffix = stringResource(R.string.unit_rows),
        modifier = Modifier.fillMaxWidth(),
        isLast = true,
    )
}

@Composable
private fun GaugeResultCard(result: GaugeConversionResult) {
    ResultCard(title = stringResource(R.string.adjusted_for_your_gauge)) {
        AnimatedResultNumber(
            targetValue = stringResource(R.string.adjusted_result, result.adjustedStitches, result.adjustedRows),
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                stringResource(
                    R.string.exact_result,
                    "%.1f".format(result.adjustedStitchesExact),
                    "%.1f".format(result.adjustedRowsExact),
                ),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val sign = if (result.stitchPercentDifference >= 0) "+" else ""
        BadgePill(
            text =
                stringResource(
                    R.string.stitch_gauge_diff,
                    "$sign${"%.1f".format(result.stitchPercentDifference)}",
                ),
        )
    }
}

internal data class GaugeFields(
    val yourSt: String? = null,
    val yourRows: String? = null,
    val swatchWidth: String? = null,
    val swatchStitches: String? = null,
    val swatchHeight: String? = null,
    val swatchRows: String? = null,
)

internal fun parseGaugeInstruction(
    parsed: ParsedInstruction,
    useImperial: Boolean,
): GaugeFields? =
    when (parsed) {
        is ParsedInstruction.Gauge -> {
            GaugeFields(
                yourSt = formatGaugeForCurrentUnit(parsed.stitchesPer10cm, parsed.unit, useImperial),
                yourRows = formatGaugeForCurrentUnit(parsed.rowsPer10cm, parsed.unit, useImperial),
            )
        }

        is ParsedInstruction.GaugeSwatch -> {
            GaugeFields(
                swatchWidth = parsed.width?.let { formatLengthForCurrentUnit(it, parsed.lengthUnit, useImperial) },
                swatchStitches = parsed.stitches?.toString(),
                swatchHeight = parsed.height?.let { formatLengthForCurrentUnit(it, parsed.lengthUnit, useImperial) },
                swatchRows = parsed.rows?.toString(),
            )
        }

        else -> {
            null
        }
    }

private fun formatGaugeForCurrentUnit(
    value: Double,
    unit: ParsedInstruction.GaugeUnit,
    useImperial: Boolean,
): String {
    val raw = value.toString()
    return when {
        useImperial && unit == ParsedInstruction.GaugeUnit.PER_10_CM -> convertGaugeValue(raw, true)
        !useImperial && unit == ParsedInstruction.GaugeUnit.PER_4_INCHES -> convertGaugeValue(raw, false)
        else -> raw
    }
}

private fun formatLengthForCurrentUnit(
    value: Double,
    unit: ParsedInstruction.LengthUnit?,
    useImperial: Boolean,
): String {
    val raw = value.toString()
    return when {
        useImperial && unit == ParsedInstruction.LengthUnit.CM -> convertFieldValue(raw, true)
        !useImperial && unit == ParsedInstruction.LengthUnit.INCHES -> convertFieldValue(raw, false)
        else -> raw
    }
}
