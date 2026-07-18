package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLimits
import com.finnvek.knittools.domain.model.ShapePayload
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt

fun simplifyFreehandPoints(
    points: List<NormalizedPatternPoint>,
    tolerance: Float,
    maxPoints: Int = PatternAnnotationLimits.MAX_FREEHAND_POINTS,
): List<NormalizedPatternPoint> {
    if (points.size <= 2) return points
    val safeTolerance = tolerance.takeIf { it.isFinite() && it >= 0f } ?: 0f
    val simplified = simplifyRange(points, safeTolerance)
    if (simplified.size <= maxPoints.coerceAtLeast(2)) return simplified
    val step = ceil((simplified.size - 1).toDouble() / (maxPoints - 1).coerceAtLeast(1)).toInt()
    return buildList {
        add(simplified.first())
        var index = step
        while (index < simplified.lastIndex) {
            add(simplified[index])
            index += step
        }
        add(simplified.last())
    }
}

fun boundingBox(points: List<NormalizedPatternPoint>): NormalizedPatternBounds? {
    if (points.isEmpty()) return null
    return NormalizedPatternBounds(
        left = points.minOf(NormalizedPatternPoint::x),
        top = points.minOf(NormalizedPatternPoint::y),
        right = points.maxOf(NormalizedPatternPoint::x),
        bottom = points.maxOf(NormalizedPatternPoint::y),
    )
}

fun isPointNearStroke(
    point: NormalizedPatternPoint,
    stroke: List<NormalizedPatternPoint>,
    tolerance: Float,
): Boolean {
    if (stroke.isEmpty()) return false
    if (stroke.size == 1) return distance(point, stroke.first()) <= tolerance
    return stroke.zipWithNext().any { (start, end) -> pointSegmentDistance(point, start, end) <= tolerance }
}

fun isPointNearShape(
    kind: PatternAnnotationKind,
    shape: ShapePayload,
    point: NormalizedPatternPoint,
    tolerance: Float,
): Boolean =
    when (kind) {
        PatternAnnotationKind.LINE,
        PatternAnnotationKind.ARROW,
        -> pointSegmentDistance(point, shape.start, shape.end) <= tolerance

        PatternAnnotationKind.RECTANGLE -> isPointNearRectangle(point, shape, tolerance)
        PatternAnnotationKind.ELLIPSE -> isPointNearEllipse(point, shape, tolerance)
        else -> false
    }

private fun simplifyRange(
    points: List<NormalizedPatternPoint>,
    tolerance: Float,
): List<NormalizedPatternPoint> {
    val first = points.first()
    val last = points.last()
    var farthestDistance = 0f
    var farthestIndex = 0
    for (index in 1 until points.lastIndex) {
        val distance = pointSegmentDistance(points[index], first, last)
        if (distance > farthestDistance) {
            farthestDistance = distance
            farthestIndex = index
        }
    }
    if (farthestDistance <= tolerance) return listOf(first, last)
    val left = simplifyRange(points.subList(0, farthestIndex + 1), tolerance)
    val right = simplifyRange(points.subList(farthestIndex, points.size), tolerance)
    return left.dropLast(1) + right
}

private fun isPointNearRectangle(
    point: NormalizedPatternPoint,
    shape: ShapePayload,
    tolerance: Float,
): Boolean {
    val left = minOf(shape.start.x, shape.end.x)
    val right = maxOf(shape.start.x, shape.end.x)
    val top = minOf(shape.start.y, shape.end.y)
    val bottom = maxOf(shape.start.y, shape.end.y)
    val inside = point.x in left..right && point.y in top..bottom
    if (shape.fillArgb != null && shape.fillAlpha > 0f && inside) return true
    val nearHorizontal =
        point.x in (left - tolerance)..(right + tolerance) &&
            (
                abs(point.y - top) <= tolerance ||
                    abs(point.y - bottom) <= tolerance
            )
    val nearVertical =
        point.y in (top - tolerance)..(bottom + tolerance) &&
            (
                abs(point.x - left) <= tolerance ||
                    abs(point.x - right) <= tolerance
            )
    return nearHorizontal || nearVertical
}

private fun isPointNearEllipse(
    point: NormalizedPatternPoint,
    shape: ShapePayload,
    tolerance: Float,
): Boolean {
    val radiusX = abs(shape.end.x - shape.start.x) / 2f
    val radiusY = abs(shape.end.y - shape.start.y) / 2f
    if (radiusX == 0f || radiusY == 0f) return pointSegmentDistance(point, shape.start, shape.end) <= tolerance
    val centerX = (shape.start.x + shape.end.x) / 2f
    val centerY = (shape.start.y + shape.end.y) / 2f
    val normalizedDistance =
        sqrt(
            ((point.x - centerX) / radiusX) * ((point.x - centerX) / radiusX) +
                ((point.y - centerY) / radiusY) * ((point.y - centerY) / radiusY),
        )
    if (shape.fillArgb != null && shape.fillAlpha > 0f && normalizedDistance <= 1f) return true
    return abs(normalizedDistance - 1f) <= tolerance / minOf(radiusX, radiusY)
}

private fun pointSegmentDistance(
    point: NormalizedPatternPoint,
    start: NormalizedPatternPoint,
    end: NormalizedPatternPoint,
): Float {
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    val lengthSquared = deltaX * deltaX + deltaY * deltaY
    if (lengthSquared == 0f) return distance(point, start)
    val projection = (((point.x - start.x) * deltaX + (point.y - start.y) * deltaY) / lengthSquared).coerceIn(0f, 1f)
    return hypot(point.x - (start.x + projection * deltaX), point.y - (start.y + projection * deltaY))
}

private fun distance(
    first: NormalizedPatternPoint,
    second: NormalizedPatternPoint,
): Float = hypot(first.x - second.x, first.y - second.y)
