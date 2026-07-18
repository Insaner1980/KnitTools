package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternAnnotationEditingTest {
    @Test
    fun `hit testing selects topmost editable annotation`() {
        val bottom = shape(id = 1L, zIndex = 2L)
        val top = shape(id = 2L, zIndex = 5L)

        assertEquals(
            top,
            topmostAnnotationAt(
                annotations = listOf(bottom, top),
                point = NormalizedPatternPoint(0.3f, 0.5f),
                tolerance = 0.02f,
            ),
        )
        assertNull(
            topmostAnnotationAt(
                annotations = listOf(bottom, top),
                point = NormalizedPatternPoint(0.1f, 0.1f),
                tolerance = 0.01f,
            ),
        )
    }

    @Test
    fun `translation and scaling clamp shape to page bounds`() {
        val original = shape(id = 4L, zIndex = 1L)

        val translated = translatePatternAnnotation(original, deltaX = 0.8f, deltaY = 0.8f)
        val translatedPayload = translated.payload as ShapePayload
        assertEquals(0.6f, translatedPayload.start.x)
        assertEquals(1f, translatedPayload.end.x)

        val scaled = scalePatternAnnotation(original, scale = 2f)
        val scaledPayload = scaled.payload as ShapePayload
        assertEquals(0.1f, scaledPayload.start.x, 0.0001f)
        assertEquals(0.9f, scaledPayload.end.x, 0.0001f)
    }

    private fun shape(
        id: Long,
        zIndex: Long,
    ) = PatternAnnotation(
        id = id,
        layerId = 3L,
        page = 0,
        kind = PatternAnnotationKind.RECTANGLE,
        payload =
            ShapePayload(
                start = NormalizedPatternPoint(0.3f, 0.3f),
                end = NormalizedPatternPoint(0.7f, 0.7f),
                strokeArgb = 0xFF000000.toInt(),
                strokeWidth = 2f,
            ),
        zIndex = zIndex,
    )
}
