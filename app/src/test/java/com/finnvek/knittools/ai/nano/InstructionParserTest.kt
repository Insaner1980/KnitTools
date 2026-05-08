package com.finnvek.knittools.ai.nano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionParserTest {
    // === parseResponse: key:value Nano-vastaukset ===

    @Test
    fun `parseResponse - increase`() {
        val result = InstructionParser.parseResponse("TYPE: INCREASE\nCURRENT: 96\nCHANGE: 12")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `parseResponse - decrease`() {
        val result = InstructionParser.parseResponse("TYPE: DECREASE\nCURRENT: 120\nCHANGE: 8")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(120, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    @Test
    fun `parseResponse - gauge`() {
        val result = InstructionParser.parseResponse("GAUGE_STITCHES: 22.5\nGAUGE_ROWS: 30\nGAUGE_UNIT: 10CM")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.5, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `parseResponse - swatch`() {
        val result =
            InstructionParser.parseResponse(
                "SWATCH_WIDTH: 13.5\nSWATCH_STITCHES: 30\nSWATCH_HEIGHT: 15\nSWATCH_ROWS: 38\nSWATCH_UNIT: CM",
            )
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(13.5, s.width!!, 0.01)
        assertEquals(30, s.stitches)
        assertEquals(15.0, s.height!!, 0.01)
        assertEquals(38, s.rows)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `parseResponse - swatch partial`() {
        val result = InstructionParser.parseResponse("SWATCH_WIDTH: 30\nSWATCH_STITCHES: 22\nSWATCH_UNIT: IN")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(30.0, s.width!!, 0.01)
        assertEquals(22, s.stitches)
        assertEquals(null, s.height)
        assertEquals(null, s.rows)
        assertEquals(ParsedInstruction.LengthUnit.INCHES, s.lengthUnit)
    }

    @Test
    fun `parseResponse - extra whitespace`() {
        val result = InstructionParser.parseResponse("TYPE:   INCREASE\nCURRENT:  96\nCHANGE:  12")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
    }

    @Test
    fun `parseResponse - cannot parse`() {
        assertTrue(InstructionParser.parseResponse("CANNOT_PARSE") is ParsedInstruction.Failure)
    }

    @Test
    fun `parseResponse - empty`() {
        assertTrue(InstructionParser.parseResponse("") is ParsedInstruction.Failure)
    }

    @Test
    fun `parseResponse - missing field`() {
        assertTrue(InstructionParser.parseResponse("TYPE: INCREASE\nCURRENT: 96") is ParsedInstruction.Failure)
    }

    // === parseWithRegex: Increase/Decrease ===

    @Test
    fun `regex - increase X stitches across Y`() {
        val result = InstructionParser.parseWithRegex("INCREASE 12 STITCHES EVENLY ACROSS 96 STITCHES")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `regex - dec 8 sts over 120`() {
        val result = InstructionParser.parseWithRegex("DEC 8 STS OVER 120 STS")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(120, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    @Test
    fun `regex - increase to 108 from 96`() {
        val result = InstructionParser.parseWithRegex("INCREASE EVENLY TO 108 FROM 96")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `regex - increase to 108 currently 96`() {
        val result = InstructionParser.parseWithRegex("INCREASE TO 108 STITCHES CURRENTLY 96")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
    }

    @Test
    fun `regex - k2tog every 12th st 96 sts`() {
        val result = InstructionParser.parseWithRegex("K2TOG EVERY 12TH STITCH (96 STS)")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    @Test
    fun `regex - inc 1 stitch every 8th stitch across 96`() {
        val result = InstructionParser.parseWithRegex("INC 1 ST IN EVERY 8TH ST ACROSS 96")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `regex - inc abbreviated`() {
        val result = InstructionParser.parseWithRegex("INC 10 STS ACROSS 80 STS")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(80, inc.currentStitches)
        assertEquals(10, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    // === parseWithRegex: Gauge ===

    @Test
    fun `regex - 22 sts and 30 rows = 10cm`() {
        val result = InstructionParser.parseWithRegex("22 STS AND 30 ROWS = 10CM")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `regex - tension 28 sts x 36 rows to 10cm`() {
        val result = InstructionParser.parseWithRegex("TENSION: 28 STS X 36 ROWS TO 10CM ON 4MM NEEDLES")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(28.0, g.stitchesPer10cm, 0.01)
        assertEquals(36.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `regex - gauge 22 per 30`() {
        val result = InstructionParser.parseWithRegex("GAUGE 22/30")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `regex - sts per inch normalizes to 4 inch gauge unit`() {
        val result = InstructionParser.parseWithRegex("5.5 STS PER INCH, 7 ROWS PER INCH")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(28.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_4_INCHES, g.unit)
    }

    @Test
    fun `regex - 20 stitches = 4 inches`() {
        val result = InstructionParser.parseWithRegex("20 STITCHES AND 28 ROWS = 4 INCHES")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(20.0, g.stitchesPer10cm, 0.01)
        assertEquals(28.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_4_INCHES, g.unit)
    }

    @Test
    fun `regex - 22 stitches 30 rows no context`() {
        val result = InstructionParser.parseWithRegex("22 STITCHES 30 ROWS")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    // === parseWithRegex: Swatch ===

    @Test
    fun `regex - measured width is 30 cm`() {
        val result = InstructionParser.parseWithRegex("MEASURED WIDTH IS 30 CM")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(30.0, s.width!!, 0.01)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `regex - width 30 22 stitches`() {
        val result = InstructionParser.parseWithRegex("WIDTH 30, 22 STITCHES")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(30.0, s.width!!, 0.01)
        assertEquals(22, s.stitches)
        assertEquals(null, s.lengthUnit)
    }

    @Test
    fun `regex - swatch 12cm with 26 stitches`() {
        val result = InstructionParser.parseWithRegex("MY SWATCH IS 12CM WITH 26 STITCHES")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(12.0, s.width!!, 0.01)
        assertEquals(26, s.stitches)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `regex - 24 sts over 10cm`() {
        val result = InstructionParser.parseWithRegex("24 STS OVER 10 CM")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(10.0, s.width!!, 0.01)
        assertEquals(24, s.stitches)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `regex - I got 22 sts in 10cm`() {
        val result = InstructionParser.parseWithRegex("I GOT 22 STS IN 10CM")
        assertTrue("Expected GaugeSwatch but got $result", result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(22, s.stitches)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `regex - height and rows`() {
        val result = InstructionParser.parseWithRegex("HEIGHT 15 CM, 30 ROWS")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(15.0, s.height!!, 0.01)
        assertEquals(30, s.rows)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    // === Typo tolerance ===

    @Test
    fun `typo - increse 12 stiches accross 96`() {
        val result = InstructionParser.parseWithRegex("INCRESE 12 STICHES ACCROSS 96 STICHES")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
    }

    @Test
    fun `typo - guage 22 stiches 30 roews`() {
        val result = InstructionParser.parseWithRegex("GUAGE: 22 STICHES AND 30 ROEWS")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `typo - mesured widht is 30`() {
        val result = InstructionParser.parseWithRegex("MESURED WIDHT IS 30 CM")
        assertTrue(result is ParsedInstruction.GaugeSwatch)
        val s = result as ParsedInstruction.GaugeSwatch
        assertEquals(30.0, s.width!!, 0.01)
        assertEquals(ParsedInstruction.LengthUnit.CM, s.lengthUnit)
    }

    @Test
    fun `typo - decrese 8 accross 120`() {
        val result = InstructionParser.parseWithRegex("DECRESE 8 STS ACCROSS 120 STS")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(120, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    // === Edge cases ===

    @Test
    fun `regex - garbage returns failure`() {
        assertTrue(InstructionParser.parseWithRegex("HELLO WORLD") is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - empty returns failure`() {
        assertTrue(InstructionParser.parseWithRegex("") is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - only numbers returns failure`() {
        assertTrue(InstructionParser.parseWithRegex("42 96 12") is ParsedInstruction.Failure)
    }
}
