package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkSessionPresentationTest {
    @Test
    fun `duration formatting keeps hours beyond one day`() {
        assertEquals("27:05:09", formatWorkSessionDuration(97_509L))
    }

    @Test
    fun `recovery editor accepts hours and bounded minutes`() {
        assertEquals(9_000L, parseRecoveryDurationSeconds("2", "30"))
        assertNull(parseRecoveryDurationSeconds("2", "60"))
        assertNull(parseRecoveryDurationSeconds("", "15"))
        assertNull(parseRecoveryDurationSeconds(Long.MAX_VALUE.toString(), "0"))
    }

    @Test
    fun `recovery summary addition is overflow safe`() {
        assertEquals(Long.MAX_VALUE, safeDurationSum(Long.MAX_VALUE - 1L, 2L))
        assertEquals(0L, safeDurationSum(-1L, 2L))
    }
}
