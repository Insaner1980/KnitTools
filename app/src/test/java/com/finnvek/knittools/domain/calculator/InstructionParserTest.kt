package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionParserTest {
    @Test(timeout = 3_000)
    fun `long unmatched numeric and whitespace runs finish parsing`() {
        listOf("9".repeat(10_000), "TENSION " + " ".repeat(9_992)).forEach { input ->
            assertEquals(
                ParsedInstruction.Failure("parse_failed", ParsedInstruction.ErrorType.PARSE_FAILED),
                InstructionParser.parse(input),
            )
        }
    }

    @Test
    fun `gauge accepts abbreviated singular and plural inches with numeric boundaries`() {
        listOf("4in", "4 in", "4 inch", "4 inches").forEach { unit ->
            assertEquals(
                ParsedInstruction.Gauge(20.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES),
                InstructionParser.parse("gauge: 20 / 28 over $unit"),
            )
        }
        listOf("14 inches", "24 inches", "1.4 inches").forEach { unit ->
            assertTrue(InstructionParser.parse("gauge: 20 / 28 over $unit") is ParsedInstruction.Failure)
        }
        listOf("4 index", "4 inche").forEach { unit ->
            assertEquals(
                ParsedInstruction.Gauge(20.0, 28.0, ParsedInstruction.GaugeUnit.PER_10_CM),
                InstructionParser.parse("gauge: 20 / 28 over $unit"),
            )
        }
    }

    @Test fun lengthAndGaugeUnitsDoNotTreatPrepositionsOrOtherNumbersAsInches() {
        assertEquals(
            ParsedInstruction.GaugeSwatch(width = 12.0, stitches = 20, lengthUnit = null),
            InstructionParser.parse("20 stitches in width 12"),
        )
        assertEquals(
            ParsedInstruction.GaugeSwatch(width = 4.0, stitches = 20, lengthUnit = ParsedInstruction.LengthUnit.INCHES),
            InstructionParser.parse("20 stitches in width 4in"),
        )
        assertEquals(
            ParsedInstruction.Gauge(20.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES),
            InstructionParser.parse("gauge: 20 / 28 over 4in"),
        )
        assertTrue(InstructionParser.parse("gauge: 20 / 28 over 14 inches") is ParsedInstruction.Failure)
    }

    @Test
    fun `parseWithRegex - syotteen pituusraja erottaa sallitun ja ylipitkan syotteen`() {
        val instruction = "Increase 12 stitches evenly across 96 stitches"
        listOf(9_999, 10_000).forEach { length ->
            assertEquals(
                ParsedInstruction.IncreaseDecrease(96, 12, true),
                InstructionParser.parse(instruction.padEnd(length)),
            )
        }
        assertEquals(
            ParsedInstruction.Failure("input_too_long", ParsedInstruction.ErrorType.PARSE_FAILED),
            InstructionParser.parse(instruction.padEnd(10_001)),
        )
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
    fun `regex - contradictory increase target returns failure`() {
        assertTrue(InstructionParser.parseWithRegex("INCREASE TO 96 FROM 108") is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - contradictory decrease target returns failure`() {
        assertTrue(InstructionParser.parseWithRegex("DECREASE TO 108 FROM 96") is ParsedInstruction.Failure)
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
    fun `every nth increase accepts a count followed by punctuation`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 12, true),
            InstructionParser.parse("Inc 1 st in every 8th st across 96."),
        )
        assertTrue(InstructionParser.parse("Inc 1 st in every 8th st across 4 sections") is ParsedInstruction.Failure)
    }

    @Test
    fun `every nth change preserves direction and works without a preposition`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 12, true),
            InstructionParser.parse("Inc 1 st every 8th st across 96 sts"),
        )
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 12, false),
            InstructionParser.parse("Decrease 1 stitch in every 8th stitch over 96 stitches"),
        )
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 12, false),
            InstructionParser.parse("Dec 1 st every 8th st across 96 sts"),
        )
    }

    @Test
    fun `leading stitch count applies to every nth decrease`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(80, 8, false),
            InstructionParser.parse("80 stitches, decrease 1 stitch every 10th stitch"),
        )
    }

    @Test
    fun `leading stitch count applies to every nth increase`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 12, true),
            InstructionParser.parse("96 sts, inc 1 st in every 8th st"),
        )
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

    @Test
    fun `regex - section count after across does not become current stitch count`() {
        val result = InstructionParser.parseWithRegex("Increase 8 sts evenly across 4 sections, ending with 96 sts")

        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - reverse increase form`() {
        val result = InstructionParser.parseWithRegex("96 STITCHES INCREASE 12")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `regex - reverse decrease form`() {
        val result = InstructionParser.parseWithRegex("120 STITCHES DECREASE 8")
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(120, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    // === parseWithRegex: Gauge ===

    @Test
    fun `regex - 22 sts and 30 rows = 10cm`() {
        // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
        val result = InstructionParser.parseWithRegex("22 STS AND 30 ROWS = 10CM")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    @Test
    fun `regex - tension 28 sts x 36 rows to 10cm`() {
        // CPD-ON
        val result = InstructionParser.parseWithRegex("TENSION: 28 STS X 36 ROWS TO 10CM ON 4MM NEEDLES")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(28.0, g.stitchesPer10cm, 0.01)
        assertEquals(36.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }

    // CPD-OFF: Parseriesimerkin odotukset pidetaan testitapauksen yhteydessa.
    @Test
    fun `regex - gauge 22 per 30`() {
        val result = InstructionParser.parseWithRegex("GAUGE 22/30")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }
    // CPD-ON

    @Test
    fun `regex - labelled gauge values win over unrelated leading number`() {
        val result = InstructionParser.parseWithRegex("GAUGE ON 4 MM: 22 STS 30 ROWS")

        assertTrue(result is ParsedInstruction.Gauge)
        val gauge = result as ParsedInstruction.Gauge
        assertEquals(22.0, gauge.stitchesPer10cm, 0.01)
        assertEquals(30.0, gauge.rowsPer10cm, 0.01)
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
    fun `labelled per inch gauge retains its physical basis`() {
        listOf("", "Gauge: ", "Tension: ").forEach { prefix ->
            assertEquals(
                ParsedInstruction.Gauge(22.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES),
                InstructionParser.parse("${prefix}5.5 stitches per inch, 7 rows per inch"),
            )
            assertEquals(
                ParsedInstruction.Gauge(22.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES),
                InstructionParser.parse("${prefix}5.5 stitches and 7 rows per inch"),
            )
        }
    }

    @Test
    fun `unrelated numbers do not complete a single per inch gauge axis`() {
        listOf(
            "Gauge on 4 mm needles: 5.5 stitches per inch",
            "Gauge: 7 rows per inch on 4 mm needles",
            "Gauge: 5.5 stitches per inch",
        ).forEach { instruction ->
            assertTrue(InstructionParser.parse(instruction) is ParsedInstruction.Failure)
        }
    }

    @Test
    fun `an explicit swatch unit is not taken from unrelated text`() {
        assertEquals(
            ParsedInstruction.GaugeSwatch(4.0, 22, lengthUnit = ParsedInstruction.LengthUnit.INCHES),
            InstructionParser.parse("10 cm reference. Width 4 in; 22 stitches"),
        )
    }

    @Test
    fun `explicit swatch dimensions take precedence over adjacent stitch and row counts`() {
        assertEquals(
            ParsedInstruction.GaugeSwatch(14.0, 33, 15.0, 44, ParsedInstruction.LengthUnit.CM),
            InstructionParser.parse("Width 14 cm, height 15 cm, 33 stitches and 44 rows"),
        )
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

    // CPD-OFF: Parseriesimerkin odotukset pidetaan testitapauksen yhteydessa.
    @Test
    fun `regex - 22 stitches 30 rows no context`() {
        val result = InstructionParser.parseWithRegex("22 STITCHES 30 ROWS")
        assertTrue(result is ParsedInstruction.Gauge)
        val g = result as ParsedInstruction.Gauge
        assertEquals(22.0, g.stitchesPer10cm, 0.01)
        assertEquals(30.0, g.rowsPer10cm, 0.01)
        assertEquals(ParsedInstruction.GaugeUnit.PER_10_CM, g.unit)
    }
    // CPD-ON

    @Test
    fun `regex - row repeat does not become gauge`() {
        val result = InstructionParser.parseWithRegex("Repeat the last 4 rows until 96 sts remain")

        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - row range with stitch count returns failure`() {
        val result = InstructionParser.parseWithRegex("Rows 1-4: repeat from asterisk across; 96 sts remain")

        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `regex - crochet round range returns failure`() {
        val result = InstructionParser.parseWithRegex("Rounds 3-6: repeat from asterisk around; 48 sts remain")

        assertTrue(result is ParsedInstruction.Failure)
    }

    // === parseWithRegex: Swatch ===

    @Test
    fun `measured stitch count is not interpreted as a swatch width`() {
        val result = InstructionParser.parse("I measured 20 stitches over 10 cm") as ParsedInstruction.GaugeSwatch
        assertEquals(10.0, result.width)
        assertEquals(20, result.stitches)
    }

    @Test
    fun `unsupported explicit gauge basis cannot use the default basis`() {
        val result = InstructionParser.parse("Gauge: 20 sts and 30 rows per 5 cm")
        assertTrue(result is ParsedInstruction.Failure)
    }

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
    fun `regex counts both stitches removed by sk2p`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 16, false),
            InstructionParser.parse("SK2P EVERY 12TH STITCH (96 STS)"),
        )
    }

    @Test
    fun `regex counts both stitches removed by s2kp`() {
        assertEquals(
            ParsedInstruction.IncreaseDecrease(96, 16, false),
            InstructionParser.parse("S2KP EVERY 12TH STITCH (96 STS)"),
        )
    }

    @Test
    fun `regex rejects an unrepresentable double decrease count`() {
        assertTrue(InstructionParser.parse("S2KP EVERY 1ST STITCH (2147483647 STS)") is ParsedInstruction.Failure)
    }

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
