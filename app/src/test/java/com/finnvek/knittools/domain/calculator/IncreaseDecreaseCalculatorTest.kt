package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.IncreaseDecreaseMessage
import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.KnittingStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncreaseDecreaseCalculatorTest {
    @Test
    fun `increase 8 from 120 gives 128 total`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertEquals(128, result.totalStitches)
        assertFalse(result.easyPattern.contains("total:"))
        assertFalse(result.balancedPattern.contains("total:"))
    }

    @Test
    fun `decrease 8 from 120 gives 112 total`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.isValid)
        assertEquals(112, result.totalStitches)
    }

    @Test
    fun `even increase produces clean pattern`() {
        val result = IncreaseDecreaseCalculator.calculate(80, 8, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertTrue(result.easyPattern.contains("K10"))
        assertTrue(result.easyPattern.contains("M1"))
        assertTrue(result.easyPattern.contains("× 8"))
    }

    @Test
    fun `decrease 8 from 35 flat uses correct stitch count`() {
        // Each K2tog consumes two stitches and produces one, leaving 35 - 2 * 8 = 19 plain knits.
        // K1, (K2, K2tog) x 8, K2 consumes 1 + 8 * 4 + 2 = 35 and produces 1 + 8 * 3 + 2 = 27.
        val result = IncreaseDecreaseCalculator.calculate(35, 8, IncreaseDecreaseMode.DECREASE, KnittingStyle.FLAT)
        assertTrue(result.isValid)
        assertEquals(27, result.totalStitches)
        assertEquals("K1, (K2, K2tog) × 8, K2", result.easyPattern)
    }

    @Test
    fun `uneven increase handles remainder`() {
        val result = IncreaseDecreaseCalculator.calculate(100, 7, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertEquals(107, result.totalStitches)
        assertTrue(result.easyPattern.isNotEmpty())
        assertTrue(result.balancedPattern.isNotEmpty())
    }

    @Test
    fun `decrease all stitches is invalid`() {
        val result = IncreaseDecreaseCalculator.calculate(10, 10, IncreaseDecreaseMode.DECREASE)
        assertFalse(result.isValid)
        assertEquals(IncreaseDecreaseMessage.CannotDecreaseBy(10, 10), result.message)
        assertNull(result.errorMessage)
    }

    @Test
    fun `decrease more than available is invalid`() {
        val result = IncreaseDecreaseCalculator.calculate(10, 15, IncreaseDecreaseMode.DECREASE)
        assertFalse(result.isValid)
    }

    @Test
    fun `zero current stitches is invalid`() {
        val result = IncreaseDecreaseCalculator.calculate(0, 5, IncreaseDecreaseMode.INCREASE)
        assertFalse(result.isValid)
    }

    @Test
    fun `zero change is invalid`() {
        val result = IncreaseDecreaseCalculator.calculate(100, 0, IncreaseDecreaseMode.INCREASE)
        assertFalse(result.isValid)
    }

    @Test
    fun `circular increase produces valid pattern`() {
        val result = IncreaseDecreaseCalculator.calculate(96, 12, IncreaseDecreaseMode.INCREASE, KnittingStyle.CIRCULAR)
        assertTrue(result.isValid)
        assertEquals(108, result.totalStitches)
        assertTrue(result.easyPattern.contains("M1"))
    }

    @Test
    fun `balanced pattern respects flat and circular style`() {
        val flat = IncreaseDecreaseCalculator.calculate(100, 7, IncreaseDecreaseMode.INCREASE, KnittingStyle.FLAT)
        val circular =
            IncreaseDecreaseCalculator.calculate(
                100,
                7,
                IncreaseDecreaseMode.INCREASE,
                KnittingStyle.CIRCULAR,
            )

        assertTrue(flat.isValid)
        assertTrue(circular.isValid)
        assertNotEquals(flat.balancedPattern, circular.balancedPattern)
    }

    @Test
    fun `decrease uses K2tog notation`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.easyPattern.contains("K2tog"))
    }

    @Test
    fun `minimum viable decrease passes validation`() {
        // 3 stitches, decrease 1: changeBy*2=2 < 3, so valid
        // availableForKnit = 3 - 2 = 1, pattern: (K1, K2tog) × 1
        val result = IncreaseDecreaseCalculator.calculate(3, 1, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.isValid)
        assertEquals(2, result.totalStitches)
        assertTrue(result.easyPattern.contains("K1"))
    }

    @Test
    fun `increase more than current warns but still valid`() {
        val result = IncreaseDecreaseCalculator.calculate(35, 68, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertEquals(IncreaseDecreaseMessage.IncreaseMoreThanCurrent, result.message)
        assertNull(result.errorMessage)
    }

    @Test
    fun `decrease with too few stitches for K2tog is invalid`() {
        // 10 stitches, decrease 6: would need 6 K2tog (12 stitches) + some K, but only 10 available
        val result = IncreaseDecreaseCalculator.calculate(10, 6, IncreaseDecreaseMode.DECREASE)
        assertFalse(result.isValid)
    }

    @Test
    fun `decrease one from two is valid without zero-knit instructions`() {
        val result = IncreaseDecreaseCalculator.calculate(2, 1, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.isValid)
        assertEquals(1, result.totalStitches)
        assertEquals("K2tog", result.easyPattern)
        assertFalse(result.easyPattern.contains("K0"))
        assertFalse(result.balancedPattern.contains("K0"))
    }

    @Test
    fun `decrease two from four is valid without zero-knit instructions`() {
        val result = IncreaseDecreaseCalculator.calculate(4, 2, IncreaseDecreaseMode.DECREASE, KnittingStyle.CIRCULAR)
        assertTrue(result.isValid)
        assertEquals(2, result.totalStitches)
        assertEquals("K2tog × 2", result.easyPattern)
        assertFalse(result.easyPattern.contains("K0"))
        assertFalse(result.balancedPattern.contains("K0"))
    }

    @Test
    fun `valid edge decrease does not show zero-knit sections`() {
        val result = IncreaseDecreaseCalculator.calculate(5, 2, IncreaseDecreaseMode.DECREASE, KnittingStyle.FLAT)
        assertTrue(result.isValid)
        assertEquals(3, result.totalStitches)
        assertFalse(result.easyPattern.contains("K0"))
        assertFalse(result.balancedPattern.contains("K0"))
    }

    @Test
    fun `increase more than current does not show zero-knit sections`() {
        val result = IncreaseDecreaseCalculator.calculate(3, 5, IncreaseDecreaseMode.INCREASE, KnittingStyle.CIRCULAR)
        assertTrue(result.isValid)
        assertEquals(8, result.totalStitches)
        assertEquals(IncreaseDecreaseMessage.IncreaseMoreThanCurrent, result.message)
        assertFalse(result.easyPattern.contains("K0"))
        assertFalse(result.balancedPattern.contains("K0"))
    }

    @Test
    fun `increase total overflow is invalid`() {
        val result = IncreaseDecreaseCalculator.calculate(Int.MAX_VALUE, 1, IncreaseDecreaseMode.INCREASE)
        assertFalse(result.isValid)
        assertEquals(IncreaseDecreaseMessage.TotalStitchesOutOfRange, result.message)
        assertNull(result.errorMessage)
    }

    @Test
    fun `decrease validation does not overflow`() {
        val result =
            IncreaseDecreaseCalculator.calculate(
                Int.MAX_VALUE,
                Int.MAX_VALUE - 1,
                IncreaseDecreaseMode.DECREASE,
            )
        assertFalse(result.isValid)
        assertEquals(
            IncreaseDecreaseMessage.NotEnoughStitchesForDecrease(Int.MAX_VALUE - 1, 4_294_967_292L),
            result.message,
        )
    }
}
