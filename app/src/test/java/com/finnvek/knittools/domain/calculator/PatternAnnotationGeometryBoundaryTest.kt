package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationGeometryBoundaryTest {
    private val center = NormalizedPatternPoint(0.5f, 0.5f)
    private val shape = ShapePayload(NormalizedPatternPoint(0.2f, 0.2f), NormalizedPatternPoint(0.8f, 0.8f), 1, 2f)

    @Test fun emptyAndDegenerateStrokesHavePredictableHitTargets() {
        assertNull(boundingBox(emptyList()))
        assertFalse(isPointNearStroke(center, emptyList(), 0.1f))
        for (stroke in listOf(listOf(center), listOf(center, center))) {
            assertTrue(isPointNearStroke(center, stroke, 0f))
            assertFalse(isPointNearStroke(NormalizedPatternPoint(0f, 0f), stroke, 0.1f))
            assertEquals(stroke, simplifyFreehandPoints(stroke, 0.1f))
        }
    }

    @Test fun pointLimitsPreserveEndpointsEvenWithInvalidTolerance() {
        val points = List(20) { NormalizedPatternPoint(it / 20f, if (it % 2 == 0) 0f else 1f) }
        for (tolerance in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f)) {
            for (limit in listOf(1, 2, 5)) {
                val result = simplifyFreehandPoints(points, tolerance, limit)
                assertTrue(result.size <= maxOf(limit, 2))
                assertEquals(points.first(), result.first())
                assertEquals(points.last(), result.last())
            }
        }
    }

    @Test fun filledShapesAcceptInteriorButTransparentShapesOnlyAcceptBorders() {
        for (kind in listOf(PatternAnnotationKind.RECTANGLE, PatternAnnotationKind.ELLIPSE)) {
            assertTrue(isPointNearShape(kind, shape.copy(fillArgb = 1, fillAlpha = 0.5f), center, 0.01f))
            assertFalse(isPointNearShape(kind, shape.copy(fillArgb = 1, fillAlpha = 0f), center, 0.01f))
            for (outside in listOf(
                NormalizedPatternPoint(0f, 0f),
                NormalizedPatternPoint(1f, 0.5f),
                NormalizedPatternPoint(0.5f, 1f),
            )) {
                assertFalse(isPointNearShape(kind, shape.copy(fillArgb = 1, fillAlpha = 0.5f), outside, 0.01f))
            }
        }
        for (point in listOf(
            NormalizedPatternPoint(0.5f, 0.2f),
            NormalizedPatternPoint(0.5f, 0.8f),
            NormalizedPatternPoint(0.2f, 0.5f),
            NormalizedPatternPoint(0.8f, 0.5f),
        )) {
            assertTrue(isPointNearShape(PatternAnnotationKind.RECTANGLE, shape, point, 0.01f))
        }
    }

    @Test fun flattenedEllipsesBehaveAsSegmentsAndUnsupportedKindsCannotBeSelectedAsShapes() {
        for (end in listOf(shape.start, NormalizedPatternPoint(0.2f, 0.8f), NormalizedPatternPoint(0.8f, 0.2f))) {
            val flat = shape.copy(end = end)
            assertTrue(isPointNearShape(PatternAnnotationKind.ELLIPSE, flat, shape.start, 0f))
            assertFalse(isPointNearShape(PatternAnnotationKind.ELLIPSE, flat, NormalizedPatternPoint(1f, 1f), 0.01f))
        }
        assertTrue(isPointNearShape(PatternAnnotationKind.ARROW, shape, center, 0.01f))
        assertFalse(isPointNearShape(PatternAnnotationKind.FREEHAND, shape, center, 0.01f))
    }
}
