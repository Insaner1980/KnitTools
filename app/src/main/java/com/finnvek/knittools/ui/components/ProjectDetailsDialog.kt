package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel

data class ProjectDetailsValues(
    val name: String,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsDialog(
    title: String,
    confirmText: String,
    initialValues: ProjectDetailsValues,
    onConfirm: (ProjectDetailsValues) -> Unit,
    onDismiss: () -> Unit,
    destinationText: String? = null,
    errorMessage: String? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialValues.name) }
    var craftType by rememberSaveable { mutableStateOf(initialValues.craftType) }
    var labelType by rememberSaveable { mutableStateOf(initialValues.mainCounterLabelType) }
    var customLabel by rememberSaveable { mutableStateOf(initialValues.mainCounterCustomLabel.orEmpty()) }
    val sanitizedCustomLabel = sanitizeMainCounterCustomLabel(customLabel)
    val hasValidCustomLabel =
        labelType != MainCounterLabelType.CUSTOM || sanitizedCustomLabel != null
    val canConfirm = name.trim().isNotEmpty() && hasValidCustomLabel

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                destinationText?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.project_name_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = projectDetailsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.craft_type_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                CraftTypeChips(
                    selected = craftType,
                    onSelect = { selected ->
                        val previousCraftType = craftType
                        craftType = selected
                        labelType = updatedLabelTypeForCraftChange(labelType, previousCraftType, selected)
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.main_counter_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MainCounterLabelChips(selected = labelType, onSelect = { labelType = it })
                if (labelType == MainCounterLabelType.CUSTOM) {
                    CustomMainCounterLabelField(
                        value = customLabel,
                        sanitizedValue = sanitizedCustomLabel,
                        onValueChange = { customLabel = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        ProjectDetailsValues(
                            name = name.trim(),
                            craftType = craftType,
                            mainCounterLabelType = labelType,
                            mainCounterCustomLabel = sanitizedCustomLabel,
                        ),
                    )
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun updatedLabelTypeForCraftChange(
    currentLabelType: MainCounterLabelType,
    previousCraftType: CraftType,
    selectedCraftType: CraftType,
): MainCounterLabelType =
    if (currentLabelType == previousCraftType.defaultMainCounterLabelType()) {
        selectedCraftType.defaultMainCounterLabelType()
    } else {
        currentLabelType
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun projectDetailsTextFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomMainCounterLabelField(
    value: String,
    sanitizedValue: String?,
    onValueChange: (String) -> Unit,
) {
    val showError = value.isNotBlank() && sanitizedValue == null
    Spacer(modifier = Modifier.height(12.dp))
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.main_counter_custom_label)) },
        singleLine = true,
        isError = showError,
        supportingText =
            if (showError) {
                { Text(stringResource(R.string.main_counter_custom_label_error)) }
            } else {
                null
            },
        shape = MaterialTheme.shapes.large,
        colors = projectDetailsTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CraftTypeChips(
    selected: CraftType,
    onSelect: (CraftType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CraftType.entries.forEach { craftType ->
            FilterChip(
                selected = selected == craftType,
                onClick = { onSelect(craftType) },
                label = { Text(craftTypeLabel(craftType)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainCounterLabelChips(
    selected: MainCounterLabelType,
    onSelect: (MainCounterLabelType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MainCounterLabelType.entries.forEach { labelType ->
            FilterChip(
                selected = selected == labelType,
                onClick = { onSelect(labelType) },
                label = {
                    Text(
                        if (labelType == MainCounterLabelType.CUSTOM) {
                            stringResource(R.string.main_counter_custom)
                        } else {
                            mainCounterLabelText(labelType, customLabel = null)
                        },
                    )
                },
            )
        }
    }
}
