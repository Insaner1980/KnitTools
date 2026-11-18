package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectCounterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddCounterFormValidationTest {
    @Test
    fun `locked repeat section selection cannot create a repeat section counter`() {
        assertEquals(ProjectCounterType.COUNT_UP, counterTypeForDraft(selectedType = 3, isRepeatSection = false))
        assertEquals(ProjectCounterType.REPEAT_SECTION, counterTypeForDraft(selectedType = 3, isRepeatSection = true))
    }

    @Test
    fun `repeat section accepts positive start row`() {
        assertTrue(validate(validRepeatSectionParams()))
    }

    @Test
    fun `repeat section rejects zero start row`() {
        assertFalse(validate(validRepeatSectionParams(repeatStartRow = 0)))
    }

    @Test
    fun `malformed step text cannot fall back to a savable default`() {
        listOf("", "1e3", "12.5", "+2", "-2", "oops", "2147483648").forEach { text ->
            val parsedStep = parseCounterInput(text)
            assertNull(parsedStep)
            assertFalse(validate(validRepeatSectionParams().copy(stepSize = parsedStep ?: 0)))
        }
        assertTrue(validate(validRepeatSectionParams().copy(stepSize = requireNotNull(parseCounterInput(" 2 ")))))
    }

    @Test
    fun `unsigned starting stitches reject negative and malformed text`() {
        listOf("-2", "+2", "1e3", "12.5").forEach { text ->
            val params =
                validRepeatSectionParams().copy(
                    isRepeatSection = false,
                    isShaping = true,
                    startingStitches = parseCounterInput(text),
                    stitchChange = parseCounterInput("-2", allowNegative = true),
                    shapeEveryN = 4,
                )
            assertNull(params.startingStitches)
            assertFalse(validate(params))
        }
    }

    @Test
    fun `signed shaping delta remains valid while other counts remain unsigned`() {
        val params =
            validRepeatSectionParams().copy(
                isRepeatSection = false,
                isShaping = true,
                startingStitches = parseCounterInput("40"),
                stitchChange = parseCounterInput("-2", allowNegative = true),
                shapeEveryN = 4,
            )
        assertEquals(-2, params.stitchChange)
        assertTrue(validate(params))
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
