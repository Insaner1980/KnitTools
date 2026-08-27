package com.finnvek.knittools.ui.components

import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.MainCounterTargetSlot
import com.finnvek.knittools.domain.model.MainCounterLabelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainCounterTargetStatusTest {
    @Test
    fun `missing target has no status or progress fraction`() {
        assertNull(mainCounterTargetStatus(null))
        assertNull(mainCounterTargetFraction(null))
    }

    @Test
    fun `zero target has no status or progress fraction`() {
        val targetLine = targetSlot(count = 0, target = 0)

        assertNull(mainCounterTargetStatus(targetLine))
        assertNull(mainCounterTargetFraction(targetLine))
    }

    @Test
    fun `zero of forty is remaining with empty progress`() {
        val targetLine = targetSlot(count = 0, target = 40)

        assertEquals(
            MainCounterTargetStatus.Remaining(MainCounterCountSlot(40, MainCounterLabelType.ROWS)),
            mainCounterTargetStatus(targetLine),
        )
        assertEquals(0f, mainCounterTargetFraction(targetLine) ?: error("Expected fraction"), 0f)
    }

    @Test
    fun `eighteen of forty reports remaining progress`() {
        val targetLine = targetSlot(count = 18, target = 40, labelType = MainCounterLabelType.ROUNDS)

        assertEquals(
            MainCounterTargetStatus.Remaining(MainCounterCountSlot(22, MainCounterLabelType.ROUNDS)),
            mainCounterTargetStatus(targetLine),
        )
        assertEquals(0.45f, mainCounterTargetFraction(targetLine) ?: error("Expected fraction"), 0f)
    }

    @Test
    fun `forty of forty is reached with complete progress`() {
        val targetLine = targetSlot(count = 40, target = 40)

        assertEquals(MainCounterTargetStatus.Reached, mainCounterTargetStatus(targetLine))
        assertEquals(1f, mainCounterTargetFraction(targetLine) ?: error("Expected fraction"), 0f)
    }

    @Test
    fun `forty three of forty is past with complete progress`() {
        val targetLine = targetSlot(count = 43, target = 40, labelType = MainCounterLabelType.REPEATS)

        assertEquals(
            MainCounterTargetStatus.Past(MainCounterCountSlot(3, MainCounterLabelType.REPEATS)),
            mainCounterTargetStatus(targetLine),
        )
        assertEquals(1f, mainCounterTargetFraction(targetLine) ?: error("Expected fraction"), 0f)
    }

    @Test
    fun `negative current value keeps remaining status and zero progress`() {
        val targetLine =
            targetSlot(
                count = -5,
                target = 40,
                labelType = MainCounterLabelType.CUSTOM,
                customLabel = "Chart rows",
            )

        assertEquals(
            MainCounterTargetStatus.Remaining(MainCounterCountSlot(45, MainCounterLabelType.CUSTOM, "Chart rows")),
            mainCounterTargetStatus(targetLine),
        )
        assertEquals(0f, mainCounterTargetFraction(targetLine) ?: error("Expected fraction"), 0f)
    }

    private fun targetSlot(
        count: Int,
        target: Int,
        labelType: MainCounterLabelType = MainCounterLabelType.ROWS,
        customLabel: String? = null,
    ): MainCounterTargetSlot =
        MainCounterTargetSlot(
            count = count,
            target = target,
            labelType = labelType,
            customLabel = customLabel,
        )
}
