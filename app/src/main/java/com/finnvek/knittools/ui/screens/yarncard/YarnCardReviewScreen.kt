package com.finnvek.knittools.ui.screens.yarncard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@Composable
fun YarnCardReviewScreen(
    viewModel: YarnCardViewModel,
    onSaveAndUse: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onDiscard: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onBack: () -> Unit,
) {
    val form by viewModel.formState.collectAsStateWithLifecycle()

    ToolScreenScaffold(title = stringResource(R.string.scanned_yarn), onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabelField(stringResource(R.string.brand), form.brand) {
                viewModel.updateField { copy(brand = it) }
            }
            LabelField(stringResource(R.string.yarn_name), form.yarnName) {
                viewModel.updateField { copy(yarnName = it) }
            }
            LabelField(stringResource(R.string.fiber_content), form.fiberContent) {
                viewModel.updateField { copy(fiberContent = it) }
            }
            LabelField(stringResource(R.string.color_name), form.colorName) {
                viewModel.updateField { copy(colorName = it) }
            }
            LabelField(stringResource(R.string.color_number), form.colorNumber) {
                viewModel.updateField { copy(colorNumber = it) }
            }
            LabelField(stringResource(R.string.dye_lot), form.dyeLot) {
                viewModel.updateField { copy(dyeLot = it) }
            }

            Text(
                text = stringResource(R.string.result),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            LabelField(stringResource(R.string.weight_grams), form.weightGrams) {
                viewModel.updateField { copy(weightGrams = it) }
            }
            LabelField(stringResource(R.string.length_meters), form.lengthMeters) {
                viewModel.updateField { copy(lengthMeters = it) }
            }
            LabelField(stringResource(R.string.needle_size_label), form.needleSize) {
                viewModel.updateField { copy(needleSize = it) }
            }
            LabelField(stringResource(R.string.gauge_label), form.gaugeInfo) {
                viewModel.updateField { copy(gaugeInfo = it) }
            }
            LabelField(stringResource(R.string.weight_category), form.weightCategory) {
                viewModel.updateField { copy(weightCategory = it) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val (w, l, n) = viewModel.getCalculatorValues()
                        onDiscard(w, l, n)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.discard))
                }
                Button(
                    onClick = {
                        if (viewModel.isPro) {
                            viewModel.saveCard { _ ->
                                val (w, l, n) = viewModel.getCalculatorValues()
                                onSaveAndUse(w, l, n)
                            }
                        } else {
                            val (w, l, n) = viewModel.getCalculatorValues()
                            onDiscard(w, l, n)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (viewModel.isPro) {
                            stringResource(R.string.save_and_use)
                        } else {
                            stringResource(R.string.use_in_calculator)
                        },
                    )
                }
            }

            if (!viewModel.isPro) {
                Text(
                    text = stringResource(R.string.pro_required_save),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
