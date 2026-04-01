package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterLogicTest {

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
