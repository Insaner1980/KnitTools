package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatSectionLogicTest {
    private fun repeatSectionCounter() =
        ProjectCounter(
            id = 1L,
            projectId = 1L,
            name = "Sleeve repeat",
            counterType = "REPEAT_SECTION",
            repeatStartRow = 10,
            repeatEndRow = 13,
            totalRepeats = 3,
            currentRepeat = 1,
        )

    @Test
    fun `updatePosition returns domain counter at current repeat row`() {
        val result = RepeatSectionLogic.updatePosition(repeatSectionCounter(), mainRowCount = 15)

        assertEquals(2, result.currentRepeat)
        assertEquals(2, result.count)
    }

    @Test
    fun `isComplete is true at final tracked row`() {
        assertTrue(RepeatSectionLogic.isComplete(repeatSectionCounter(), mainRowCount = 21))
    }

    @Test
    fun `isComplete is false before final tracked row`() {
        assertFalse(RepeatSectionLogic.isComplete(repeatSectionCounter(), mainRowCount = 20))
    }
}
