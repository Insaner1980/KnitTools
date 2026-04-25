package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.calculator.ShapingCounterLogic
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.SegmentedToggle

data class CounterItemActions(
    val onIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onRename: (String) -> Unit,
    val onReset: () -> Unit,
    val onDelete: () -> Unit,
    val performHaptic: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterListItem(
    counter: ProjectCounterEntity,
    actions: CounterItemActions,
    modifier: Modifier = Modifier,
) {
    val onIncrement = actions.onIncrement
    val onDecrement = actions.onDecrement
    val onRename = actions.onRename
    val onReset = actions.onReset
    val onDelete = actions.onDelete
    val performHaptic = actions.performHaptic
    var showContextMenu by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showContextMenu = true },
                ).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = counter.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (counter.counterType == "SHAPING" &&
                counter.startingStitches != null &&
                counter.stitchChange != null &&
                counter.shapeEveryN != null
            ) {
                val nextRow = ShapingCounterLogic.nextShapingRow(counter.shapeEveryN, counter.count)
                val stsAtNext =
                    ShapingCounterLogic.calculateCurrentStitches(
                        counter.startingStitches,
                        counter.stitchChange,
                        counter.shapeEveryN,
                        nextRow,
                    )
                val isShaping = ShapingCounterLogic.isShapingRow(counter.shapeEveryN, counter.count)
                Text(
                    text = stringResource(R.string.next_shaping_format, nextRow, stsAtNext),
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (isShaping) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }

        OutlinedButton(
            onClick = {
                performHaptic()
                onDecrement()
            },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        val displayText =
            if (counter.repeatAt != null) {
                "${counter.count}/${counter.repeatAt}"
            } else {
                "${counter.count}"
            }
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(12.dp))

        OutlinedButton(
            onClick = {
                performHaptic()
                onIncrement()
            },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.counter_increase),
                modifier = Modifier.size(16.dp),
            )
        }

        // Kontekstivalikko pitkällä painalluksella
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename_counter)) },
                onClick = {
                    showContextMenu = false
                    showRenameDialog = true
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reset_counter_name)) },
                onClick = {
                    showContextMenu = false
                    onReset()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.delete_counter),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    showContextMenu = false
                    showDeleteDialog = true
                },
            )
        }
    }

    if (showRenameDialog) {
        RenameCounterDialog(
            currentName = counter.name,
            onConfirm = {
                onRename(it)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_counter_title),
            message = stringResource(R.string.delete_counter_message, counter.name),
            confirmText = stringResource(R.string.delete_counter),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun RenameCounterDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_counter)) },
        text = {
            TextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun AddCounterDialog(
    onSave: (ProjectCounterDraft) -> Unit,
    onDismiss: () -> Unit,
    canUseRepeatSection: Boolean,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable {
        mutableIntStateOf(0)
    } // 0 = count up, 1 = repeating, 2 = shaping, 3 = repeat section
    var repeatAtText by rememberSaveable { mutableStateOf("") }
    var stepSizeText by rememberSaveable { mutableStateOf("1") }
    var startingStitchesText by rememberSaveable { mutableStateOf("") }
    var stitchChangeText by rememberSaveable { mutableStateOf("") }
    var shapeEveryNText by rememberSaveable { mutableStateOf("") }
    var repeatStartRowText by rememberSaveable { mutableStateOf("") }
    var repeatEndRowText by rememberSaveable { mutableStateOf("") }
    var totalRepeatsText by rememberSaveable { mutableStateOf("") }

    val isRepeating = selectedType == 1
    val isShaping = selectedType == 2
    val isRepeatSection = selectedType == 3 && canUseRepeatSection
    val repeatAt = repeatAtText.toIntOrNull()
    val stepSize = stepSizeText.toIntOrNull() ?: 1
    val startingStitches = startingStitchesText.toIntOrNull()
    val stitchChange = stitchChangeText.toIntOrNull()
    val shapeEveryN = shapeEveryNText.toIntOrNull()
    val repeatStartRow = repeatStartRowText.toIntOrNull()
    val repeatEndRow = repeatEndRowText.toIntOrNull()
    val totalRepeats = totalRepeatsText.toIntOrNull()
    val formParams =
        AddCounterFormParams(
            name = name,
            stepSize = stepSize,
            isRepeating = isRepeating,
            repeatAt = repeatAt,
            isShaping = isShaping,
            startingStitches = startingStitches,
            stitchChange = stitchChange,
            shapeEveryN = shapeEveryN,
            isRepeatSection = isRepeatSection,
            repeatStartRow = repeatStartRow,
            repeatEndRow = repeatEndRow,
            totalRepeats = totalRepeats,
        )
    val canSave =
        isAddCounterFormValid(formParams)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_counter)) },
        text = {
            AddCounterDialogContent(
                state =
                    AddCounterDialogContentState(
                        name = name,
                        selectedType = selectedType,
                        isRepeating = isRepeating,
                        repeatAtText = repeatAtText,
                        isShaping = isShaping,
                        startingStitchesText = startingStitchesText,
                        stitchChangeText = stitchChangeText,
                        shapeEveryNText = shapeEveryNText,
                        isRepeatSection = isRepeatSection,
                        repeatStartRowText = repeatStartRowText,
                        repeatEndRowText = repeatEndRowText,
                        totalRepeatsText = totalRepeatsText,
                        stepSizeText = stepSizeText,
                        canUseRepeatSection = canUseRepeatSection,
                    ),
                actions =
                    AddCounterDialogContentActions(
                        onNameChange = { if (it.length <= 50) name = it },
                        onTypeChange = { selectedType = it },
                        onRepeatAtChange = { repeatAtText = it },
                        onStartingStitchesChange = { startingStitchesText = it },
                        onStitchChangeChange = { stitchChangeText = it },
                        onShapeEveryNChange = { shapeEveryNText = it },
                        onRepeatStartRowChange = { repeatStartRowText = it },
                        onRepeatEndRowChange = { repeatEndRowText = it },
                        onTotalRepeatsChange = { totalRepeatsText = it },
                        onStepSizeChange = { stepSizeText = it },
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        createProjectCounterDraft(
                            params = formParams,
                            selectedType = selectedType,
                        ),
                    )
                },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun createProjectCounterDraft(
    params: AddCounterFormParams,
    selectedType: Int,
): ProjectCounterDraft =
    ProjectCounterDraft(
        name = params.name.trim(),
        repeatAt = if (params.isRepeating) params.repeatAt else null,
        stepSize = params.stepSize,
        counterType = counterTypeFromIndex(selectedType),
        startingStitches = if (params.isShaping) params.startingStitches else null,
        stitchChange = if (params.isShaping) params.stitchChange else null,
        shapeEveryN = if (params.isShaping) params.shapeEveryN else null,
        repeatStartRow = if (params.isRepeatSection) params.repeatStartRow else null,
        repeatEndRow = if (params.isRepeatSection) params.repeatEndRow else null,
        totalRepeats = if (params.isRepeatSection) params.totalRepeats else null,
        currentRepeat = if (params.isRepeatSection) 1 else null,
    )

// Data-luokka lomakkeen validointiparametrien ryhmittelyyn (S107)
data class AddCounterFormParams(
    val name: String,
    val stepSize: Int,
    val isRepeating: Boolean,
    val repeatAt: Int?,
    val isShaping: Boolean,
    val startingStitches: Int?,
    val stitchChange: Int?,
    val shapeEveryN: Int?,
    val isRepeatSection: Boolean,
    val repeatStartRow: Int?,
    val repeatEndRow: Int?,
    val totalRepeats: Int?,
)

private fun isAddCounterFormValid(params: AddCounterFormParams): Boolean =
    params.name.isNotBlank() &&
        params.stepSize > 0 &&
        (!params.isRepeating || (params.repeatAt != null && params.repeatAt > 0)) &&
        (
            !params.isShaping ||
                (
                    params.startingStitches != null &&
                        params.stitchChange != null &&
                        params.shapeEveryN != null &&
                        params.shapeEveryN > 0
                )
        ) &&
        (
            !params.isRepeatSection ||
                (
                    params.repeatStartRow != null &&
                        params.repeatEndRow != null &&
                        params.totalRepeats != null &&
                        params.repeatStartRow < params.repeatEndRow &&
                        params.totalRepeats > 0
                )
        )

private fun counterTypeFromIndex(index: Int): String =
    when (index) {
        1 -> "REPEATING"
        2 -> "SHAPING"
        3 -> "REPEAT_SECTION"
        else -> "COUNT_UP"
    }

// Data-luokka AddCounterDialogContent-parametrien ryhmittelyyn (S107)
data class AddCounterDialogContentState(
    val name: String,
    val selectedType: Int,
    val isRepeating: Boolean,
    val repeatAtText: String,
    val isShaping: Boolean,
    val startingStitchesText: String,
    val stitchChangeText: String,
    val shapeEveryNText: String,
    val isRepeatSection: Boolean,
    val repeatStartRowText: String,
    val repeatEndRowText: String,
    val totalRepeatsText: String,
    val stepSizeText: String,
    val canUseRepeatSection: Boolean,
)

data class AddCounterDialogContentActions(
    val onNameChange: (String) -> Unit,
    val onTypeChange: (Int) -> Unit,
    val onRepeatAtChange: (String) -> Unit,
    val onStartingStitchesChange: (String) -> Unit,
    val onStitchChangeChange: (String) -> Unit,
    val onShapeEveryNChange: (String) -> Unit,
    val onRepeatStartRowChange: (String) -> Unit,
    val onRepeatEndRowChange: (String) -> Unit,
    val onTotalRepeatsChange: (String) -> Unit,
    val onStepSizeChange: (String) -> Unit,
)

@Composable
private fun AddCounterDialogContent(
    state: AddCounterDialogContentState,
    actions: AddCounterDialogContentActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = { Text(stringResource(R.string.counter_name)) },
            placeholder = { Text(stringResource(R.string.counter_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        )
        SegmentedToggle(
            options =
                buildList {
                    add(stringResource(R.string.count_up))
                    add(stringResource(R.string.repeating))
                    add(stringResource(R.string.shaping))
                    if (state.canUseRepeatSection) {
                        add(stringResource(R.string.repeat_section))
                    }
                },
            selectedIndex = state.selectedType,
            onSelect = actions.onTypeChange,
        )
        if (state.isRepeating) {
            NumberInputField(
                value = state.repeatAtText,
                onValueChange = actions.onRepeatAtChange,
                label = stringResource(R.string.repeat_every),
            )
        }
        if (state.isShaping) {
            ShapingFields(
                startingStitchesText = state.startingStitchesText,
                onStartingStitchesChange = actions.onStartingStitchesChange,
                stitchChangeText = state.stitchChangeText,
                onStitchChangeChange = actions.onStitchChangeChange,
                shapeEveryNText = state.shapeEveryNText,
                onShapeEveryNChange = actions.onShapeEveryNChange,
            )
        }
        if (state.isRepeatSection) {
            RepeatSectionFields(
                repeatStartRowText = state.repeatStartRowText,
                onRepeatStartRowChange = actions.onRepeatStartRowChange,
                repeatEndRowText = state.repeatEndRowText,
                onRepeatEndRowChange = actions.onRepeatEndRowChange,
                totalRepeatsText = state.totalRepeatsText,
                onTotalRepeatsChange = actions.onTotalRepeatsChange,
            )
        }
        NumberInputField(
            value = state.stepSizeText,
            onValueChange = actions.onStepSizeChange,
            label = stringResource(R.string.step_size),
            isLast = true,
        )
    }
}

@Composable
private fun ShapingFields(
    startingStitchesText: String,
    onStartingStitchesChange: (String) -> Unit,
    stitchChangeText: String,
    onStitchChangeChange: (String) -> Unit,
    shapeEveryNText: String,
    onShapeEveryNChange: (String) -> Unit,
) {
    NumberInputField(
        value = startingStitchesText,
        onValueChange = onStartingStitchesChange,
        label = stringResource(R.string.starting_stitches),
    )
    NumberInputField(
        value = stitchChangeText,
        onValueChange = onStitchChangeChange,
        label = stringResource(R.string.stitch_change),
    )
    NumberInputField(
        value = shapeEveryNText,
        onValueChange = onShapeEveryNChange,
        label = stringResource(R.string.shape_every_n),
    )
}

@Composable
private fun RepeatSectionFields(
    repeatStartRowText: String,
    onRepeatStartRowChange: (String) -> Unit,
    repeatEndRowText: String,
    onRepeatEndRowChange: (String) -> Unit,
    totalRepeatsText: String,
    onTotalRepeatsChange: (String) -> Unit,
) {
    NumberInputField(
        value = repeatStartRowText,
        onValueChange = onRepeatStartRowChange,
        label = stringResource(R.string.repeat_section_start_row),
    )
    NumberInputField(
        value = repeatEndRowText,
        onValueChange = onRepeatEndRowChange,
        label = stringResource(R.string.repeat_section_end_row),
    )
    NumberInputField(
        value = totalRepeatsText,
        onValueChange = onTotalRepeatsChange,
        label = stringResource(R.string.total_repeats),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepeatSectionItem(
    counter: ProjectCounterEntity,
    mainRowCount: Int,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeatStartRow = counter.repeatStartRow ?: return
    val repeatEndRow = counter.repeatEndRow ?: return
    val totalRepeats = counter.totalRepeats ?: return
    val rowCountInRepeat = repeatEndRow - repeatStartRow + 1
    val syncedCounter = RepeatSectionLogic.updatePosition(counter, mainRowCount)
    val isComplete = RepeatSectionLogic.isComplete(counter, mainRowCount)
    val currentRepeat = syncedCounter.currentRepeat?.coerceIn(1, totalRepeats) ?: 1
    val currentRowInRepeat = RepeatSectionLogic.currentRowInRepeat(counter, mainRowCount)
    var showContextMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_counter_title),
            message = stringResource(R.string.delete_counter_message, counter.name),
            confirmText = stringResource(R.string.delete_counter),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showContextMenu = true },
                ).background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.delete_counter),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    showContextMenu = false
                    showDeleteDialog = true
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = counter.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text =
                if (isComplete) {
                    stringResource(R.string.repeat_section_complete)
                } else {
                    stringResource(
                        R.string.repeat_section_progress_format,
                        currentRepeat,
                        totalRepeats,
                        currentRowInRepeat,
                        rowCountInRepeat,
                    )
                },
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
        )
        LinearProgressIndicator(
            progress = { RepeatSectionLogic.progress(counter, mainRowCount) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
