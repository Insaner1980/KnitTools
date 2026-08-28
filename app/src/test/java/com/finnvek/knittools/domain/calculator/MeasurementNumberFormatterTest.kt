package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MeasurementNumberFormatterTest {
    private val finnish = Locale.forLanguageTag("fi-FI")

    @Test
    fun `display uses Finnish or English decimal conventions and removes trailing zeroes`() {
        assertEquals("12,5", MeasurementNumberFormatter.format(12.5, finnish))
        assertEquals("12.5", MeasurementNumberFormatter.format(12.5, Locale.US))
        assertEquals("12", MeasurementNumberFormatter.format(12.0, finnish))
        assertEquals("23,57", MeasurementNumberFormatter.format(33.0 / 14.0 * 10.0, finnish))
        assertEquals("23.57", MeasurementNumberFormatter.format(33.0 / 14.0 * 10.0, Locale.US))
    }

    @Test
    fun `small positive measurements never display as a misleading zero`() {
        assertEquals("0,00012", MeasurementNumberFormatter.format(0.0001234, finnish))
        assertEquals("0.00012", MeasurementNumberFormatter.format(0.0001234, Locale.US))
        assertTrue(MeasurementNumberFormatter.format(Double.MIN_VALUE, Locale.US).contains('4'))
        assertEquals("0", MeasurementNumberFormatter.format(0.0, Locale.US))
    }

    @Test
    fun `editing conversion retains full numeric precision without exponent notation`() {
        listOf(33.0 / 14.0, 1e-20, 1e20, Double.MIN_VALUE, Double.MAX_VALUE).forEach { value ->
            val editing = MeasurementNumberFormatter.formatEditing(value)
            assertFalse(editing.contains('E', ignoreCase = true))
            assertEquals(value, requireNotNull(MeasurementNumberParser.parse(editing, Locale.US).value), 0.0)
        }
    }

    @Test
    fun `unit switch editing text and internal physical value remain consistent`() {
        val converted = requireNotNull(MeasurementCalculator.convert(1.0, MeasurementUnit.METER, MeasurementUnit.YARD))
        val editing = MeasurementNumberFormatter.formatEditing(converted)
        assertEquals(converted, requireNotNull(MeasurementNumberParser.parse(editing, finnish).value), 0.0)
        val restored =
            requireNotNull(
                MeasurementCalculator.convert(editing.toDouble(), MeasurementUnit.YARD, MeasurementUnit.METER),
            )
        assertEquals(1.0, restored, 1e-14)
        assertEquals("1,09", MeasurementNumberFormatter.format(converted, finnish))
        assertTrue(editing.length > "1.09".length)
    }

    @Test
    fun `display formatting cannot change the downstream swatch calculation`() {
        val density = requireNotNull(MeasurementCalculator.density(33.0, 140.0))
        assertEquals("23,57", MeasurementNumberFormatter.format(density * 100.0, finnish))
        val adjusted = requireNotNull(MeasurementCalculator.adjust(1000, 0.2, density))
        assertEquals(1179, adjusted.roundedCount)
    }

    @Test
    fun `percentage uses locale formatting and one decimal without forced zeroes`() {
        assertEquals("+10%", MeasurementNumberFormatter.percent(10.0, Locale.US))
        assertEquals("-1.6%", MeasurementNumberFormatter.percent(-1.5748, Locale.US))
        assertEquals("+10 %", MeasurementNumberFormatter.percent(10.0, finnish).replace('\u00a0', ' '))
        assertEquals(
            "-1,6 %",
            MeasurementNumberFormatter.percent(-1.5748, finnish).replace('\u2212', '-').replace('\u00a0', ' '),
        )
        assertEquals("0%", MeasurementNumberFormatter.percent(0.0, Locale.US))
    }
}
