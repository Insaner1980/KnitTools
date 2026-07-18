package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationGeometryTest {
    @Test
    fun `straight freehand stroke simplifies to endpoints`() {
        val points =
            List(101) { index ->
                val coordinate = index / 100f
                NormalizedPatternPoint(coordinate, coordinate)
            }

        val simplified = simplifyFreehandPoints(points, tolerance = 0.002f)

        assertEquals(listOf(points.first(), points.last()), simplified)
    }

    @Test
    fun `freehand simplification preserves turns and endpoint order`() {
        val points =
            listOf(
                NormalizedPatternPoint(0f, 0f),
                NormalizedPatternPoint(0.5f, 0f),
                NormalizedPatternPoint(0.5f, 1f),
                NormalizedPatternPoint(1f, 1f),
            )

        val simplified = simplifyFreehandPoints(points, tolerance = 0.01f)

        assertEquals(points, simplified)
    }

    @Test
    fun `bounding box contains all normalized points`() {
        val bounds =
            boundingBox(
                listOf(
                    NormalizedPatternPoint(0.7f, 0.3f),
                    NormalizedPatternPoint(0.1f, 0.9f),
                    NormalizedPatternPoint(0.4f, 0.2f),
                ),
            )

        assertEquals(NormalizedPatternBounds(0.1f, 0.2f, 0.7f, 0.9f), bounds)
    }

    @Test
    fun `stroke hit testing accepts near segment and rejects distant point`() {
        val stroke = listOf(NormalizedPatternPoint(0.1f, 0.2f), NormalizedPatternPoint(0.9f, 0.2f))

        assertTrue(isPointNearStroke(NormalizedPatternPoint(0.5f, 0.21f), stroke, tolerance = 0.02f))
        assertFalse(isPointNearStroke(NormalizedPatternPoint(0.5f, 0.4f), stroke, tolerance = 0.02f))
    }

    @Test
    fun `shape hit testing follows line rectangle and ellipse geometry`() {
        val shape =
            ShapePayload(
                start = NormalizedPatternPoint(0.2f, 0.2f),
                end = NormalizedPatternPoint(0.8f, 0.8f),
                strokeArgb = 1,
                strokeWidth = 2f,
            )

        assertTrue(isPointNearShape(PatternAnnotationKind.LINE, shape, NormalizedPatternPoint(0.5f, 0.51f), 0.02f))
        assertTrue(isPointNearShape(PatternAnnotationKind.RECTANGLE, shape, NormalizedPatternPoint(0.2f, 0.5f), 0.02f))
        assertFalse(isPointNearShape(PatternAnnotationKind.RECTANGLE, shape, NormalizedPatternPoint(0.5f, 0.5f), 0.02f))
        assertTrue(isPointNearShape(PatternAnnotationKind.ELLIPSE, shape, NormalizedPatternPoint(0.5f, 0.2f), 0.02f))
        assertFalse(isPointNearShape(PatternAnnotationKind.ELLIPSE, shape, NormalizedPatternPoint(0.5f, 0.5f), 0.02f))
    }
}
