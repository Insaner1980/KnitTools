package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RowMappingParserTest {
    @Test
    fun `parseMapping returns empty list for missing or invalid json`() {
        assertEquals(emptyList<RowMarker>(), parseMapping(null))
        assertEquals(emptyList<RowMarker>(), parseMapping("   "))
        assertEquals(emptyList<RowMarker>(), parseMapping("not json"))
    }

    @Test
    fun `parseMapping ignores unknown keys and sorts by page then row`() {
        val json =
            """
            [
              {"row": 12, "page": 2, "yPosition": 0.7, "ignored": true},
              {"row": 4, "page": 1, "yPosition": 0.2},
              {"row": 2, "page": 1, "yPosition": 0.1}
            ]
            """.trimIndent()

        val result = parseMapping(json)

        assertEquals(
            listOf(
                RowMarker(row = 2, page = 1, yPosition = 0.1f),
                RowMarker(row = 4, page = 1, yPosition = 0.2f),
                RowMarker(row = 12, page = 2, yPosition = 0.7f),
            ),
            result,
        )
    }

    @Test
    fun `serializeMapping removes duplicate row-page pairs and sorts output`() {
        val result =
            serializeMapping(
                listOf(
                    RowMarker(row = 5, page = 2, yPosition = 0.5f),
                    RowMarker(row = 1, page = 1, yPosition = 0.1f),
                    RowMarker(row = 5, page = 2, yPosition = 0.9f),
                    RowMarker(row = 3, page = 1, yPosition = 0.3f),
                ),
            )

        assertEquals(
            listOf(
                RowMarker(row = 1, page = 1, yPosition = 0.1f),
                RowMarker(row = 3, page = 1, yPosition = 0.3f),
                RowMarker(row = 5, page = 2, yPosition = 0.5f),
            ),
            parseMapping(result),
        )
    }

    @Test
    fun `createCalibrationRowMarkers returns two clamped markers for interpolation`() {
        val markers =
            requireNotNull(
                createCalibrationRowMarkers(
                    firstRow = 10,
                    firstPage = 2,
                    firstYPosition = -0.2f,
                    lastRow = 30,
                    lastPage = 2,
                    lastYPosition = 1.2f,
                ),
            )

        assertEquals(
            listOf(
                RowMarker(row = 10, page = 2, yPosition = READING_LINE_MIN_Y_FRACTION),
                RowMarker(row = 30, page = 2, yPosition = READING_LINE_MAX_Y_FRACTION),
            ),
            markers,
        )
        assertEquals(0.5f, interpolateYPosition(markers, targetRow = 20, page = 2))
    }

    @Test
    fun `createCalibrationRowMarkers rejects matching row values`() {
        assertNull(
            createCalibrationRowMarkers(
                firstRow = 12,
                firstPage = 0,
                firstYPosition = 0.3f,
                lastRow = 12,
                lastPage = 0,
                lastYPosition = 0.8f,
            ),
        )
    }

    @Test
    fun `interpolateYPosition returns exact and interpolated positions without one sided extrapolation`() {
        val markers =
            listOf(
                RowMarker(row = 10, page = 1, yPosition = 0.2f),
                RowMarker(row = 20, page = 1, yPosition = 0.8f),
            )

        assertEquals(0.2f, interpolateYPosition(markers, targetRow = 10, page = 1))
        assertEquals(0.5f, interpolateYPosition(markers, targetRow = 15, page = 1))
        assertNull(interpolateYPosition(markers, targetRow = 5, page = 1))
        assertNull(interpolateYPosition(markers, targetRow = 25, page = 1))
        assertNull(interpolateYPosition(markers, targetRow = 15, page = 2))
    }

    @Test
    fun `resolveReadingLineYFraction returns exact marker and interpolates between two page markers`() {
        val markers =
            listOf(
                RowMarker(row = 10, page = 1, yPosition = 0.2f),
                RowMarker(row = 20, page = 1, yPosition = 0.8f),
            )

        assertEquals(
            0.2f,
            requireNotNull(
                resolveReadingLineYFraction(
                    markers = markers,
                    currentRow = 10,
                    currentPage = 1,
                    currentYFraction = 0.5f,
                    rowDelta = 0,
                ),
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            requireNotNull(
                resolveReadingLineYFraction(
                    markers = markers,
                    currentRow = 15,
                    currentPage = 1,
                    currentYFraction = 0.5f,
                    rowDelta = 5,
                ),
            ),
            0.0001f,
        )
    }

    @Test
    fun `resolveReadingLineYFraction uses row step fallback for one marker instead of locking to anchor`() {
        val markers = listOf(RowMarker(row = 13, page = 1, yPosition = 0.45f))

        assertEquals(
            0.57f,
            requireNotNull(
                resolveReadingLineYFraction(
                    markers = markers,
                    currentRow = 19,
                    currentPage = 1,
                    currentYFraction = 0.45f,
                    rowDelta = 6,
                ),
            ),
            0.0001f,
        )
    }

    @Test
    fun `resolveReadingLineYFraction ignores markers from other pages and returns null without row delta`() {
        val markers =
            listOf(
                RowMarker(row = 10, page = 1, yPosition = 0.2f),
                RowMarker(row = 20, page = 1, yPosition = 0.8f),
            )

        assertNull(
            resolveReadingLineYFraction(
                markers = markers,
                currentRow = 15,
                currentPage = 2,
                currentYFraction = 0.5f,
                rowDelta = 0,
            ),
        )
        assertEquals(
            0.34f,
            requireNotNull(
                resolveReadingLineYFraction(
                    markers = markers,
                    currentRow = 17,
                    currentPage = 2,
                    currentYFraction = 0.3f,
                    rowDelta = 2,
                ),
            ),
            0.0001f,
        )
    }
}
