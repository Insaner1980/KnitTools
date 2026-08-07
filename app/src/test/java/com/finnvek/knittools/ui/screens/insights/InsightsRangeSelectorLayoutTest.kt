package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsRangeSelectorLayoutTest {
    @Test
    fun `range selector falls back only above the readable font scale threshold`() {
        assertFalse(useRangeSelectorFallback(fontScale = 1.3f))
        assertTrue(useRangeSelectorFallback(fontScale = 1.31f))
    }
}
