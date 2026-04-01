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
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle

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
            val pSt = patternSt.toDoubleOrNull() ?: return@derivedStateOf null
            val pR = patternRows.toDoubleOrNull() ?: return@derivedStateOf null
            val ySt = yourSt.toDoubleOrNull() ?: return@derivedStateOf null
            val yR = yourRows.toDoubleOrNull() ?: return@derivedStateOf null
            val sc = stitchCount.toIntOrNull() ?: return@derivedStateOf null
            val rc = rowCount.toIntOrNull() ?: return@derivedStateOf null
            if (pSt <= 0 || pR <= 0) return@derivedStateOf null
            GaugeConverter.convert(pSt, pR, ySt, yR, sc, rc)
        }
    }

    ToolScreenScaffold(title = "Gauge Converter", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UnitToggle(useImperial = useImperial, onToggle = { useImperial = it })
            val gaugeUnit = if (useImperial) "per 4 in" else "per 10 cm"

            Text("Pattern gauge", style = MaterialTheme.typography.titleSmall)
            NumberInputField(
                value = patternSt,
                onValueChange = { patternSt = it },
                label = "Stitches $gaugeUnit",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = patternRows,
                onValueChange = { patternRows = it },
                label = "Rows $gaugeUnit",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Your gauge", style = MaterialTheme.typography.titleSmall)
            NumberInputField(
                value = yourSt,
                onValueChange = { yourSt = it },
                label = "Stitches $gaugeUnit",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = yourRows,
                onValueChange = { yourRows = it },
                label = "Rows $gaugeUnit",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Pattern instructions", style = MaterialTheme.typography.titleSmall)
            NumberInputField(
                value = stitchCount,
                onValueChange = { stitchCount = it },
                label = "Stitches in pattern",
                suffix = "st",
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = rowCount,
                onValueChange = { rowCount = it },
                label = "Rows in pattern",
                suffix = "rows",
                modifier = Modifier.fillMaxWidth(),
            )

            result?.let { r ->
                ResultCard(title = "Adjusted for your gauge") {
                    Text(
                        text = "${r.adjustedStitches} stitches, ${r.adjustedRows} rows",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Exact: ${"%.1f".format(r.adjustedStitchesExact)} st, ${"%.1f".format(r.adjustedRowsExact)} rows",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val sign = if (r.stitchPercentDifference >= 0) "+" else ""
                    Text(
                        text = "Stitch gauge: $sign${"%.1f".format(r.stitchPercentDifference)}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
