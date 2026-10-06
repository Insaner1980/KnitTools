package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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

/** Uuden projektin luonti ja projektin tietojen muokkaus samalla lisäyssheetillä kuin lanka ja verkkokaava. */
@Composable
fun ProjectDetailsSheet(
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

    FormSheet(
        title = title,
        description = destinationText,
        onDismiss = onDismiss,
        confirm =
            FormSheetConfirm(
                text = confirmText,
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
            ),
    ) {
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.project_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = cardTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        LabelWithInfo(label = stringResource(R.string.craft_type_label), info = null)
        CraftTypeToggle(
            selected = craftType,
            onSelect = { selected ->
                val previousCraftType = craftType
                craftType = selected
                labelType = updatedLabelTypeForCraftChange(labelType, previousCraftType, selected)
            },
        )
        LabelWithInfo(label = stringResource(R.string.main_counter_label), info = null)
        MainCounterLabelToggle(selected = labelType, onSelect = { labelType = it })
        if (labelType == MainCounterLabelType.CUSTOM) {
            CustomMainCounterLabelField(
                value = customLabel,
                sanitizedValue = sanitizedCustomLabel,
                onValueChange = { customLabel = it },
            )
        }
    }
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
private fun CustomMainCounterLabelField(
    value: String,
    sanitizedValue: String?,
    onValueChange: (String) -> Unit,
) {
    val showError = value.isNotBlank() && sanitizedValue == null
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
        colors = cardTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

// Sama segmenttivalitsin kuin muualla: vihreät sirut olivat ainoa eri näköinen valinta, ja
// tummassa teemassa valittu siru erottui tuskin lainkaan valitsemattomasta.
@Composable
private fun CraftTypeToggle(
    selected: CraftType,
    onSelect: (CraftType) -> Unit,
) {
    val options = CraftType.entries
    SegmentedToggle(
        options = options.map { craftTypeLabel(it) },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
    )
}

@Composable
private fun MainCounterLabelToggle(
    selected: MainCounterLabelType,
    onSelect: (MainCounterLabelType) -> Unit,
) {
    val options = MainCounterLabelType.entries
    SegmentedToggle(
        options =
            options.map { labelType ->
                if (labelType == MainCounterLabelType.CUSTOM) {
                    stringResource(R.string.main_counter_custom)
                } else {
                    mainCounterLabelText(labelType, customLabel = null)
                }
            },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
    )
}
