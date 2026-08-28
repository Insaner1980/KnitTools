package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementCalculatorTest {
    private val tolerance = 1e-10

    @Test
    fun `physical conversion anchors use exact inch and yard definitions`() {
        assertEquals(10.0, converted(25.4, MeasurementUnit.CM, MeasurementUnit.INCH), tolerance)
        assertEquals(25.4, converted(10.0, MeasurementUnit.INCH, MeasurementUnit.CM), tolerance)
        assertEquals(1.0, converted(0.9144, MeasurementUnit.METER, MeasurementUnit.YARD), tolerance)
        assertEquals(0.9144, converted(1.0, MeasurementUnit.YARD, MeasurementUnit.METER), tolerance)
    }

    @Test
    fun `round trips retain precision across every supported unit`() {
        MeasurementUnit.entries.forEach { from ->
            MeasurementUnit.entries.forEach { to ->
                val original = 33.123456789
                val converted = converted(original, from, to)
                assertEquals(original, converted(converted, to, from), tolerance)
            }
        }
    }

    @Test
    fun `conversion permits zero but rejects negative nonfinite and overflowing lengths`() {
        assertEquals(0.0, converted(0.0, MeasurementUnit.METER, MeasurementUnit.YARD), 0.0)
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE).forEach {
            assertNull(MeasurementCalculator.convert(it, MeasurementUnit.METER, MeasurementUnit.YARD))
        }
        assertNull(MeasurementCalculator.convert(Double.MIN_VALUE, MeasurementUnit.CM, MeasurementUnit.METER))
    }

    @Test
    fun `ten centimeters and four inches are physically distinct gauge bases`() {
        val metric = density(20.0, GaugeBasis.PER_10_CM.lengthMm)
        val imperial = density(20.0, GaugeBasis.PER_4_INCHES.lengthMm)
        assertEquals(100.0, GaugeBasis.PER_10_CM.lengthMm, tolerance)
        assertEquals(101.6, GaugeBasis.PER_4_INCHES.lengthMm, tolerance)
        assertNotEquals(metric, imperial, tolerance)
        val imperialGauge = requireNotNull(MeasurementCalculator.gaugeForBasis(metric, GaugeBasis.PER_4_INCHES))
        assertEquals(20.32, imperialGauge, tolerance)
        assertEquals(metric, density(imperialGauge, GaugeBasis.PER_4_INCHES.lengthMm), tolerance)
    }

    @Test
    fun `stitch and row swatches independently use the same density calculation`() {
        val stitchOnly = density(33.0, MeasurementUnit.CM.toMillimeters(14.0))
        assertEquals(33.0 / 140.0, stitchOnly, 0.0)
        val rowOnly = density(28.0, MeasurementUnit.INCH.toMillimeters(4.0))
        assertEquals(28.0 / 101.6, rowOnly, tolerance)
        assertEquals(33.0 / 140.0, stitchOnly, 0.0)
        assertEquals(
            28.0,
            requireNotNull(MeasurementCalculator.gaugeForBasis(rowOnly, GaugeBasis.PER_4_INCHES)),
            tolerance,
        )
    }

    @Test
    fun `density rejects invalid count length and nonfinite result`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNull(MeasurementCalculator.density(it, 100.0))
            assertNull(MeasurementCalculator.density(20.0, it))
        }
        assertNull(MeasurementCalculator.density(Double.MAX_VALUE, Double.MIN_VALUE))
        assertNull(MeasurementCalculator.density(Double.MIN_VALUE, Double.MAX_VALUE))
        assertNull(MeasurementCalculator.gaugeForBasis(Double.MAX_VALUE, GaugeBasis.PER_10_CM))
    }

    @Test
    fun `target width and height return decimal whole count and rounded dimension`() {
        val stitches = requireNotNull(MeasurementCalculator.countForSize(density(20.0, 100.0), 102.5))
        assertEquals(20.5, stitches.exactCount, tolerance)
        assertEquals(21, stitches.roundedCount)
        assertEquals(105.0, stitches.roundedLengthMm, tolerance)
        val rows = requireNotNull(MeasurementCalculator.countForSize(density(28.0, 100.0), 125.0))
        assertEquals(35.0, rows.exactCount, tolerance)
        assertEquals(35, rows.roundedCount)
        assertEquals(125.0, rows.roundedLengthMm, tolerance)
    }

    @Test
    fun `whole count range is checked before integer conversion`() {
        val maximum = Int.MAX_VALUE.toDouble()
        assertEquals(Int.MAX_VALUE, requireNotNull(MeasurementCalculator.countForSize(1.0, maximum)).roundedCount)
        assertEquals(Int.MAX_VALUE, requireNotNull(MeasurementCalculator.countForSize(1.0, maximum + 0.4)).roundedCount)
        assertNull(MeasurementCalculator.countForSize(1.0, maximum + 0.5))
        assertNull(MeasurementCalculator.countForSize(Double.MAX_VALUE, 2.0))
    }

    @Test
    fun `values immediately below and at a positive half retain distinct rounding`() {
        val belowHalf = requireNotNull(MeasurementCalculator.countForSize(1.0, Math.nextDown(0.5)))
        assertEquals(Math.nextDown(0.5), belowHalf.exactCount, 0.0)
        assertEquals(0, belowHalf.roundedCount)
        assertEquals(0.0, belowHalf.roundedLengthMm, 0.0)
        assertEquals(1, requireNotNull(MeasurementCalculator.countForSize(1.0, 0.5)).roundedCount)
        assertEquals(20, requireNotNull(MeasurementCalculator.countForSize(1.0, Math.nextDown(20.5))).roundedCount)
        assertEquals(21, requireNotNull(MeasurementCalculator.countForSize(1.0, 20.5)).roundedCount)
    }

    @Test
    fun `positive target count below a half has a valid zero nearest count and zero rounded size`() {
        val result = requireNotNull(MeasurementCalculator.countForSize(density(20.0, 100.0), 0.5))
        assertEquals(0.1, result.exactCount, tolerance)
        assertEquals(0, result.roundedCount)
        assertEquals(0.0, result.roundedLengthMm, 0.0)
        assertEquals(0, requireNotNull(MeasurementCalculator.countForSize(0.01, 1.0)).roundedCount)
    }

    @Test
    fun `positive pattern adjustment may round to zero without a denominator failure`() {
        val result = requireNotNull(MeasurementCalculator.adjust(1, density(20.0, 100.0), density(1.0, 100.0)))
        assertEquals(0.05, result.exactCount, tolerance)
        assertEquals(0, result.roundedCount)
        assertEquals(0.0, result.roundedLengthMm, 0.0)
        assertEquals(5.0, result.originalLengthMm, tolerance)
        assertEquals(100.0, result.unchangedLengthMm, tolerance)
        assertEquals(-95.0, result.differencePercent, tolerance)
    }

    @Test
    fun `target counts reject zero negative and nonfinite inputs`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNull(MeasurementCalculator.countForSize(it, 100.0))
            assertNull(MeasurementCalculator.countForSize(0.2, it))
        }
    }

    @Test
    fun `count to width and height invert target count without intermediate rounding`() {
        listOf(density(22.0, 100.0), density(30.0, 101.6)).forEach { density ->
            val originalLength = 137.123456789
            val result = requireNotNull(MeasurementCalculator.countForSize(density, originalLength))
            val length = requireNotNull(MeasurementCalculator.sizeFromCount(result.exactCount, density))
            assertEquals(originalLength, length, tolerance)
        }
    }

    @Test
    fun `resulting dimensions reject invalid densities and nonfinite lengths`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNull(MeasurementCalculator.sizeFromCount(20.0, it))
            assertNull(MeasurementCalculator.sizeFromCount(it, 0.2))
        }
        assertNull(MeasurementCalculator.sizeFromCount(Double.MAX_VALUE, Double.MIN_VALUE))
    }

    @Test
    fun `pattern adjustment reports original unchanged and rounded physical dimensions`() {
        val result = requireNotNull(MeasurementCalculator.adjust(100, density(20.0, 100.0), density(22.0, 100.0)))
        assertEquals(110.0, result.exactCount, tolerance)
        assertEquals(110, result.roundedCount)
        assertEquals(500.0, result.originalLengthMm, tolerance)
        assertEquals(454.54545454545456, result.unchangedLengthMm, tolerance)
        assertEquals(500.0, result.roundedLengthMm, tolerance)
        assertEquals(10.0, result.differencePercent, tolerance)
    }

    @Test
    fun `independent row adjustment does not replace a previous stitch result`() {
        val stitches = requireNotNull(MeasurementCalculator.adjust(100, density(20.0, 100.0), density(22.0, 100.0)))
        val rows = requireNotNull(MeasurementCalculator.adjust(80, density(28.0, 100.0), density(30.0, 100.0)))
        assertEquals(86, rows.roundedCount)
        assertEquals(85.71428571428571, rows.exactCount, tolerance)
        assertEquals(286.6666666666667, rows.roundedLengthMm, tolerance)
        assertEquals(110, stitches.roundedCount)
        assertEquals(10.0, stitches.differencePercent, tolerance)
    }

    @Test
    fun `adjusted positive half count rounds upward after density normalization`() {
        val result = requireNotNull(MeasurementCalculator.adjust(10, density(20.0, 100.0), density(21.0, 100.0)))
        assertEquals(10.5, result.exactCount, tolerance)
        assertEquals(11, result.roundedCount)
    }

    @Test
    fun `adjustment compares normalized densities instead of equal numeric gauge labels`() {
        val result = requireNotNull(MeasurementCalculator.adjust(100, density(20.0, 100.0), density(20.0, 101.6)))
        assertEquals(98.4251968503937, result.exactCount, tolerance)
        assertEquals(98, result.roundedCount)
        assertEquals(-1.5748031496063, result.differencePercent, tolerance)
    }

    @Test
    fun `pattern adjustment rejects invalid denominators counts and overflowing results`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertNull(MeasurementCalculator.adjust(100, it, 0.22))
            assertNull(MeasurementCalculator.adjust(100, 0.2, it))
        }
        assertNull(MeasurementCalculator.adjust(0, 0.2, 0.22))
        assertNull(MeasurementCalculator.adjust(-1, 0.2, 0.22))
        assertNull(MeasurementCalculator.adjust(Int.MAX_VALUE, 0.2, 0.22))
        assertNull(MeasurementCalculator.adjust(100, Double.MIN_VALUE, Double.MAX_VALUE))
    }

    @Test
    fun `thirty three over fourteen swatch keeps full precision in downstream adjustment`() {
        val actualDensity = density(33.0, MeasurementUnit.CM.toMillimeters(14.0))
        val gauge = requireNotNull(MeasurementCalculator.gaugeForBasis(actualDensity, GaugeBasis.PER_10_CM))
        assertEquals(23.57142857142857, gauge, tolerance)
        val result = requireNotNull(MeasurementCalculator.adjust(1000, density(20.0, 100.0), actualDensity))
        assertEquals(1178.5714285714287, result.exactCount, tolerance)
        assertEquals(1179, result.roundedCount)
        val prematurelyRounded = requireNotNull(MeasurementCalculator.adjust(1000, 0.2, density(23.6, 100.0)))
        assertEquals(1180, prematurelyRounded.roundedCount)
    }

    private fun converted(
        value: Double,
        from: MeasurementUnit,
        to: MeasurementUnit,
    ): Double = requireNotNull(MeasurementCalculator.convert(value, from, to))

    private fun density(
        count: Double,
        lengthMm: Double,
    ): Double = requireNotNull(MeasurementCalculator.density(count, lengthMm))
}
