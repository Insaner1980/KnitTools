package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueDisplay
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.CounterStepSymbol
import com.finnvek.knittools.ui.components.CounterStepperButton
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.counterExtraName
import com.finnvek.knittools.ui.theme.counterExtraValue
import java.util.Locale

private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val COUNTER_NAME_MAX_LENGTH = 50

data class CounterItemActions(
    val onIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onRename: (String) -> Unit,
    val onReset: () -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterListItem(
    counter: ProjectCounter,
    actions: CounterItemActions,
    modifier: Modifier = Modifier,
) {
    val onIncrement = actions.onIncrement
    val onDecrement = actions.onDecrement
    val onRename = actions.onRename
    val onReset = actions.onReset
    val onDelete = actions.onDelete
    var showContextMenu by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showResetDialog by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(counter.id) { mutableStateOf(false) }
    val canDecrement = counter.count > 0
    val incrementProminent = extraCounterIncrementIsProminent(counter)
    val displayText = CounterValueFormatter.forExtraCounter(counter).asText()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CounterDimens.ExtraCounterCardMinHeight)
                .clip(RoundedCornerShape(CounterDimens.ExtraCounterCardCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .combinedClickable(
                    onClick = { showContextMenu = true },
                    onLongClick = { showContextMenu = true },
                    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
                ).padding(
                    horizontal = CounterDimens.ExtraCounterCardHorizontalPadding,
                    vertical = CounterDimens.ExtraCounterCardVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.ExtraCounterContentSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = counter.name,
                // CPD-ON
                style = MaterialTheme.typography.counterExtraName,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            CounterOverflowMenu(
                counterName = counter.name,
                expanded = showContextMenu,
                onExpandedChange = { showContextMenu = it },
                onRename = { showRenameDialog = true },
                onReset = { showResetDialog = true },
                onDelete = { showDeleteDialog = true },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CounterStepperButton(
                symbol = CounterStepSymbol.Minus,
                isIncrement = false,
                contentDescription = stringResource(R.string.counter_decrease_named, counter.name),
                onClick = onDecrement,
                enabled = canDecrement,
            )

            Spacer(modifier = Modifier.width(CounterDimens.ExtraCounterValueSpacing))

            Text(
                text = displayText,
                style = MaterialTheme.typography.counterExtraValue,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(CounterDimens.ExtraCounterValueSpacing))

            CounterStepperButton(
                symbol = CounterStepSymbol.Plus,
                isIncrement = true,
                contentDescription = stringResource(R.string.counter_increase_named, counter.name),
                onClick = onIncrement,
                prominent = incrementProminent,
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

    if (showResetDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.reset_counter),
            message = stringResource(R.string.reset_counter_message),
            confirmText = stringResource(R.string.reset_counter_name),
            isDestructive = true,
            onConfirm = {
                showResetDialog = false
                onReset()
            },
            onDismiss = { showResetDialog = false },
            // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
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
    // CPD-ON
}

internal fun extraCounterIncrementIsProminent(counter: ProjectCounter): Boolean {
    val display = CounterValueFormatter.forExtraCounter(counter)
    return when (display) {
        is CounterValueDisplay.Cycle -> display.current < display.length
        else -> true
    }
}

@Composable
internal fun CounterValueDisplay.asText(): String =
    when (this) {
        is CounterValueDisplay.Plain -> formatIntegerForDisplay(count.toLong(), rememberCurrentLocale())
        is CounterValueDisplay.Cycle ->
            stringResource(R.string.repeating_counter_value_format, current, length)
        is CounterValueDisplay.Section ->
            stringResource(
                R.string.repeat_section_progress_format,
                repeat,
                totalRepeats,
                rowInRepeat,
                rowsInRepeat,
            )
        CounterValueDisplay.SectionComplete ->
            stringResource(R.string.repeat_section_complete)
        is CounterValueDisplay.ReminderRepeat ->
            pluralStringResource(
                R.plurals.reminder_repeat_occurrence_format,
                intervalRows,
                occurrence,
                intervalRows,
            )
    }

@Composable
private fun CounterOverflowMenu(
    counterName: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRename: (() -> Unit)?,
    onReset: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    Box {
        IconButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.size(CounterDimens.ExtraCounterOverflowTouchSize),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.counter_actions, counterName),
                modifier = Modifier.size(CounterDimens.ExtraCounterOverflowIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            onRename?.let { rename ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename_counter)) },
                    onClick = {
                        onExpandedChange(false)
                        rename()
                    },
                )
            }
            onReset?.let { reset ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reset_counter_name)) },
                    onClick = {
                        onExpandedChange(false)
                        reset()
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.delete_counter),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            )
        }
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
                onValueChange = { if (it.length <= COUNTER_NAME_MAX_LENGTH) name = it },
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
    var linkedToMainCounter by rememberSaveable { mutableStateOf(false) }

    val isRepeating = selectedType == 1
    val isShaping = selectedType == 2
    val isRepeatSection = selectedType == 3 && canUseRepeatSection
    val canLinkToMainCounter = !isRepeatSection
    val repeatAt = parseCounterInput(repeatAtText)
    val stepSize = parseCounterInput(stepSizeText) ?: 0
    val startingStitches = parseCounterInput(startingStitchesText)
    val stitchChange = parseCounterInput(stitchChangeText, allowNegative = true)
    val shapeEveryN = parseCounterInput(shapeEveryNText)
    val repeatStartRow = parseCounterInput(repeatStartRowText)
    val repeatEndRow = parseCounterInput(repeatEndRowText)
    val totalRepeats = parseCounterInput(totalRepeatsText)
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
            linkedToMainCounter = linkedToMainCounter && canLinkToMainCounter,
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
                        linkedToMainCounter = linkedToMainCounter && canLinkToMainCounter,
                        canUseRepeatSection = canUseRepeatSection,
                    ),
                actions =
                    AddCounterDialogContentActions(
                        onNameChange = { if (it.length <= COUNTER_NAME_MAX_LENGTH) name = it },
                        onTypeChange = { index ->
                            selectedType = index
                            if (index == 3) {
                                linkedToMainCounter = false
                            }
                        },
                        onRepeatAtChange = { repeatAtText = it },
                        onStartingStitchesChange = { startingStitchesText = it },
                        onStitchChangeChange = { stitchChangeText = it },
                        onShapeEveryNChange = { shapeEveryNText = it },
                        onRepeatStartRowChange = { repeatStartRowText = it },
                        onRepeatEndRowChange = { repeatEndRowText = it },
                        onTotalRepeatsChange = { totalRepeatsText = it },
                        onStepSizeChange = { stepSizeText = it },
                        onLinkedToMainCounterChange = { linkedToMainCounter = it && canLinkToMainCounter },
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

internal fun parseCounterInput(
    text: String,
    allowNegative: Boolean = false,
): Int? =
    MeasurementNumberParser
        .parse(
            text = text,
            locale = Locale.ROOT,
            integer = true,
            allowZero = true,
            allowNegative = allowNegative,
        ).value
        ?.toInt()

private fun createProjectCounterDraft(
    params: AddCounterFormParams,
    selectedType: Int,
): ProjectCounterDraft =
    ProjectCounterDraft(
        name = params.name.trim(),
        repeatAt = if (params.isRepeating) params.repeatAt else null,
        stepSize = params.stepSize,
        counterType = counterTypeForDraft(selectedType, params.isRepeatSection),
        startingStitches = if (params.isShaping) params.startingStitches else null,
        stitchChange = if (params.isShaping) params.stitchChange else null,
        shapeEveryN = if (params.isShaping) params.shapeEveryN else null,
        repeatStartRow = if (params.isRepeatSection) params.repeatStartRow else null,
        repeatEndRow = if (params.isRepeatSection) params.repeatEndRow else null,
        totalRepeats = if (params.isRepeatSection) params.totalRepeats else null,
        currentRepeat = if (params.isRepeatSection) 1 else null,
        linkedToMainCounter = params.linkedToMainCounter && !params.isRepeatSection,
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
    val linkedToMainCounter: Boolean = false,
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
                        params.repeatStartRow > 0 &&
                        params.repeatStartRow <= params.repeatEndRow &&
                        params.totalRepeats > 0
                )
        )

private fun counterTypeFromIndex(index: Int): ProjectCounterType =
    when (index) {
        1 -> ProjectCounterType.REPEATING
        2 -> ProjectCounterType.SHAPING
        3 -> ProjectCounterType.REPEAT_SECTION
        else -> ProjectCounterType.COUNT_UP
    }

internal fun counterTypeForDraft(
    selectedType: Int,
    isRepeatSection: Boolean,
): ProjectCounterType =
    when {
        selectedType == 3 && isRepeatSection -> ProjectCounterType.REPEAT_SECTION
        selectedType == 3 -> ProjectCounterType.COUNT_UP
        else -> counterTypeFromIndex(selectedType)
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
    val linkedToMainCounter: Boolean,
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
    val onLinkedToMainCounterChange: (Boolean) -> Unit,
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
        LinkedCounterSetting(
            checked = state.linkedToMainCounter,
            enabled = !state.isRepeatSection,
            onCheckedChange = actions.onLinkedToMainCounterChange,
        )
        NumberInputField(
            value = state.stepSizeText,
            onValueChange = actions.onStepSizeChange,
            label = stringResource(R.string.step_size),
            options = NumberInputOptions(isLast = true),
        )
    }
}

@Composable
private fun LinkedCounterSetting(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.linked_counter),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
                    },
            )
            Text(
                text = stringResource(R.string.linked_counter_description),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_CONTENT_ALPHA)
                    },
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
        options = NumberInputOptions(allowNegative = true),
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
    counter: ProjectCounter,
    mainRowCount: Int,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (counter.repeatStartRow == null || counter.repeatEndRow == null || counter.totalRepeats == null) {
        return
    }
    val display = CounterValueFormatter.forRepeatSection(counter, mainRowCount)
    val isComplete = display is CounterValueDisplay.SectionComplete
    var showContextMenu by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(counter.id) { mutableStateOf(false) }

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
                .heightIn(min = CounterDimens.ExtraCounterCardMinHeight)
                .clip(RoundedCornerShape(CounterDimens.ExtraCounterCardCornerRadius))
                .combinedClickable(
                    onClick = { showContextMenu = true },
                    onLongClick = { showContextMenu = true },
                ).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    horizontal = CounterDimens.ExtraCounterCardHorizontalPadding,
                    vertical = CounterDimens.ExtraCounterCardVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.ExtraCounterContentSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = counter.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            CounterOverflowMenu(
                counterName = counter.name,
                expanded = showContextMenu,
                onExpandedChange = { showContextMenu = it },
                onRename = null,
                onReset = null,
                onDelete = { showDeleteDialog = true },
            )
        }
        Text(
            text = display.asText(),
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CounterDimens.RepeatSectionProgressHeight),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
