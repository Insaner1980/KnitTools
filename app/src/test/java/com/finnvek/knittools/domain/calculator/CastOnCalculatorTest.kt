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
    fun `pattern repeat proximity uses width derived body stitches`() {
        // rawStitches=27, edgeStitches=3 → bodyStitches=27
        // nearestDown=20, nearestUp=30
        // Body 27 is closer to 30 (diff 3) than 20 (diff 7) → should pick up
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 12.3,
                stitchGauge = 22.0,
                patternRepeat = 10,
                edgeStitches = 3,
            )
        // totalDown = 20 + 3 = 23, totalUp = 30 + 3 = 33
        assertEquals(33, result.stitches)
        assertEquals(23, result.adjustedDown)
        assertEquals(33, result.adjustedUp)
    }

    @Test
    fun `pattern repeat with edge stitches snaps body before adding edges`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 25.0,
                stitchGauge = 22.0,
                patternRepeat = 6,
                edgeStitches = 4,
            )
        assertEquals(58, result.stitches)
        assertEquals(58, result.adjustedDown)
        assertEquals(64, result.adjustedUp)
    }

    @Test
    fun `pattern repeat of 1 works`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 20.0,
                stitchGauge = 22.0,
                patternRepeat = 1,
            )
        // Every integer is a multiple of 1, so nearestDown = bodyStitches
        assertEquals(44, result.stitches)
        assertEquals(44, result.adjustedDown)
        assertEquals(45, result.adjustedUp)
    }

    @Test
    fun `edge stitches larger than raw does not crash`() {
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 5.0,
                stitchGauge = 22.0,
                patternRepeat = 10,
                edgeStitches = 20,
            )
        // rawStitches=11, bodyStitches=11, nearestDown=10, totalDown=30
        assertEquals(30, result.adjustedDown)
        assertNotNull(result.adjustedUp)
    }

    @Test
    fun `half stitch rounds up`() {
        // 10.25 × (20/10) = 20.5 → roundToInt() = 21
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 10.25,
                stitchGauge = 20.0,
            )
        assertEquals(21, result.stitches)
    }

    @Test
    fun `equidistant pattern repeat picks down`() {
        // bodyStitches=25, patternRepeat=10 → nearestDown=20, nearestUp=30, diff=5 both
        // Tie-breaks to down via <=
        val result =
            CastOnCalculator.calculate(
                desiredWidth = 12.5,
                stitchGauge = 20.0,
                patternRepeat = 10,
            )
        assertEquals(20, result.stitches)
        assertEquals(20, result.adjustedDown)
        assertEquals(30, result.adjustedUp)
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
