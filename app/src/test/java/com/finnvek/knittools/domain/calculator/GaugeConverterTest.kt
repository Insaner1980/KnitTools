package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class GaugeConverterTest {
    @Test
    fun `same gauge returns same counts`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 22.0,
                patternRowGauge = 30.0,
                yourStitchGauge = 22.0,
                yourRowGauge = 30.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(100, result.adjustedStitches)
        assertEquals(80, result.adjustedRows)
        assertEquals(0.0, result.stitchPercentDifference, 0.01)
    }

    @Test
    fun `tighter gauge increases stitch count`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 20.0,
                patternRowGauge = 28.0,
                yourStitchGauge = 22.0,
                yourRowGauge = 30.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(110, result.adjustedStitches)
        assertEquals(86, result.adjustedRows)
        assertTrue(result.stitchPercentDifference > 0)
    }

    @Test
    fun `looser gauge decreases stitch count`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 22.0,
                patternRowGauge = 30.0,
                yourStitchGauge = 20.0,
                yourRowGauge = 28.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(91, result.adjustedStitches)
        assertTrue(result.stitchPercentDifference < 0)
    }

    @Test
    fun `exact values are provided`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 20.0,
                patternRowGauge = 28.0,
                yourStitchGauge = 22.0,
                yourRowGauge = 30.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(110.0, result.adjustedStitchesExact, 0.01)
    }

    @Test
    fun `zero pattern gauge handles gracefully`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 0.0,
                patternRowGauge = 0.0,
                yourStitchGauge = 22.0,
                yourRowGauge = 30.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(0, result.adjustedStitches)
    }

    @Test
    fun `percent difference is calculated correctly`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 20.0,
                patternRowGauge = 30.0,
                yourStitchGauge = 21.0,
                yourRowGauge = 30.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(5.0, result.stitchPercentDifference, 0.01)
        assertEquals(0.0, result.rowPercentDifference, 0.01)
    }

    @Test
    fun `your gauge zero returns zero adjusted counts`() {
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 22.0,
                patternRowGauge = 30.0,
                yourStitchGauge = 0.0,
                yourRowGauge = 0.0,
                patternStitches = 100,
                patternRows = 80,
            )
        assertEquals(0, result.adjustedStitches)
        assertEquals(0, result.adjustedRows)
        assertEquals(0.0, result.adjustedStitchesExact, 0.001)
    }

    @Test
    fun `half value rounds up`() {
        // 10 × (21/20) = 10.5 → roundToInt() = 11 (not 10)
        val result =
            GaugeConverter.convert(
                patternStitchGauge = 20.0,
                patternRowGauge = 20.0,
                yourStitchGauge = 21.0,
                yourRowGauge = 21.0,
                patternStitches = 10,
                patternRows = 10,
            )
        assertEquals(11, result.adjustedStitches)
        assertEquals(11, result.adjustedRows)
        assertEquals(10.5, result.adjustedStitchesExact, 0.001)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
