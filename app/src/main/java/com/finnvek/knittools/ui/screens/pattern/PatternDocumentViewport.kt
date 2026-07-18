package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.ui.geometry.Offset

internal const val PATTERN_VIEWPORT_MIN_SCALE = 1f
internal const val PATTERN_VIEWPORT_MAX_SCALE = 5f

internal data class PatternViewportState(
    val scale: Float = PATTERN_VIEWPORT_MIN_SCALE,
    val offset: Offset = Offset.Zero,
) {
    fun applyTransform(
        zoomChange: Float,
        panChange: Offset,
    ): PatternViewportState {
        val nextScale = (scale * zoomChange).coerceIn(PATTERN_VIEWPORT_MIN_SCALE, PATTERN_VIEWPORT_MAX_SCALE)
        return copy(
            scale = nextScale,
            offset = if (nextScale > PATTERN_VIEWPORT_MIN_SCALE) offset + panChange else Offset.Zero,
        )
    }

    fun reset(): PatternViewportState = PatternViewportState()
}

internal fun clampPatternPage(
    page: Int,
    pageCount: Int,
): Int = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
