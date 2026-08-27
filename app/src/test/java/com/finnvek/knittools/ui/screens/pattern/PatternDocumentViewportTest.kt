package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.finnvek.knittools.domain.calculator.PatternScreenPoint
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `viewport creates one center pivot transform for screen and page coordinates`() {
        val viewport = PatternViewportState(scale = 2f, offset = Offset(15f, -25f))
        val transform =
            viewport.toPageCoordinateTransform(
                pageSize = Size(400f, 800f),
                viewportOrigin = Offset(20f, 40f),
            )

        val screenPoint = requireNotNull(transform.pageToScreen(NormalizedPatternPoint(0.25f, 0.75f)))
        val pagePoint = requireNotNull(transform.screenToPage(screenPoint))

        assertEquals(PatternScreenPoint(35f, 815f), screenPoint)
        assertEquals(NormalizedPatternPoint(0.25f, 0.75f), pagePoint)
    }

    @Test
    fun `identity viewport preserves page aspect ratio coordinates`() {
        val wide =
            PatternViewportState().toPageCoordinateTransform(
                pageSize = Size(1_200f, 600f),
                viewportOrigin = Offset(8f, 16f),
            )
        val tall =
            PatternViewportState().toPageCoordinateTransform(
                pageSize = Size(600f, 1_200f),
                viewportOrigin = Offset(8f, 16f),
            )

        assertEquals(PatternScreenPoint(908f, 166f), wide.pageToScreen(NormalizedPatternPoint(0.75f, 0.25f)))
        assertEquals(PatternScreenPoint(458f, 316f), tall.pageToScreen(NormalizedPatternPoint(0.75f, 0.25f)))
    }

    @Test
    fun `focus request waits for its rendered page`() {
        val request = PatternViewportFocusRequest(requestId = 9L, pageIndex = 2, yFraction = 0.7f)

        assertNull(eligiblePatternViewportFocusRequest(currentPage = 1, renderedPageReady = true, request = request))
        assertNull(eligiblePatternViewportFocusRequest(currentPage = 2, renderedPageReady = false, request = request))
        assertEquals(
            request,
            eligiblePatternViewportFocusRequest(
                currentPage = 2,
                renderedPageReady = true,
                request = request,
            ),
        )
    }

    @Test
    fun `focus offset centers bookmark and clamps malformed fractions`() {
        assertEquals(
            300,
            patternViewportFocusScrollOffset(
                pageHeightPx = 1_000,
                viewportHeightPx = 400,
                yFraction = 0.5f,
                scale = 1f,
                translationY = 0f,
            ),
        )
        assertEquals(
            0,
            patternViewportFocusScrollOffset(
                pageHeightPx = 1_000,
                viewportHeightPx = 400,
                yFraction = -5f,
                scale = 1f,
                translationY = 0f,
            ),
        )
    }
}
