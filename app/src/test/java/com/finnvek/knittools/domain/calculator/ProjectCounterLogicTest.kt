package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.data.local.ProjectCounterEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectCounterLogicTest {
    private fun counter(
        count: Int = 0,
        stepSize: Int = 1,
        repeatAt: Int? = null,
    ) = ProjectCounterEntity(
        id = 1,
        projectId = 1,
        name = "Test",
        count = count,
        stepSize = stepSize,
        repeatAt = repeatAt,
    )

    @Test
    fun `increment adds step size`() {
        val result = ProjectCounterLogic.increment(counter(count = 5, stepSize = 1))
        assertEquals(6, result.count)
    }

    @Test
    fun `increment with step size 3`() {
        val result = ProjectCounterLogic.increment(counter(count = 5, stepSize = 3))
        assertEquals(8, result.count)
    }

    @Test
    fun `decrement subtracts step size`() {
        val result = ProjectCounterLogic.decrement(counter(count = 5, stepSize = 1))
        assertEquals(4, result.count)
    }

    @Test
    fun `decrement at 0 stays at 0`() {
        val result = ProjectCounterLogic.decrement(counter(count = 0))
        assertEquals(0, result.count)
    }

    @Test
    fun `decrement does not go negative`() {
        val result = ProjectCounterLogic.decrement(counter(count = 1, stepSize = 3))
        assertEquals(0, result.count)
    }

    @Test
    fun `repeat cycling resets at repeatAt`() {
        val result = ProjectCounterLogic.increment(counter(count = 7, stepSize = 1, repeatAt = 8))
        assertEquals(0, result.count)
    }

    @Test
    fun `repeat cycling at exact boundary`() {
        val result = ProjectCounterLogic.increment(counter(count = 6, stepSize = 2, repeatAt = 8))
        assertEquals(0, result.count)
    }

    @Test
    fun `no repeat cycling when below repeatAt`() {
        val result = ProjectCounterLogic.increment(counter(count = 5, stepSize = 1, repeatAt = 8))
        assertEquals(6, result.count)
    }

    @Test
    fun `no repeat cycling when repeatAt is null`() {
        val result = ProjectCounterLogic.increment(counter(count = 100, stepSize = 1, repeatAt = null))
        assertEquals(101, result.count)
    }
}
