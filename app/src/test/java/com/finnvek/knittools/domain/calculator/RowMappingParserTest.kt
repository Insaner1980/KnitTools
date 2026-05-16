package com.finnvek.knittools.domain.calculator

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
    fun `interpolateYPosition returns exact previous next and interpolated positions`() {
        val markers =
            listOf(
                RowMarker(row = 10, page = 1, yPosition = 0.2f),
                RowMarker(row = 20, page = 1, yPosition = 0.8f),
            )

        assertEquals(0.2f, interpolateYPosition(markers, targetRow = 10, page = 1))
        assertEquals(0.5f, interpolateYPosition(markers, targetRow = 15, page = 1))
        assertEquals(0.2f, interpolateYPosition(markers, targetRow = 5, page = 1))
        assertEquals(0.8f, interpolateYPosition(markers, targetRow = 25, page = 1))
        assertNull(interpolateYPosition(markers, targetRow = 15, page = 2))
    }
}
