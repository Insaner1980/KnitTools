package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GaugeSwatchCalculatorTest {
    private val delta = 0.01

    @Test
    fun `basic metric swatch calculation`() {
        // 10cm leveys, 22 silmukkaa, 14cm korkeus, 30 kerrosta → 22 st/10cm, 21.4 rows/10cm
        val result = GaugeSwatchCalculator.calculate(10.0, 22, 14.0, 30)!!
        assertEquals(22.0, result.stitchesPerGaugeUnit, delta)
        assertEquals(21.43, result.rowsPerGaugeUnit, delta)
    }

    @Test
    fun `imperial swatch calculation (4 inches)`() {
        // 4in leveys, 20 silmukkaa → 20 st/4in
        val result = GaugeSwatchCalculator.calculate(4.0, 20, 4.0, 28, gaugeBase = 4.0)!!
        assertEquals(20.0, result.stitchesPerGaugeUnit, delta)
        assertEquals(28.0, result.rowsPerGaugeUnit, delta)
    }

    @Test
    fun `non-standard swatch size scales correctly`() {
        // 15cm leveys, 33 silmukkaa → 33/15*10 = 22 st/10cm
        val result = GaugeSwatchCalculator.calculate(15.0, 33, 15.0, 42)!!
        assertEquals(22.0, result.stitchesPerGaugeUnit, delta)
        assertEquals(28.0, result.rowsPerGaugeUnit, delta)
    }

    @Test
    fun `zero width returns null`() {
        assertNull(GaugeSwatchCalculator.calculate(0.0, 22, 10.0, 30))
    }

    @Test
    fun `zero height returns null`() {
        assertNull(GaugeSwatchCalculator.calculate(10.0, 22, 0.0, 30))
    }

    @Test
    fun `zero stitches returns null`() {
        assertNull(GaugeSwatchCalculator.calculate(10.0, 0, 10.0, 30))
    }

    @Test
    fun `zero rows returns null`() {
        assertNull(GaugeSwatchCalculator.calculate(10.0, 22, 10.0, 0))
    }

    @Test
    fun `negative width returns null`() {
        assertNull(GaugeSwatchCalculator.calculate(-5.0, 22, 10.0, 30))
    }

    @Test
    fun `small swatch scales up correctly`() {
        // 5cm leveys, 11 silmukkaa → 11/5*10 = 22 st/10cm
        val result = GaugeSwatchCalculator.calculate(5.0, 11, 5.0, 14)!!
        assertEquals(22.0, result.stitchesPerGaugeUnit, delta)
        assertEquals(28.0, result.rowsPerGaugeUnit, delta)
    }
}
