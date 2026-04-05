package com.finnvek.knittools.ai.nano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YarnLabelNanoParserTest {
    @Test
    fun `parse complete response`() {
        val response =
            """
            BRAND: Drops
            YARN_NAME: Baby Merino
            WEIGHT_GRAMS: 50
            LENGTH_METERS: 175
            NEEDLE_SIZE: 3.5
            FIBER: 100% Wool
            COLOR_NAME: Old Pink
            COLOR_NUMBER: 46
            DYE_LOT: 2024-A
            WEIGHT_CATEGORY: DK
            GAUGE: 22 sts x 30 rows = 10cm
            """.trimIndent()

        val result = YarnLabelNanoParser.parseResponse(response)
        assertNotNull(result)
        assertEquals("Drops", result!!.brand)
        assertEquals("Baby Merino", result.yarnName)
        assertEquals("50", result.weightGrams)
        assertEquals("175", result.lengthMeters)
        assertEquals("3.5", result.needleSize)
        assertEquals("100% Wool", result.fiberContent)
        assertEquals("Old Pink", result.colorName)
        assertEquals("46", result.colorNumber)
        assertEquals("2024-A", result.dyeLot)
        assertEquals("DK", result.weightCategory)
    }

    @Test
    fun `parse partial response`() {
        val response =
            """
            BRAND: Novita
            YARN_NAME: Nalle
            WEIGHT_GRAMS: 100
            LENGTH_METERS: 260
            NEEDLE_SIZE: 3.5 - 4.0
            FIBER: 75% Wool, 25% Polyamide
            COLOR_NAME:
            COLOR_NUMBER:
            DYE_LOT:
            WEIGHT_CATEGORY: Fingering
            GAUGE:
            """.trimIndent()

        val result = YarnLabelNanoParser.parseResponse(response)
        assertNotNull(result)
        assertEquals("Novita", result!!.brand)
        assertEquals("Nalle", result.yarnName)
        assertEquals("100", result.weightGrams)
        assertEquals("260", result.lengthMeters)
        assertEquals("75% Wool, 25% Polyamide", result.fiberContent)
    }

    @Test
    fun `parse response with extra whitespace`() {
        val response =
            """
            BRAND:   Schachenmayr
            YARN_NAME:  Catania
            WEIGHT_GRAMS: 50
            LENGTH_METERS: 125
            NEEDLE_SIZE: 3.0
            FIBER: 100% Cotton
            COLOR_NAME: White
            COLOR_NUMBER: 00106
            DYE_LOT:
            WEIGHT_CATEGORY: Sport
            GAUGE: 26 sts x 36 rows = 10cm
            """.trimIndent()

        val result = YarnLabelNanoParser.parseResponse(response)
        assertNotNull(result)
        assertEquals("Schachenmayr", result!!.brand)
        assertEquals("Catania", result.yarnName)
    }

    @Test
    fun `parse empty response returns null`() {
        assertNull(YarnLabelNanoParser.parseResponse(""))
    }

    @Test
    fun `parse all-empty values returns null`() {
        val response =
            """
            BRAND:
            YARN_NAME:
            WEIGHT_GRAMS:
            LENGTH_METERS:
            NEEDLE_SIZE:
            FIBER:
            COLOR_NAME:
            COLOR_NUMBER:
            DYE_LOT:
            WEIGHT_CATEGORY:
            GAUGE:
            """.trimIndent()
        assertNull(YarnLabelNanoParser.parseResponse(response))
    }

    @Test
    fun `parse garbage returns null`() {
        assertNull(YarnLabelNanoParser.parseResponse("I don't understand this text"))
    }
}
