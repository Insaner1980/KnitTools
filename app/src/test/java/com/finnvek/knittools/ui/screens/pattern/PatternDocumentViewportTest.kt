package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternDocumentViewportTest {
    @Test
    fun `viewport starts at identity transform`() {
        val state = PatternViewportState()

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `zoom stays between current one and five limits`() {
        val zoomedOut = PatternViewportState(scale = 2f).applyTransform(zoomChange = 0.1f, panChange = Offset.Zero)
        val zoomedIn = PatternViewportState(scale = 2f).applyTransform(zoomChange = 10f, panChange = Offset.Zero)

        assertEquals(1f, zoomedOut.scale)
        assertEquals(5f, zoomedIn.scale)
    }

    @Test
    fun `pan accumulates only while zoomed in`() {
        val zoomed =
            PatternViewportState()
                .applyTransform(zoomChange = 2f, panChange = Offset(12f, -8f))
                .applyTransform(zoomChange = 1f, panChange = Offset(3f, 5f))
        val resetByZoomOut = zoomed.applyTransform(zoomChange = 0.1f, panChange = Offset(100f, 100f))

        assertEquals(Offset(15f, -3f), zoomed.offset)
        assertEquals(Offset.Zero, resetByZoomOut.offset)
    }

    @Test
    fun `double tap reset returns identity transform`() {
        val zoomed = PatternViewportState(scale = 4f, offset = Offset(80f, -20f))

        assertEquals(PatternViewportState(), zoomed.reset())
    }

    @Test
    fun `page index is clamped to rendered document bounds`() {
        assertEquals(0, clampPatternPage(page = -1, pageCount = 4))
        assertEquals(2, clampPatternPage(page = 2, pageCount = 4))
        assertEquals(3, clampPatternPage(page = 8, pageCount = 4))
        assertEquals(0, clampPatternPage(page = 2, pageCount = 0))
    }
}
