package com.finnvek.knittools.ui.screens.gauge

import com.finnvek.knittools.ai.nano.ParsedInstruction
import org.junit.Assert.assertEquals
import org.junit.Test

class GaugeScreenParsingTest {
    @Test
    fun `metric gauge pasted into imperial screen is converted before autofill`() {
        val fields =
            parseGaugeInstruction(
                parsed =
                    ParsedInstruction.Gauge(
                        stitchesPer10cm = 22.0,
                        rowsPer10cm = 30.0,
                        unit = ParsedInstruction.GaugeUnit.PER_10_CM,
                    ),
                useImperial = true,
            )

        assertEquals("22.4", fields?.yourSt)
        assertEquals("30.5", fields?.yourRows)
    }

    @Test
    fun `metric swatch pasted into imperial screen is converted before autofill`() {
        val fields =
            parseGaugeInstruction(
                parsed =
                    ParsedInstruction.GaugeSwatch(
                        width = 10.0,
                        stitches = 24,
                        height = 10.0,
                        rows = 32,
                        lengthUnit = ParsedInstruction.LengthUnit.CM,
                    ),
                useImperial = true,
            )

        assertEquals("3.9", fields?.swatchWidth)
        assertEquals("24", fields?.swatchStitches)
        assertEquals("3.9", fields?.swatchHeight)
        assertEquals("32", fields?.swatchRows)
    }

    @Test
    fun `imperial gauge pasted into metric screen is converted before autofill`() {
        val fields =
            parseGaugeInstruction(
                parsed =
                    ParsedInstruction.Gauge(
                        stitchesPer10cm = 22.0,
                        rowsPer10cm = 28.0,
                        unit = ParsedInstruction.GaugeUnit.PER_4_INCHES,
                    ),
                useImperial = false,
            )

        assertEquals("21.7", fields?.yourSt)
        assertEquals("27.6", fields?.yourRows)
    }
}
