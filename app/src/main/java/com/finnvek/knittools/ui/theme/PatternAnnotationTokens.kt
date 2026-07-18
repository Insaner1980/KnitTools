package com.finnvek.knittools.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle

internal object PatternAnnotationTokens {
    const val PEN_DEFAULT_ARGB = 0xFF1F1F1F.toInt()
    const val HIGHLIGHTER_DEFAULT_ARGB = 0x60FFD54F
    const val PEN_DEFAULT_WIDTH = 4f
    const val HIGHLIGHTER_DEFAULT_WIDTH = 20f
    const val ERASER_HIT_TOLERANCE = 0.02f
    const val SELECTION_HIT_TOLERANCE = 0.025f
    const val SELECTION_OUTLINE_WIDTH = 3f
    const val TEXT_DEFAULT_SIZE = 18f
    const val TEXT_BACKGROUND_ARGB = 0xFFFFFFFF.toInt()
    const val TEXT_BACKGROUND_ALPHA = 0.85f
    const val CALLOUT_DEFAULT_ARGB = 0xFF6750A4.toInt()
    const val CHART_HIGHLIGHT_ALPHA = 0.35f
    val COLOR_PALETTE_ARGB =
        listOf(
            0xFF1F1F1F.toInt(),
            0xFFD32F2F.toInt(),
            0xFF1976D2.toInt(),
            0xFF388E3C.toInt(),
            0xFFFFC107.toInt(),
        )
    const val REFERENCE_PAGE_SIZE = 1_000f
    const val HIGHLIGHTER_DEFAULT_ALPHA = 96
    const val CALLOUT_BACKGROUND_ALPHA = 0.16f
    const val CALLOUT_TEXT_SIZE = 24f
    const val CALLOUT_CORNER_RADIUS = 12f
    const val CHART_GRID_WIDTH = 2f
    const val ARROW_HEAD_MULTIPLIER = 4f
    const val ARROW_HEAD_ANGLE = 0.55f
    const val MINIMUM_PRESSURE_SCALE = 0.15f
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
            minimumPressureScale = PatternAnnotationTokens.MINIMUM_PRESSURE_SCALE,
        )
    }
}
