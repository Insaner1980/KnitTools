package com.finnvek.knittools.util.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {
    private val delta = 0.01

    // --- Perusmuunnokset ---

    @Test
    fun `cm to inches`() {
        assertEquals(3.94, cmToInches(10.0), delta)
    }

    @Test
    fun `inches to cm`() {
        assertEquals(10.16, inchesToCm(4.0), delta)
    }

    @Test
    fun `meters to yards`() {
        assertEquals(109.36, metersToYards(100.0), delta)
    }

    @Test
    fun `yards to meters`() {
        assertEquals(91.44, yardsToMeters(100.0), delta)
    }

    @Test
    fun `zero stays zero`() {
        assertEquals(0.0, cmToInches(0.0), delta)
        assertEquals(0.0, inchesToCm(0.0), delta)
        assertEquals(0.0, metersToYards(0.0), delta)
        assertEquals(0.0, yardsToMeters(0.0), delta)
    }

    // --- convertFieldValue ---

    @Test
    fun `convertFieldValue cm to inches (length)`() {
        assertEquals("3.9", convertFieldValue("10", toImperial = true, isLength = true))
    }

    @Test
    fun `convertFieldValue inches to cm (length)`() {
        assertEquals("10.2", convertFieldValue("4", toImperial = false, isLength = true))
    }

    @Test
    fun `convertFieldValue meters to yards (non-length)`() {
        assertEquals("109.4", convertFieldValue("100", toImperial = true, isLength = false))
    }

    @Test
    fun `convertFieldValue yards to meters (non-length)`() {
        assertEquals("91.4", convertFieldValue("100", toImperial = false, isLength = false))
    }

    @Test
    fun `convertFieldValue returns original for invalid input`() {
        assertEquals("abc", convertFieldValue("abc", toImperial = true))
        assertEquals("", convertFieldValue("", toImperial = false))
    }

    @Test
    fun `convertFieldValue returns original for zero`() {
        assertEquals("0", convertFieldValue("0", toImperial = true))
        assertEquals("0.0", convertFieldValue("0.0", toImperial = false))
    }

    // --- convertGaugeValue ---

    @Test
    fun `convertGaugeValue 10cm to 4in`() {
        // 22 st/10cm → ~22.4 st/4in
        assertEquals("22.4", convertGaugeValue("22", toImperial = true))
    }

    @Test
    fun `convertGaugeValue 4in to 10cm`() {
        // 22 st/4in → ~21.7 st/10cm
        assertEquals("21.7", convertGaugeValue("22", toImperial = false))
    }

    @Test
    fun `convertGaugeValue roundtrip is close to original`() {
        val original = "22"
        val imperial = convertGaugeValue(original, toImperial = true)
        val backToMetric = convertGaugeValue(imperial, toImperial = false)
        // Pyöristyksen takia ei täysin sama, mutta lähellä
        assertEquals(22.0, backToMetric.toDouble(), 0.2)
    }

    @Test
    fun `convertGaugeValue returns original for invalid input`() {
        assertEquals("abc", convertGaugeValue("abc", toImperial = true))
    }

    @Test
    fun `convertGaugeValue returns original for zero`() {
        assertEquals("0", convertGaugeValue("0", toImperial = true))
    }
}
