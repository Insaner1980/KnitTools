package com.finnvek.knittools.ui.screens.counter

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterMainNumberFitTest {
    @Test
    fun `roomy cell keeps the full font size after one measurement`() {
        var measurements = 0
        val fitted =
            counterMainNumberFittedFontSize(115.sp, 48.sp) {
                measurements++
                true
            }
        assertEquals(115.sp, fitted)
        assertEquals(1, measurements)
    }

    @Test
    fun `wide short cell uses measured height at each candidate size`() {
        // Fixed font metrics make the height larger than proportional scaling would predict.
        val fitted = counterMainNumberFittedFontSize(115.sp, 48.sp) { it.value + 30f <= 120f }
        assertTrue(fitted.value in 89.7f..90f)
    }

    @Test
    fun `long count fits a narrow cell with a bounded number of measurements`() {
        var measurements = 0
        val fitted =
            counterMainNumberFittedFontSize(115.sp, 48.sp) {
                measurements++
                it.value * 10 <= 240f
            }
        assertEquals(24.sp, fitted)
        assertEquals(10, measurements)
    }
}
