package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class YarnEstimatorTest {
    @Test
    fun `exact number of skeins`() {
        val result =
            YarnEstimator.estimate(
                totalYarnNeeded = 400.0,
                yarnPerSkein = 200.0,
                weightPerSkein = 100.0,
            )
        assertEquals(2, result.skeinsNeeded)
        assertEquals(200.0, result.totalWeight, 0.01)
        assertEquals(2.0, result.exactSkeins, 0.01)
    }

    @Test
    fun `rounds up to next skein`() {
        val result =
            YarnEstimator.estimate(
                totalYarnNeeded = 450.0,
                yarnPerSkein = 200.0,
                weightPerSkein = 100.0,
            )
        assertEquals(3, result.skeinsNeeded)
        assertEquals(300.0, result.totalWeight, 0.01)
        assertEquals(2.25, result.exactSkeins, 0.01)
    }

    @Test
    fun `small amount needs at least one skein`() {
        val result =
            YarnEstimator.estimate(
                totalYarnNeeded = 10.0,
                yarnPerSkein = 200.0,
                weightPerSkein = 50.0,
            )
        assertEquals(1, result.skeinsNeeded)
    }

    @Test
    fun `zero yarn per skein returns zero`() {
        val result =
            YarnEstimator.estimate(
                totalYarnNeeded = 400.0,
                yarnPerSkein = 0.0,
                weightPerSkein = 100.0,
            )
        assertEquals(0, result.skeinsNeeded)
    }

    @Test
    fun `weight calculation is based on whole skeins`() {
        val result =
            YarnEstimator.estimate(
                totalYarnNeeded = 500.0,
                yarnPerSkein = 150.0,
                weightPerSkein = 50.0,
            )
        assertEquals(4, result.skeinsNeeded)
        assertEquals(200.0, result.totalWeight, 0.01)
    }
}
