package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsRangeSelectorLayoutTest {
    @Test
    fun `range selector falls back only above the readable font scale threshold`() {
        assertFalse(useRangeSelectorFallback(fontScale = 1.3f))
        assertTrue(useRangeSelectorFallback(fontScale = 1.31f))
    }

    @Test
    fun `sliding indicator keeps primary label color under its animated midpoint`() {
        assertEquals(0, rangeSelectorIndicatorLabelIndex(indicatorOffsetFraction = 0f, segmentCount = 3))
        assertEquals(0, rangeSelectorIndicatorLabelIndex(indicatorOffsetFraction = 0.16f, segmentCount = 3))
        assertEquals(1, rangeSelectorIndicatorLabelIndex(indicatorOffsetFraction = 0.17f, segmentCount = 3))
        assertEquals(1, rangeSelectorIndicatorLabelIndex(indicatorOffsetFraction = 0.49f, segmentCount = 3))
        assertEquals(2, rangeSelectorIndicatorLabelIndex(indicatorOffsetFraction = 0.5f, segmentCount = 3))
    }
}
