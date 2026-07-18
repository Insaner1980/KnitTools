package com.finnvek.knittools.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle

internal object PatternAnnotationTokens {
    const val REFERENCE_PAGE_SIZE = 1_000f
    const val HIGHLIGHTER_DEFAULT_ALPHA = 96
    const val CALLOUT_BACKGROUND_ALPHA = 0.16f
    const val CALLOUT_TEXT_SIZE = 24f
    const val CALLOUT_CORNER_RADIUS = 12f
    const val CHART_GRID_WIDTH = 2f
    const val ARROW_HEAD_MULTIPLIER = 4f
    const val ARROW_HEAD_ANGLE = 0.55f
}

@Composable
internal fun rememberPatternAnnotationRenderStyle(): PatternAnnotationRenderStyle {
    val chartGridArgb = MaterialTheme.colorScheme.outline.toArgb()
    return remember(chartGridArgb) {
        PatternAnnotationRenderStyle(
            referencePageSize = PatternAnnotationTokens.REFERENCE_PAGE_SIZE,
            highlighterDefaultAlpha = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ALPHA,
            calloutBackgroundAlpha = PatternAnnotationTokens.CALLOUT_BACKGROUND_ALPHA,
            calloutTextSize = PatternAnnotationTokens.CALLOUT_TEXT_SIZE,
            calloutCornerRadius = PatternAnnotationTokens.CALLOUT_CORNER_RADIUS,
            chartGridArgb = chartGridArgb,
            chartGridWidth = PatternAnnotationTokens.CHART_GRID_WIDTH,
            arrowHeadMultiplier = PatternAnnotationTokens.ARROW_HEAD_MULTIPLIER,
            arrowHeadAngle = PatternAnnotationTokens.ARROW_HEAD_ANGLE,
        )
    }
}
