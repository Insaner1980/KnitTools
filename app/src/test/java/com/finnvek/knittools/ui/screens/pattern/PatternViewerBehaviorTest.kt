package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.PatternBookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternViewerBehaviorTest {
    @Test
    fun `nearest previous and next selection follow deterministic bookmark order`() {
        val bookmarks =
            listOf(
                bookmark(id = 1, page = 0, y = 0.2f),
                bookmark(id = 2, page = 0, y = 0.7f),
                bookmark(id = 3, page = 2, y = 0.1f),
            )

        assertEquals(1, bookmarkIndexAtOrAfter(bookmarks, pageIndex = 0, yFraction = 0.5f))
        assertEquals(2, bookmarkIndexAtOrAfter(bookmarks, pageIndex = 1, yFraction = 0.9f))
        assertEquals(2L, adjacentBookmarkId(bookmarks, selectedBookmarkId = 1L, offset = 1))
        assertEquals(2L, adjacentBookmarkId(bookmarks, selectedBookmarkId = 3L, offset = -1))
        assertNull(adjacentBookmarkId(bookmarks, selectedBookmarkId = 1L, offset = -1))
        assertNull(adjacentBookmarkId(bookmarks, selectedBookmarkId = null, offset = 1))
    }

    @Test
    fun `annotated export name removes only the pdf extension`() {
        assertEquals("Huivi v1.2", annotatedPdfExportBaseName("Huivi v1.2", PHOTO_FALLBACK, DEFAULT_NAME))
        assertEquals("Sukat 2.0", annotatedPdfExportBaseName("Sukat 2.0", PHOTO_FALLBACK, DEFAULT_NAME))
        assertEquals("Lace Shawl", annotatedPdfExportBaseName("lace_shawl.pdf", PHOTO_FALLBACK, DEFAULT_NAME))
        assertEquals(PHOTO_FALLBACK, annotatedPdfExportBaseName("pattern-images-17.pdf", PHOTO_FALLBACK, DEFAULT_NAME))
        assertEquals(DEFAULT_NAME, annotatedPdfExportBaseName(null, PHOTO_FALLBACK, DEFAULT_NAME))
        assertEquals(DEFAULT_NAME, annotatedPdfExportBaseName("   ", PHOTO_FALLBACK, DEFAULT_NAME))
    }

    @Test
    fun `unchanged prefilled rename keeps the stored document label`() {
        assertFalse(isDocumentRenameChange("Lace Shawl", "Lace Shawl"))
        assertFalse(isDocumentRenameChange("  Lace Shawl ", "Lace Shawl"))
        assertTrue(isDocumentRenameChange("Lace Shawl 2", "Lace Shawl"))
    }

    @Test
    fun `horizontal and vertical accessibility steps clamp to guide bounds`() {
        assertEquals(0.52f, horizontalReadingLineAccessibilityStep(0.5f, forward = true), 0.0001f)
        assertEquals(0.48f, horizontalReadingLineAccessibilityStep(0.5f, forward = false), 0.0001f)
        assertEquals(0.95f, horizontalReadingLineAccessibilityStep(0.95f, forward = true), 0.0001f)
        assertEquals(0.52f, verticalReadingGuideAccessibilityStep(0.5f, forward = true), 0.0001f)
        assertEquals(0.05f, verticalReadingGuideAccessibilityStep(0.05f, forward = false), 0.0001f)
    }

    private fun bookmark(
        id: Long,
        page: Int,
        y: Float,
    ) = PatternBookmark(
        id = id,
        projectId = 7,
        documentKey = "saved:91:v1",
        name = "Bookmark $id",
        pageIndex = page,
        yFraction = y,
        createdAt = id,
    )

    private companion object {
        const val PHOTO_FALLBACK = "Pattern photos"
        const val DEFAULT_NAME = "Pattern"
    }
}
