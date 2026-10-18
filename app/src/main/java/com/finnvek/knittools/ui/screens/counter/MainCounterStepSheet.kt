package com.finnvek.knittools.ui.screens.counter

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MAX_MAIN_COUNTER_STEP_SIZE
import com.finnvek.knittools.domain.calculator.parseMainCounterStepSize
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.mainCounterStepTitle
import kotlinx.coroutines.Job

@Composable
internal fun MainCounterStepSheet(
    stepSize: Int,
    labelType: MainCounterLabelType,
    customLabel: String?,
    onSave: (Int, (Boolean) -> Unit) -> Job,
    onDismiss: () -> Unit,
) {
    val presets = listOf(1, 2, 5)
    var selectedPreset by rememberSaveable { mutableStateOf(stepSize.takeIf { it in presets }) }
    var input by rememberSaveable { mutableStateOf(if (stepSize in presets) "" else stepSize.toString()) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) {
        onDispose { saveJob?.cancel() }
    }
    val parsed = selectedPreset ?: parseMainCounterStepSize(input)
    FormSheet(
        title = mainCounterStepTitle(labelType, customLabel),
        description = stringResource(R.string.counter_step_description),
        onDismiss = { if (!saving) onDismiss() },
        cancelEnabled = !saving,
        confirm =
            FormSheetConfirm(
                text = stringResource(R.string.save),
                enabled = parsed != null,
                busy = saving,
                onClick = {
                    if (parsed != null && !saving) {
                        saving = true
                        saveJob =
                            onSave(parsed) { saved ->
                                saving = false
                                if (saved) onDismiss() else saveFailed = true
                            }
                    }
                },
            ),
    ) {
        SegmentedToggle(
            options = presets.map(Int::toString),
            selectedIndex = presets.indexOf(selectedPreset),
            onSelect = {
                if (!saving) {
                    selectedPreset = presets[it]
                    input = ""
                    saveFailed = false
                }
            },
            itemTestTagPrefix = "main-step-preset",
        )
        NumberInputField(
            value = input,
            onValueChange = {
                if (!saving) {
                    selectedPreset = null
                    input = it
                    saveFailed = false
                }
            },
            label = stringResource(R.string.counter_step_custom_amount),
            options =
                NumberInputOptions(
                    preserveRawInput = true,
                    allowZero = false,
                    isLast = true,
                    placeholder = stringResource(R.string.counter_step_custom_placeholder),
                    readOnly = saving,
                ),
            errorMessage =
                if (parsed == null) {
                    stringResource(R.string.counter_step_invalid, MAX_MAIN_COUNTER_STEP_SIZE)
                } else {
                    null
                },
            inputModifier = Modifier.testTag("main-step-input"),
        )
        if (saveFailed) {
            Text(
                text = stringResource(R.string.counter_step_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
