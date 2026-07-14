package com.finnvek.knittools.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageRendererSizeTest {
    @Test
    fun `bitmap size preserves normal page ratio at requested width`() {
        val size =
            calculatePdfRenderBitmapSize(
                pageWidth = 800,
                pageHeight = 1200,
                targetWidth = 1600,
            )

        assertEquals(PdfRenderBitmapSize(width = 1600, height = 2400), size)
    }

    @Test
    fun `bitmap size caps huge requested dimensions before allocation`() {
        val size =
            calculatePdfRenderBitmapSize(
                pageWidth = 100,
                pageHeight = 100,
                targetWidth = 10_000,
            )

        assertEquals(
            PdfRenderBitmapSize(
                width = PDF_RENDER_MAX_BITMAP_DIMENSION,
                height = PDF_RENDER_MAX_BITMAP_DIMENSION,
            ),
            size,
        )
    }

    @Test
    fun `bitmap size caps extremely tall pages before allocation`() {
        val size =
            calculatePdfRenderBitmapSize(
                pageWidth = 100,
                pageHeight = 10_000,
                targetWidth = 1600,
            )

        assertTrue(size.width in 1..PDF_RENDER_MAX_BITMAP_DIMENSION)
        assertEquals(PDF_RENDER_MAX_BITMAP_DIMENSION, size.height)
    }

    @Test
    fun `bitmap size keeps invalid inputs allocatable`() {
        val size =
            calculatePdfRenderBitmapSize(
                pageWidth = 0,
                pageHeight = 0,
                targetWidth = 0,
            )

        assertEquals(PdfRenderBitmapSize(width = 1, height = 1), size)
    }
}
