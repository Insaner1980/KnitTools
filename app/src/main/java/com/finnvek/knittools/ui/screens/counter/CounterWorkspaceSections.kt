package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.CounterStepButtonFace
import com.finnvek.knittools.ui.components.CounterStepButtonFaceAppearance
import com.finnvek.knittools.ui.components.CounterStepSymbol
import com.finnvek.knittools.ui.components.MainCounterTargetStatus
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.components.StitchCounter
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterDecreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterIncreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterProjectCardCountText
import com.finnvek.knittools.ui.components.mainCounterTargetStatus
import com.finnvek.knittools.ui.components.mainCounterTargetText
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.CounterDimens
import java.util.Locale

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
)

data class CounterWorkspaceActions(
    val onSurfaceIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onIncrement: () -> Unit,
    val onUndo: () -> Unit,
    val onOpenPattern: () -> Unit,
    val onShowDocuments: () -> Unit,
    val onShowPatternPicker: () -> Unit,
    val onOpenSavedPatternDetail: () -> Unit,
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
    val onStopSession: () -> Unit,
    val onResolveSessionRecovery: () -> Unit,
)

internal data class CounterHeroState(
    val counter: CounterState,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
    val targetRows: Int?,
    val canUseSecondaryCounter: Boolean,
    val secondaryCount: Int,
    val visibleStitchTotal: Int?,
    val currentStitch: Int,
)

internal fun CounterUiState.toCounterHeroState(): CounterHeroState =
    CounterHeroState(
        counter = counter,
        craftType = craftType,
        mainCounterLabelType = mainCounterLabelType,
        mainCounterCustomLabel = mainCounterCustomLabel,
        targetRows = targetRows,
        canUseSecondaryCounter = canUseSecondaryCounter,
        secondaryCount = secondaryCount,
        visibleStitchTotal = stitchCount?.takeIf { stitchTrackingEnabled && it > 0 },
        currentStitch = currentStitch,
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
                state = state.toCounterHeroState(),
                actions = actions,
                modifier = Modifier.fillParentMaxHeight(),
            )
        }
        state.activeSession?.takeIf { it.projectId == state.projectId }?.let { activeSession ->
            item(key = "active-work-session") {
                ActiveWorkSessionRow(
                    projectName = state.projectName,
                    durationSeconds = state.sessionSeconds,
                    rowsWorked =
                        (activeSession.trustedRowsWorked.toLong() + activeSession.pendingRowsWorked.toLong())
                            .coerceAtMost(Int.MAX_VALUE.toLong())
                            .toInt(),
                    needsRecoveryReview = activeSession.needsRecoveryReview,
                    onStop = actions.onStopSession,
                    onResolve = actions.onResolveSessionRecovery,
                )
            }
        }
        item(key = "counter-hero-reveal-gap") {
            Spacer(modifier = Modifier.height(CounterDimens.CounterProjectRevealGap))
        }
        item(key = "project-content") {
            ProjectContentCards(
                onCardClick = { kind -> actions.onProjectContentClick(kind, state) },
                hasPattern =
                    hasProjectPatternContent(
                        hasMetadataLink = state.linkedPattern != null,
                        hasDocuments = state.projectDocuments.isNotEmpty(),
                    ),
            )
        }
        if (state.projectCounters.isNotEmpty()) {
            item(key = "extra-counters-title") {
                WorkspaceSectionTitle(
                    title = stringResource(R.string.extra_counters_title),
                    actionLabel = stringResource(R.string.add_counter),
                    onAction = actions.onShowAddCounter,
                    proStatus = state.proStatus,
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
        if (state.activeAlert != null) {
            state.activeAlert.let { reminder ->
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
}

@Composable
internal fun ActiveWorkSessionRow(
    projectName: String,
    durationSeconds: Long,
    rowsWorked: Int,
    needsRecoveryReview: Boolean,
    onStop: () -> Unit,
    onResolve: () -> Unit,
) {
    val duration = formatWorkSessionDuration(durationSeconds)
    val sessionDescription = stringResource(R.string.work_session_active_description, projectName, duration)
    val label =
        stringResource(
            if (needsRecoveryReview) {
                R.string.work_session_recovery_needed
            } else {
                R.string.work_session_active
            },
        )
    val actionLabel =
        stringResource(
            if (needsRecoveryReview) {
                R.string.work_session_resolve
            } else {
                R.string.work_session_stop
            },
        )
    val rowsText = pluralStringResource(R.plurals.rows_format, rowsWorked, rowsWorked)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .semantics { contentDescription = sessionDescription }
                .padding(
                    start = CounterDimens.WorkSessionStartPadding,
                    end = CounterDimens.WorkSessionEndPadding,
                    top = CounterDimens.WorkSessionVerticalPadding,
                    bottom = CounterDimens.WorkSessionVerticalPadding,
                ),
    ) {
        val stackContent = maxWidth < CounterDimens.WorkSessionStackBreakpoint || fontScale >= 1.5f
        val sessionContent: @Composable () -> Unit = {
            WorkSessionStatusText(
                label = label,
                duration = duration,
                rowsText = rowsText,
            )
        }
        val action: @Composable () -> Unit = {
            TextButton(
                onClick = if (needsRecoveryReview) onResolve else onStop,
                modifier = Modifier.heightIn(min = CounterDimens.WorkSessionActionMinHeight),
            ) {
                Text(actionLabel)
            }
        }
        if (stackContent) {
            Column(verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkSessionStackedSpacing)) {
                sessionContent()
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    action()
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CounterDimens.WorkSessionInlineSpacing),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    sessionContent()
                }
                action()
            }
        }
    }
}

@Composable
private fun WorkSessionStatusText(
    label: String,
    duration: String,
    rowsText: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CounterDimens.WorkSessionInlineSpacing)) {
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = rowsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun formatWorkSessionDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    val remainingSeconds = safeSeconds % 60L
    return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
}

private fun CounterWorkspaceActions.onProjectContentClick(
    kind: ProjectContentCardKind,
    state: CounterUiState,
) {
    when (kind) {
        ProjectContentCardKind.PATTERN -> {
            val primary = state.primaryDocument
            when (
                projectPatternCardAction(
                    hasMetadataLink = state.linkedPattern != null,
                    hasPrimaryDocument = primary != null,
                    primaryAvailable = primary != null && state.projectDocumentAvailability[primary.id] == true,
                )
            ) {
                ProjectPatternCardAction.OpenPrimaryDocument -> onOpenPattern()
                ProjectPatternCardAction.OpenDocumentRecovery -> onShowDocuments()
                ProjectPatternCardAction.OpenMetadataDetail -> onOpenSavedPatternDetail()
                ProjectPatternCardAction.OpenPicker -> onShowPatternPicker()
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
    state: CounterHeroState,
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
        val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
        CounterTargetHelperLabel(
            status = mainCounterTargetStatus(display.targetLine),
            onShowTargetDialog = actions.onShowTargetDialog,
        )
        Spacer(modifier = Modifier.height(heroButtonSpacing))
        CounterButtons(
            state = state,
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
            onUndo = actions.onUndo,
        )
        if (state.visibleStitchTotal != null) {
            Spacer(modifier = Modifier.height(controlsToStitchTrackerSpacing))
            CounterStitchTracker(state = state, actions = actions)
        }
        Spacer(modifier = Modifier.weight(CounterDimens.CounterHeroBottomWeight))
        Spacer(modifier = Modifier.height(CounterDimens.HeroBottomBreathingSpace))
    }
}

@Composable
private fun CounterStitchTracker(
    state: CounterHeroState,
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
    state: CounterHeroState,
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
private fun CounterMainNumber(state: CounterHeroState) {
    val locale = rememberCurrentLocale()
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val counterDescription =
        display.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(display.heroTitle)
    val countText = formatIntegerForDisplay(state.counter.count.toLong(), locale)
    val baseTextStyle =
        MaterialTheme.typography.displayMedium.copy(
            fontSize = CounterDimens.CounterMainNumberFontSize,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
        )
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val fittedFontSize =
            counterMainNumberFittedFontSize(
                maxFontSize = CounterDimens.CounterMainNumberFontSize,
                minFontSize = CounterDimens.CounterMainNumberMinimumFontSize,
                maxWidthPx = constraints.maxWidth,
                measureWidth = { candidateFontSize ->
                    textMeasurer
                        .measure(
                            text = countText,
                            style = baseTextStyle.copy(fontSize = candidateFontSize),
                            softWrap = false,
                            maxLines = 1,
                        ).size.width
                },
            )
        RollingCounter(
            count = state.counter.count,
            textStyle = baseTextStyle.copy(fontSize = fittedFontSize),
            contentDescription = counterDescription,
        )
    }
}

internal fun counterMainNumberFittedFontSize(
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxWidthPx: Int,
    measureWidth: (TextUnit) -> Int,
): TextUnit {
    if (maxWidthPx == Constraints.Infinity) {
        return maxFontSize
    }
    if (maxWidthPx <= 0) {
        return minFontSize
    }
    if (measureWidth(maxFontSize) <= maxWidthPx) {
        return maxFontSize
    }
    if (measureWidth(minFontSize) > maxWidthPx) {
        return minFontSize
    }

    var lowFontSize = minFontSize
    var highFontSize = maxFontSize
    repeat(CounterDimens.CounterMainNumberFitIterations) {
        val candidateFontSize = ((lowFontSize.value + highFontSize.value) / 2f).sp
        if (measureWidth(candidateFontSize) <= maxWidthPx) {
            lowFontSize = candidateFontSize
        } else {
            highFontSize = candidateFontSize
        }
    }
    return lowFontSize
}

@Composable
private fun CounterTargetHelperLabel(
    status: MainCounterTargetStatus?,
    onShowTargetDialog: () -> Unit,
) {
    val text =
        when (status) {
            is MainCounterTargetStatus.Remaining ->
                stringResource(
                    R.string.counter_target_remaining_format,
                    mainCounterProjectCardCountText(status.countSlot),
                )
            MainCounterTargetStatus.Reached -> stringResource(R.string.counter_target_reached)
            is MainCounterTargetStatus.Past ->
                stringResource(
                    R.string.counter_target_past_format,
                    mainCounterProjectCardCountText(status.countSlot),
                )

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
    state: CounterHeroState,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
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
            visualOffsetY = CounterDimens.CounterMinusOpticalOffsetY,
            onClick = onDecrement,
            modifier =
                Modifier
                    .size(CounterDimens.CounterMinusTouchSize)
                    .align(Alignment.TopStart),
        )
        CounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            contentDescription = mainCounterIncreaseContentDescription(display.increaseContentDescription),
            visualSize = CounterDimens.CounterPrimaryVisualSize,
            onClick = onIncrement,
            modifier =
                Modifier
                    .size(CounterDimens.CounterPrimaryTouchSize)
                    .align(Alignment.TopEnd),
        )
        CounterImageButton(
            imageRes = R.drawable.counter_undo_button,
            contentDescription = stringResource(R.string.counter_undo_last_change),
            visualSize = CounterDimens.CounterUndoVisualSize,
            onClick = onUndo,
            modifier =
                Modifier
                    .size(CounterDimens.CounterUndoTouchSize)
                    .align(Alignment.BottomCenter),
        )
    }
}

private fun CounterHeroState.toMainCounterProject(): CounterProject =
    CounterProject(
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
    val locale = rememberCurrentLocale()
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
            text = stringResource(R.string.counter_repeat_label).localizedUppercase(),
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
                text = formatIntegerForDisplay(count.toLong(), locale),
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
            appearance =
                CounterStepButtonFaceAppearance(
                    visualSize = CounterDimens.CounterRepeatButtonVisualSize,
                    symbolSize = CounterDimens.CounterRepeatIconSize,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = contentColor,
                ),
            enabled = enabled,
        )
    }
}

@Composable
private fun WorkspaceSectionTitle(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    proStatus: ProStatus,
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
            proStatus = proStatus,
        )
    }
}

@Composable
private fun WorkspaceSectionAction(
    label: String,
    onClick: () -> Unit,
    proStatus: ProStatus,
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(CounterDimens.WorkspaceSectionActionIconSize),
        )
        Spacer(modifier = Modifier.width(CounterDimens.WorkspaceSectionActionIconSpacing))
        Text(label)
        Spacer(modifier = Modifier.width(CounterDimens.WorkspaceSectionActionIconSpacing))
        ProBadge(status = proStatus)
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
                ),
        )
    }
}
