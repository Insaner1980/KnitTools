package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.repository.YarnUsageResult
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.screens.gauge.GaugeSelector
import java.util.Locale

data class YarnUsageEditorActions(
    val onEdit: (YarnUsageField, String, Locale) -> Unit,
    val onUnit: (YarnUsageUnit) -> Unit,
    val onConversion: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onDismiss: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776") // Editorin validointi-, vahvistus- ja syöttötilat kuuluvat samaan sheetiin.
fun ProjectYarnUsageSheet(
    state: YarnUsageEditorState,
    sheetState: SheetState,
    actions: YarnUsageEditorActions,
    initiallyConfirmDelete: Boolean = false,
) {
    val draft = state.draft ?: return
    var confirmDelete by rememberSaveable(draft.usageId) { mutableStateOf(initiallyConfirmDelete) }
    val headingFocus = remember { FocusRequester() }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.isVisible) headingFocus.requestFocus()
    }
    ModalBottomSheet(
        onDismissRequest = { if (!state.busy) actions.onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .testTag("yarn_usage_editor"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.yarn_usage_title),
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier
                        .focusRequester(
                            headingFocus,
                        ).focusable()
                        .semantics { heading() }
                        .testTag("yarn_usage_heading"),
            )
            Text(draft.name, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.yarn_usage_scope), style = MaterialTheme.typography.bodySmall)
            GaugeSelector(
                label = stringResource(R.string.yarn_usage_unit),
                selectedValue = draft.unit,
                choices = YarnUsageUnit.entries.map { it to stringResource(it.titleResource()) },
                onSelect = actions.onUnit,
                tag = "yarn_usage_unit",
            )
            if (!draft.canSwitch) {
                Text(
                    stringResource(R.string.yarn_usage_fix_before_switch),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (draft.pendingUnit !=
                null
            ) {
                Text(
                    stringResource(R.string.yarn_usage_conversion_required),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            YarnUsageField.entries.take(3).forEach { field -> UsageField(draft, field, actions.onEdit) }
            TextButton(
                onClick = { actions.onConversion(!draft.conversionEnabled) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("yarn_usage_conversion"),
            ) {
                Text(
                    stringResource(
                        if (draft.conversionEnabled) {
                            R.string.yarn_usage_remove_conversion
                        } else {
                            R.string.yarn_usage_add_conversion
                        },
                    ),
                )
            }
            if (draft.conversionEnabled) {
                UsageField(draft, YarnUsageField.LENGTH, actions.onEdit)
                UsageField(draft, YarnUsageField.WEIGHT, actions.onEdit)
                if (!draft.conversionValid) {
                    Text(
                        stringResource(R.string.yarn_usage_conversion_required),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            UsageDerivedSummary(draft)
            state.error?.let { error ->
                Text(
                    stringResource(usageErrorResource(error, state.deleting)),
                    modifier = Modifier.testTag("yarn_usage_error"),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = actions.onSave,
                enabled = draft.canSave && !state.busy && !state.completed,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("yarn_usage_save"),
            ) {
                Text(stringResource(R.string.save))
            }
            if (draft.usageId != null) {
                val description = stringResource(R.string.yarn_usage_delete_named, draft.name)
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.busy,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("yarn_usage_delete").semantics {
                            contentDescription =
                                description
                        },
                ) {
                    Text(stringResource(R.string.yarn_usage_delete))
                }
            }
            TextButton(
                onClick = actions.onDismiss,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("yarn_usage_cancel"),
            ) { Text(stringResource(R.string.cancel)) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.yarn_usage_delete_title)) },
            text = { Text(stringResource(R.string.yarn_usage_delete_body, draft.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        actions.onDelete()
                    },
                    enabled = !state.busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("yarn_usage_confirm_delete"),
                ) { Text(stringResource(R.string.yarn_usage_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDelete = false
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun UsageField(
    draft: YarnUsageDraft,
    field: YarnUsageField,
    onEdit: (YarnUsageField, String, Locale) -> Unit,
) {
    val input = draft.input(field)
    val locale = rememberCurrentLocale()
    val ratio = field == YarnUsageField.LENGTH || field == YarnUsageField.WEIGHT
    val label =
        stringResource(
            when (field) {
                YarnUsageField.PLANNED -> R.string.yarn_usage_planned
                YarnUsageField.ALLOCATED -> R.string.yarn_usage_allocated
                YarnUsageField.USED -> R.string.yarn_usage_used
                YarnUsageField.LENGTH -> R.string.yarn_usage_length
                YarnUsageField.WEIGHT -> R.string.yarn_usage_weight
            },
        )
    val unit =
        if (field ==
            YarnUsageField.LENGTH
        ) {
            YarnUsageUnit.METERS
        } else if (ratio) {
            YarnUsageUnit.GRAMS
        } else {
            draft.unit
        }
    val error =
        when (input.error) {
            MeasurementNumberError.MUST_BE_POSITIVE ->
                if (ratio) R.string.yarn_usage_positive else R.string.yarn_usage_nonnegative
            MeasurementNumberError.INVALID_NUMBER, MeasurementNumberError.TOO_LARGE ->
                R.string.yarn_usage_invalid_amount
            null -> if (input.incomplete) R.string.yarn_usage_invalid_amount else null
        }
    NumberInputField(
        value = input.text,
        onValueChange = { onEdit(field, it, locale) },
        label = stringResource(R.string.measurement_field_with_unit, label, stringResource(unit.titleResource())),
        options =
            NumberInputOptions(
                isDecimal = true,
                preserveRawInput = true,
                allowZero = !ratio,
                isLast =
                    field == YarnUsageField.WEIGHT || field == YarnUsageField.USED,
            ),
        errorMessage = error?.let { stringResource(it) },
        inputModifier = Modifier.testTag("yarn_usage_input_${field.name}"),
    )
}

@Composable
private fun UsageDerivedSummary(draft: YarnUsageDraft) {
    val amounts =
        draft.amounts.copy(
            allocatedMeters = draft.allocated.value.takeIf { draft.allocated.valid },
            usedMeters = draft.used.value.takeIf { draft.used.valid },
        )
    Text(
        yarnUsageRemaining(amounts, draft.unit),
        modifier = Modifier.testTag("yarn_usage_remaining"),
        style = MaterialTheme.typography.titleMedium,
    )
    if (draft.conversionValid && draft.remaining != null) {
        listOf(YarnUsageUnit.METERS, YarnUsageUnit.GRAMS, YarnUsageUnit.SKEINS)
            .filterNot { it == draft.unit }
            .forEach { unit ->
                Text(yarnUsageRemaining(amounts, unit), style = MaterialTheme.typography.bodyMedium)
            }
    }
}

private fun usageErrorResource(
    result: YarnUsageResult,
    deleting: Boolean,
): Int =
    when (result) {
        is YarnUsageResult.AlreadyExists -> R.string.yarn_usage_exists
        YarnUsageResult.ProjectMissing, YarnUsageResult.SourceMissing, YarnUsageResult.SourceNotOwnedByProject,
        YarnUsageResult.UsageMissing, YarnUsageResult.StaleAction,
        -> R.string.yarn_usage_stale
        else -> if (deleting) R.string.yarn_usage_delete_failed else R.string.yarn_usage_save_failed
    }
