package com.finnvek.knittools.ui.screens.counter

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MainCounterTargetSlot
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.MainCounterTargetStatus
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.mainCounterTargetStatus
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.layout.AdaptiveLayoutSize
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.ProjectListDimens

/** Pääluvun ja -nappien mitat. Iso variantti skaalaa samat mitat tabletin telinekäyttöön. */
@Immutable
internal data class CounterHeroMetrics(
    val numberFontSize: TextUnit,
    val primaryTouchSize: Dp,
    val primaryVisualSize: Dp,
    val minusTouchSize: Dp,
    val minusVisualSize: Dp,
    val undoSize: Dp,
    val controlsMaxWidth: Dp,
) {
    val controlsHeight: Dp get() = primaryTouchSize + CounterDimens.CounterUndoVerticalSpacing + undoSize

    companion object {
        val Regular = scaled(1f)
        val Large = scaled(CounterDimens.LargeHeroScale)

        private fun scaled(scale: Float) =
            CounterHeroMetrics(
                numberFontSize = CounterDimens.CounterMainNumberFontSize * scale,
                primaryTouchSize = CounterDimens.CounterPrimaryTouchSize * scale,
                primaryVisualSize = CounterDimens.CounterPrimaryVisualSize * scale,
                minusTouchSize = CounterDimens.CounterMinusTouchSize * scale,
                minusVisualSize = CounterDimens.CounterMinusVisualSize * scale,
                undoSize = CounterDimens.CounterUndoTouchSize * scale,
                controlsMaxWidth = CounterDimens.CounterControlsMaxWidth * scale,
            )
    }
}

internal val LocalCounterHeroMetrics = staticCompositionLocalOf { CounterHeroMetrics.Regular }

internal enum class CounterHeroArrangement {
    /** Pinottu laskuri väljillä väleillä; mahtuu kokonaan. */
    Stacked,

    /** Pinottu laskuri tiiviillä väleillä; mahtuu kokonaan tai kapeassa ikkunassa vierittyy. */
    StackedCompact,

    /** Luku ja napit vierekkäin, Kumoa nappien välissä; matala ja leveä ikkuna. */
    SideBySide,
}

internal enum class SupportingControlsPlacement { InHero, StitchTrackerBelowHero, BelowHero }

@Immutable
internal data class CounterHeroLayout(
    val arrangement: CounterHeroArrangement,
    val metrics: CounterHeroMetrics,
    val minimumBodyHeight: Dp,
    val maximumHeight: Dp = Dp.Unspecified,
    val numberMaximumHeight: Dp = Dp.Unspecified,
    val forceInlineSessionStrip: Boolean = false,
    val sessionStripSpace: Dp = 0.dp,
    val supportingControlsPlacement: SupportingControlsPlacement = SupportingControlsPlacement.InHero,
)

/**
 * Asettelu valitaan sen mukaan, mahtuuko pinottu laskuri mitattuun korkeuteen, ei kiinteästä
 * korkeusrajasta: kiinteällä 480 dp:n rajalla 620 dp:n ikkuna leikkasi Kumoa-napin, vaikka
 * 580 dp:n ikkuna näytti kaiken vierekkäisenä.
 */
@Composable
internal fun rememberCounterHeroLayout(
    state: CounterHeroState,
    available: AdaptiveLayoutSize,
): CounterHeroLayout {
    val viewportHeight = available.height - CounterDimens.ContentBottomPadding
    val contentWidth = (available.width - CounterDimens.ScreenHorizontalPadding * 2).coerceAtLeast(0.dp)
    val session = state.activeSession
    val stripWidth = minOf(contentWidth, CounterHeroMetrics.Regular.controlsMaxWidth)
    // Reserve the same strip space before and after Start to keep the counter controls in place.
    val stripHeight = workSessionStripHeight(stripWidth, forceInline = false)
    val bodyViewportHeight = viewportHeight - stripHeight
    val largeCandidate =
        available.width >= CounterDimens.LargeHeroMinWidth && available.height >= CounterDimens.LargeHeroMinHeight
    val largeHeight =
        if (largeCandidate) {
            stackedCounterHeroHeight(
                state,
                CounterHeroMetrics.Large,
                contentWidth,
                compactSpacing = false,
                numberHeight = counterNaturalNumberHeight(state, CounterHeroMetrics.Large, contentWidth),
            )
        } else {
            null
        }
    val useLarge = largeHeight != null && bodyViewportHeight >= largeHeight
    val metrics = if (useLarge) CounterHeroMetrics.Large else CounterHeroMetrics.Regular
    val numberHeight = counterNaturalNumberHeight(state, metrics, contentWidth)
    val regularHeight =
        largeHeight?.takeIf { useLarge }
            ?: stackedCounterHeroHeight(
                state,
                metrics,
                contentWidth,
                compactSpacing = false,
                numberHeight = numberHeight,
            )
    if (bodyViewportHeight >= regularHeight) {
        return CounterHeroLayout(
            arrangement = CounterHeroArrangement.Stacked,
            metrics = metrics,
            minimumBodyHeight = minOf(bodyViewportHeight, regularHeight + CounterDimens.HeroMaxExtraHeight),
            sessionStripSpace = stripHeight,
        )
    }

    val compactHeight =
        stackedCounterHeroHeight(state, metrics, contentWidth, compactSpacing = true, numberHeight = numberHeight)
    if (bodyViewportHeight >= compactHeight) {
        return CounterHeroLayout(
            CounterHeroArrangement.StackedCompact,
            metrics,
            minimumBodyHeight = bodyViewportHeight,
            sessionStripSpace = stripHeight,
        )
    }

    val inlineStripHeight = workSessionStripHeight(stripWidth, forceInline = true)
    val inlineBodyHeight = viewportHeight - inlineStripHeight
    val minimumNumberHeight = counterMainNumberMinHeight()
    val fullCompactNumberSpace = inlineBodyHeight - (compactHeight - numberHeight)
    val omitStitchTracker =
        fullCompactNumberSpace < minimumNumberHeight && LocalDensity.current.fontScale < FontScaleThresholds.LARGE
    val compactNumberSpace =
        if (omitStitchTracker) {
            // The tracker and bottom spacing can scroll below the repeat, target and primary actions.
            val primaryHeight =
                stackedCounterHeroHeight(
                    state,
                    metrics,
                    contentWidth,
                    compactSpacing = true,
                    numberHeight = 0.dp,
                    includeStitchTracker = false,
                ) - CounterDimens.HeroBottomBreathingSpace
            inlineBodyHeight - primaryHeight
        } else {
            fullCompactNumberSpace
        }
    if (compactNumberSpace >= minimumNumberHeight) {
        val stitchTrackerBelowHero = omitStitchTracker && state.visibleStitchTotal != null
        return CounterHeroLayout(
            CounterHeroArrangement.StackedCompact,
            metrics,
            minimumBodyHeight = if (stitchTrackerBelowHero) available.height - inlineStripHeight else inlineBodyHeight,
            numberMaximumHeight = compactNumberSpace,
            forceInlineSessionStrip = true,
            sessionStripSpace = inlineStripHeight,
            supportingControlsPlacement =
                if (stitchTrackerBelowHero) {
                    SupportingControlsPlacement.StitchTrackerBelowHero
                } else {
                    SupportingControlsPlacement.InHero
                },
        )
    }

    if (session == null && viewportHeight >= compactHeight) {
        return CounterHeroLayout(CounterHeroArrangement.StackedCompact, metrics, viewportHeight)
    }

    val primaryNumberSpace =
        if (session != null) {
            inlineBodyHeight - counterHeroCaptionHeight(state, stripWidth) -
                CounterHeroMetrics.Regular.controlsHeight - CounterDimens.HeroButtonCompactSpacing
        } else {
            0.dp
        }
    return when {
        session != null && primaryNumberSpace >= minimumNumberHeight -> {
            CounterHeroLayout(
                CounterHeroArrangement.StackedCompact,
                metrics,
                minimumBodyHeight = 0.dp,
                numberMaximumHeight = primaryNumberSpace,
                forceInlineSessionStrip = true,
                supportingControlsPlacement = SupportingControlsPlacement.BelowHero,
            )
        }

        contentWidth >= CounterDimens.ShortHeroMinWidth -> {
            CounterHeroLayout(
                CounterHeroArrangement.SideBySide,
                CounterHeroMetrics.Regular,
                minimumBodyHeight = 0.dp,
                maximumHeight = available.height.coerceAtLeast(CounterDimens.StepRingMinimumSize),
                sessionStripSpace =
                    if (session == null) 0.dp else workSessionStripHeight(0.dp, forceInline = false),
                supportingControlsPlacement = SupportingControlsPlacement.BelowHero,
            )
        }

        session != null -> {
            CounterHeroLayout(
                CounterHeroArrangement.StackedCompact,
                CounterHeroMetrics.Regular,
                minimumBodyHeight = 0.dp,
                numberMaximumHeight = minimumNumberHeight,
                forceInlineSessionStrip = true,
                supportingControlsPlacement = SupportingControlsPlacement.BelowHero,
            )
        }

        else -> {
            CounterHeroLayout(CounterHeroArrangement.StackedCompact, CounterHeroMetrics.Regular, 0.dp)
        }
    }
}

internal fun shouldStackWorkSessionStrip(
    width: Dp,
    fontScale: Float,
): Boolean = width < CounterDimens.WorkSessionStackBreakpoint || fontScale >= FontScaleThresholds.LARGE

@Composable
private fun workSessionStripHeight(
    width: Dp,
    forceInline: Boolean,
): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val labelHeight =
        measurer.measure(stringResource(R.string.work_session_active), typography.labelLarge, maxLines = 1).size.height
    val durationHeight = measurer.measure("0", typography.bodyMedium, maxLines = 1).size.height
    val statusHeight =
        if (!forceInline && shouldStackWorkSessionStrip(width, density.fontScale)) {
            labelHeight + durationHeight
        } else {
            maxOf(labelHeight, durationHeight)
        }
    val buttonHeight =
        measurer
            .measure(
                stringResource(R.string.work_session_stop_short),
                typography.labelLarge,
                maxLines = 1,
            ).size.height
    val buttonPadding = ButtonDefaults.TextButtonContentPadding
    return with(density) {
        maxOf(
            CounterDimens.WorkSessionStripMinHeight,
            statusHeight.toDp(),
            buttonHeight.toDp() + buttonPadding.calculateTopPadding() + buttonPadding.calculateBottomPadding(),
        )
    }
}

@Composable
internal fun counterMainNumberMinHeight(): Dp {
    val captionHeight = rememberTextMeasurer().measure("0", counterCaptionStyle(), maxLines = 1).size.height
    return maxOf(CounterDimens.MainNumberMinHeight, with(LocalDensity.current) { captionHeight.toDp() * 2 })
}

@Composable
internal fun counterHeroCaptionHeight(
    state: CounterHeroState,
    width: Dp,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val title = CounterValueFormatter.forMainCounter(state.toMainCounterProject()).heroTitle
    val caption = mainCounterLabelText(title.labelType, title.customLabel)
    val style = counterCaptionStyle()
    return with(density) {
        measurer
            .measure(caption, style, constraints = Constraints(maxWidth = width.roundToPx()))
            .size.height
            .toDp()
    }
}

@Composable
private fun counterNaturalNumberHeight(
    state: CounterHeroState,
    metrics: CounterHeroMetrics,
    contentWidth: Dp,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widthPx = with(density) { minOf(contentWidth, metrics.controlsMaxWidth).roundToPx() }
    val countText = formatIntegerForDisplay(state.counter.count.toLong(), rememberCurrentLocale())
    val numberStyle = counterMainNumberStyle(metrics.numberFontSize)
    return with(density) {
        val feedbackWidth = if (state.counter.stepSize > 1) CounterDimens.StepFeedbackWidth.roundToPx() * 2 else 0
        val numberWidth = (widthPx - feedbackWidth).coerceAtLeast(0)
        val numberFontSize = measurer.fitCounterNumber(countText, numberStyle, numberWidth)
        measurer
            .measure(countText, numberStyle.copy(fontSize = numberFontSize), maxLines = 1)
            .size.height
            .toDp()
    }
}

/** Pinotun laskurin luonnollinen korkeus samoista osista kuin `CounterHero` piirtää. */
@Composable
private fun stackedCounterHeroHeight(
    state: CounterHeroState,
    metrics: CounterHeroMetrics,
    contentWidth: Dp,
    compactSpacing: Boolean,
    numberHeight: Dp,
    includeStitchTracker: Boolean = true,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val width = minOf(contentWidth, metrics.controlsMaxWidth)
    val widthPx = with(density) { width.roundToPx() }
    val captionStyle = counterCaptionStyle()
    val captionHeight = counterHeroCaptionHeight(state, width)
    val compact = compactSpacing || (state.canUseSecondaryCounter && state.visibleStitchTotal != null)
    var height = captionHeight + numberHeight + metrics.controlsHeight + CounterDimens.HeroBottomBreathingSpace
    height += if (compact) CounterDimens.HeroButtonCompactSpacing else CounterDimens.HeroButtonSpacing
    if (state.canUseSecondaryCounter) {
        height += CounterDimens.CounterRepeatHeight + CounterDimens.CounterRepeatToRowSpacing
    }
    val target = display.targetLine
    val status = mainCounterTargetStatus(target)
    if (target != null && status != null) {
        val showCompletion = !state.isCompleted && status !is MainCounterTargetStatus.Remaining
        val (leading, trailing) = counterTargetLabels(target, status, showCompletion)
        val rowHeight =
            with(density) {
                val spacing = CounterDimens.CounterTargetHelperSpacing.roundToPx()
                val contentWidthPx = (widthPx - spacing).coerceAtLeast(0)
                val leftWidth = if (showCompletion) contentWidthPx / 2 else widthPx
                val left =
                    measurer.measure(
                        leading,
                        captionStyle,
                        maxLines = if (showCompletion) Int.MAX_VALUE else 1,
                        constraints = Constraints(maxWidth = leftWidth),
                    )
                val rightWidth = (contentWidthPx - if (showCompletion) leftWidth else left.size.width).coerceAtLeast(0)
                val right =
                    measurer.measure(trailing, captionStyle, constraints = Constraints(maxWidth = rightWidth))
                maxOf(left.size.height, right.size.height).toDp()
            }
        height += CounterDimens.CounterTargetHelperSpacing * 2 + ProjectListDimens.ProgressTrackHeight + rowHeight
    }
    val stitchTotal = state.visibleStitchTotal
    if (includeStitchTracker && stitchTotal != null) {
        val stitchLabel = stringResource(R.string.stitch_tracker_label)
        val stitchValue =
            stringResource(
                R.string.stitch_counter_compact_format,
                state.currentStitch.coerceIn(0, stitchTotal),
                stitchTotal,
            )
        val typography = MaterialTheme.typography
        val largeFont = density.fontScale >= FontScaleThresholds.LARGE
        val stitchContentHeight =
            with(density) {
                val padding = CounterDimens.StitchTrackerHorizontalPadding.roundToPx() * 2
                val innerWidth = (widthPx - padding).coerceAtLeast(0)
                val label =
                    measurer.measure(
                        stitchLabel,
                        typography.labelMedium,
                        constraints = Constraints(maxWidth = innerWidth),
                    )
                val gap = CounterDimens.StitchTrackerContentSpacing.roundToPx()
                val buttonsWidth = CounterDimens.ExtraCounterStepperTouchSize.roundToPx() * 2
                val rowPrefix = if (largeFont) 0 else label.size.width + gap
                val valueWidth = (innerWidth - rowPrefix - buttonsWidth - gap * 2).coerceAtLeast(0)
                val value =
                    measurer.measure(
                        stitchValue,
                        typography.titleMedium,
                        constraints = Constraints(maxWidth = valueWidth),
                    )
                val controls = maxOf(value.size.height.toDp(), CounterDimens.ExtraCounterStepperTouchSize)
                if (largeFont) {
                    label.size.height.toDp() + CounterDimens.StitchTrackerContentSpacing + controls
                } else {
                    maxOf(label.size.height.toDp(), controls)
                }
            }
        val stitchHeight =
            maxOf(
                CounterDimens.StitchTrackerMinHeight,
                stitchContentHeight + CounterDimens.StitchTrackerVerticalPadding * 2,
            )
        height +=
            stitchHeight +
            if (compact) {
                CounterDimens.CounterControlsToStitchTrackerCompactSpacing
            } else {
                CounterDimens.CounterControlsToStitchTrackerSpacing
            }
    }
    return height
}

@Composable
internal fun counterTargetLabels(
    targetLine: MainCounterTargetSlot,
    status: MainCounterTargetStatus,
    showCompletion: Boolean,
): Pair<String, String> {
    val statusText = counterTargetStatusText(status)
    return if (showCompletion) {
        statusText to stringResource(R.string.counter_mark_complete)
    } else {
        stringResource(R.string.counter_target_of_format, targetLine.target) to statusText
    }
}
