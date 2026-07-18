package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload

fun topmostAnnotationAt(
    annotations: List<PatternAnnotation>,
    point: NormalizedPatternPoint,
    tolerance: Float,
): PatternAnnotation? =
    annotations
        .asSequence()
        .sortedWith(compareByDescending<PatternAnnotation> { it.zIndex }.thenByDescending { it.id })
        .firstOrNull { annotation -> annotation.hitTest(point, tolerance) }

fun translatePatternAnnotation(
    annotation: PatternAnnotation,
    deltaX: Float,
    deltaY: Float,
): PatternAnnotation {
    val bounds = patternAnnotationBounds(annotation) ?: return annotation
    val safeDeltaX = deltaX.coerceIn(-bounds.left, 1f - bounds.right)
    val safeDeltaY = deltaY.coerceIn(-bounds.top, 1f - bounds.bottom)
    return annotation.mapGeometry { point ->
        point.copy(x = point.x + safeDeltaX, y = point.y + safeDeltaY)
    }
}

fun scalePatternAnnotation(
    annotation: PatternAnnotation,
    scale: Float,
): PatternAnnotation {
    val bounds = patternAnnotationBounds(annotation) ?: return annotation
    val safeScale = scale.takeIf { it.isFinite() }?.coerceIn(MIN_SCALE, MAX_SCALE) ?: 1f
    val centerX = (bounds.left + bounds.right) / 2f
    val centerY = (bounds.top + bounds.bottom) / 2f
    return annotation.mapGeometry { point ->
        point.copy(
            x = (centerX + (point.x - centerX) * safeScale).coerceIn(0f, 1f),
            y = (centerY + (point.y - centerY) * safeScale).coerceIn(0f, 1f),
        )
    }
}

private fun PatternAnnotation.hitTest(
    point: NormalizedPatternPoint,
    tolerance: Float,
): Boolean =
    when (val annotationPayload = payload) {
        is FreehandPayload -> isPointNearStroke(point, annotationPayload.points, tolerance)
        is ShapePayload -> isPointNearShape(kind, annotationPayload, point, tolerance)
        is TextBoxPayload -> annotationPayload.bounds.contains(point, tolerance)
        is CalloutPayload -> annotationPayload.bounds.contains(point, tolerance)
        is ChartRegionPayload -> annotationPayload.bounds.contains(point, tolerance)
        is ChartTrackerPayload -> annotationPayload.region.bounds.contains(point, tolerance)
    }

fun patternAnnotationBounds(annotation: PatternAnnotation): NormalizedPatternBounds? =
    when (val annotationPayload = annotation.payload) {
        is FreehandPayload -> boundingBox(annotationPayload.points)
        is ShapePayload -> boundsOf(annotationPayload.start, annotationPayload.end)
        is TextBoxPayload -> annotationPayload.bounds
        is CalloutPayload -> annotationPayload.bounds
        is ChartRegionPayload -> annotationPayload.bounds
        is ChartTrackerPayload -> annotationPayload.region.bounds
    }

private fun PatternAnnotation.mapGeometry(
    transform: (NormalizedPatternPoint) -> NormalizedPatternPoint,
): PatternAnnotation =
    copy(
        payload =
            when (val annotationPayload = payload) {
                is FreehandPayload -> annotationPayload.copy(points = annotationPayload.points.map(transform))
                is ShapePayload ->
                    annotationPayload.copy(
                        start = transform(annotationPayload.start),
                        end = transform(annotationPayload.end),
                    )
                is TextBoxPayload -> annotationPayload.copy(bounds = annotationPayload.bounds.map(transform))
                is CalloutPayload -> annotationPayload.copy(bounds = annotationPayload.bounds.map(transform))
                is ChartRegionPayload -> annotationPayload.copy(bounds = annotationPayload.bounds.map(transform))
                is ChartTrackerPayload ->
                    annotationPayload.copy(
                        region =
                            annotationPayload.region.copy(
                                bounds = annotationPayload.region.bounds.map(transform),
                            ),
                    )
            },
        updatedAt = System.currentTimeMillis(),
    )

private fun NormalizedPatternBounds.contains(
    point: NormalizedPatternPoint,
    tolerance: Float,
): Boolean =
    point.x in (left - tolerance)..(right + tolerance) &&
        point.y in (top - tolerance)..(bottom + tolerance)

private fun NormalizedPatternBounds.map(
    transform: (NormalizedPatternPoint) -> NormalizedPatternPoint,
): NormalizedPatternBounds {
    val start = transform(NormalizedPatternPoint(left, top))
    val end = transform(NormalizedPatternPoint(right, bottom))
    return boundsOf(start, end)
}

private fun boundsOf(
    first: NormalizedPatternPoint,
    second: NormalizedPatternPoint,
) = NormalizedPatternBounds(
    left = minOf(first.x, second.x),
    top = minOf(first.y, second.y),
    right = maxOf(first.x, second.x),
    bottom = maxOf(first.y, second.y),
)

private const val MIN_SCALE = 0.1f
private const val MAX_SCALE = 10f
