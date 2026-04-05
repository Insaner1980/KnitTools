package com.finnvek.knittools.ui.screens.yarncard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.care.CareSymbolPicker
import com.finnvek.knittools.ui.components.care.toggleCareSymbol

@Composable
fun YarnCardReviewScreen(
    viewModel: YarnCardViewModel,
    onSaveAndUse: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onDiscard: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onBack: () -> Unit,
    activeProjectName: String? = null,
    onLinkToProject: ((Long) -> Unit)? = null,
) {
    val form by viewModel.formState.collectAsStateWithLifecycle()
    var showLinkDialog by remember { mutableStateOf(false) }
    var savedCardId by remember { mutableLongStateOf(0L) }

    // Linkitysdialogi eriytetty omaksi composableksi
    if (showLinkDialog && activeProjectName != null && onLinkToProject != null) {
        LinkYarnDialog(
            savedCardId = savedCardId,
            activeProjectName = activeProjectName,
            onLink = { id ->
                onLinkToProject(id)
                showLinkDialog = false
                val (w, l, n) = viewModel.getCalculatorValues()
                onSaveAndUse(w, l, n)
            },
            onDismiss = {
                showLinkDialog = false
                val (w, l, n) = viewModel.getCalculatorValues()
                onSaveAndUse(w, l, n)
            },
        )
    }

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

            CareSymbolPicker(
                careSymbols = form.careSymbols,
                onToggle = { symbol ->
                    viewModel.updateField { copy(careSymbols = careSymbols.toggleCareSymbol(symbol)) }
                },
            )

            // Toimintonapit eriytetty omaksi composableksi
            ReviewActionButtons(
                isPro = viewModel.isPro,
                onDiscardClick = {
                    val (w, l, n) = viewModel.getCalculatorValues()
                    onDiscard(w, l, n)
                },
                onSaveClick = {
                    handleSaveClick(
                        viewModel = viewModel,
                        canLink = activeProjectName != null && onLinkToProject != null,
                        onSaveAndUse = onSaveAndUse,
                        onDiscard = onDiscard,
                        onShowLinkDialog = { id ->
                            savedCardId = id
                            showLinkDialog = true
                        },
                    )
                },
            )
        }
    }
}

/**
 * Dialogi: linkitä tallennettu lanka-kortti aktiiviseen projektiin.
 */
@Composable
private fun LinkYarnDialog(
    savedCardId: Long,
    activeProjectName: String,
    onLink: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.link_yarn)) },
        text = { Text(stringResource(R.string.link_to_project, activeProjectName)) },
        confirmButton = {
            TextButton(onClick = { onLink(savedCardId) }) {
                Text(stringResource(R.string.link))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Discard- ja Save/Use-nappirivi sekä pro-viesti.
 */
@Composable
private fun ReviewActionButtons(
    isPro: Boolean,
    onDiscardClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onDiscardClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.discard))
        }
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (isPro) {
                    stringResource(R.string.save_and_use)
                } else {
                    stringResource(R.string.use_in_calculator)
                },
            )
        }
    }

    if (!isPro) {
        Text(
            text = stringResource(R.string.pro_required_save),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Save-napin klikkauslogiikka eriytettynä — vähentää sisäkkäisyyttä.
 */
private fun handleSaveClick(
    viewModel: YarnCardViewModel,
    canLink: Boolean,
    onSaveAndUse: (String, String, String) -> Unit,
    onDiscard: (String, String, String) -> Unit,
    onShowLinkDialog: (Long) -> Unit,
) {
    if (!viewModel.isPro) {
        val (w, l, n) = viewModel.getCalculatorValues()
        onDiscard(w, l, n)
        return
    }
    viewModel.saveCard { id ->
        if (canLink) {
            onShowLinkDialog(id)
        } else {
            val (w, l, n) = viewModel.getCalculatorValues()
            onSaveAndUse(w, l, n)
        }
    }
}

@Composable
private fun LabelField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
    )
}
