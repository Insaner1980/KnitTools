package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternPageCoordinateTransformTest {
    @Test
    fun `page and screen coordinates round trip through zoom and pan`() {
        val transform =
            PatternPageCoordinateTransform(
                pageLeft = 20f,
                pageTop = 40f,
                pageWidth = 400f,
                pageHeight = 800f,
                scale = 2f,
                offsetX = 15f,
                offsetY = -25f,
            )
        val pagePoint = NormalizedPatternPoint(0.25f, 0.75f)

        val screenPoint = requireNotNull(transform.pageToScreen(pagePoint))
        val roundTrip = requireNotNull(transform.screenToPage(screenPoint))

        assertEquals(PatternScreenPoint(235f, 1215f), screenPoint)
        assertEquals(pagePoint, roundTrip)
    }

    @Test
    fun `screen coordinates are clamped to normalized page`() {
        val transform = PatternPageCoordinateTransform(10f, 20f, 100f, 200f, scale = 1f)

        assertEquals(NormalizedPatternPoint(0f, 1f), transform.screenToPage(PatternScreenPoint(-50f, 400f)))
    }

    @Test
    fun `invalid page dimensions or scale do not produce coordinates`() {
        assertNull(PatternPageCoordinateTransform(0f, 0f, 0f, 100f, 1f).screenToPage(PatternScreenPoint(1f, 1f)))
        assertNull(
            PatternPageCoordinateTransform(0f, 0f, 100f, 100f, 0f).pageToScreen(NormalizedPatternPoint(0.5f, 0.5f)),
        )
    }
}
