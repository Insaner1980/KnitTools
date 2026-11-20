package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MainCounterLabelSlot
import com.finnvek.knittools.domain.calculator.MainCounterTargetSlot
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.CounterStepButtonFace
import com.finnvek.knittools.ui.components.CounterStepButtonFaceAppearance
import com.finnvek.knittools.ui.components.CounterStepSymbol
import com.finnvek.knittools.ui.components.MainCounterTargetStatus
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ProjectProgressBar
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.StitchCounter
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterDecreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterIncreaseContentDescription
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.mainCounterProjectCardCountText
import com.finnvek.knittools.ui.components.mainCounterStepTitle
import com.finnvek.knittools.ui.components.mainCounterTargetFraction
import com.finnvek.knittools.ui.components.mainCounterTargetStatus
import com.finnvek.knittools.ui.components.mainCounterTargetText
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.components.workSessionDurationText
import com.finnvek.knittools.ui.layout.AdaptiveContent
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.layout.contentWidth
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.tips.tipAnchor
import com.finnvek.knittools.ui.tips.tipProtected

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
    val onShowStepSize: () -> Unit = {},
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
    val canUndo: Boolean = false,
    val activeSession: CounterHeroSessionState? = null,
)

internal data class CounterHeroSessionState(
    val durationSeconds: Long,
    val needsRecoveryReview: Boolean,
    val projectName: String,
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
        canUndo = canUndo,
        activeSession =
            activeSession?.takeIf { it.projectId == projectId }?.let {
                CounterHeroSessionState(sessionSeconds, it.needsRecoveryReview, projectName)
            },
    )

@Composable
fun CounterWorkspace(
    scaffoldPadding: PaddingValues,
    state: CounterUiState,
    projectCountersActions: ProjectCountersSectionActions,
    actions: CounterWorkspaceActions,
    stepDelta: Int? = null,
    onBlockingCounterChanged: (Long, Boolean) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()
    // Wait for the saved list position's items before allowing the first measurement.
    if (state.projectId == null || state.countersLoadedProjectId != state.projectId) return

    Box(Modifier.fillMaxSize().padding(scaffoldPadding).consumeWindowInsets(scaffoldPadding), Alignment.TopCenter) {
        AdaptiveContent(Modifier.contentWidth(ContentWidthPolicy.Form).fillMaxSize()) {
            val heroState = state.toCounterHeroState()
            val heroLayout = rememberCounterHeroLayout(heroState, LocalAdaptiveContentSize.current)
            val sideBySide = heroLayout.arrangement == CounterHeroArrangement.SideBySide
            val roomyHero = heroLayout.arrangement == CounterHeroArrangement.Stacked
            CompositionLocalProvider(LocalCounterHeroMetrics provides heroLayout.metrics) {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
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
                        if (sideBySide) {
                            ShortCounterHero(heroState, stepDelta, actions, heroLayout)
                        } else {
                            CounterHero(
                                state = heroState,
                                stepDelta = stepDelta,
                                actions = actions,
                                layout = heroLayout,
                            )
                        }
                    }
                    // Keep item indices stable when supporting controls move out of the hero.
                    item(key = "counter-hero-reveal-gap") {
                        Column(verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceSectionSpacing)) {
                            when (heroLayout.supportingControlsPlacement) {
                                SupportingControlsPlacement.InHero -> Unit
                                SupportingControlsPlacement.StitchTrackerBelowHero ->
                                    CounterStitchTracker(
                                        heroState,
                                        actions,
                                        modifier = Modifier.widthIn(max = heroLayout.metrics.controlsMaxWidth),
                                    )
                                SupportingControlsPlacement.BelowHero -> CounterSupportingControls(heroState, actions)
                            }
                            Spacer(
                                modifier =
                                    Modifier.height(
                                        if (!roomyHero) {
                                            CounterDimens.WorkspaceGroupSpacing
                                        } else {
                                            CounterDimens.CounterProjectRevealGap
                                        },
                                    ),
                            )
                        }
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
                                proState = state.proState,
                                proAccessReady = state.proAccessReady,
                            )
                        }
                        items(items = state.projectCounters, key = { counter -> counter.id }) { counter ->
                            ProjectCounterWorkspaceItem(
                                counter = counter,
                                mainRowCount = state.counter.count,
                                actions = projectCountersActions,
                                onBlockingSurfaceChanged = { onBlockingCounterChanged(counter.id, it) },
                            )
                        }
                    }
                    if (state.activeAlert != null) {
                        state.activeAlert.let { reminder ->
                            item(key = "active-reminder-alert") {
                                ReminderAlertCard(
                                    reminder = reminder,
                                    currentRow = state.counter.count,
                                    label =
                                        MainCounterLabelSlot(
                                            state.mainCounterLabelType,
                                            state.mainCounterCustomLabel,
                                        ),
                                    onDismiss = actions.onDismissReminder,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkSessionStrip(
    label: String,
    durationText: String,
    actionLabel: String,
    onAction: () -> Unit,
    contentDescription: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    forceInline: Boolean = false,
    shortLabel: String = label,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val stackContent = !forceInline && shouldStackWorkSessionStrip(maxWidth, density.fontScale)
        val labelStyle = MaterialTheme.typography.labelLarge
        val durationStyle = MaterialTheme.typography.bodyMedium
        val measurer = rememberTextMeasurer()
        val labelFits =
            with(density) {
                val buttonPadding = ButtonDefaults.TextButtonContentPadding
                val actionWidth =
                    measurer
                        .measure(actionLabel, labelStyle, maxLines = 1)
                        .size.width
                        .toDp() +
                        buttonPadding.calculateLeftPadding(LayoutDirection.Ltr) +
                        buttonPadding.calculateRightPadding(LayoutDirection.Ltr)
                val durationWidth =
                    measurer
                        .measure(durationText, durationStyle, maxLines = 1)
                        .size.width
                        .toDp()
                val labelWidth =
                    measurer
                        .measure(label, labelStyle, maxLines = 1)
                        .size.width
                        .toDp()
                val inlineDurationWidth =
                    if (stackContent) 0.dp else durationWidth + CounterDimens.WorkSessionStripInlineSpacing
                labelWidth + inlineDurationWidth + maxOf(actionWidth, CounterDimens.WorkSessionStripMinHeight) <=
                    maxWidth
            }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = CounterDimens.WorkSessionStripMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val labelContent: @Composable (Modifier) -> Unit = { modifier ->
                Text(
                    text = if (labelFits) label else shortLabel,
                    modifier = modifier,
                    style = labelStyle,
                    color = labelColor,
                    maxLines = 1,
                )
            }
            val durationContent: @Composable () -> Unit = {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val statusModifier =
                Modifier.weight(1f).semantics(mergeDescendants = false) {
                    this.contentDescription = contentDescription
                }
            if (stackContent) {
                Column(statusModifier) {
                    labelContent(Modifier)
                    durationContent()
                }
            } else {
                Row(
                    modifier = statusModifier,
                    horizontalArrangement = Arrangement.spacedBy(CounterDimens.WorkSessionStripInlineSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    labelContent(Modifier.weight(1f, fill = false))
                    durationContent()
                }
            }
            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = CounterDimens.WorkSessionStripMinHeight),
            ) {
                Text(actionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun CounterHeroSessionState.label(): String =
    stringResource(
        if (needsRecoveryReview) R.string.work_session_recovery_needed else R.string.work_session_active,
    )

@Composable
internal fun CounterHeroSessionState.actionLabel(): String =
    stringResource(
        if (needsRecoveryReview) R.string.work_session_resolve else R.string.work_session_stop_short,
    )

@Composable
private fun CounterHeroWorkSession(
    session: CounterHeroSessionState,
    actions: CounterWorkspaceActions,
    forceInline: Boolean,
) {
    val duration = workSessionDurationText(session.durationSeconds)
    WorkSessionStrip(
        label = session.label(),
        durationText = duration,
        actionLabel = session.actionLabel(),
        onAction = if (session.needsRecoveryReview) actions.onResolveSessionRecovery else actions.onStopSession,
        contentDescription = stringResource(R.string.work_session_active_description, session.projectName, duration),
        labelColor =
            if (session.needsRecoveryReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        forceInline = forceInline,
        shortLabel = stringResource(R.string.work_session_active_short),
    )
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

        ProjectContentCardKind.YARN -> {
            onOpenYarn()
        }

        ProjectContentCardKind.NOTES -> {
            onOpenNotes()
        }

        ProjectContentCardKind.PHOTOS -> {
            onOpenPhotos()
        }

        ProjectContentCardKind.REMINDER -> {
            onOpenReminders()
        }
    }
}

@Composable
private fun ShortCounterHero(
    state: CounterHeroState,
    stepDelta: Int?,
    actions: CounterWorkspaceActions,
    layout: CounterHeroLayout,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .counterClickWithoutIndication(actions.onSurfaceIncrement),
        horizontalArrangement = Arrangement.spacedBy(CounterDimens.ShortHeroSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val captionHeight = counterHeroCaptionHeight(state, maxWidth)
            val feedbackHeight =
                if (state.counter.stepSize > 1) {
                    val height = rememberTextMeasurer().measure("0", MaterialTheme.typography.titleMedium).size.height
                    with(LocalDensity.current) { height.toDp() }
                } else {
                    0.dp
                }
            val numberHeight =
                (layout.maximumHeight - layout.sessionStripSpace - captionHeight - feedbackHeight)
                    .coerceAtLeast(counterMainNumberMinHeight())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                state.activeSession?.let { CounterHeroWorkSession(it, actions, forceInline = false) }
                CounterRowLabel(state, actions.onShowTargetDialog)
                if (state.counter.stepSize > 1) {
                    CounterStepFeedback(stepDelta, Modifier.align(Alignment.End), maxLines = 1)
                }
                CounterMainNumber(
                    state = state,
                    stepDelta = null,
                    modifier = Modifier.heightIn(max = numberHeight),
                    inlineStepFeedback = false,
                )
            }
        }
        CounterButtons(
            state = state,
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
            onUndo = actions.onUndo,
            onShowStepSize = actions.onShowStepSize,
            undoPlacement = CounterUndoPlacement.BetweenButtons,
            maximumHeight = layout.maximumHeight,
        )
    }
}

@Composable
private fun CounterSupportingControls(
    state: CounterHeroState,
    actions: CounterWorkspaceActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceGroupSpacing),
    ) {
        if (state.canUseSecondaryCounter) {
            CompactPatternRepeatRow(
                state.secondaryCount,
                actions.onDecrementSecondary,
                actions.onIncrementSecondary,
            )
        }
        CounterValueFormatter.forMainCounter(state.toMainCounterProject()).targetLine?.let {
            CounterTargetProgress(it, actions.onShowTargetDialog)
        }
        CounterStitchTracker(state, actions)
    }
}

@Composable
private fun CounterHero(
    state: CounterHeroState,
    stepDelta: Int?,
    actions: CounterWorkspaceActions,
    layout: CounterHeroLayout,
) {
    val useCompactStitchLayout =
        layout.arrangement == CounterHeroArrangement.StackedCompact ||
            (state.canUseSecondaryCounter && state.visibleStitchTotal != null)
    val showRepeatAndTarget = layout.supportingControlsPlacement != SupportingControlsPlacement.BelowHero
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

    Column(Modifier.widthIn(max = LocalCounterHeroMetrics.current.controlsMaxWidth).fillMaxWidth()) {
        val session = state.activeSession
        if (session == null) {
            Spacer(Modifier.height(layout.sessionStripSpace))
        } else {
            Box(Modifier.heightIn(min = layout.sessionStripSpace), contentAlignment = Alignment.Center) {
                CounterHeroWorkSession(session, actions, layout.forceInlineSessionStrip)
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = layout.minimumBodyHeight)
                    .counterClickWithoutIndication(actions.onSurfaceIncrement),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            if (showRepeatAndTarget) {
                Spacer(modifier = Modifier.weight(CounterDimens.CounterHeroTopWeight))
            }
            if (showRepeatAndTarget && state.canUseSecondaryCounter) {
                CompactPatternRepeatRow(
                    count = state.secondaryCount,
                    onDecrement = actions.onDecrementSecondary,
                    onIncrement = actions.onIncrementSecondary,
                )
                Spacer(modifier = Modifier.height(CounterDimens.CounterRepeatToRowSpacing))
            }
            CounterRowLabel(state = state, onShowTargetDialog = actions.onShowTargetDialog)
            CounterMainNumber(
                state = state,
                stepDelta = stepDelta,
                modifier = Modifier.heightIn(max = layout.numberMaximumHeight),
            )
            if (showRepeatAndTarget) {
                val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
                display.targetLine?.let { targetLine ->
                    CounterTargetProgress(targetLine = targetLine, onShowTargetDialog = actions.onShowTargetDialog)
                }
            }
            Spacer(modifier = Modifier.height(heroButtonSpacing))
            CounterButtons(
                state = state,
                onDecrement = actions.onDecrement,
                onIncrement = actions.onIncrement,
                onUndo = actions.onUndo,
                onShowStepSize = actions.onShowStepSize,
            )
            if (layout.supportingControlsPlacement == SupportingControlsPlacement.InHero &&
                state.visibleStitchTotal != null
            ) {
                Spacer(modifier = Modifier.height(controlsToStitchTrackerSpacing))
                CounterStitchTracker(state = state, actions = actions)
            }
            Spacer(modifier = Modifier.weight(CounterDimens.CounterHeroBottomWeight))
            Spacer(modifier = Modifier.height(CounterDimens.HeroBottomBreathingSpace))
        }
    }
}

@Composable
private fun CounterStitchTracker(
    state: CounterHeroState,
    actions: CounterWorkspaceActions,
    modifier: Modifier = Modifier,
) {
    val totalStitches = state.visibleStitchTotal ?: return

    StitchCounter(
        label = stringResource(R.string.stitch_tracker_label),
        currentStitch = state.currentStitch.coerceIn(0, totalStitches),
        totalStitches = totalStitches,
        onIncrement = actions.onIncrementStitch,
        onDecrement = actions.onDecrementStitch,
        modifier = modifier.testTag("counter-stitch-tracker"),
    )
}

/**
 * Luvun yläpuolella on vain yksikkö ("Rows", "Rounds" tai oma nimike): "Row 34 / 72" toisti saman
 * luvun, joka näkyy heti alla isona. Ruudunlukija saa koko lukeman ison luvun kuvauksesta.
 */
@Composable
private fun CounterRowLabel(
    state: CounterHeroState,
    onShowTargetDialog: () -> Unit,
) {
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    Text(
        text = mainCounterLabelText(display.heroTitle.labelType, display.heroTitle.customLabel),
        style = counterCaptionStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onShowTargetDialog),
    )
}

/** Laskurin pienet tekstit: yksikkö luvun yllä ja tavoiterivi palkin alla. */
@Composable
internal fun counterCaptionStyle(): TextStyle =
    MaterialTheme.typography.labelMedium.copy(fontSize = CounterDimens.CounterTargetHelperFontSize)

/** Pääluvun tyyli; koko tulee `CounterHeroMetrics`ista (115 sp, tabletilla isompi). */
@Composable
internal fun counterMainNumberStyle(fontSize: TextUnit): TextStyle =
    MaterialTheme.typography.displayMedium.copy(
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    )

@Composable
private fun CounterMainNumber(
    state: CounterHeroState,
    stepDelta: Int?,
    modifier: Modifier = Modifier,
    inlineStepFeedback: Boolean = true,
) {
    val locale = rememberCurrentLocale()
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val counterDescription =
        display.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(display.heroTitle)
    val countText = formatIntegerForDisplay(state.counter.count.toLong(), locale)
    val feedbackSpace =
        if (state.counter.stepSize > 1 && inlineStepFeedback) {
            CounterDimens.StepFeedbackWidth
        } else {
            CounterDimens.StepRingButtonGap * 0
        }
    val density = LocalDensity.current
    val maxFontSize = LocalCounterHeroMetrics.current.numberFontSize
    val baseTextStyle = counterMainNumberStyle(maxFontSize)
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val fittedFontSize =
            textMeasurer.fitCounterNumber(
                text = countText,
                style = baseTextStyle,
                maxWidthPx = (constraints.maxWidth - with(density) { feedbackSpace.roundToPx() } * 2).coerceAtLeast(0),
                maxHeightPx = constraints.maxHeight,
            )
        RollingCounter(
            count = state.counter.count,
            textStyle = baseTextStyle.copy(fontSize = fittedFontSize),
            contentDescription = counterDescription,
            modifier = Modifier.testTag("main-counter-value").tipAnchor(TipId.TARGET_REACHED),
        )
        if (stepDelta != null && state.counter.stepSize > 1 && inlineStepFeedback) {
            CounterStepFeedback(stepDelta, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun CounterStepFeedback(
    stepDelta: Int?,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = stepDelta?.let { stringResource(R.string.counter_step_delta, it) }.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.knitToolsColors.primaryReadable,
        modifier =
            modifier
                .width(CounterDimens.StepFeedbackWidth)
                .then(if (stepDelta != null) Modifier.testTag("main-counter-step-feedback") else Modifier)
                .clearAndSetSemantics {},
        textAlign = TextAlign.Center,
        minLines = 1,
        maxLines = maxLines,
    )
}

internal fun TextMeasurer.fitCounterNumber(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
    maxHeightPx: Int = Constraints.Infinity,
): TextUnit =
    counterMainNumberFittedFontSize(style.fontSize, CounterDimens.CounterMainNumberMinimumFontSize) { candidate ->
        val candidateStyle = style.copy(fontSize = candidate)
        var width = 0
        // RollingCounter measures each character separately, including grouping spaces.
        text.all { character ->
            val size = measure(character.toString(), candidateStyle, softWrap = false, maxLines = 1).size
            width += size.width
            width <= maxWidthPx && size.height <= maxHeightPx
        }
    }

internal fun counterMainNumberFittedFontSize(
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxWidthPx: Int,
    measureWidth: (TextUnit) -> Int,
): TextUnit =
    when {
        maxWidthPx == Constraints.Infinity -> maxFontSize
        maxWidthPx <= 0 -> minFontSize
        else -> counterMainNumberFittedFontSize(maxFontSize, minFontSize) { measureWidth(it) <= maxWidthPx }
    }

internal fun counterMainNumberFittedFontSize(
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fits: (TextUnit) -> Boolean,
): TextUnit {
    if (fits(maxFontSize)) {
        return maxFontSize
    }
    // Keep the preferred minimum when possible, but never truncate a valid counter value.
    val minimumFits = fits(minFontSize)
    var lowFontSize = if (minimumFits) minFontSize else 0.sp
    var highFontSize = if (minimumFits) maxFontSize else minFontSize
    repeat(CounterDimens.CounterMainNumberFitIterations) {
        val candidateFontSize = ((lowFontSize.value + highFontSize.value) / 2f).sp
        if (fits(candidateFontSize)) {
            lowFontSize = candidateFontSize
        } else {
            highFontSize = candidateFontSize
        }
    }
    return lowFontSize
}

/**
 * Tavoitteen edistyminen samalla palkilla kuin jatka-kortissa: tavoite vasemmalla ja jäljellä oleva
 * määrä oikealla. Napautus avaa tavoitteen muokkauksen kuten aiempi tavoiterivi.
 */
@Composable
private fun CounterTargetProgress(
    targetLine: MainCounterTargetSlot,
    onShowTargetDialog: () -> Unit,
) {
    val fraction = mainCounterTargetFraction(targetLine) ?: return
    val statusText = mainCounterTargetStatus(targetLine)?.let { counterTargetStatusText(it) } ?: return
    Column(
        modifier =
            Modifier
                .padding(top = CounterDimens.CounterTargetHelperSpacing)
                .widthIn(max = LocalCounterHeroMetrics.current.controlsMaxWidth)
                .clickable(onClick = onShowTargetDialog),
    ) {
        ProjectProgressBar(fraction)
        // Tila saa jäljelle jäävän leveyden ja rivittyy siinä: kapeassa rivissä suomenkielinen "Tavoite
        // saavutettu" painui tavoitteen päälle.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = CounterDimens.CounterTargetHelperSpacing),
            horizontalArrangement = Arrangement.spacedBy(CounterDimens.CounterTargetHelperSpacing),
        ) {
            Text(
                text = stringResource(R.string.counter_target_of_format, targetLine.target),
                style = counterCaptionStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = statusText,
                style = counterCaptionStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Laskurissa jäljellä oleva määrä yksikön kanssa ("38 rows left"), toisin kuin listan lyhyt muoto. */
@Composable
internal fun counterTargetStatusText(status: MainCounterTargetStatus): String =
    when (status) {
        is MainCounterTargetStatus.Remaining -> {
            stringResource(
                R.string.counter_target_remaining_format,
                mainCounterProjectCardCountText(status.countSlot),
            )
        }

        MainCounterTargetStatus.Reached -> {
            stringResource(R.string.counter_target_reached)
        }

        is MainCounterTargetStatus.Past -> {
            stringResource(
                R.string.counter_target_past_format,
                mainCounterProjectCardCountText(status.countSlot),
            )
        }
    }

/** Kumoa on laskiessa päätoiminto, joten se pysyy näkyvissä pää­nappien kanssa kaikissa asetteluissa. */
private enum class CounterUndoPlacement {
    /** Pinotussa laskurissa nappien alla keskellä. */
    Below,

    /** Matalassa vierekkäisessä laskurissa askelrenkaan alla miinuksen ja plussan välissä. */
    BetweenButtons,
}

@Composable
private fun CounterButtons(
    state: CounterHeroState,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
    onShowStepSize: () -> Unit,
    undoPlacement: CounterUndoPlacement = CounterUndoPlacement.Below,
    maximumHeight: Dp = Dp.Unspecified,
) {
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val metrics = LocalCounterHeroMetrics.current
    val undoBetween = undoPlacement == CounterUndoPlacement.BetweenButtons
    val primaryHeight =
        if (maximumHeight.isSpecified) {
            minOf(
                maximumHeight,
                metrics.primaryTouchSize,
            )
        } else {
            metrics.primaryTouchSize
        }

    val ringText = stringResource(R.string.counter_step_multiplier, state.counter.stepSize)
    val textStyle = MaterialTheme.typography.labelLarge
    val measured = rememberTextMeasurer().measure(ringText, textStyle, maxLines = 1, softWrap = false)
    val minimumActionSize = CounterDimens.StepRingMinimumSize
    val ringSize =
        with(LocalDensity.current) { measured.size.width.toDp() + CounterDimens.StepRingTextPadding * 2 }
            .coerceIn(minimumActionSize, minOf(primaryHeight, CounterDimens.StepRingMaximumSize))
    val undoGap = CounterDimens.ShortHeroUndoGap
    val inlineUndo = undoBetween && primaryHeight < ringSize + undoGap + minimumActionSize
    val betweenUndoSize =
        (if (inlineUndo) primaryHeight else primaryHeight - ringSize - undoGap)
            .coerceIn(minimumActionSize, CounterDimens.ShortHeroUndoSize)
    val centerWidth =
        when {
            inlineUndo -> ringSize + undoGap + betweenUndoSize
            undoBetween -> maxOf(ringSize, betweenUndoSize)
            else -> ringSize
        }
    val centerHeight = if (undoBetween) ringSize + undoGap + betweenUndoSize else ringSize
    val centerTop = ((primaryHeight - centerHeight) / 2).coerceAtLeast(0.dp)
    val maxControlsWidth =
        if (undoBetween) {
            metrics.primaryTouchSize * 2 + centerWidth + CounterDimens.StepRingButtonGap * 2
        } else {
            metrics.controlsMaxWidth
        }
    BoxWithConstraints(
        modifier =
            Modifier
                .widthIn(max = maxControlsWidth)
                .fillMaxWidth()
                .height(if (undoBetween) primaryHeight else metrics.controlsHeight),
    ) {
        val buttonWidth =
            ((maxWidth - centerWidth - CounterDimens.StepRingButtonGap * 2) / 2)
                .coerceAtMost(metrics.primaryTouchSize)
        CounterImageButton(
            imageRes = R.drawable.counter_minus_button,
            contentDescription =
                mainCounterDecreaseContentDescription(
                    display.decreaseContentDescription,
                    state.counter.stepSize,
                ),
            visualSize = metrics.minusVisualSize,
            visualOffsetY = CounterDimens.CounterMinusOpticalOffsetY,
            onClick = onDecrement,
            modifier =
                Modifier
                    .width(buttonWidth)
                    .size(metrics.minusTouchSize)
                    .align(Alignment.TopStart)
                    .tipProtected(),
        )
        CounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            contentDescription =
                mainCounterIncreaseContentDescription(
                    display.increaseContentDescription,
                    state.counter.stepSize,
                ),
            visualSize = metrics.primaryVisualSize,
            onClick = onIncrement,
            modifier =
                Modifier
                    .width(buttonWidth)
                    .size(metrics.primaryTouchSize)
                    .align(Alignment.TopEnd)
                    .tipAnchor(TipId.FIRST_ROW)
                    .tipProtected(),
        )
        MainCounterStepButton(
            stepSize = state.counter.stepSize,
            text = ringText,
            title = mainCounterStepTitle(state.mainCounterLabelType, state.mainCounterCustomLabel),
            size = ringSize,
            onClick = onShowStepSize,
            modifier =
                if (inlineUndo) {
                    Modifier.align(Alignment.Center).offset(x = -(betweenUndoSize + undoGap) / 2)
                } else {
                    Modifier.align(Alignment.TopCenter).padding(top = centerTop)
                },
        )
        val undoModifier =
            when {
                inlineUndo ->
                    Modifier
                        .align(
                            Alignment.Center,
                        ).offset(x = (ringSize + undoGap) / 2)
                        .size(betweenUndoSize)
                undoBetween ->
                    Modifier
                        .align(
                            Alignment.TopCenter,
                        ).padding(top = centerTop + ringSize + undoGap)
                        .size(betweenUndoSize)
                else -> Modifier.align(Alignment.BottomCenter).size(metrics.undoSize)
            }
        CounterImageButton(
            imageRes = R.drawable.counter_undo_button,
            contentDescription = stringResource(R.string.counter_undo_last_change),
            visualSize = if (undoBetween) betweenUndoSize else metrics.undoSize,
            onClick = onUndo,
            enabled = state.canUndo,
            modifier = (if (state.canUndo) undoModifier.tipAnchor(TipId.UNDO) else undoModifier).tipProtected(),
        )
    }
}

@Composable
private fun MainCounterStepButton(
    stepSize: Int,
    text: String,
    title: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.counter_step_change_description, title, stepSize)
    val ringColor = if (stepSize > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (stepSize > 1) MaterialTheme.knitToolsColors.primaryReadable else ringColor
    Box(
        modifier =
            modifier
                .size(size)
                .testTag("main-counter-step")
                .tipAnchor(TipId.STEP_SIZE)
                .tipProtected()
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CounterStepButtonFace(
            appearance =
                CounterStepButtonFaceAppearance(
                    visualSize = size,
                    symbolSize = CounterDimens.CounterRepeatIconSize,
                    containerColor = Color.Transparent,
                    contentColor = ringColor,
                ),
        ) {
            BasicText(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = textColor, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(),
                modifier = Modifier.padding(CounterDimens.StepRingTextPadding).clearAndSetSemantics {},
            )
        }
    }
}

internal fun CounterHeroState.toMainCounterProject(): CounterProject =
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
    FlowRow(
        modifier =
            Modifier
                .widthIn(
                    min = CounterDimens.CounterRepeatMinWidth,
                    max = CounterDimens.CounterRepeatMaxWidth,
                ).heightIn(min = CounterDimens.CounterRepeatHeight)
                .clip(RoundedCornerShape(CounterDimens.CounterRepeatCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = CounterDimens.CounterRepeatHorizontalPadding),
        itemVerticalAlignment = Alignment.CenterVertically,
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
    proState: ProState,
    proAccessReady: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionLabel(text = title)
        WorkspaceSectionAction(
            label = actionLabel,
            onClick = onAction,
            proState = proState,
            proAccessReady = proAccessReady,
        )
    }
}

@Composable
private fun WorkspaceSectionAction(
    label: String,
    onClick: () -> Unit,
    proState: ProState,
    proAccessReady: Boolean,
) {
    val counterAccess = proActionAccess(proState, proAccessReady, ProFeature.MULTIPLE_COUNTERS)
    TextButton(
        onClick = onClick,
        enabled = counterAccess != ProActionAccess.Unknown || proState.hasFeature(ProFeature.MULTIPLE_COUNTERS),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(CounterDimens.WorkspaceSectionActionIconSize),
        )
        Spacer(modifier = Modifier.width(CounterDimens.WorkspaceSectionActionIconSpacing))
        Text(label)
        Spacer(modifier = Modifier.width(CounterDimens.WorkspaceSectionActionIconSpacing))
        ProMarker(access = counterAccess)
    }
}

@Composable
internal fun ProjectCounterWorkspaceItem(
    counter: ProjectCounter,
    mainRowCount: Int,
    actions: ProjectCountersSectionActions,
    onBlockingSurfaceChanged: (Boolean) -> Unit = {},
) {
    if (counter.counterType == ProjectCounterType.REPEAT_SECTION) {
        RepeatSectionItem(
            counter = counter,
            mainRowCount = mainRowCount,
            onDelete = { actions.onDeleteCounter(counter.id) },
            onBlockingSurfaceChanged = onBlockingSurfaceChanged,
        )
    } else {
        CounterListItem(
            counter = counter,
            onBlockingSurfaceChanged = onBlockingSurfaceChanged,
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
