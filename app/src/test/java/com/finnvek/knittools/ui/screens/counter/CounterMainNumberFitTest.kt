package com.finnvek.knittools.ui.screens.counter

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterMainNumberFitTest {
    @Test
    fun `unbounded or roomy height keeps the full font size`() {
        assertEquals(115.sp, counterMainNumberHeightLimitedFontSize(115.sp, Constraints.Infinity, 400))
        assertEquals(115.sp, counterMainNumberHeightLimitedFontSize(115.sp, 400, 400))
    }

    @Test
    fun `short side by side row scales the number by the overflow ratio`() {
        // 200 %:n fontilla luku oli 138 dp korkea 120 dp:n tilassa.
        val fitted = counterMainNumberHeightLimitedFontSize(115.sp, 120, 138)
        assertEquals(100f, fitted.value, 0.01f)
    }

    @Test
    fun `missing measurement does not shrink the number`() {
        assertEquals(115.sp, counterMainNumberHeightLimitedFontSize(115.sp, 120, 0))
    }
}
