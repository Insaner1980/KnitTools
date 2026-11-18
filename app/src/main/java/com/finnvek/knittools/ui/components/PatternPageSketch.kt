package com.finnvek.knittools.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/** Kuvitteellinen ohjesivu: siinä ei ole luettavaa tekstiä tai käyttäjän ohjeen sisältöä. */
@Composable
internal fun PatternPageSketch(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.knitToolsColors
    val primary = MaterialTheme.colorScheme.primary
    val lineOffset by animateDpAsState(
        targetValue = OnboardingDimens.TextRowHeight * (count % OnboardingDimens.TEXT_ROWS),
        animationSpec = tween(OnboardingDimens.GROWTH_DURATION_MILLIS),
        label = "welcomeReadingLine",
    )
    Canvas(
        modifier =
            modifier
                .size(OnboardingDimens.SketchWidth, OnboardingDimens.SketchHeight)
                .clip(MaterialTheme.shapes.small),
    ) {
        drawRect(colors.paperSurface)
        val inset = OnboardingDimens.PaperInset.toPx()
        val barHeight = OnboardingDimens.TextBarHeight.toPx()
        val rowHeight = OnboardingDimens.TextRowHeight.toPx()
        val contentWidth = size.width - inset * 2f
        OnboardingDimens.TextBarFractions.forEachIndexed { index, fraction ->
            drawRoundRect(
                color = colors.paperInk,
                topLeft = Offset(inset, inset + index * rowHeight),
                size = Size(contentWidth * fraction, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
            )
        }
        val chartTop = inset + OnboardingDimens.TEXT_ROWS * rowHeight
        val cellWidth = OnboardingDimens.ChartCellWidth.toPx()
        val cellHeight = OnboardingDimens.ChartCellHeight.toPx()
        val chartInk = colors.paperInk.shade(OnboardingDimens.CHART_INK_SHADE)
        for (row in 0..OnboardingDimens.CHART_ROWS) {
            val y = chartTop + row * cellHeight
            drawLine(
                chartInk,
                Offset(inset, y),
                Offset(inset + OnboardingDimens.CHART_COLUMNS * cellWidth, y),
                strokeWidth = OnboardingDimens.ChartLineWidth.toPx(),
            )
        }
        for (column in 0..OnboardingDimens.CHART_COLUMNS) {
            val x = inset + column * cellWidth
            drawLine(
                chartInk,
                Offset(x, chartTop),
                Offset(x, chartTop + OnboardingDimens.CHART_ROWS * cellHeight),
                strokeWidth = OnboardingDimens.ChartLineWidth.toPx(),
            )
        }
        val bandHeight = OnboardingDimens.ReadingBandHeight.toPx()
        val bandY = inset + lineOffset.toPx() - (bandHeight - barHeight) / 2f
        drawRect(
            primary.copy(alpha = OnboardingDimens.READING_BAND_ALPHA),
            topLeft = Offset(inset, bandY),
            size = Size(contentWidth, bandHeight),
        )
        drawRect(
            primary,
            topLeft = Offset(inset, bandY + bandHeight - OnboardingDimens.ReadingLineWidth.toPx()),
            size = Size(contentWidth, OnboardingDimens.ReadingLineWidth.toPx()),
        )
    }
}
