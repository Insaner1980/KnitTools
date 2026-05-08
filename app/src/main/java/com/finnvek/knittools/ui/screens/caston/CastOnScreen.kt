package com.finnvek.knittools.ui.screens.caston

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CastOnCalculator
import com.finnvek.knittools.domain.model.CastOnResult
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.InfoNote
import com.finnvek.knittools.ui.components.InfoTip
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.home.HomeViewModel

@Composable
fun CastOnScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()
    var width by rememberSaveable { mutableStateOf("") }
    var gauge by rememberSaveable { mutableStateOf("") }
    var patternRepeat by rememberSaveable { mutableStateOf("") }
    var edgeStitches by rememberSaveable { mutableStateOf("") }

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

    ToolScreenScaffold(
        title = stringResource(R.string.tool_cast_on_calculator),
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
            val unit = if (useImperial) stringResource(R.string.unit_inches) else stringResource(R.string.unit_cm)
            val gaugeUnit =
                if (useImperial) {
                    stringResource(R.string.unit_st_per_4in)
                } else {
                    stringResource(R.string.unit_st_per_10cm)
                }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                    Row(verticalAlignment = Alignment.Bottom) {
                        NumberInputField(
                            value = patternRepeat,
                            onValueChange = { patternRepeat = it },
                            label = stringResource(R.string.pattern_repeat_optional),
                            suffix = stringResource(R.string.unit_st),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoTip(
                            title = stringResource(R.string.tip_pattern_repeat_title),
                            description = stringResource(R.string.tip_pattern_repeat_desc),
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        NumberInputField(
                            value = edgeStitches,
                            onValueChange = { edgeStitches = it },
                            label = stringResource(R.string.edge_stitches_optional),
                            suffix = stringResource(R.string.unit_st),
                            modifier = Modifier.weight(1f),
                            isLast = true,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoTip(
                            title = stringResource(R.string.tip_edge_stitches_title),
                            description = stringResource(R.string.tip_edge_stitches_desc),
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
            }

            result?.let { r ->
                CastOnResultSection(r, unit, edgeStitches.toIntOrNull() ?: 0)
            }
        }
    }
}

@Composable
private fun CastOnResultSection(
    result: CastOnResult,
    unit: String,
    edgeCount: Int,
) {
    ResultCard(title = stringResource(R.string.result)) {
        AnimatedResultNumber(
            targetValue = stringResource(R.string.stitches_result, result.stitches),
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.actual_width, "%.1f".format(result.actualWidth), unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        result.adjustedDown?.let { down ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    stringResource(
                        R.string.adjusted_down,
                        down,
                        "%.1f".format(result.adjustedDownWidth),
                        unit,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        result.adjustedUp?.let { up ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.adjusted_up, up, "%.1f".format(result.adjustedUpWidth), unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (edgeCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        InfoNote(text = stringResource(R.string.edge_stitches_optional) + ": $edgeCount")
    }
}
