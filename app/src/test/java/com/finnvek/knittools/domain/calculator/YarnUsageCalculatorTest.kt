package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class YarnUsageCalculatorTest {
    @Test
    fun `meters and yards retain exact physical definitions`() {
        assertEquals(0.9144, toMeters(1.0, YarnUsageUnit.YARDS), 1e-12)
        assertEquals(1.0936132983377078, fromMeters(1.0, YarnUsageUnit.YARDS), 1e-12)
    }

    @Test
    fun `gram allocation example preserves fractional skeins`() {
        val allocated = toMeters(300.0, YarnUsageUnit.GRAMS)
        val used = toMeters(175.0, YarnUsageUnit.GRAMS)
        val remaining = requireNotNull(YarnUsageCalculator.remaining(allocated, used))
        assertEquals(600.0, allocated, 0.0)
        assertEquals(350.0, used, 0.0)
        assertEquals(250.0, remaining, 0.0)
        assertEquals(125.0, fromMeters(remaining, YarnUsageUnit.GRAMS), 0.0)
        assertEquals(1.25, fromMeters(remaining, YarnUsageUnit.SKEINS), 0.0)
        assertEquals(250.0, toMeters(1.25, YarnUsageUnit.SKEINS), 0.0)
    }

    @Test
    fun `missing values differ from known zero independently`() {
        assertFalse(YarnUsageCalculator.validAmounts(YarnUsageAmounts()))
        listOf(
            YarnUsageAmounts(plannedMeters = 0.0),
            YarnUsageAmounts(allocatedMeters = 0.0),
            YarnUsageAmounts(usedMeters = 0.0),
        ).forEach { assertTrue(YarnUsageCalculator.validAmounts(it)) }
        assertNull(YarnUsageCalculator.remaining(null, 0.0))
        assertNull(YarnUsageCalculator.remaining(0.0, null))
        assertEquals(0.0, requireNotNull(YarnUsageCalculator.remaining(0.0, 0.0)), 0.0)
        assertEquals(-50.0, requireNotNull(YarnUsageCalculator.remaining(100.0, 150.0)), 0.0)
    }

    @Test
    fun `invalid ratios and amounts are never clamped`() {
        val invalid = listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        invalid.forEach { value ->
            assertFalse(YarnUsageCalculator.validConversion(value, 100.0))
            assertFalse(YarnUsageCalculator.validConversion(200.0, value))
        }
        assertFalse(YarnUsageCalculator.validConversion(null, 100.0))
        assertFalse(YarnUsageCalculator.validConversion(200.0, null))
        listOf(YarnUsageUnit.GRAMS, YarnUsageUnit.SKEINS).forEach { unit ->
            assertNull(YarnUsageCalculator.toMeters(1.0, unit, 200.0, null))
            assertNull(YarnUsageCalculator.fromMeters(1.0, unit, null, 100.0))
        }
        invalid.filterNot { it == 0.0 }.forEach { value ->
            assertFalse(YarnUsageCalculator.validAmounts(YarnUsageAmounts(plannedMeters = value)))
            assertFalse(YarnUsageCalculator.validAmounts(YarnUsageAmounts(allocatedMeters = value)))
            assertFalse(YarnUsageCalculator.validAmounts(YarnUsageAmounts(usedMeters = value)))
            YarnUsageUnit.entries.forEach { unit ->
                assertNull(YarnUsageCalculator.toMeters(value, unit, 200.0, 100.0))
                assertNull(YarnUsageCalculator.fromMeters(value, unit, 200.0, 100.0))
            }
        }
        YarnUsageUnit.entries.forEach { unit -> assertEquals(0.0, toMeters(0.0, unit), 0.0) }
    }

    @Test
    fun `high precision round trips never use formatted output`() {
        val meters = 123.12345678901234
        YarnUsageUnit.entries.forEach { unit ->
            assertEquals(meters, toMeters(fromMeters(meters, unit), unit), 1e-12)
        }
        val amounts = YarnUsageAmounts(plannedMeters = meters, allocatedMeters = 200.0, usedMeters = meters)
        assertEquals("123.12", MeasurementNumberFormatter.format(meters, Locale.US))
        assertEquals(meters, requireNotNull(amounts.usedMeters), 0.0)
        assertEquals(
            200.0 - meters,
            requireNotNull(YarnUsageCalculator.remaining(amounts.allocatedMeters, amounts.usedMeters)),
            0.0,
        )
    }

    private fun toMeters(
        value: Double,
        unit: YarnUsageUnit,
    ): Double = requireNotNull(YarnUsageCalculator.toMeters(value, unit, 200.0, 100.0))

    private fun fromMeters(
        value: Double,
        unit: YarnUsageUnit,
    ): Double = requireNotNull(YarnUsageCalculator.fromMeters(value, unit, 200.0, 100.0))
}
