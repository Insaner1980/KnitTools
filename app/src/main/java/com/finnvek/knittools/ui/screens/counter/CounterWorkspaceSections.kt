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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
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
import com.finnvek.knittools.ui.layout.AdaptiveContent
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.layout.contentWidth
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.tips.tipAnchor
import com.finnvek.knittools.ui.tips.tipProtected
import java.util.Locale

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
    Box(Modifier.fillMaxSize().padding(scaffoldPadding).consumeWindowInsets(scaffoldPadding), Alignment.TopCenter) {
        AdaptiveContent(Modifier.contentWidth(ContentWidthPolicy.Form).fillMaxSize()) {
            val heroLayout = rememberCounterHeroLayout(state.toCounterHeroState(), LocalAdaptiveContentSize.current)
            val sideBySide = heroLayout.arrangement == CounterHeroArrangement.SideBySide
            val roomyHero = heroLayout.arrangement == CounterHeroArrangement.Stacked
            CompositionLocalProvider(LocalCounterHeroMetrics provides heroLayout.metrics) {
                LazyColumn(
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
                            ShortCounterHero(state.toCounterHeroState(), stepDelta, actions, heroLayout.maximumHeight)
                        } else {
                            CounterHero(
                                state = state.toCounterHeroState(),
                                stepDelta = stepDelta,
                                actions = actions,
                                compactHeight = !roomyHero,
                                modifier = Modifier.heightIn(min = heroLayout.minimumHeight),
                            )
                        }
                    }
                    if (sideBySide) {
                        item(key = "counter-supporting-controls") {
                            ShortCounterSupportingControls(state.toCounterHeroState(), actions)
                        }
                    }
                    state.activeSession?.takeIf { it.projectId == state.projectId }?.let { activeSession ->
                        val rowsWorked =
                            (activeSession.trustedRowsWorked.toLong() + activeSession.pendingRowsWorked.toLong())
                                .coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        item(key = "active-work-session") {
                            ActiveWorkSessionRow(
                                projectName = state.projectName,
                                durationSeconds = state.sessionSeconds,
                                rowsWorked = rowsWorked,
                                needsRecoveryReview = activeSession.needsRecoveryReview,
                                onStop = actions.onStopSession,
                                onResolve = actions.onResolveSessionRecovery,
                            )
                        }
                    }
                    item(key = "counter-hero-reveal-gap") {
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
    val rowsText = stringResource(R.string.counter_progress_format, rowsWorked)
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
        val stackContent = maxWidth < CounterDimens.WorkSessionStackBreakpoint || fontScale >= FontScaleThresholds.LARGE
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
    maxHeight: Dp,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .counterClickWithoutIndication(actions.onSurfaceIncrement),
        horizontalArrangement = Arrangement.spacedBy(CounterDimens.ShortHeroSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            CounterRowLabel(state, actions.onShowTargetDialog)
            // Luku saa rivistä sen korkeuden, joka nimikkeeltä jää; 200 %:n fontilla se muuten leikkautui.
            CounterMainNumber(state, stepDelta, feedbackAtTop = true, modifier = Modifier.weight(1f, fill = false))
        }
        CounterButtons(
            state = state,
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
            onUndo = actions.onUndo,
            onShowStepSize = actions.onShowStepSize,
            undoPlacement = CounterUndoPlacement.BetweenButtons,
        )
    }
}

@Composable
private fun ShortCounterSupportingControls(
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
    compactHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val useCompactStitchLayout = compactHeight || (state.canUseSecondaryCounter && state.visibleStitchTotal != null)
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
                .widthIn(max = LocalCounterHeroMetrics.current.controlsMaxWidth)
                .fillMaxWidth()
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
        CounterMainNumber(state = state, stepDelta = stepDelta)
        val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
        display.targetLine?.let { targetLine ->
            CounterTargetProgress(targetLine = targetLine, onShowTargetDialog = actions.onShowTargetDialog)
        }
        Spacer(modifier = Modifier.height(heroButtonSpacing))
        CounterButtons(
            state = state,
            onDecrement = actions.onDecrement,
            onIncrement = actions.onIncrement,
            onUndo = actions.onUndo,
            onShowStepSize = actions.onShowStepSize,
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
    feedbackAtTop: Boolean = false,
) {
    val locale = rememberCurrentLocale()
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val counterDescription =
        display.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(display.heroTitle)
    val countText = formatIntegerForDisplay(state.counter.count.toLong(), locale)
    val feedbackSpace =
        if (state.counter.stepSize > 1 && !feedbackAtTop) {
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
        if (stepDelta != null && state.counter.stepSize > 1) {
            Text(
                text = stringResource(R.string.counter_step_delta, stepDelta),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.knitToolsColors.primaryReadable,
                modifier =
                    Modifier
                        .align(if (feedbackAtTop) Alignment.TopEnd else Alignment.CenterEnd)
                        .width(CounterDimens.StepFeedbackWidth)
                        .testTag("main-counter-step-feedback")
                        .clearAndSetSemantics {},
                textAlign = TextAlign.Center,
            )
        }
    }
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
) {
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val metrics = LocalCounterHeroMetrics.current

    val ringText = stringResource(R.string.counter_step_multiplier, state.counter.stepSize)
    val textStyle = MaterialTheme.typography.labelLarge
    val measured = rememberTextMeasurer().measure(ringText, textStyle, maxLines = 1, softWrap = false)
    val ringSize =
        with(LocalDensity.current) { measured.size.width.toDp() + CounterDimens.StepRingTextPadding * 2 }
            .coerceIn(CounterDimens.StepRingMinimumSize, CounterDimens.StepRingMaximumSize)
    val undoBetween = undoPlacement == CounterUndoPlacement.BetweenButtons
    val betweenUndoSize =
        (metrics.primaryTouchSize - ringSize - CounterDimens.ShortHeroUndoGap)
            .coerceIn(CounterDimens.StepRingMinimumSize, CounterDimens.ShortHeroUndoSize)
    val centerColumnWidth = if (undoBetween) maxOf(ringSize, betweenUndoSize) else ringSize
    val centerColumnHeight = if (undoBetween) ringSize + CounterDimens.ShortHeroUndoGap + betweenUndoSize else ringSize
    val centerColumnTop = ((metrics.primaryTouchSize - centerColumnHeight) / 2).coerceAtLeast(0.dp)
    val maxControlsWidth =
        if (undoBetween) {
            metrics.primaryTouchSize * 2 + centerColumnWidth + CounterDimens.StepRingButtonGap * 2
        } else {
            metrics.controlsMaxWidth
        }
    BoxWithConstraints(
        modifier =
            Modifier
                .widthIn(max = maxControlsWidth)
                .fillMaxWidth()
                .height(if (undoBetween) metrics.primaryTouchSize else metrics.controlsHeight),
    ) {
        val buttonWidth =
            ((maxWidth - centerColumnWidth - CounterDimens.StepRingButtonGap * 2) / 2)
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
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = centerColumnTop),
        )
        val undoModifier =
            if (undoBetween) {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = centerColumnTop + ringSize + CounterDimens.ShortHeroUndoGap)
                    .size(betweenUndoSize)
            } else {
                Modifier.align(Alignment.BottomCenter).size(metrics.undoSize)
            }
        CounterImageButton(
            imageRes = R.drawable.counter_undo_button,
            contentDescription = stringResource(R.string.counter_undo_last_change),
            visualSize = if (undoBetween) betweenUndoSize else metrics.undoSize,
            onClick = onUndo,
            enabled = state.canUndo,
            modifier = undoModifier.tipAnchor(TipId.UNDO).tipProtected(),
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
