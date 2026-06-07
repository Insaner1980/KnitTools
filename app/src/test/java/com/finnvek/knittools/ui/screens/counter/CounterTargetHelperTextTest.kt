package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterTargetHelperTextTest {
    @Test
    fun `target helper is hidden when target is missing or not positive`() {
        assertNull(counterTargetHelperText(count = 62, targetRows = null))
        assertNull(counterTargetHelperText(count = 62, targetRows = 0))
        assertNull(counterTargetHelperText(count = 62, targetRows = -1))
    }

    @Test
    fun `target helper reports rows left reached and past target states`() {
        assertEquals(CounterTargetHelperText.RowsLeft(2), counterTargetHelperText(count = 62, targetRows = 64))
        assertEquals(CounterTargetHelperText.OneRowLeft, counterTargetHelperText(count = 63, targetRows = 64))
        assertEquals(CounterTargetHelperText.TargetReached, counterTargetHelperText(count = 64, targetRows = 64))
        assertEquals(CounterTargetHelperText.PastTarget(1), counterTargetHelperText(count = 65, targetRows = 64))
        assertEquals(CounterTargetHelperText.PastTarget(3), counterTargetHelperText(count = 67, targetRows = 64))
    }
}
