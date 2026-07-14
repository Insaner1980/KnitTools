package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.MainCounterTargetSlot
import com.finnvek.knittools.domain.model.MainCounterLabelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterTargetHelperTextTest {
    @Test
    fun `target helper is hidden when target is missing or not positive`() {
        assertNull(counterTargetHelperText(null))
    }

    @Test
    fun `target helper carries built in label type for left reached and past target states`() {
        assertEquals(
            CounterTargetHelperText.ItemsLeft(MainCounterCountSlot(2, MainCounterLabelType.ROUNDS)),
            counterTargetHelperText(targetSlot(count = 62, target = 64, labelType = MainCounterLabelType.ROUNDS)),
        )
        assertEquals(
            CounterTargetHelperText.ItemsLeft(MainCounterCountSlot(1, MainCounterLabelType.REPEATS)),
            counterTargetHelperText(targetSlot(count = 63, target = 64, labelType = MainCounterLabelType.REPEATS)),
        )
        assertEquals(
            CounterTargetHelperText.TargetReached,
            counterTargetHelperText(targetSlot(count = 64, target = 64, labelType = MainCounterLabelType.ROWS)),
        )
        assertEquals(
            CounterTargetHelperText.PastTarget(MainCounterCountSlot(1, MainCounterLabelType.ROUNDS)),
            counterTargetHelperText(targetSlot(count = 65, target = 64, labelType = MainCounterLabelType.ROUNDS)),
        )
        assertEquals(
            CounterTargetHelperText.PastTarget(MainCounterCountSlot(3, MainCounterLabelType.REPEATS)),
            counterTargetHelperText(targetSlot(count = 67, target = 64, labelType = MainCounterLabelType.REPEATS)),
        )
    }

    @Test
    fun `target helper carries custom label for left and past target states`() {
        assertEquals(
            CounterTargetHelperText.ItemsLeft(
                MainCounterCountSlot(
                    count = 2,
                    labelType = MainCounterLabelType.CUSTOM,
                    customLabel = "Chart rows",
                ),
            ),
            counterTargetHelperText(
                targetSlot(
                    count = 62,
                    target = 64,
                    labelType = MainCounterLabelType.CUSTOM,
                    customLabel = "Chart rows",
                ),
            ),
        )
        assertEquals(
            CounterTargetHelperText.PastTarget(
                MainCounterCountSlot(
                    count = 3,
                    labelType = MainCounterLabelType.CUSTOM,
                    customLabel = "Chart rows",
                ),
            ),
            counterTargetHelperText(
                targetSlot(
                    count = 67,
                    target = 64,
                    labelType = MainCounterLabelType.CUSTOM,
                    customLabel = "Chart rows",
                ),
            ),
        )
    }

    private fun targetSlot(
        count: Int,
        target: Int,
        labelType: MainCounterLabelType,
        customLabel: String? = null,
    ): MainCounterTargetSlot =
        MainCounterTargetSlot(
            count = count,
            target = target,
            labelType = labelType,
            customLabel = customLabel,
        )
}
