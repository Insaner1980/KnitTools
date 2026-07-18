package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternPoint

data class PatternScreenPoint(
    val x: Float,
    val y: Float,
)

data class PatternPageCoordinateTransform(
    val pageLeft: Float,
    val pageTop: Float,
    val pageWidth: Float,
    val pageHeight: Float,
    val scale: Float,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun pageToScreen(point: NormalizedPatternPoint): PatternScreenPoint? {
        if (!isValid()) return null
        if (!point.x.isFinite() || !point.y.isFinite()) return null
        return PatternScreenPoint(
            x = pageLeft + point.x.coerceIn(0f, 1f) * pageWidth * scale + offsetX,
            y = pageTop + point.y.coerceIn(0f, 1f) * pageHeight * scale + offsetY,
        )
    }

    fun screenToPage(point: PatternScreenPoint): NormalizedPatternPoint? {
        if (!isValid() || !point.x.isFinite() || !point.y.isFinite()) return null
        return NormalizedPatternPoint(
            x = ((point.x - pageLeft - offsetX) / (pageWidth * scale)).coerceIn(0f, 1f),
            y = ((point.y - pageTop - offsetY) / (pageHeight * scale)).coerceIn(0f, 1f),
        )
    }

    private fun isValid(): Boolean =
        pageLeft.isFinite() &&
            pageTop.isFinite() &&
            pageWidth.isFinite() &&
            pageHeight.isFinite() &&
            scale.isFinite() &&
            offsetX.isFinite() &&
            offsetY.isFinite() &&
            pageWidth > 0f &&
            pageHeight > 0f &&
            scale > 0f
}
