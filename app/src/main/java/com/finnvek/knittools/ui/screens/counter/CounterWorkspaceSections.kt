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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.components.StitchCounter
import com.finnvek.knittools.ui.theme.CounterDimens

data class ProjectHeaderActions(
    val onNameSave: (String) -> Unit,
    val onEditingNameChange: (Boolean) -> Unit,
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
    projectCountersActions: ProjectCountersSectionActions,
    actions: CounterWorkspaceActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .clipToBounds(),
        contentPadding =
            PaddingValues(
                start = CounterDimens.ScreenHorizontalPadding,
                end = CounterDimens.ScreenHorizontalPadding,
                bottom = CounterDimens.ContentBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceSectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "counter-hero") {
            CounterHero(
                state = state,
                actions = actions,
                modifier = Modifier.fillParentMaxHeight(),
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
        item(key = "stitch-tracker") {
            CounterStitchTracker(state = state, actions = actions)
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CounterDimens.HeroMinimumHeight)
                .clickable(onClick = actions.onSurfaceIncrement),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        state.activeAlert?.let { reminder ->
            ReminderAlertCard(
                reminder = reminder,
                currentRow = state.counter.count,
                onDismiss = actions.onDismissReminder,
            )
            Spacer(modifier = Modifier.height(CounterDimens.WorkspaceGroupSpacing))
        }
        if (state.canUseSecondaryCounter) {
            CompactPatternRepeatRow(
                count = state.secondaryCount,
                onDecrement = actions.onDecrementSecondary,
                onIncrement = actions.onIncrementSecondary,
            )
            Spacer(modifier = Modifier.height(CounterDimens.WorkspaceGroupSpacing))
        }
        CounterRowLabel(state = state, onShowTargetDialog = actions.onShowTargetDialog)
        CounterMainNumber(state = state)
        CounterTargetProgressBar(state = state, onShowTargetDialog = actions.onShowTargetDialog)
        Spacer(modifier = Modifier.height(CounterDimens.HeroButtonSpacing))
        CounterButtons(
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
            onUndo = actions.onUndo,
        )
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
    Spacer(modifier = Modifier.height(CounterDimens.CounterProgressSpacing))
    val fraction = (state.counter.count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val completed = state.counter.count >= target
    val fillColor =
        if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CounterDimens.CounterProgressHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable(onClick = onShowTargetDialog),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(CounterDimens.CounterProgressHeight)
                    .background(fillColor),
        )
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
                    .size(CounterDimens.CounterSideButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                modifier = Modifier.size(CounterDimens.CounterSideIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Image(
            painter = painterResource(R.drawable.plus_button),
            contentDescription = stringResource(R.string.counter_increase),
            modifier =
                Modifier
                    .size(CounterDimens.CounterPrimaryButtonSize)
                    .clip(CircleShape)
                    .clickable(onClick = onIncrement),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .size(CounterDimens.CounterSideButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onUndo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.counter_undo),
                modifier = Modifier.size(CounterDimens.CounterSideIconSize),
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
                .clip(RoundedCornerShape(CounterDimens.CounterRepeatCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = CounterDimens.CounterRepeatHorizontalPadding,
                    vertical = CounterDimens.CounterRepeatVerticalPadding,
                ),
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
            horizontalArrangement = Arrangement.spacedBy(CounterDimens.ProjectCardGridSpacing),
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
                .size(CounterDimens.CounterRepeatButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(CounterDimens.CounterRepeatIconSize),
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
