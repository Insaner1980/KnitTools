package com.finnvek.knittools.ai.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YarnLabelParserTest {
    @Test
    fun `extract weight in grams`() {
        assertEquals("50", YarnLabelParser.extractWeight("50g / 175m"))
        assertEquals("100", YarnLabelParser.extractWeight("Weight: 100 grams"))
        assertEquals("50", YarnLabelParser.extractWeight("50 gr"))
    }

    @Test
    fun `extract length in meters`() {
        assertEquals("175", YarnLabelParser.extractLength("50g / 175m"))
        assertEquals("200", YarnLabelParser.extractLength("200 meters"))
        assertEquals("150", YarnLabelParser.extractLength("approx. 150 mtr"))
    }

    @Test
    fun `extract length in yards`() {
        assertEquals("220", YarnLabelParser.extractLength("220 yds"))
        assertEquals("191", YarnLabelParser.extractLength("191 yards"))
    }

    @Test
    fun `extract needle size mm`() {
        assertEquals("3.5", YarnLabelParser.extractNeedleSize("Needle: 3.5mm"))
        assertEquals("4", YarnLabelParser.extractNeedleSize("4mm needles"))
        assertEquals("3.5", YarnLabelParser.extractNeedleSize("3,5 mm"))
    }

    @Test
    fun `extract needle size US`() {
        assertEquals("US 4", YarnLabelParser.extractNeedleSize("US 4 needles"))
    }

    @Test
    fun `extract fiber content`() {
        val result = YarnLabelParser.extractFiber("100% Merino Wool")
        assertEquals("100% Merino", result)
    }

    @Test
    fun `extract mixed fiber content`() {
        val result = YarnLabelParser.extractFiber("75% Wool 25% Nylon")
        assertTrue(result.contains("75% Wool"))
        assertTrue(result.contains("25% Nylon"))
    }

    @Test
    fun `extract color number`() {
        assertEquals("46", YarnLabelParser.extractColorNumber("Color 46 Old Pink"))
        assertEquals("123", YarnLabelParser.extractColorNumber("Col. #123"))
        assertEquals("5", YarnLabelParser.extractColorNumber("Colour No. 5"))
    }

    @Test
    fun `extract dye lot`() {
        assertEquals("23891", YarnLabelParser.extractDyeLot("Dye Lot 23891"))
        assertEquals("45678", YarnLabelParser.extractDyeLot("Lot #45678"))
    }

    @Test
    fun `extract weight category`() {
        assertEquals("DK", YarnLabelParser.extractWeightCategory("DK weight yarn"))
        assertEquals("Worsted", YarnLabelParser.extractWeightCategory("Worsted Weight"))
        assertEquals("Fingering", YarnLabelParser.extractWeightCategory("Fingering / 4-ply"))
        assertEquals("Super Bulky", YarnLabelParser.extractWeightCategory("Super Bulky"))
    }

    @Test
    fun `extract weight category prefers longer match`() {
        assertEquals("Super Bulky", YarnLabelParser.extractWeightCategory("Super Bulky yarn"))
    }

    @Test
    fun `full label parse`() {
        val label =
            """
            Drops
            Baby Merino
            100% Merino Wool
            50g / 175m
            Needle: 3.5mm
            Color 46 Old Pink
            Dye Lot 23891
            DK
            """.trimIndent()

        val result = YarnLabelParser.parse(label)
        assertEquals("50", result.weightGrams)
        assertEquals("175", result.lengthMeters)
        assertEquals("3.5", result.needleSize)
        assertEquals("DK", result.weightCategory)
        assertEquals("46", result.colorNumber)
        assertEquals("23891", result.dyeLot)
        assertTrue(result.fiberContent.contains("Merino"))
    }

    @Test
    fun `empty text returns empty fields`() {
        val result = YarnLabelParser.parse("")
        assertEquals("", result.weightGrams)
        assertEquals("", result.lengthMeters)
        assertEquals("", result.needleSize)
    }

    @Test
    fun `partial label extracts what it can`() {
        val label = "50g 175m"
        val result = YarnLabelParser.parse(label)
        assertEquals("50", result.weightGrams)
        assertEquals("175", result.lengthMeters)
        assertEquals("", result.needleSize)
        assertEquals("", result.dyeLot)
    }
}
