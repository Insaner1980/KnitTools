package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectCounterDraftTest {
    @Test
    fun `constructor uses count-up defaults for optional shaping fields`() {
        val draft =
            ProjectCounterDraft(
                name = "Sleeve",
                repeatAt = null,
                stepSize = 1,
            )

        assertEquals("Sleeve", draft.name)
        assertNull(draft.repeatAt)
        assertEquals(1, draft.stepSize)
        assertEquals("COUNT_UP", draft.counterType)
        assertNull(draft.startingStitches)
        assertNull(draft.stitchChange)
        assertNull(draft.shapeEveryN)
        assertNull(draft.repeatStartRow)
        assertNull(draft.repeatEndRow)
        assertNull(draft.totalRepeats)
        assertNull(draft.currentRepeat)
    }

    @Test
    fun `draft preserves repeat and shaping configuration`() {
        val draft =
            ProjectCounterDraft(
                name = "Waist shaping",
                repeatAt = 6,
                stepSize = 2,
                counterType = "SHAPING",
                startingStitches = 96,
                stitchChange = -2,
                shapeEveryN = 4,
                repeatStartRow = 12,
                repeatEndRow = 18,
                totalRepeats = 5,
                currentRepeat = 2,
            )

        assertEquals("Waist shaping", draft.name)
        assertEquals(6, draft.repeatAt)
        assertEquals(2, draft.stepSize)
        assertEquals("SHAPING", draft.counterType)
        assertEquals(96, draft.startingStitches)
        assertEquals(-2, draft.stitchChange)
        assertEquals(4, draft.shapeEveryN)
        assertEquals(12, draft.repeatStartRow)
        assertEquals(18, draft.repeatEndRow)
        assertEquals(5, draft.totalRepeats)
        assertEquals(2, draft.currentRepeat)
    }
}
