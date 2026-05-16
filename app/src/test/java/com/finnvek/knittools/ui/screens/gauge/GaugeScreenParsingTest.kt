package com.finnvek.knittools.ui.screens.gauge

import com.finnvek.knittools.ai.nano.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeConversionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `conversion rejects zero or negative personal gauge`() {
        assertNull(parseAndConvertForTest(patternSt = "20", patternRows = "30", yourSt = "0", yourRows = "30"))
        assertNull(parseAndConvertForTest(patternSt = "20", patternRows = "30", yourSt = "20", yourRows = "0"))
        assertNull(parseAndConvertForTest(patternSt = "20", patternRows = "30", yourSt = "-1", yourRows = "30"))
    }

    @Test
    fun `conversion rejects zero or negative pattern instruction counts`() {
        assertNull(parseAndConvertForTest(stitchCount = "0", rowCount = "80"))
        assertNull(parseAndConvertForTest(stitchCount = "100", rowCount = "0"))
        assertNull(parseAndConvertForTest(stitchCount = "-1", rowCount = "80"))
    }

    private fun parseAndConvertForTest(
        patternSt: String = "20",
        patternRows: String = "30",
        yourSt: String = "22",
        yourRows: String = "32",
        stitchCount: String = "100",
        rowCount: String = "80",
    ): GaugeConversionResult? =
        parseAndConvertMethod.invoke(
            null,
            patternSt,
            patternRows,
            yourSt,
            yourRows,
            stitchCount,
            rowCount,
        ) as GaugeConversionResult?

    private companion object {
        private val parseAndConvertMethod =
            Class
                .forName("com.finnvek.knittools.ui.screens.gauge.GaugeScreenKt")
                .getDeclaredMethod(
                    "parseAndConvert",
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                ).apply { isAccessible = true }
    }
}
