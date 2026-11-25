package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RowMappingParserTest {
    @Test
    fun `only exact and interpolated positions count as calibrated`() {
        assertEquals(
            setOf(
                ReadingLineResolutionKind.EXACT_MARKER,
                ReadingLineResolutionKind.SAME_PAGE_INTERPOLATION,
                ReadingLineResolutionKind.CROSS_PAGE_INTERPOLATION,
            ),
            ReadingLineResolutionKind.entries.filter { it.isCalibrated }.toSet(),
        )
    }

    @Test
    fun `cross page tracking falls back for duplicate or coincident bracket endpoints`() {
        val lower = RowMarker(1, 0, 1f)
        val upper = RowMarker(5, 1, 0f)
        val brackets =
            listOf(
                listOf(lower, upper),
                listOf(lower, lower.copy(page = 1), upper.copy(page = 2)),
                listOf(lower, upper, upper.copy(page = 2)),
            )
        for (markers in brackets) {
            val result = resolveReadingLineLocation(markers, 2, 3, 0, 0.5f)
            assertEquals(ReadingLineResolutionKind.ROW_DELTA_FALLBACK, result.kind)
            assertEquals(0, result.targetPage)
        }
    }

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
    fun `parseMapping preserves valid markers around malformed records`() {
        val json =
            """
            [
              {"row": 2, "page": 0, "yPosition": 0.2},
              {"row": "invalid", "page": 0, "yPosition": 0.4},
              {"row": 4, "page": 0},
              {"row": 6, "page": 1, "yPosition": 0.6}
            ]
            """.trimIndent()

        assertEquals(
            listOf(
                RowMarker(row = 2, page = 0, yPosition = 0.2f),
                RowMarker(row = 6, page = 1, yPosition = 0.6f),
            ),
            parseMapping(json),
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
    fun `mapping input and serialization are bounded`() {
        assertTrue(parseMapping("[" + "0".repeat(ROW_MAPPING_MAX_JSON_LENGTH)).isEmpty())

        val serialized =
            serializeMapping(
                (0..ROW_MAPPING_MAX_MARKERS).map { row ->
                    RowMarker(row = row, page = 0, yPosition = 0.5f)
                },
            )

        assertTrue(serialized.length <= ROW_MAPPING_MAX_JSON_LENGTH)
        assertEquals(ROW_MAPPING_MAX_MARKERS, Json.parseToJsonElement(serialized).jsonArray.size)
        assertEquals(ROW_MAPPING_MAX_MARKERS, parseMapping(serialized).size)
    }

    @Test
    fun `mapping input limit counts UTF-8 bytes in unknown fields`() {
        val prefix = """[{"row":2,"page":0,"yPosition":0.5,"ignored":""" + "\""
        val suffix = "\"}]"
        val padding = "ä".repeat((ROW_MAPPING_MAX_JSON_LENGTH - prefix.length - suffix.length) / 2)
        val atLimit = prefix + padding + suffix

        assertEquals(listOf(RowMarker(2, 0, 0.5f)), parseMapping(atLimit))
        assertTrue(parseMapping(prefix + padding + "ä" + suffix).isEmpty())
    }

    @Test
    fun `calibration rejects invalid coordinates without accepting one endpoint`() {
        assertNull(createCalibrationRowMarkers(-1, 0, 0.2f, 4, 0, 0.4f))
        assertNull(createCalibrationRowMarkers(2, 0, 0.2f, 4, -1, 0.4f))
        assertNull(createCalibrationRowMarkers(2, 0, Float.NaN, 4, 0, 0.4f))
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
}
