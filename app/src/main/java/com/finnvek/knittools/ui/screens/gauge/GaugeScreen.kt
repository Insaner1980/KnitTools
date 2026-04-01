package com.finnvek.knittools.ui.screens.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.calculator.GaugeConverter
import com.finnvek.knittools.domain.model.GaugeConversionResult
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle
import com.finnvek.knittools.util.extensions.convertGaugeValue

@Composable
fun GaugeScreen(onBack: () -> Unit) {
    var patternSt by remember { mutableStateOf("") }
    var patternRows by remember { mutableStateOf("") }
    var yourSt by remember { mutableStateOf("") }
    var yourRows by remember { mutableStateOf("") }
    var stitchCount by remember { mutableStateOf("") }
    var rowCount by remember { mutableStateOf("") }
    var useImperial by remember { mutableStateOf(false) }

    val result by remember(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount) {
        derivedStateOf {
            parseAndConvert(patternSt, patternRows, yourSt, yourRows, stitchCount, rowCount)
        }
    }

    ToolScreenScaffold(title = "Gauge Converter", onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            val gaugeUnit = if (useImperial) "per 4 in" else "per 10 cm"

            GaugeInputFields(
                patternSt = patternSt,
                patternRows = patternRows,
                yourSt = yourSt,
                yourRows = yourRows,
                stitchCount = stitchCount,
                rowCount = rowCount,
                gaugeUnit = gaugeUnit,
                onPatternStChange = { patternSt = it },
                onPatternRowsChange = { patternRows = it },
                onYourStChange = { yourSt = it },
                onYourRowsChange = { yourRows = it },
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
private fun GaugeInputFields(
    patternSt: String,
    patternRows: String,
    yourSt: String,
    yourRows: String,
    stitchCount: String,
    rowCount: String,
    gaugeUnit: String,
    onPatternStChange: (String) -> Unit,
    onPatternRowsChange: (String) -> Unit,
    onYourStChange: (String) -> Unit,
    onYourRowsChange: (String) -> Unit,
    onStitchCountChange: (String) -> Unit,
    onRowCountChange: (String) -> Unit,
) {
    Text("Pattern gauge", style = MaterialTheme.typography.titleSmall)
    NumberInputField(
        value = patternSt,
        onValueChange = onPatternStChange,
        label = "Stitches $gaugeUnit",
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = patternRows,
        onValueChange = onPatternRowsChange,
        label = "Rows $gaugeUnit",
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Your gauge", style = MaterialTheme.typography.titleSmall)
    NumberInputField(
        value = yourSt,
        onValueChange = onYourStChange,
        label = "Stitches $gaugeUnit",
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = yourRows,
        onValueChange = onYourRowsChange,
        label = "Rows $gaugeUnit",
        isDecimal = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Pattern instructions", style = MaterialTheme.typography.titleSmall)
    NumberInputField(
        value = stitchCount,
        onValueChange = onStitchCountChange,
        label = "Stitches in pattern",
        suffix = "st",
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = rowCount,
        onValueChange = onRowCountChange,
        label = "Rows in pattern",
        suffix = "rows",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GaugeResultCard(result: GaugeConversionResult) {
    ResultCard(title = "Adjusted for your gauge") {
        Text(
            text = "${result.adjustedStitches} stitches, ${result.adjustedRows} rows",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Exact: ${"%.1f".format(
                result.adjustedStitchesExact,
            )} st, ${"%.1f".format(result.adjustedRowsExact)} rows",
            style = MaterialTheme.typography.bodySmall,
        )
        val sign = if (result.stitchPercentDifference >= 0) "+" else ""
        Text(
            text = "Stitch gauge: $sign${"%.1f".format(result.stitchPercentDifference)}%",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
