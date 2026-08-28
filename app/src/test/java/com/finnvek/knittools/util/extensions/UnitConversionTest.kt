package com.finnvek.knittools.util.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

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
        assertEquals(10.0 / 2.54, convertFieldValue("10", toImperial = true, isLength = true).toDouble(), 1e-12)
    }

    @Test
    fun `convertFieldValue inches to cm (length)`() {
        assertEquals(10.16, convertFieldValue("4", toImperial = false, isLength = true).toDouble(), 1e-12)
    }

    @Test
    fun `convertFieldValue meters to yards (non-length)`() {
        assertEquals(100.0 / 0.9144, convertFieldValue("100", toImperial = true, isLength = false).toDouble(), 1e-12)
    }

    @Test
    fun `convertFieldValue yards to meters (non-length)`() {
        assertEquals(91.44, convertFieldValue("100", toImperial = false, isLength = false).toDouble(), 1e-12)
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

    @Test
    fun `convertFieldValue returns parser-safe decimals under Finnish locale`() {
        withDefaultLocale(Locale.forLanguageTag("fi-FI")) {
            val converted = convertFieldValue("10", toImperial = true, isLength = true)

            assertFalse(converted.contains(','))
            assertEquals(10.0 / 2.54, converted.toDouble(), 1e-12)
        }
    }

    // --- convertGaugeValue ---

    @Test
    fun `convertGaugeValue 10cm to 4in`() {
        assertEquals(22.352, convertGaugeValue("22", toImperial = true).toDouble(), 1e-12)
    }

    @Test
    fun `convertGaugeValue 4in to 10cm`() {
        assertEquals(22.0 / 1.016, convertGaugeValue("22", toImperial = false).toDouble(), 1e-12)
    }

    @Test
    fun `convertGaugeValue roundtrip is close to original`() {
        val original = "22"
        val imperial = convertGaugeValue(original, toImperial = true)
        val backToMetric = convertGaugeValue(imperial, toImperial = false)
        assertEquals(22.0, backToMetric.toDouble(), 1e-12)
    }

    @Test
    fun `convertGaugeValue returns original for invalid input`() {
        assertEquals("abc", convertGaugeValue("abc", toImperial = true))
    }

    @Test
    fun `convertGaugeValue returns original for zero`() {
        assertEquals("0", convertGaugeValue("0", toImperial = true))
    }

    @Test
    fun `convertGaugeValue returns parser-safe decimals under Finnish locale`() {
        withDefaultLocale(Locale.forLanguageTag("fi-FI")) {
            val converted = convertGaugeValue("22", toImperial = true)

            assertFalse(converted.contains(','))
            assertEquals(22.352, converted.toDouble(), 1e-12)
        }
    }

    @Test
    fun `conversion helpers do not reinterpret malformed or nonfinite text`() {
        listOf("-2", "1e3", "NaN", "Infinity", "1.2.3", "9".repeat(400)).forEach {
            assertEquals(it, convertFieldValue(it, toImperial = true))
            assertEquals(it, convertGaugeValue(it, toImperial = true))
        }
    }

    @Test
    fun `repeated field conversion does not accumulate display rounding`() {
        val original = "33.123456789"
        var value = original
        repeat(20) {
            value = convertFieldValue(convertFieldValue(value, toImperial = true), toImperial = false)
        }
        assertEquals(original.toDouble(), value.toDouble(), 1e-12)
    }

    private fun withDefaultLocale(
        locale: Locale,
        block: () -> Unit,
    ) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            block()
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
