package com.finnvek.knittools.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YarnLabelGeminiScannerTest {
    @Test
    fun `parseResponse extracts all fields from valid JSON`() {
        val json =
            """
            {
              "brand": "Drops",
              "name": "Alpaca",
              "fiberContent": "100% Alpaca",
              "weightCategory": "DK",
              "metersPerSkein": 167,
              "gramsPerSkein": 50,
              "recommendedNeedleMm": 4.0,
              "gaugeStitches": 22,
              "gaugeRows": 30,
              "colorName": "Natural White",
              "colorNumber": "0100",
              "dyeLot": "2847",
              "careInstructions": "Hand wash 30°C"
            }
            """.trimIndent()

        val result = YarnLabelGeminiScanner.parseResponse(json)
        assertNotNull(result)
        assertEquals("Drops", result!!.brand)
        assertEquals("Alpaca", result.yarnName)
        assertEquals("100% Alpaca", result.fiberContent)
        assertEquals("DK", result.weightCategory)
        assertEquals("167", result.lengthMeters)
        assertEquals("50", result.weightGrams)
        assertEquals("4mm", result.needleSize)
        assertEquals("22 sts × 30 rows = 10cm", result.gaugeInfo)
        assertEquals("Natural White", result.colorName)
        assertEquals("0100", result.colorNumber)
        assertEquals("2847", result.dyeLot)
    }

    @Test
    fun `parseResponse handles null fields gracefully`() {
        val json =
            """
            {
              "brand": "Novita",
              "name": "Nalle",
              "fiberContent": null,
              "weightCategory": null,
              "metersPerSkein": null,
              "gramsPerSkein": 100,
              "recommendedNeedleMm": null,
              "gaugeStitches": null,
              "gaugeRows": null,
              "colorName": null,
              "colorNumber": null,
              "dyeLot": null,
              "careInstructions": null
            }
            """.trimIndent()

        val result = YarnLabelGeminiScanner.parseResponse(json)
        assertNotNull(result)
        assertEquals("Novita", result!!.brand)
        assertEquals("Nalle", result.yarnName)
        assertEquals("", result.fiberContent)
        assertEquals("100", result.weightGrams)
        assertEquals("", result.lengthMeters)
        assertEquals("", result.needleSize)
        assertEquals("", result.gaugeInfo)
    }

    @Test
    fun `parseResponse returns null for error response`() {
        val json = """{"error": "not_a_yarn_label"}"""
        assertNull(YarnLabelGeminiScanner.parseResponse(json))
    }

    @Test
    fun `parseResponse returns null for garbage input`() {
        assertNull(YarnLabelGeminiScanner.parseResponse("this is not json"))
    }

    @Test
    fun `extractJson handles markdown code block`() {
        val response =
            """
            Here is the result:
            ```json
            {"brand": "Test"}
            ```
            """.trimIndent()

        val json = YarnLabelGeminiScanner.extractJson(response)
        assertEquals("""{"brand": "Test"}""", json)
    }

    @Test
    fun `extractJson handles raw JSON`() {
        val response = """{"brand": "Test"}"""
        assertEquals(response, YarnLabelGeminiScanner.extractJson(response))
    }

    @Test
    fun `extractJson handles JSON embedded in text`() {
        val response = """Sure! Here's the data: {"brand": "Test", "name": "Yarn"} Hope this helps!"""
        val json = YarnLabelGeminiScanner.extractJson(response)
        assertEquals("""{"brand": "Test", "name": "Yarn"}""", json)
    }

    @Test
    fun `parseResponse handles only gauge stitches without rows`() {
        val json =
            """
            {
              "brand": "Test",
              "name": "Yarn",
              "gaugeStitches": 22,
              "gaugeRows": null
            }
            """.trimIndent()

        val result = YarnLabelGeminiScanner.parseResponse(json)
        assertEquals("22 sts = 10cm", result!!.gaugeInfo)
    }

    @Test
    fun `parseResponse formats fractional needle size`() {
        val json =
            """
            {
              "brand": "Test",
              "name": "Yarn",
              "recommendedNeedleMm": 3.5
            }
            """.trimIndent()

        val result = YarnLabelGeminiScanner.parseResponse(json)
        assertEquals("3.5mm", result!!.needleSize)
    }
}
