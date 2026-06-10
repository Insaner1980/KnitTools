package com.finnvek.knittools.ui.screens.counter

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.CounterStepButtonFace
import com.finnvek.knittools.ui.components.CounterStepSymbol
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.components.StitchCounter
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterDecreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterIncreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterTargetText
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
        item(key = "counter-hero-reveal-gap") {
            Spacer(modifier = Modifier.height(CounterDimens.CounterProjectRevealGap))
        }
        item(key = "project-content") {
            ProjectContentCards(
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
        state.activeAlert?.let { reminder ->
            item(key = "active-reminder-alert") {
                ReminderAlertCard(
                    reminder = reminder,
                    currentRow = state.counter.count,
                    onDismiss = actions.onDismissReminder,
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
    modifier: Modifier = Modifier,
) {
    val useCompactStitchLayout = state.canUseSecondaryCounter && state.visibleStitchTotal != null
    val heroButtonSpacing =
        if (useCompactStitchLayout) {
            CounterDimens.HeroButtonCompactSpacing
        } else {
            CounterDimens.HeroButtonSpacing
        }
    val controlsToStitchTrackerSpacing =
        if (useCompactStitchLayout) {
            CounterDimens.CounterControlsToStitchTrackerCompactSpacing
        } else {
            CounterDimens.CounterControlsToStitchTrackerSpacing
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CounterDimens.HeroMinimumHeight)
                .counterClickWithoutIndication(actions.onSurfaceIncrement),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.weight(CounterDimens.CounterHeroTopWeight))
        if (state.canUseSecondaryCounter) {
            CompactPatternRepeatRow(
                count = state.secondaryCount,
                onDecrement = actions.onDecrementSecondary,
                onIncrement = actions.onIncrementSecondary,
            )
            Spacer(modifier = Modifier.height(CounterDimens.CounterRepeatToRowSpacing))
        }
        CounterRowLabel(state = state, onShowTargetDialog = actions.onShowTargetDialog)
        CounterMainNumber(state = state)
        CounterTargetProgressBar(state = state, onShowTargetDialog = actions.onShowTargetDialog)
        Spacer(modifier = Modifier.height(heroButtonSpacing))
        CounterButtons(
            state = state,
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
        )
        if (state.visibleStitchTotal != null) {
            Spacer(modifier = Modifier.height(controlsToStitchTrackerSpacing))
            CounterStitchTracker(state = state, actions = actions)
        }
        Spacer(modifier = Modifier.weight(CounterDimens.CounterHeroBottomWeight))
        Spacer(modifier = Modifier.height(CounterDimens.HeroBottomBreathingSpace))
    }
}

private val CounterUiState.visibleStitchTotal: Int?
    get() = stitchCount?.takeIf { stitchTrackingEnabled && it > 0 }

@Composable
private fun CounterStitchTracker(
    state: CounterUiState,
    actions: CounterWorkspaceActions,
) {
    val totalStitches = state.visibleStitchTotal ?: return

    StitchCounter(
        label = stringResource(R.string.stitch_tracker_label),
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
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val labelText =
        display.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(display.heroTitle)
    Text(
        text = labelText,
        style =
            MaterialTheme.typography.headlineSmall.copy(
                fontSize = CounterDimens.CounterRowLabelFontSize,
                fontWeight = FontWeight.SemiBold,
            ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clickable(onClick = onShowTargetDialog),
    )
}

@Composable
private fun CounterMainNumber(state: CounterUiState) {
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val counterDescription =
        display.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(display.heroTitle)
    RollingCounter(
        count = state.counter.count,
        textStyle =
            MaterialTheme.typography.displayMedium.copy(
                fontSize = CounterDimens.CounterMainNumberFontSize,
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
                .widthIn(max = CounterDimens.CounterControlsMaxWidth)
                .height(CounterDimens.CounterProgressHeight)
                .clip(RoundedCornerShape(CounterDimens.CounterProgressCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onShowTargetDialog),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(CounterDimens.CounterProgressHeight)
                    .clip(RoundedCornerShape(CounterDimens.CounterProgressCornerRadius))
                    .background(fillColor),
        )
    }
    CounterTargetHelperLabel(
        helperText = counterTargetHelperText(state.counter.count, target),
        onShowTargetDialog = onShowTargetDialog,
    )
}

internal sealed interface CounterTargetHelperText {
    data object OneRowLeft : CounterTargetHelperText

    data class RowsLeft(
        val rows: Int,
    ) : CounterTargetHelperText

    data object TargetReached : CounterTargetHelperText

    data class PastTarget(
        val rows: Int,
    ) : CounterTargetHelperText
}

internal fun counterTargetHelperText(
    count: Int,
    targetRows: Int?,
): CounterTargetHelperText? {
    val target = targetRows?.takeIf { it > 0 } ?: return null
    val rowsUntilTarget = target - count
    return when {
        rowsUntilTarget > 1 -> CounterTargetHelperText.RowsLeft(rowsUntilTarget)
        rowsUntilTarget == 1 -> CounterTargetHelperText.OneRowLeft
        rowsUntilTarget == 0 -> CounterTargetHelperText.TargetReached
        rowsUntilTarget == -1 -> CounterTargetHelperText.PastTarget(1)
        else -> CounterTargetHelperText.PastTarget(-rowsUntilTarget)
    }
}

@Composable
private fun CounterTargetHelperLabel(
    helperText: CounterTargetHelperText?,
    onShowTargetDialog: () -> Unit,
) {
    val text =
        when (helperText) {
            is CounterTargetHelperText.RowsLeft ->
                pluralStringResource(R.plurals.counter_target_rows_left, helperText.rows, helperText.rows)

            CounterTargetHelperText.OneRowLeft ->
                pluralStringResource(R.plurals.counter_target_rows_left, 1, 1)
            CounterTargetHelperText.TargetReached -> stringResource(R.string.counter_target_reached)
            is CounterTargetHelperText.PastTarget ->
                pluralStringResource(R.plurals.counter_target_rows_past, helperText.rows, helperText.rows)

            null -> return
        }
    Spacer(modifier = Modifier.height(CounterDimens.CounterTargetHelperSpacing))
    Text(
        text = text,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontSize = CounterDimens.CounterTargetHelperFontSize,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onShowTargetDialog),
    )
}

@Composable
private fun CounterButtons(
    state: CounterUiState,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = CounterDimens.CounterControlsMaxWidth)
                .height(CounterDimens.CounterControlsHeight),
    ) {
        CounterImageButton(
            imageRes = R.drawable.counter_minus_button,
            contentDescription = mainCounterDecreaseContentDescription(display.decreaseContentDescription),
            visualSize = CounterDimens.CounterMinusVisualSize,
            onClick = onDecrement,
            modifier =
                Modifier
                    .size(CounterDimens.CounterMinusTouchSize)
                    .align(Alignment.CenterStart),
        )
        CounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            contentDescription = mainCounterIncreaseContentDescription(display.increaseContentDescription),
            visualSize = CounterDimens.CounterPrimaryVisualSize,
            onClick = onIncrement,
            modifier =
                Modifier
                    .size(CounterDimens.CounterPrimaryTouchSize)
                    .align(Alignment.CenterEnd),
        )
    }
}

private fun CounterUiState.toMainCounterProject(): CounterProject =
    CounterProject(
        id = projectId ?: 0L,
        name = projectName,
        count = counter.count,
        craftType = craftType,
        mainCounterLabelType = mainCounterLabelType,
        mainCounterCustomLabel = mainCounterCustomLabel,
        stepSize = counter.stepSize,
        targetRows = targetRows,
    )

@Composable
private fun CompactPatternRepeatRow(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .widthIn(
                    min = CounterDimens.CounterRepeatMinWidth,
                    max = CounterDimens.CounterRepeatMaxWidth,
                ).height(CounterDimens.CounterRepeatHeight)
                .clip(RoundedCornerShape(CounterDimens.CounterRepeatCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(
                    horizontal = CounterDimens.CounterRepeatHorizontalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.counter_repeat_label),
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = CounterDimens.CounterRepeatLabelFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceGroupSpacing),
        ) {
            PatternRepeatButton(
                symbol = CounterStepSymbol.Minus,
                contentDescription = stringResource(R.string.counter_decrease),
                onClick = onDecrement,
                enabled = count > 0,
            )
            Text(
                text = count.toString(),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = CounterDimens.CounterRepeatValueFontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            PatternRepeatButton(
                symbol = CounterStepSymbol.Plus,
                contentDescription = stringResource(R.string.counter_increase),
                onClick = onIncrement,
            )
        }
    }
}

private fun Modifier.counterClickWithoutIndication(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier =
    clickable(
        interactionSource = null,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )

@Composable
private fun PatternRepeatButton(
    symbol: CounterStepSymbol,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor =
        if (symbol == CounterStepSymbol.Plus) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier =
            Modifier
                .size(CounterDimens.CounterRepeatButtonTouchSize)
                .counterClickWithoutIndication(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        CounterStepButtonFace(
            symbol = symbol,
            contentDescription = contentDescription,
            visualSize = CounterDimens.CounterRepeatButtonVisualSize,
            symbolSize = CounterDimens.CounterRepeatIconSize,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = contentColor,
            enabled = enabled,
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
            color = MaterialTheme.colorScheme.secondary,
        )
        WorkspaceSectionAction(
            label = actionLabel,
            onClick = onAction,
        )
    }
}

@Composable
private fun WorkspaceSectionAction(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(CounterDimens.WorkspaceSectionActionIconSize),
        )
        Spacer(modifier = Modifier.width(CounterDimens.WorkspaceSectionActionIconSpacing))
        Text(label)
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
