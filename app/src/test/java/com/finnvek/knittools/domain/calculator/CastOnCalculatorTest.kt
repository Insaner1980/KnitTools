package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CastOnCalculatorTest {
    @Test
    fun `basic cast on for 20cm at 22 stitches per 10cm`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
            )
        assertEquals(44, result.stitches)
        assertNull(result.adjustedDown)
        assertNull(result.adjustedUp)
    }

    @Test
    fun `cast on with edge stitches`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
                edgeStitches = 2,
            )
        assertEquals(46, result.stitches)
    }

    @Test
    fun `cast on with pattern repeat adjusts to nearest multiple`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
                patternRepeat = 6,
            )
        assertNotNull(result.adjustedDown)
        assertNotNull(result.adjustedUp)
        assertEquals(0, result.adjustedDown!! % 6)
        assertEquals(0, result.adjustedUp!! % 6)
    }

    @Test
    fun `cast on with pattern repeat and edge stitches`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
                patternRepeat = 8,
                edgeStitches = 2,
            )
        assertNotNull(result.adjustedDown)
        assertNotNull(result.adjustedUp)
        assertEquals(0, (result.adjustedDown!! - 2) % 8)
    }

    @Test
    fun `imperial gauge calculation`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 8.0,
                stitchGauge = 22.0,
                useInches = true,
            )
        assertEquals(44, result.stitches)
    }

    @Test
    fun `actual width is calculated`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
            )
        assertEquals(20.0, result.actualWidth, 0.5)
    }
}
