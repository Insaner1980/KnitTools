package com.finnvek.knittools.ui.screens.counter

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
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
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

@Immutable
internal data class CounterHeroLayout(
    val arrangement: CounterHeroArrangement,
    val metrics: CounterHeroMetrics,
    val minimumHeight: Dp,
    // Vierekkäisen rivin enimmäiskorkeus: suurella fonttikoolla luku pienenee mahtuakseen näkymään.
    val maximumHeight: Dp = Dp.Unspecified,
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
    val largeCandidate =
        available.width >= CounterDimens.LargeHeroMinWidth && available.height >= CounterDimens.LargeHeroMinHeight
    val largeHeight =
        if (largeCandidate) {
            stackedCounterHeroHeight(state, CounterHeroMetrics.Large, contentWidth, compactSpacing = false)
        } else {
            null
        }
    val useLarge = largeHeight != null && viewportHeight >= largeHeight
    val metrics = if (useLarge) CounterHeroMetrics.Large else CounterHeroMetrics.Regular
    val regularHeight =
        largeHeight?.takeIf { useLarge }
            ?: stackedCounterHeroHeight(state, metrics, contentWidth, compactSpacing = false)
    if (viewportHeight >= regularHeight) {
        return CounterHeroLayout(
            arrangement = CounterHeroArrangement.Stacked,
            metrics = metrics,
            minimumHeight = minOf(viewportHeight, regularHeight + CounterDimens.HeroMaxExtraHeight),
        )
    }

    val compactHeight = stackedCounterHeroHeight(state, metrics, contentWidth, compactSpacing = true)
    return when {
        viewportHeight >= compactHeight -> {
            CounterHeroLayout(CounterHeroArrangement.StackedCompact, metrics, viewportHeight)
        }

        contentWidth >= CounterDimens.ShortHeroMinWidth -> {
            CounterHeroLayout(
                CounterHeroArrangement.SideBySide,
                CounterHeroMetrics.Regular,
                0.dp,
                available.height.coerceAtLeast(CounterHeroMetrics.Regular.primaryTouchSize),
            )
        }

        else -> {
            CounterHeroLayout(CounterHeroArrangement.StackedCompact, CounterHeroMetrics.Regular, 0.dp)
        }
    }
}

/** Pinotun laskurin luonnollinen korkeus samoista osista kuin `CounterHero` piirtää. */
@Composable
private fun stackedCounterHeroHeight(
    state: CounterHeroState,
    metrics: CounterHeroMetrics,
    contentWidth: Dp,
    compactSpacing: Boolean,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val display = CounterValueFormatter.forMainCounter(state.toMainCounterProject())
    val width = minOf(contentWidth, metrics.controlsMaxWidth)
    val widthPx = with(density) { width.roundToPx() }
    val captionStyle = counterCaptionStyle()
    val caption = mainCounterLabelText(display.heroTitle.labelType, display.heroTitle.customLabel)
    val captionHeight =
        with(density) {
            measurer
                .measure(caption, captionStyle, constraints = Constraints(maxWidth = widthPx))
                .size.height
                .toDp()
        }
    val countText = formatIntegerForDisplay(state.counter.count.toLong(), rememberCurrentLocale())
    val numberStyle = counterMainNumberStyle(metrics.numberFontSize)
    val numberHeight =
        with(density) {
            val feedbackWidth = if (state.counter.stepSize > 1) CounterDimens.StepFeedbackWidth.roundToPx() * 2 else 0
            val numberWidth = (widthPx - feedbackWidth).coerceAtLeast(0)
            val numberFontSize = measurer.fitCounterNumber(countText, numberStyle, numberWidth)
            measurer
                .measure(countText, numberStyle.copy(fontSize = numberFontSize), maxLines = 1)
                .size.height
                .toDp()
        }
    val compact = compactSpacing || (state.canUseSecondaryCounter && state.visibleStitchTotal != null)
    var height = captionHeight + numberHeight + metrics.controlsHeight + CounterDimens.HeroBottomBreathingSpace
    height += if (compact) CounterDimens.HeroButtonCompactSpacing else CounterDimens.HeroButtonSpacing
    if (state.canUseSecondaryCounter) {
        height += CounterDimens.CounterRepeatHeight + CounterDimens.CounterRepeatToRowSpacing
    }
    val target = display.targetLine
    val status = mainCounterTargetStatus(target)
    if (target != null && status != null) {
        val targetText = stringResource(R.string.counter_target_of_format, target.target)
        val statusText = counterTargetStatusText(status)
        val rowHeight =
            with(density) {
                val left =
                    measurer.measure(
                        targetText,
                        captionStyle,
                        maxLines = 1,
                        constraints = Constraints(maxWidth = widthPx),
                    )
                val statusWidth =
                    (widthPx - left.size.width - CounterDimens.CounterTargetHelperSpacing.roundToPx()).coerceAtLeast(0)
                val right =
                    measurer.measure(statusText, captionStyle, constraints = Constraints(maxWidth = statusWidth))
                maxOf(left.size.height, right.size.height).toDp()
            }
        height += CounterDimens.CounterTargetHelperSpacing * 2 + ProjectListDimens.ProgressTrackHeight + rowHeight
    }
    val stitchTotal = state.visibleStitchTotal
    if (stitchTotal != null) {
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
