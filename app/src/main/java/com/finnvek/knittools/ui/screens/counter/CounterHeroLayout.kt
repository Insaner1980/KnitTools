package com.finnvek.knittools.ui.screens.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.ui.layout.AdaptiveLayoutSize
import com.finnvek.knittools.ui.theme.CounterDimens
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
    val contentWidth = available.width - CounterDimens.ScreenHorizontalPadding * 2
    val largeCandidate =
        available.width >= CounterDimens.LargeHeroMinWidth && available.height >= CounterDimens.LargeHeroMinHeight
    val large = stackedCounterHeroHeight(state, CounterHeroMetrics.Large, compactSpacing = false)
    val useLarge = largeCandidate && viewportHeight >= large
    val metrics = if (useLarge) CounterHeroMetrics.Large else CounterHeroMetrics.Regular
    val regularHeight = stackedCounterHeroHeight(state, metrics, compactSpacing = false)
    val compactHeight = stackedCounterHeroHeight(state, metrics, compactSpacing = true)
    return when {
        viewportHeight >= regularHeight -> {
            CounterHeroLayout(
                arrangement = CounterHeroArrangement.Stacked,
                metrics = metrics,
                minimumHeight = minOf(viewportHeight, regularHeight + CounterDimens.HeroMaxExtraHeight),
            )
        }

        viewportHeight >= compactHeight -> {
            CounterHeroLayout(CounterHeroArrangement.StackedCompact, metrics, viewportHeight)
        }

        contentWidth >= CounterDimens.ShortHeroMinWidth -> {
            CounterHeroLayout(CounterHeroArrangement.SideBySide, CounterHeroMetrics.Regular, 0.dp, available.height)
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
    compactSpacing: Boolean,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val captionHeight =
        with(density) {
            measurer
                .measure("0", counterCaptionStyle())
                .size.height
                .toDp()
        }
    val numberHeight =
        with(density) {
            measurer
                .measure("0", counterMainNumberStyle(metrics.numberFontSize))
                .size.height
                .toDp()
        }
    val compact = compactSpacing || (state.canUseSecondaryCounter && state.visibleStitchTotal != null)
    var height = captionHeight + numberHeight + metrics.controlsHeight + CounterDimens.HeroBottomBreathingSpace
    height += if (compact) CounterDimens.HeroButtonCompactSpacing else CounterDimens.HeroButtonSpacing
    if (state.canUseSecondaryCounter) {
        height += CounterDimens.CounterRepeatHeight + CounterDimens.CounterRepeatToRowSpacing
    }
    if (CounterValueFormatter.forMainCounter(state.toMainCounterProject()).targetLine != null) {
        height += CounterDimens.CounterTargetHelperSpacing * 2 + ProjectListDimens.ProgressTrackHeight + captionHeight
    }
    if (state.visibleStitchTotal != null) {
        height +=
            CounterDimens.StitchTrackerMinHeight +
            if (compact) {
                CounterDimens.CounterControlsToStitchTrackerCompactSpacing
            } else {
                CounterDimens.CounterControlsToStitchTrackerSpacing
            }
    }
    return height
}
