package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddCounterFormValidationTest {
    @Test
    fun `repeat section accepts positive start row`() {
        assertTrue(validate(validRepeatSectionParams()))
    }

    @Test
    fun `repeat section rejects zero start row`() {
        assertFalse(validate(validRepeatSectionParams(repeatStartRow = 0)))
    }

    private fun validate(params: AddCounterFormParams): Boolean {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.screens.counter.MultiCounterComponentsKt")
                .getDeclaredMethod("isAddCounterFormValid", AddCounterFormParams::class.java)
                .apply { isAccessible = true }
        return method.invoke(null, params) as Boolean
    }

    private fun validRepeatSectionParams(repeatStartRow: Int = 1) =
        AddCounterFormParams(
            name = "Sleeve repeat",
            stepSize = 1,
            isRepeating = false,
            repeatAt = null,
            isShaping = false,
            startingStitches = null,
            stitchChange = null,
            shapeEveryN = null,
            isRepeatSection = true,
            repeatStartRow = repeatStartRow,
            repeatEndRow = 4,
            totalRepeats = 3,
        )
}
