package com.finnvek.knittools.ui.screens.caston

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CastOnCalculator
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle
import com.finnvek.knittools.util.extensions.convertFieldValue
import com.finnvek.knittools.util.extensions.convertGaugeValue

@Composable
fun CastOnScreen(onBack: () -> Unit) {
    var width by remember { mutableStateOf("") }
    var gauge by remember { mutableStateOf("") }
    var patternRepeat by remember { mutableStateOf("") }
    var edgeStitches by remember { mutableStateOf("") }
    var useImperial by remember { mutableStateOf(false) }

    val result by remember(width, gauge, patternRepeat, edgeStitches, useImperial) {
        derivedStateOf {
            val w = width.toDoubleOrNull() ?: return@derivedStateOf null
            val g = gauge.toDoubleOrNull() ?: return@derivedStateOf null
            if (w <= 0 || g <= 0) return@derivedStateOf null
            CastOnCalculator.calculate(
                desiredWidth = w,
                stitchGauge = g,
                useInches = useImperial,
                patternRepeat = patternRepeat.toIntOrNull(),
                edgeStitches = edgeStitches.toIntOrNull() ?: 0,
            )
        }
    }

    ToolScreenScaffold(title = stringResource(R.string.tool_cast_on_calculator), onBack = onBack) { padding ->
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
                        width = convertFieldValue(width, newImperial)
                        gauge = convertGaugeValue(gauge, newImperial)
                        useImperial = newImperial
                    }
                },
            )
            val unit = if (useImperial) stringResource(R.string.unit_inches) else stringResource(R.string.unit_cm)
            val gaugeUnit =
                if (useImperial) {
                    stringResource(
                        R.string.unit_st_per_4in,
                    )
                } else {
                    stringResource(R.string.unit_st_per_10cm)
                }
            NumberInputField(
                value = width,
                onValueChange = { width = it },
                label = stringResource(R.string.desired_width),
                isDecimal = true,
                suffix = unit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = gauge,
                onValueChange = { gauge = it },
                label = stringResource(R.string.stitch_gauge),
                isDecimal = true,
                suffix = gaugeUnit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = patternRepeat,
                onValueChange = { patternRepeat = it },
                label = stringResource(R.string.pattern_repeat_optional),
                suffix = stringResource(R.string.unit_st),
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = edgeStitches,
                onValueChange = { edgeStitches = it },
                label = stringResource(R.string.edge_stitches_optional),
                suffix = stringResource(R.string.unit_st),
                modifier = Modifier.fillMaxWidth(),
            )

            result?.let { r ->
                ResultCard(title = stringResource(R.string.result)) {
                    Text(
                        text = stringResource(R.string.stitches_result, r.stitches),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.actual_width, "%.1f".format(r.actualWidth), unit),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    r.adjustedDown?.let { down ->
                        Text(
                            text =
                                stringResource(
                                    R.string.adjusted_down,
                                    down,
                                    "%.1f".format(r.adjustedDownWidth),
                                    unit,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    r.adjustedUp?.let { up ->
                        Text(
                            text = stringResource(R.string.adjusted_up, up, "%.1f".format(r.adjustedUpWidth), unit),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
