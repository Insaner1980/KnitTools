package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.KnittingStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncreaseDecreaseCalculatorTest {
    @Test
    fun `increase 8 from 120 gives 128 total`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertEquals(128, result.totalStitches)
        assertTrue(result.easyPattern.contains("128"))
    }

    @Test
    fun `decrease 8 from 120 gives 112 total`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.isValid)
        assertEquals(112, result.totalStitches)
    }

    @Test
    fun `even division produces clean pattern`() {
        val result = IncreaseDecreaseCalculator.calculate(80, 8, IncreaseDecreaseMode.INCREASE)
        assertTrue(result.isValid)
        assertTrue(result.easyPattern.contains("K10"))
        assertTrue(result.easyPattern.contains("M1"))
        assertTrue(result.easyPattern.contains("× 8"))
    }

    @Test
    fun `uneven division handles remainder`() {
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
        assertNotNull(result.errorMessage)
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
    fun `circular mode produces valid pattern`() {
        val result = IncreaseDecreaseCalculator.calculate(96, 12, IncreaseDecreaseMode.INCREASE, KnittingStyle.CIRCULAR)
        assertTrue(result.isValid)
        assertEquals(108, result.totalStitches)
        assertTrue(result.easyPattern.contains("M1"))
    }

    @Test
    fun `decrease uses K2tog notation`() {
        val result = IncreaseDecreaseCalculator.calculate(120, 8, IncreaseDecreaseMode.DECREASE)
        assertTrue(result.easyPattern.contains("K2tog"))
    }
}
