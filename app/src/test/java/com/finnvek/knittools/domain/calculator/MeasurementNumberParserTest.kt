package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MeasurementNumberParserTest {
    private val finnish = Locale.forLanguageTag("fi-FI")

    @Test
    fun `Finnish comma and ungrouped decimal point both parse without altering raw text`() {
        listOf("12,5", "12.5", " 12,5 ", "\t12.5\n").forEach { raw ->
            val result = MeasurementNumberParser.parse(raw, finnish)
            assertEquals(12.5, requireNotNull(result.value), 0.0)
            assertNull(result.error)
            assertFalse(result.incomplete)
        }
    }

    @Test
    fun `English accepts point but does not treat comma grouping as a decimal`() {
        assertEquals(12.5, requireNotNull(MeasurementNumberParser.parse(" 12.5 ", Locale.US).value), 0.0)
        assertEquals(MeasurementNumberError.INVALID_NUMBER, MeasurementNumberParser.parse("1,000", Locale.US).error)
        assertEquals(MeasurementNumberError.INVALID_NUMBER, MeasurementNumberParser.parse("12,5", Locale.US).error)
    }

    @Test
    fun `empty and trailing decimal separators stay incomplete without a value or error`() {
        listOf("", " ", ".", ",", "12.", "12,").forEach {
            val result = MeasurementNumberParser.parse(it, finnish)
            assertNull(result.value)
            assertNull(result.error)
            assertTrue(result.incomplete)
        }
    }

    @Test
    fun `unsigned negative input never becomes a positive number`() {
        val result = MeasurementNumberParser.parse("-2", finnish, integer = true)
        assertNull(result.value)
        assertEquals(MeasurementNumberError.MUST_BE_POSITIVE, result.error)
        assertFalse(result.incomplete)
    }

    @Test
    fun `legitimate signed fields retain the sign and temporary minus`() {
        assertEquals(
            -2.5,
            requireNotNull(MeasurementNumberParser.parse("-2,5", finnish, allowNegative = true).value),
            0.0,
        )
        assertEquals(
            -2.0,
            requireNotNull(MeasurementNumberParser.parse("-2", finnish, integer = true, allowNegative = true).value),
            0.0,
        )
        assertTrue(MeasurementNumberParser.parse("-", finnish, integer = true, allowNegative = true).incomplete)
        assertEquals(MeasurementNumberError.INVALID_NUMBER, MeasurementNumberParser.parse("2-1", finnish).error)
    }

    @Test
    fun `scientific notation and malformed pasted text remain invalid`() {
        listOf("1e3", "1E3", "1e-3", "12 cm", "one", "12/5", "1+2", "+12", "12\n5").forEach {
            val result = MeasurementNumberParser.parse(it, finnish)
            assertNull(result.value)
            assertEquals(MeasurementNumberError.INVALID_NUMBER, result.error)
            assertFalse(result.incomplete)
        }
    }

    @Test
    fun `integer fields reject decimal syntax rather than joining digits`() {
        listOf("12.5", "12,5", "12.", "12,", "12.0").forEach {
            val result = MeasurementNumberParser.parse(it, finnish, integer = true)
            assertNull(result.value)
            assertEquals(MeasurementNumberError.INVALID_NUMBER, result.error)
        }
    }

    @Test
    fun `mixed repeated and internal grouping separators are rejected`() {
        listOf("1,2.3", "1.2,3", "1..2", "1,,2", "1.000.000", "1 000", "1\u00a0000", "1\u202f000").forEach {
            val result = MeasurementNumberParser.parse(it, finnish)
            assertNull(result.value)
            assertEquals(MeasurementNumberError.INVALID_NUMBER, result.error)
        }
    }

    @Test
    fun `NaN infinity and numeric overflow never return nonfinite values`() {
        listOf("NaN", "Infinity", "-Infinity", "∞").forEach {
            assertNull(MeasurementNumberParser.parse(it, Locale.US).value)
        }
        val overflow = MeasurementNumberParser.parse("9".repeat(400), Locale.US)
        assertNull(overflow.value)
        assertEquals(MeasurementNumberError.TOO_LARGE, overflow.error)
    }

    @Test
    fun `zero requires an explicit nonnegative field contract`() {
        assertEquals(MeasurementNumberError.MUST_BE_POSITIVE, MeasurementNumberParser.parse("0", finnish).error)
        assertEquals(0.0, requireNotNull(MeasurementNumberParser.parse("0", finnish, allowZero = true).value), 0.0)
        assertNull(MeasurementNumberParser.parse("-0", finnish, allowZero = true).value)
    }

    @Test
    fun `positive text below Double range must not become accepted zero`() {
        val result = MeasurementNumberParser.parse("0." + "0".repeat(400) + "1", Locale.US, allowZero = true)
        assertNull(result.value)
        assertEquals(MeasurementNumberError.INVALID_NUMBER, result.error)
    }

    @Test
    fun `integer limits are validated before converting a parsed number`() {
        val maximum = MeasurementNumberParser.parse(Int.MAX_VALUE.toString(), Locale.US, integer = true)
        assertEquals(Int.MAX_VALUE.toDouble(), requireNotNull(maximum.value), 0.0)
        val overflow = MeasurementNumberParser.parse((Int.MAX_VALUE.toLong() + 1).toString(), Locale.US, integer = true)
        assertNull(overflow.value)
        assertEquals(MeasurementNumberError.TOO_LARGE, overflow.error)
        val minimum =
            MeasurementNumberParser.parse(
                Int.MIN_VALUE.toString(),
                Locale.US,
                integer = true,
                allowNegative = true,
            )
        assertEquals(Int.MIN_VALUE.toDouble(), requireNotNull(minimum.value), 0.0)
        val underflow =
            MeasurementNumberParser.parse(
                (Int.MIN_VALUE.toLong() - 1).toString(),
                Locale.US,
                integer = true,
                allowNegative = true,
            )
        assertEquals(MeasurementNumberError.TOO_LARGE, underflow.error)
    }
}
