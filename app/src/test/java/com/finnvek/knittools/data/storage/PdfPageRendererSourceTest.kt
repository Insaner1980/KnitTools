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

    private companion object {
        const val PDF_PAGE_RENDERER =
            "app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt"
    }
}
