package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MAX_PROJECT_START_COUNT
import com.finnvek.knittools.domain.calculator.MAX_TARGET_ROWS
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.parseProjectStartCount
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel

data class ProjectDetailsValues(
    val name: String,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
    val initialCount: Int = 0,
    val targetRows: Int? = null,
)

enum class ProjectDetailsMode { Create, Edit }

/** Projektin luonti ja muokkaus käyttävät samaa lomakepohjaa kuin lanka ja verkko-ohje. */
@Composable
fun ProjectDetailsSheet(
    title: String,
    confirmText: String,
    initialValues: ProjectDetailsValues,
    onConfirm: (ProjectDetailsValues) -> Unit,
    onDismiss: () -> Unit,
    destinationText: String? = null,
    errorMessage: String? = null,
    mode: ProjectDetailsMode = ProjectDetailsMode.Edit,
    showPreview: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf(initialValues.name) }
    var craftType by rememberSaveable { mutableStateOf(initialValues.craftType) }
    var labelType by rememberSaveable { mutableStateOf(initialValues.mainCounterLabelType) }
    var customLabel by rememberSaveable { mutableStateOf(initialValues.mainCounterCustomLabel.orEmpty()) }
    var startText by rememberSaveable {
        mutableStateOf(
            initialValues.initialCount
                .takeIf { it != 0 }
                ?.toString()
                .orEmpty(),
        )
    }
    var targetText by rememberSaveable { mutableStateOf(initialValues.targetRows?.toString().orEmpty()) }
    var initialFocusRequested by rememberSaveable { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val targetFocus = remember { FocusRequester() }
    val customLabelFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isCreate = mode == ProjectDetailsMode.Create
    val startCount = parseProjectStartCount(startText)
    val target = parseTargetRows(targetText)
    val placeholder =
        stringResource(
            if (craftType == CraftType.CROCHET) {
                R.string.project_name_placeholder_crochet
            } else {
                R.string.project_name_placeholder_knitting
            },
        )
    val sanitizedCustomLabel = sanitizeMainCounterCustomLabel(customLabel)
    val hasValidCustomLabel =
        labelType != MainCounterLabelType.CUSTOM || sanitizedCustomLabel != null
    var moreOptionsExpanded by rememberSaveable {
        mutableStateOf(
            labelType != craftType.defaultMainCounterLabelType() ||
                customLabel.isNotBlank() ||
                targetText.isNotBlank(),
        )
    }
    LaunchedEffect(isCreate) {
        if (isCreate &&
            (
                labelType != craftType.defaultMainCounterLabelType() ||
                    customLabel.isNotBlank() ||
                    targetText.isNotBlank()
            )
        ) {
            moreOptionsExpanded = true
        }
    }
    LaunchedEffect(isCreate, hasValidCustomLabel, target) {
        if (isCreate && (!hasValidCustomLabel || target == TargetInput.Invalid)) moreOptionsExpanded = true
    }
    val canConfirm =
        name.trim().isNotEmpty() &&
            hasValidCustomLabel &&
            (!isCreate || startCount != null && target != TargetInput.Invalid)

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
                            initialCount = if (isCreate) startCount ?: 0 else initialValues.initialCount,
                            targetRows =
                                if (isCreate) (target as? TargetInput.Valid)?.value else initialValues.targetRows,
                        ),
                    )
                },
            ),
    ) {
        LaunchedEffect(isCreate, showPreview) {
            if (isCreate && showPreview && !initialFocusRequested) {
                withFrameNanos { }
                nameFocus.requestFocus()
                initialFocusRequested = true
            }
        }
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        if (isCreate && showPreview) {
            ProjectDetailsPreview(
                CounterProject(
                    name = name.trim().ifBlank { placeholder },
                    craftType = craftType,
                    mainCounterLabelType = labelType,
                    mainCounterCustomLabel = sanitizedCustomLabel,
                    count = startCount ?: 0,
                    targetRows = (target as? TargetInput.Valid)?.value,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
        }
        LabeledTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.project_name_label),
            placeholder = placeholder.takeIf { isCreate },
            modifier = Modifier.fillMaxWidth(),
            inputModifier =
                Modifier.focusRequester(nameFocus).focusProperties {
                    if (isCreate) next = startFocus
                },
            keyboardOptions = KeyboardOptions(imeAction = if (isCreate) ImeAction.Next else ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onNext = { startFocus.requestFocus() },
                    onDone = { focusManager.clearFocus() },
                ),
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
        if (isCreate) {
            ProjectStartingCountField(
                value = startText,
                onValueChange = { startText = it },
                startCount = startCount,
                labelType = labelType,
                moreOptionsExpanded = moreOptionsExpanded,
                startFocus = startFocus,
                targetFocus = targetFocus,
                nameFocus = nameFocus,
            )
            ProjectMoreOptionsButton(
                expanded = moreOptionsExpanded,
                hasValidCustomLabel = hasValidCustomLabel,
                isTargetInvalid = target == TargetInput.Invalid,
                customLabelFocus = customLabelFocus,
                targetFocus = targetFocus,
                onToggle = { moreOptionsExpanded = !moreOptionsExpanded },
            )
        }
        if (!isCreate || moreOptionsExpanded) {
            LabelWithInfo(label = stringResource(R.string.main_counter_label), info = null)
            MainCounterLabelToggle(selected = labelType, onSelect = { labelType = it })
            if (labelType == MainCounterLabelType.CUSTOM) {
                CustomMainCounterLabelField(
                    value = customLabel,
                    sanitizedValue = sanitizedCustomLabel,
                    onValueChange = { customLabel = it },
                    modifier = Modifier.focusRequester(customLabelFocus),
                )
            }
            if (isCreate) {
                ProjectTargetField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    isTargetInvalid = target == TargetInput.Invalid,
                    targetFocus = targetFocus,
                    startFocus = startFocus,
                )
            }
        }
    }
}

@Composable
private fun ProjectStartingCountField(
    value: String,
    onValueChange: (String) -> Unit,
    startCount: Int?,
    labelType: MainCounterLabelType,
    moreOptionsExpanded: Boolean,
    startFocus: FocusRequester,
    targetFocus: FocusRequester,
    nameFocus: FocusRequester,
) {
    val locale = rememberCurrentLocale()
    Column {
        NumberInputField(
            value = value,
            onValueChange = onValueChange,
            label = projectStartCountLabel(labelType),
            options = NumberInputOptions(placeholder = "0", isLast = !moreOptionsExpanded),
            errorMessage =
                if (startCount == null) {
                    stringResource(
                        R.string.project_start_count_error,
                        formatIntegerForDisplay(MAX_PROJECT_START_COUNT.toLong(), locale),
                    )
                } else {
                    null
                },
            inputModifier =
                Modifier.focusRequester(startFocus).focusProperties {
                    if (moreOptionsExpanded) {
                        next = targetFocus
                        down = targetFocus
                    }
                    previous = nameFocus
                },
        )
        if (startCount != null) ProjectDetailsHelper(projectStartCountHelp(startCount, labelType))
    }
}

@Composable
private fun ProjectMoreOptionsButton(
    expanded: Boolean,
    hasValidCustomLabel: Boolean,
    isTargetInvalid: Boolean,
    customLabelFocus: FocusRequester,
    targetFocus: FocusRequester,
    onToggle: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    TextButton(
        onClick = {
            if (expanded && (!hasValidCustomLabel || isTargetInvalid)) {
                if (!hasValidCustomLabel) customLabelFocus.requestFocus() else targetFocus.requestFocus()
            } else {
                focusManager.clearFocus()
                onToggle()
            }
        },
    ) {
        Text(
            stringResource(
                if (expanded) {
                    R.string.project_create_fewer_options
                } else {
                    R.string.project_create_more_options
                },
            ),
        )
    }
}

@Composable
private fun ProjectTargetField(
    value: String,
    onValueChange: (String) -> Unit,
    isTargetInvalid: Boolean,
    targetFocus: FocusRequester,
    startFocus: FocusRequester,
) {
    val locale = rememberCurrentLocale()
    Column {
        NumberInputField(
            value = value,
            onValueChange = onValueChange,
            label = stringResource(R.string.project_target_label),
            options = NumberInputOptions(isLast = true, allowZero = false),
            errorMessage =
                if (isTargetInvalid) {
                    stringResource(
                        R.string.project_target_error,
                        formatIntegerForDisplay(MAX_TARGET_ROWS.toLong(), locale),
                    )
                } else {
                    null
                },
            inputModifier = Modifier.focusRequester(targetFocus).focusProperties { previous = startFocus },
        )
        if (!isTargetInvalid) {
            ProjectDetailsHelper(stringResource(R.string.project_target_help))
        }
    }
}

@Composable
private fun ProjectDetailsPreview(project: CounterProject) {
    val display = CounterValueFormatter.forMainCounter(project)
    val countText =
        display.targetLine?.let { mainCounterTargetText(it) } ?: mainCounterCountText(display.projectCardCount)
    val description = stringResource(R.string.project_create_preview_a11y, "${project.name}, $countText")
    ContinueProjectCard(
        project = project,
        sessionStatus = null,
        onClick = null,
        onClickLabel = null,
        onOpenCounter = null,
        modifier =
            Modifier.clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    )
}

@Composable
private fun ProjectDetailsHelper(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun projectStartCountLabel(type: MainCounterLabelType): String =
    stringResource(
        when (type) {
            MainCounterLabelType.ROWS -> R.string.project_start_count_label_rows
            MainCounterLabelType.ROUNDS -> R.string.project_start_count_label_rounds
            MainCounterLabelType.REPEATS -> R.string.project_start_count_label_repeats
            MainCounterLabelType.CUSTOM -> R.string.project_start_count_label_custom
        },
    )

@Composable
private fun projectStartCountHelp(
    count: Int,
    type: MainCounterLabelType,
): String =
    if (count == 0) {
        stringResource(R.string.project_start_count_help_new)
    } else {
        stringResource(
            when (type) {
                MainCounterLabelType.ROWS -> R.string.project_start_count_next_rows
                MainCounterLabelType.ROUNDS -> R.string.project_start_count_next_rounds
                MainCounterLabelType.REPEATS -> R.string.project_start_count_next_repeats
                MainCounterLabelType.CUSTOM -> R.string.project_start_count_next_custom
            },
            count + 1,
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

@Composable
private fun CustomMainCounterLabelField(
    value: String,
    sanitizedValue: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showError = value.isNotBlank() && sanitizedValue == null
    LabeledTextField(
        value = value,
        onValueChange = onValueChange,
        inputModifier = modifier,
        label = stringResource(R.string.main_counter_custom_label),
        errorMessage =
            if (showError) {
                stringResource(R.string.main_counter_custom_label_error)
            } else {
                null
            },
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
