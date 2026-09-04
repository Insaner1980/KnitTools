package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YarnEstimatorTest {
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
}
