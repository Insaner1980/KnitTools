package com.finnvek.knittools.ui.screens.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.PasteInstructionButton
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.SectionHeader
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import com.finnvek.knittools.util.extensions.convertGaugeValue

@Composable
fun GaugeScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val proState by homeViewModel.proState.collectAsStateWithLifecycle()
    var patternSt by rememberSaveable { mutableStateOf("") }
    var patternRows by rememberSaveable { mutableStateOf("") }
    var yourSt by rememberSaveable { mutableStateOf("") }
    var yourRows by rememberSaveable { mutableStateOf("") }
    var stitchCount by rememberSaveable { mutableStateOf("") }
    var rowCount by rememberSaveable { mutableStateOf("") }
    var useImperial by rememberSaveable { mutableStateOf(false) }

    // Swatch-mittaus
    var swatchWidth by rememberSaveable { mutableStateOf("") }
    var swatchStitches by rememberSaveable { mutableStateOf("") }
    var swatchHeight by rememberSaveable { mutableStateOf("") }
    var swatchRows by rememberSaveable { mutableStateOf("") }

    val gaugeBase = if (useImperial) 4.0 else 10.0
    val swatchResult by remember(swatchWidth, swatchStitches, swatchHeight, swatchRows, gaugeBase) {
        derivedStateOf {
            val w = swatchWidth.toDoubleOrNull() ?: return@derivedStateOf null
            val sc = swatchStitches.toIntOrNull() ?: return@derivedStateOf null
            val h = swatchHeight.toDoubleOrNull() ?: return@derivedStateOf null
            val rc = swatchRows.toIntOrNull() ?: return@derivedStateOf null
            GaugeSwatchCalculator.calculate(w, sc, h, rc, gaugeBase)
        }
    }

    // Automaattitäyttö swatch-tuloksesta "Your gauge" -kenttiin
    LaunchedEffect(swatchResult) {
        swatchResult?.let { result ->
            yourSt = "%.1f".format(result.stitchesPerGaugeUnit)
            yourRows = "%.1f".format(result.rowsPerGaugeUnit)
        }
    }

    val result by remember(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount) {
        derivedStateOf {
            parseAndConvert(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount)
        }
    }

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
                onResult = { parsed ->
                    parseGaugeInstruction(parsed)?.let { fields ->
                        fields.yourSt?.let { yourSt = it }
                        fields.yourRows?.let { yourRows = it }
                        fields.swatchWidth?.let { swatchWidth = it }
                        fields.swatchStitches?.let { swatchStitches = it }
                        fields.swatchHeight?.let { swatchHeight = it }
                        fields.swatchRows?.let { swatchRows = it }
                        true
                    } ?: false
                },
            )

            UnitToggle(
                useImperial = useImperial,
                onToggle = { newImperial ->
                    if (newImperial != useImperial) {
                        patternSt = convertGaugeValue(patternSt, newImperial)
                        patternRows = convertGaugeValue(patternRows, newImperial)
                        yourSt = convertGaugeValue(yourSt, newImperial)
                        yourRows = convertGaugeValue(yourRows, newImperial)
                        useImperial = newImperial
                    }
                },
            )
            val unit = if (useImperial) stringResource(R.string.unit_inches) else stringResource(R.string.unit_cm)
            val gaugeUnit =
                if (useImperial) {
                    stringResource(R.string.unit_per_4in)
                } else {
                    stringResource(R.string.unit_per_10cm)
                }

            // My Gauge — swatch-mittaus
            SectionHeader(text = stringResource(R.string.my_gauge), icon = Icons.Filled.Straighten)
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = swatchWidth,
                onValueChange = { swatchWidth = it },
                label = stringResource(R.string.measured_width),
                isDecimal = true,
                suffix = unit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = swatchStitches,
                onValueChange = { swatchStitches = it },
                label = stringResource(R.string.stitch_count_in_swatch),
                suffix = stringResource(R.string.unit_st),
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = swatchHeight,
                onValueChange = { swatchHeight = it },
                label = stringResource(R.string.measured_height),
                isDecimal = true,
                suffix = unit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = swatchRows,
                onValueChange = { swatchRows = it },
                label = stringResource(R.string.row_count_in_swatch),
                suffix = stringResource(R.string.unit_rows),
                modifier = Modifier.fillMaxWidth(),
            )
            swatchResult?.let { sr ->
                ResultCard(title = stringResource(R.string.my_gauge)) {
                    Text(
                        text =
                            stringResource(
                                R.string.your_stitch_gauge,
                                "%.1f".format(sr.stitchesPerGaugeUnit) + " " + gaugeUnit,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.your_row_gauge,
                                "%.1f".format(sr.rowsPerGaugeUnit) + " " + gaugeUnit,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Convert Pattern -osio
            GaugeSection(
                title = stringResource(R.string.pattern_gauge),
                stitches = patternSt,
                rows = patternRows,
                gaugeUnit = gaugeUnit,
                onStitchesChange = { patternSt = it },
                onRowsChange = { patternRows = it },
            )
            GaugeSection(
                title = stringResource(R.string.your_gauge),
                stitches = yourSt,
                rows = yourRows,
                gaugeUnit = gaugeUnit,
                onStitchesChange = { yourSt = it },
                onRowsChange = { yourRows = it },
            )
            PatternInputSection(
                stitchCount = stitchCount,
                rowCount = rowCount,
                onStitchCountChange = { stitchCount = it },
                onRowCountChange = { rowCount = it },
            )

            result?.let { r -> GaugeResultCard(r) }
        }
    }
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
) {
    SectionHeader(text = title, icon = Icons.Filled.Straighten)
    Spacer(modifier = Modifier.height(8.dp))
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
) {
    SectionHeader(text = stringResource(R.string.pattern_instructions), icon = Icons.Filled.Description)
    Spacer(modifier = Modifier.height(8.dp))
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

private data class GaugeFields(
    val yourSt: String? = null,
    val yourRows: String? = null,
    val swatchWidth: String? = null,
    val swatchStitches: String? = null,
    val swatchHeight: String? = null,
    val swatchRows: String? = null,
)

private fun parseGaugeInstruction(parsed: ParsedInstruction): GaugeFields? =
    when (parsed) {
        is ParsedInstruction.Gauge -> {
            GaugeFields(
                yourSt = parsed.stitchesPer10cm.toString(),
                yourRows = parsed.rowsPer10cm.toString(),
            )
        }

        is ParsedInstruction.GaugeSwatch -> {
            GaugeFields(
                swatchWidth = parsed.width?.toString(),
                swatchStitches = parsed.stitches?.toString(),
                swatchHeight = parsed.height?.toString(),
                swatchRows = parsed.rows?.toString(),
            )
        }

        else -> {
            null
        }
    }
