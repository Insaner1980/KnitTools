package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import kotlin.math.abs

enum class PatternAnnotationTool {
    BROWSE,
    PEN,
    HIGHLIGHTER,
    ERASER,
    SELECT,
    LINE,
    ARROW,
    RECTANGLE,
    ELLIPSE,
    TEXT,
    CALLOUT,
    CHART,
}

enum class PatternHighlighterAxisLock {
    FREE,
    HORIZONTAL,
    VERTICAL,
    DOMINANT_AXIS,
}

internal enum class PatternInputPointerType {
    TOUCH,
    STYLUS,
    ERASER,
    MOUSE,
    UNKNOWN,
}

internal fun shouldHandleAnnotationPointer(
    activeTool: PatternAnnotationTool,
    pointerType: PatternInputPointerType,
    activePointerCount: Int,
    stylusPresent: Boolean,
): Boolean {
    if (activePointerCount != 1) return false
    if (pointerType == PatternInputPointerType.TOUCH && stylusPresent) return false
    if (pointerType == PatternInputPointerType.ERASER) return true
    if (activeTool == PatternAnnotationTool.BROWSE) return false
    return pointerType in ANNOTATION_POINTER_TYPES
}

internal fun resolvedAnnotationTool(
    activeTool: PatternAnnotationTool,
    pointerType: PatternInputPointerType,
): PatternAnnotationTool =
    if (pointerType == PatternInputPointerType.ERASER) PatternAnnotationTool.ERASER else activeTool

internal fun lockHighlighterPoints(
    points: List<NormalizedPatternPoint>,
    axisLock: PatternHighlighterAxisLock,
): List<NormalizedPatternPoint> {
    if (points.size < 2 || axisLock == PatternHighlighterAxisLock.FREE) return points
    val first = points.first()
    val last = points.last()
    val resolvedLock =
        if (axisLock == PatternHighlighterAxisLock.DOMINANT_AXIS) {
            if (abs(last.x - first.x) >= abs(last.y - first.y)) {
                PatternHighlighterAxisLock.HORIZONTAL
            } else {
                PatternHighlighterAxisLock.VERTICAL
            }
        } else {
            axisLock
        }
    return points.map { point ->
        when (resolvedLock) {
            PatternHighlighterAxisLock.HORIZONTAL -> point.copy(y = first.y)
            PatternHighlighterAxisLock.VERTICAL -> point.copy(x = first.x)
            PatternHighlighterAxisLock.FREE,
            PatternHighlighterAxisLock.DOMINANT_AXIS,
            -> point
        }
    }
}

private val ANNOTATION_POINTER_TYPES =
    setOf(
        PatternInputPointerType.TOUCH,
        PatternInputPointerType.STYLUS,
        PatternInputPointerType.MOUSE,
    )
