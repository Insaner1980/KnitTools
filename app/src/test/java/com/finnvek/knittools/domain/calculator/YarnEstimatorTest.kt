package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YarnEstimatorTest {
    @Test
    fun `decimal multiples do not require an extra skein`() {
        val below = requireNotNull(YarnEstimator.estimate(375.29, 125.1, 100.0))
        val exact = requireNotNull(YarnEstimator.estimate(375.3, 125.1, 100.0))
        val above = requireNotNull(YarnEstimator.estimate(375.31, 125.1, 100.0))

        assertEquals(3, below.skeinsNeeded)
        assertEquals(3, exact.skeinsNeeded)
        assertEquals(300.0, exact.totalWeight, 0.0)
        assertEquals(4, above.skeinsNeeded)
    }

    @Test
    fun `exact number of skeins`() {
        val result =
            requireNotNull(
                YarnEstimator.estimate(
                    totalYarnNeeded = 400.0,
                    yarnPerSkein = 200.0,
                    weightPerSkein = 100.0,
                ),
            )
        assertEquals(2, result.skeinsNeeded)
        assertEquals(200.0, result.totalWeight, 0.01)
        assertEquals(2.0, result.exactSkeins, 0.01)
    }

    @Test
    fun `rounds up to next skein`() {
        val result =
            requireNotNull(
                YarnEstimator.estimate(
                    totalYarnNeeded = 450.0,
                    yarnPerSkein = 200.0,
                    weightPerSkein = 100.0,
                ),
            )
        assertEquals(3, result.skeinsNeeded)
        assertEquals(300.0, result.totalWeight, 0.01)
        assertEquals(2.25, result.exactSkeins, 0.01)
    }

    @Test
    fun `small amount needs at least one skein`() {
        val result =
            requireNotNull(
                YarnEstimator.estimate(
                    totalYarnNeeded = 10.0,
                    yarnPerSkein = 200.0,
                    weightPerSkein = 50.0,
                ),
            )
        assertEquals(1, result.skeinsNeeded)
    }

    @Test
    fun `zero yarn per skein is rejected`() {
        assertNull(YarnEstimator.estimate(totalYarnNeeded = 400.0, yarnPerSkein = 0.0, weightPerSkein = 100.0))
    }

    @Test
    fun `weight calculation is based on whole skeins`() {
        val result =
            requireNotNull(
                YarnEstimator.estimate(
                    totalYarnNeeded = 500.0,
                    yarnPerSkein = 150.0,
                    weightPerSkein = 50.0,
                ),
            )
        assertEquals(4, result.skeinsNeeded)
        assertEquals(200.0, result.totalWeight, 0.01)
    }

    @Test
    fun `invalid and overflowing values are rejected before integer conversion`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { invalid ->
            assertNull(YarnEstimator.estimate(invalid, 200.0, 100.0))
            assertNull(YarnEstimator.estimate(400.0, invalid, 100.0))
            assertNull(YarnEstimator.estimate(400.0, 200.0, invalid))
        }
        assertNull(YarnEstimator.estimate(Int.MAX_VALUE.toDouble() + 1.0, 1.0, 100.0))
        assertNull(YarnEstimator.estimate(Int.MAX_VALUE.toDouble(), 1.0, Double.MAX_VALUE))
    }

    @Test
    fun `positive ratio underflow cannot report zero required skeins`() {
        assertNull(YarnEstimator.estimate(1e-308, 1e308, 50.0))
    }

    @Test
    fun `integer and finite weight boundaries retain valid results`() {
        val maxSkeins = requireNotNull(YarnEstimator.estimate(Int.MAX_VALUE.toDouble(), 1.0, 1.0))
        assertEquals(Int.MAX_VALUE, maxSkeins.skeinsNeeded)
        assertEquals(Int.MAX_VALUE.toDouble(), maxSkeins.totalWeight, 0.0)
        assertNull(YarnEstimator.estimate(Int.MAX_VALUE.toDouble() + 0.25, 1.0, 1.0))
        assertNull(YarnEstimator.estimate(Double.MAX_VALUE, Double.MIN_VALUE, 1.0))

        val maxWeight = requireNotNull(YarnEstimator.estimate(1.0, 1.0, Double.MAX_VALUE))
        assertEquals(Double.MAX_VALUE, maxWeight.totalWeight, 0.0)
        assertNull(YarnEstimator.estimate(2.0, 1.0, Double.MAX_VALUE))
    }
}
