package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.components.StitchCounter

data class ProjectHeaderActions(
    val onNameSave: (String) -> Unit,
    val onEditingNameChange: (Boolean) -> Unit,
    val onShowPatternInfo: () -> Unit,
    val onShowPatternPicker: () -> Unit,
    val onOpenPattern: () -> Unit,
)

data class ProjectCountersSectionActions(
    val onAddCounter: () -> Unit,
    val onIncrementCounter: (ProjectCounter) -> Unit,
    val onDecrementCounter: (ProjectCounter) -> Unit,
    val onRenameCounter: (Long, String) -> Unit,
    val onResetCounter: (Long) -> Unit,
    val onDeleteCounter: (Long) -> Unit,
    val performHaptic: () -> Unit,
)

data class CounterWorkspaceActions(
    val onSurfaceIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onIncrement: () -> Unit,
    val onUndo: () -> Unit,
    val onOpenPattern: () -> Unit,
    val onShowPatternPicker: () -> Unit,
    val onShowPatternInfo: () -> Unit,
    val onOpenNotes: () -> Unit,
    val onOpenYarn: () -> Unit,
    val onOpenPhotos: () -> Unit,
    val onOpenReminders: () -> Unit,
    val onOpenProjectActions: () -> Unit,
    val onShowAddCounter: () -> Unit,
    val onDecrementSecondary: () -> Unit,
    val onIncrementSecondary: () -> Unit,
    val onDecrementStitch: () -> Unit,
    val onIncrementStitch: () -> Unit,
    val onShowTargetDialog: () -> Unit,
    val onDismissReminder: (Long) -> Unit,
)

@Composable
fun CounterWorkspace(
    scaffoldPadding: PaddingValues,
    state: CounterUiState,
    isEditingName: Boolean,
    projectHeaderActions: ProjectHeaderActions,
    projectCountersActions: ProjectCountersSectionActions,
    actions: CounterWorkspaceActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        contentPadding =
            PaddingValues(
                start = 24.dp,
                top = scaffoldPadding.calculateTopPadding(),
                end = 24.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "project-header") {
            ProjectHeader(
                state = state,
                isEditingName = isEditingName,
                actions = projectHeaderActions,
            )
        }
        item(key = "counter-hero") {
            CounterHero(
                state = state,
                actions = actions,
            )
        }
        state.activeAlert?.let { reminder ->
            item(key = "active-reminder-${reminder.id}") {
                ReminderAlertCard(
                    reminder = reminder,
                    currentRow = state.counter.count,
                    onDismiss = actions.onDismissReminder,
                )
            }
        }
        item(key = "stitch-tracker") {
            CounterStitchTracker(state = state, actions = actions)
        }
        item(key = "counter-buttons") {
            CounterButtons(
                onDecrement = actions.onDecrement,
                onIncrement = actions.onIncrement,
                onUndo = actions.onUndo,
            )
        }
        item(key = "project-content") {
            ProjectContentCards(
                state = state,
                onCardClick = { kind -> actions.onProjectContentClick(kind, state) },
            )
        }
        if (state.projectCounters.isNotEmpty()) {
            item(key = "extra-counters-title") {
                WorkspaceSectionTitle(
                    title = stringResource(R.string.extra_counters_title),
                    actionLabel = stringResource(R.string.add_counter),
                    onAction = actions.onShowAddCounter,
                )
            }
            items(items = state.projectCounters, key = { counter -> counter.id }) { counter ->
                ProjectCounterWorkspaceItem(
                    counter = counter,
                    mainRowCount = state.counter.count,
                    actions = projectCountersActions,
                )
            }
        }
    }
}

private fun CounterWorkspaceActions.onProjectContentClick(
    kind: ProjectContentCardKind,
    state: CounterUiState,
) {
    when (kind) {
        ProjectContentCardKind.PATTERN -> {
            when {
                state.patternUri != null -> onOpenPattern()
                state.linkedPattern != null -> onShowPatternInfo()
                else -> onShowPatternPicker()
            }
        }

        ProjectContentCardKind.YARN -> onOpenYarn()
        ProjectContentCardKind.NOTES -> onOpenNotes()
        ProjectContentCardKind.PHOTOS -> onOpenPhotos()
        ProjectContentCardKind.REMINDER -> onOpenReminders()
    }
}

@Composable
private fun CounterHero(
    state: CounterUiState,
    actions: CounterWorkspaceActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp)
                .clickable(onClick = actions.onSurfaceIncrement),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.canUseSecondaryCounter) {
            CompactPatternRepeatRow(
                count = state.secondaryCount,
                onDecrement = actions.onDecrementSecondary,
                onIncrement = actions.onIncrementSecondary,
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        CounterRowLabel(state = state, onShowTargetDialog = actions.onShowTargetDialog)
        CounterMainNumber(state = state)
        CounterTargetProgressBar(state = state, onShowTargetDialog = actions.onShowTargetDialog)
    }
}

@Composable
private fun CounterStitchTracker(
    state: CounterUiState,
    actions: CounterWorkspaceActions,
) {
    val totalStitches = state.stitchCount?.takeIf { it > 0 } ?: return
    if (!state.stitchTrackingEnabled) return

    StitchCounter(
        currentStitch = state.currentStitch.coerceIn(0, totalStitches),
        totalStitches = totalStitches,
        onIncrement = actions.onIncrementStitch,
        onDecrement = actions.onDecrementStitch,
    )
}

@Composable
private fun CounterRowLabel(
    state: CounterUiState,
    onShowTargetDialog: () -> Unit,
) {
    val labelText =
        if (state.targetRows != null) {
            stringResource(R.string.row_label_with_target, state.counter.count, state.targetRows)
        } else {
            stringResource(R.string.current_row)
        }
    Text(
        text = labelText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onShowTargetDialog),
    )
}

@Composable
private fun CounterMainNumber(state: CounterUiState) {
    val counterFontSize = (115f / androidx.compose.ui.platform.LocalDensity.current.fontScale).sp
    val counterDescription =
        if (state.targetRows != null && state.targetRows > 0) {
            stringResource(R.string.row_label_with_target, state.counter.count, state.targetRows)
        } else {
            stringResource(R.string.current_row_short, state.counter.count)
        }
    RollingCounter(
        count = state.counter.count,
        textStyle =
            MaterialTheme.typography.displayMedium.copy(
                fontSize = counterFontSize,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        contentDescription = counterDescription,
    )
}

@Composable
private fun CounterTargetProgressBar(
    state: CounterUiState,
    onShowTargetDialog: () -> Unit,
) {
    val target = state.targetRows ?: return
    if (target <= 0) return
    Spacer(modifier = Modifier.height(12.dp))
    val fraction = (state.counter.count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val completed = state.counter.count >= target
    val fillColor =
        if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable(onClick = onShowTargetDialog),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(2.dp)
                    .background(fillColor),
        )
    }
}

@Composable
private fun ProjectHeader(
    state: CounterUiState,
    isEditingName: Boolean,
    actions: ProjectHeaderActions,
) {
    var draftName by rememberSaveable(state.projectId) { mutableStateOf(state.projectName) }

    LaunchedEffect(isEditingName, state.projectName) {
        if (!isEditingName) {
            draftName = state.projectName
        }
    }

    if (isEditingName) {
        TextField(
            value = draftName,
            onValueChange = { draftName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.default_project_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions.Default,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        actions.onNameSave(draftName)
                        actions.onEditingNameChange(false)
                    },
                    enabled = draftName.isNotBlank(),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.save),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    } else {
        Text(
            text = state.projectName.ifEmpty { stringResource(R.string.default_project_name) }.uppercase(),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 0.5.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { actions.onEditingNameChange(true) }),
        )
        Spacer(modifier = Modifier.height(6.dp))
        PatternHeaderRow(
            attachedPatternName = state.patternName,
            linkedPatternName = state.linkedPattern?.name,
            onShowPatternPicker = actions.onShowPatternPicker,
            onOpenPattern = actions.onOpenPattern,
            onShowPatternInfo = actions.onShowPatternInfo,
        )
    }
}

@Composable
private fun PatternHeaderRow(
    attachedPatternName: String?,
    linkedPatternName: String?,
    onShowPatternPicker: () -> Unit,
    onOpenPattern: () -> Unit,
    onShowPatternInfo: () -> Unit,
) {
    when {
        !attachedPatternName.isNullOrBlank() -> {
            Text(
                text = stringResource(R.string.project_header_pattern_attached),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPattern)
                        .padding(vertical = 2.dp),
            )
        }

        !linkedPatternName.isNullOrBlank() -> {
            Text(
                text = "$linkedPatternName · Ravelry",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowPatternInfo)
                        .padding(vertical = 2.dp),
            )
        }

        else -> {
            Text(
                text = stringResource(R.string.attach_pattern),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier =
                    Modifier
                        .clickable(onClick = onShowPatternPicker)
                        .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CounterButtons(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Image(
            painter = painterResource(R.drawable.plus_button),
            contentDescription = stringResource(R.string.counter_increase),
            modifier =
                Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onIncrement),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onUndo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.counter_undo),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactPatternRepeatRow(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.counter_repeat_label),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PatternRepeatButton(
                icon = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                onClick = onDecrement,
            )
            Text(
                text = count.toString(),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            PatternRepeatButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.counter_increase),
                onClick = onIncrement,
            )
        }
    }
}

@Composable
private fun PatternRepeatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WorkspaceSectionTitle(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
internal fun ProjectCounterWorkspaceItem(
    counter: ProjectCounter,
    mainRowCount: Int,
    actions: ProjectCountersSectionActions,
) {
    if (counter.counterType == ProjectCounterType.REPEAT_SECTION) {
        RepeatSectionItem(
            counter = counter,
            mainRowCount = mainRowCount,
            onDelete = { actions.onDeleteCounter(counter.id) },
        )
    } else {
        CounterListItem(
            counter = counter,
            actions =
                CounterItemActions(
                    onIncrement = { actions.onIncrementCounter(counter) },
                    onDecrement = { actions.onDecrementCounter(counter) },
                    onRename = { actions.onRenameCounter(counter.id, it) },
                    onReset = { actions.onResetCounter(counter.id) },
                    onDelete = { actions.onDeleteCounter(counter.id) },
                    performHaptic = actions.performHaptic,
                ),
        )
    }
}
