package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterLogicTest {
    @Test
    fun `step input accepts positive bounded integers only`() {
        for (value in listOf("1", "2", "5", "99", " 12 ")) {
            assertEquals(value.trim().toInt(), parseMainCounterStepSize(value))
        }
        for (value in listOf("", "0", "-1", "100", "1.5", "1,5", "1e2", "+5", "999999999999")) {
            assertNull(value, parseMainCounterStepSize(value))
        }
    }

    @Test
    fun `one undo restores the entire positive or negative step including a clamped step`() {
        val original = CounterState(count = 12, stepSize = 5)
        assertEquals(17, CounterLogic.increment(original).count)
        assertEquals(7, CounterLogic.decrement(original).count)
        assertEquals(12, CounterLogic.undo(CounterLogic.increment(original)).count)
        assertEquals(12, CounterLogic.undo(CounterLogic.decrement(original)).count)
        val nearZero = CounterState(count = 2, stepSize = 5)
        assertEquals(0, CounterLogic.decrement(nearZero).count)
        assertEquals(2, CounterLogic.undo(CounterLogic.decrement(nearZero)).count)
    }

    @Test
    fun `increment increases count by step size`() {
        val state = CounterState(count = 5, stepSize = 1)
        val result = CounterLogic.increment(state)
        assertEquals(6, result.count)
        assertEquals(5, result.previousCount)
    }

    @Test
    fun `increment with step size 3`() {
        val state = CounterState(count = 10, stepSize = 3)
        val result = CounterLogic.increment(state)
        assertEquals(13, result.count)
    }

    @Test
    fun `increment saturates instead of overflowing`() {
        val state = CounterState(count = Int.MAX_VALUE - 1, stepSize = Int.MAX_VALUE)

        val result = CounterLogic.increment(state)

        assertEquals(Int.MAX_VALUE, result.count)
        assertEquals(Int.MAX_VALUE - 1, result.previousCount)
    }

    @Test
    fun `decrement decreases count by step size`() {
        val state = CounterState(count = 5, stepSize = 1)
        val result = CounterLogic.decrement(state)
        assertEquals(4, result.count)
    }

    @Test
    fun `decrement does not go below zero`() {
        val state = CounterState(count = 2, stepSize = 5)
        val result = CounterLogic.decrement(state)
        assertEquals(0, result.count)
    }

    @Test
    fun `extreme decrement reaches zero without overflowing`() {
        val state = CounterState(count = 2, stepSize = Int.MAX_VALUE)

        val result = CounterLogic.decrement(state)

        assertEquals(0, result.count)
    }

    @Test
    fun `counter changes normalize a malformed nonpositive step`() {
        assertEquals(6, CounterLogic.increment(CounterState(count = 5, stepSize = 0)).count)
        assertEquals(4, CounterLogic.decrement(CounterState(count = 5, stepSize = -1)).count)
    }

    @Test
    fun `decrement at zero keeps undo state unchanged`() {
        val state = CounterState(count = 0, previousCount = 5, stepSize = 3)
        val result = CounterLogic.decrement(state)
        assertEquals(state, result)
    }

    @Test
    fun `undo restores previous count`() {
        val state = CounterState(count = 6, previousCount = 5)
        val result = CounterLogic.undo(state)
        assertEquals(5, result.count)
        assertNull(result.previousCount)
    }

    @Test
    fun `undo with no previous does nothing`() {
        val state = CounterState(count = 5, previousCount = null)
        val result = CounterLogic.undo(state)
        assertEquals(5, result.count)
    }

    @Test
    fun `reset sets count to zero`() {
        val state = CounterState(count = 42, stepSize = 1)
        val result = CounterLogic.reset(state)
        assertEquals(0, result.count)
        assertEquals(42, result.previousCount)
    }

    @Test
    fun `reset at zero keeps the earlier undo value`() {
        val state = CounterState(count = 0, previousCount = 42)
        assertEquals(state, CounterLogic.reset(state))
    }

    @Test
    fun `set step size minimum is 1`() {
        val state = CounterState(stepSize = 1)
        val result = CounterLogic.setStepSize(state, 0)
        assertEquals(1, result.stepSize)
    }

    @Test
    fun `set step size to valid value`() {
        val state = CounterState(stepSize = 1)
        val result = CounterLogic.setStepSize(state, 5)
        assertEquals(5, result.stepSize)
    }

    @Test
    fun `increment then undo returns original`() {
        val original = CounterState(count = 10, stepSize = 1)
        val incremented = CounterLogic.increment(original)
        val undone = CounterLogic.undo(incremented)
        assertEquals(original.count, undone.count)
    }
}
