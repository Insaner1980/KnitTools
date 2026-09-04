package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
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
            counterType = ProjectCounterType.REPEAT_SECTION,
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
    fun `currentRowInRepeat returns first row before repeat section starts`() {
        val result = RepeatSectionLogic.currentRowInRepeat(repeatSectionCounter(), mainRowCount = 9)

        assertEquals(1, result)
    }

    @Test
    fun `isComplete is true at final tracked row`() {
        assertTrue(RepeatSectionLogic.isComplete(repeatSectionCounter(), mainRowCount = 21))
    }

    @Test
    fun `isComplete is false before final tracked row`() {
        assertFalse(RepeatSectionLogic.isComplete(repeatSectionCounter(), mainRowCount = 20))
    }

    @Test
    fun `updatePosition clamps before start and at final tracked row`() {
        val counter = repeatSectionCounter()

        assertEquals(counter.copy(count = 0, currentRepeat = 1), RepeatSectionLogic.updatePosition(counter, 9))
        assertEquals(counter.copy(count = 4, currentRepeat = 3), RepeatSectionLogic.updatePosition(counter, 21))
        assertEquals(counter.copy(count = 4, currentRepeat = 3), RepeatSectionLogic.updatePosition(counter, 99))
    }

    @Test
    fun `missing and invalid repeat metadata leaves counter unchanged and incomplete`() {
        val counter = repeatSectionCounter()
        val invalidCounters =
            listOf(
                counter.copy(repeatStartRow = null),
                counter.copy(repeatEndRow = null),
                counter.copy(totalRepeats = null),
                counter.copy(counterType = ProjectCounterType.COUNT_UP),
                counter.copy(repeatEndRow = 8),
                counter.copy(totalRepeats = 0),
            )

        invalidCounters.forEach { invalid ->
            assertEquals(invalid, RepeatSectionLogic.updatePosition(invalid, 15))
            assertFalse(RepeatSectionLogic.isComplete(invalid, 99))
        }
        assertEquals(counter.count, RepeatSectionLogic.currentRowInRepeat(counter.copy(repeatStartRow = null), 15))
        assertEquals(counter.count, RepeatSectionLogic.currentRowInRepeat(counter.copy(repeatEndRow = null), 15))
        assertEquals(counter.count, RepeatSectionLogic.currentRowInRepeat(counter.copy(repeatEndRow = 8), 15))
        assertEquals(
            counter.count,
            RepeatSectionLogic.currentRowInRepeat(counter.copy(counterType = ProjectCounterType.COUNT_UP), 15),
        )
    }

    @Test
    fun `progress is bounded across the repeat section`() {
        val counter = repeatSectionCounter()

        assertEquals(0f, RepeatSectionLogic.progress(counter, 9))
        assertEquals(0.5f, RepeatSectionLogic.progress(counter, 15))
        assertEquals(1f, RepeatSectionLogic.progress(counter, 99))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(repeatStartRow = null), 15))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(repeatEndRow = null), 15))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(totalRepeats = null), 15))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(counterType = ProjectCounterType.COUNT_UP), 15))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(repeatEndRow = 8), 15))
        assertEquals(0f, RepeatSectionLogic.progress(counter.copy(totalRepeats = 0), 15))
    }

    @Test
    fun `large repeat ranges do not overflow`() {
        val counter =
            repeatSectionCounter().copy(
                repeatStartRow = 1,
                repeatEndRow = Int.MAX_VALUE,
                totalRepeats = Int.MAX_VALUE,
            )

        assertFalse(RepeatSectionLogic.isComplete(counter, Int.MAX_VALUE))
        assertEquals(1, RepeatSectionLogic.updatePosition(counter, Int.MAX_VALUE).currentRepeat)
        assertTrue(RepeatSectionLogic.progress(counter, Int.MAX_VALUE) in 0f..1f)
    }
}
