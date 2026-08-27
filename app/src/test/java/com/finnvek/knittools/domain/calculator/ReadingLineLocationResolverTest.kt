package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingLineLocationResolverTest {
    @Test
    fun `exact current-page marker wins`() {
        val result = resolve(markers = listOf(marker(8, 1, 0.3f)), newRow = 8, currentPage = 1)

        assertLocation(result, page = 1, y = 0.3f, kind = ReadingLineResolutionKind.EXACT_MARKER)
        assertFalse(result.automaticPageChange)
    }

    @Test
    fun `one exact marker on another page changes page`() {
        val result = resolve(markers = listOf(marker(8, 2, 0.7f)), newRow = 8)

        assertLocation(result, page = 2, y = 0.7f, kind = ReadingLineResolutionKind.EXACT_MARKER)
        assertTrue(result.automaticPageChange)
    }

    @Test
    fun `ambiguous exact markers use current-page row fallback`() {
        val result =
            resolve(
                markers = listOf(marker(8, 1, 0.2f), marker(8, 2, 0.8f)),
                previousRow = 7,
                newRow = 8,
            )

        assertLocation(result, page = 0, y = 0.52f, kind = ReadingLineResolutionKind.AMBIGUOUS_FALLBACK)
    }

    @Test
    fun `current-page bracket is preferred`() {
        val result =
            resolve(
                markers = listOf(marker(0, 0, 0.2f), marker(10, 0, 0.8f), marker(0, 1, 0.1f), marker(10, 1, 0.9f)),
                previousRow = 4,
                newRow = 5,
            )

        assertLocation(result, page = 0, y = 0.5f, kind = ReadingLineResolutionKind.SAME_PAGE_INTERPOLATION)
    }

    @Test
    fun `unique bracket on another page changes page`() {
        val result =
            resolve(
                markers = listOf(marker(2, 3, 0.2f), marker(6, 3, 0.6f)),
                previousRow = 3,
                newRow = 4,
            )

        assertLocation(result, page = 3, y = 0.4f, kind = ReadingLineResolutionKind.SAME_PAGE_INTERPOLATION)
        assertTrue(result.automaticPageChange)
    }

    @Test
    fun `equally plausible brackets on other pages are ambiguous`() {
        val result =
            resolve(
                markers = listOf(marker(2, 1, 0.2f), marker(6, 1, 0.6f), marker(2, 2, 0.3f), marker(6, 2, 0.7f)),
                previousRow = 3,
                newRow = 4,
            )

        assertLocation(result, page = 0, y = 0.52f, kind = ReadingLineResolutionKind.AMBIGUOUS_FALLBACK)
    }

    @Test
    fun `increment decrement reset and undo use actual row delta`() {
        assertLocation(resolve(previousRow = 4, newRow = 5), 0, 0.52f, ReadingLineResolutionKind.ROW_DELTA_FALLBACK)
        assertLocation(resolve(previousRow = 5, newRow = 4), 0, 0.48f, ReadingLineResolutionKind.ROW_DELTA_FALLBACK)
        assertLocation(resolve(previousRow = 40, newRow = 0), 0, 0.05f, ReadingLineResolutionKind.ROW_DELTA_FALLBACK)
        assertLocation(resolve(previousRow = 12, newRow = 10), 0, 0.46f, ReadingLineResolutionKind.ROW_DELTA_FALLBACK)
    }

    @Test
    fun `page count clamps current page and ignores out-of-range markers`() {
        val result =
            resolve(
                markers = listOf(marker(6, 9, 0.8f)),
                previousRow = 5,
                newRow = 6,
                currentPage = 8,
                pageCount = 2,
            )

        assertLocation(result, page = 1, y = 0.52f, kind = ReadingLineResolutionKind.ROW_DELTA_FALLBACK)
        assertTrue(result.automaticPageChange)
    }

    @Test
    fun `malformed markers and unrelated one-sided pages are ignored`() {
        val malformed = listOf(marker(-1, 0, 0.2f), marker(6, -1, 0.3f), marker(6, 0, Float.NaN), marker(6, 0, 2f))
        val unrelated = listOf(marker(1, 4, 0.2f))

        assertLocation(
            resolve(markers = malformed + unrelated, previousRow = 5, newRow = 6),
            page = 0,
            y = 0.52f,
            kind = ReadingLineResolutionKind.ROW_DELTA_FALLBACK,
        )
    }

    @Test
    fun `row zero is a valid exact marker`() {
        val result = resolve(markers = listOf(marker(0, 1, 0.25f)), previousRow = 8, newRow = 0)

        assertLocation(result, page = 1, y = 0.25f, kind = ReadingLineResolutionKind.EXACT_MARKER)
    }

    @Test
    fun `zero delta without markers keeps location`() {
        val result = resolve(previousRow = 5, newRow = 5, currentPage = 2)

        assertLocation(result, page = 2, y = 0.5f, kind = ReadingLineResolutionKind.UNCHANGED)
        assertFalse(result.automaticPageChange)
    }

    private fun resolve(
        markers: List<RowMarker> = emptyList(),
        previousRow: Int = 4,
        newRow: Int = 5,
        currentPage: Int = 0,
        pageCount: Int? = null,
    ): ReadingLineLocationResolution =
        resolveReadingLineLocation(
            markers = markers,
            previousRow = previousRow,
            newRow = newRow,
            currentPage = currentPage,
            currentYFraction = 0.5f,
            pageCount = pageCount,
        )

    private fun marker(
        row: Int,
        page: Int,
        y: Float,
    ) = RowMarker(row = row, page = page, yPosition = y)

    private fun assertLocation(
        result: ReadingLineLocationResolution,
        page: Int,
        y: Float,
        kind: ReadingLineResolutionKind,
    ) {
        assertEquals(page, result.targetPage)
        assertEquals(y, result.targetYFraction, 0.0001f)
        assertEquals(kind, result.kind)
    }
}
