package com.finnvek.knittools.data.storage

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageRendererSourceTest {
    @Test
    fun `pdf pages are rendered onto a white paper background`() {
        val source = ProjectSourceFiles.read(PDF_PAGE_RENDERER)
        val whiteBackgroundIndex = source.indexOf("bitmap.eraseColor(Color.WHITE)")
        val renderIndex = source.indexOf("page.render(bitmap")

        assertTrue(
            "PDF bitmap must be filled white before PdfRenderer draws transparent page content.",
            whiteBackgroundIndex >= 0,
        )
        assertTrue(
            "White background must be applied before page.render.",
            whiteBackgroundIndex < renderIndex,
        )
    }

    @Test
    fun `pdf descriptor is closed if renderer construction fails`() {
        val source = ProjectSourceFiles.read(PDF_PAGE_RENDERER)
        val helperIndex =
            source.indexOf(
                "private fun createRenderer(fileDescriptor: ParcelFileDescriptor): PdfRenderer",
            )
        val guardedSource = source.substring(helperIndex.coerceAtLeast(0))

        assertTrue(
            "PdfRenderer construction must go through a helper that can close the descriptor on failure.",
            helperIndex >= 0,
        )
        assertTrue(
            "The helper must construct PdfRenderer from the opened descriptor.",
            guardedSource.contains("PdfRenderer(fileDescriptor)"),
        )
        assertTrue(
            "The helper must close the descriptor when PdfRenderer construction fails.",
            guardedSource.contains("fileDescriptor.close()"),
        )
        assertTrue(
            "The helper must rethrow the original renderer construction failure.",
            guardedSource.contains("throw failure"),
        )
    }

    private companion object {
        const val PDF_PAGE_RENDERER =
            "app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt"
    }
}
