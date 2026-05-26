package com.finnvek.knittools.ui.screens.yarncard

import org.junit.Assert.assertEquals
import org.junit.Test

class YarnMeasurementFormattingTest {
    @Test
    fun `numeric yarn measurements get their display unit`() {
        assertEquals("100 g", formatYarnMeasurement("100", "g"))
        assertEquals("420 m", formatYarnMeasurement("420", "m"))
    }

    @Test
    fun `yarn measurements that already include their unit are not duplicated`() {
        assertEquals("100 g", formatYarnMeasurement("100 g", "g"))
        assertEquals("420 m", formatYarnMeasurement("420 m", "m"))
    }
}
