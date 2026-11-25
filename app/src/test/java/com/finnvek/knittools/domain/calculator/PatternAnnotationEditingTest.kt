package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
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

    @Test
    fun `scaling near the page edge keeps the shape proportions and stays on the page`() {
        val nearEdge =
            shape(id = 5L, zIndex = 1L).copy(
                payload =
                    ShapePayload(
                        start = NormalizedPatternPoint(0.8f, 0.4f),
                        end = NormalizedPatternPoint(1f, 0.6f),
                        strokeArgb = 0xFF000000.toInt(),
                        strokeWidth = 2f,
                    ),
            )

        val scaled = scalePatternAnnotation(nearEdge, scale = 2f).payload as ShapePayload

        // 0,2 leveä neliö kasvaa 0,4:ään ja siirtyy kokonaan sivulle eikä litisty reunaa vasten.
        assertEquals(0.4f, scaled.end.x - scaled.start.x, 0.0001f)
        assertEquals(0.4f, scaled.end.y - scaled.start.y, 0.0001f)
        assertEquals(1f, scaled.end.x, 0.0001f)
        assertEquals(0.3f, scaled.start.y, 0.0001f)
    }

    @Test
    fun `scaling larger than the page is limited to the page size`() {
        val scaled = scalePatternAnnotation(shape(id = 6L, zIndex = 1L), scale = 10f).payload as ShapePayload

        assertEquals(0f, scaled.start.x, 0.0001f)
        assertEquals(1f, scaled.end.x, 0.0001f)
        assertEquals(0f, scaled.start.y, 0.0001f)
        assertEquals(1f, scaled.end.y, 0.0001f)
    }

    @Test
    fun `dragging a text box to the page edge preserves its size text and colors`() {
        val payload =
            TextBoxPayload(
                bounds = NormalizedPatternBounds(0.25f, 0.5f, 0.5f, 0.75f),
                text = "Repeat rows 1–4",
                textSizeSp = 18f,
                textArgb = 0xFF123456.toInt(),
                backgroundArgb = 0xFFFFDD00.toInt(),
            )
        val annotation =
            PatternAnnotation(
                layerId = 3,
                page = 1,
                kind = PatternAnnotationKind.TEXT_BOX,
                payload = payload,
                zIndex = 2,
            )

        val moved = translatePatternAnnotation(annotation, deltaX = 1f, deltaY = -1f)

        assertEquals(payload.copy(bounds = NormalizedPatternBounds(0.75f, 0f, 1f, 0.25f)), moved.payload)
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
