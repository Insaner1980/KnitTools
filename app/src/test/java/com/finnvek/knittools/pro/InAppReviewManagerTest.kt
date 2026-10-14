package com.finnvek.knittools.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewManagerTest {
    @Test
    fun `recorded action count saturates without losing review eligibility`() {
        val count = InAppReviewManager.nextActionCount(Int.MAX_VALUE)

        assertEquals(Int.MAX_VALUE, count)
        assertTrue(InAppReviewManager.shouldRequestReview(false, count))
        assertEquals(Int.MAX_VALUE, InAppReviewManager.nextActionCount(Int.MAX_VALUE - 1))
    }

    @Test
    fun `recorded action count recovers invalid negative values and increments normally`() {
        assertEquals(1, InAppReviewManager.nextActionCount(Int.MIN_VALUE))
        assertEquals(1, InAppReviewManager.nextActionCount(-1))
        assertEquals(1, InAppReviewManager.nextActionCount(0))
        assertEquals(20, InAppReviewManager.nextActionCount(19))
    }

    @Test
    fun `shouldRequestReview returns false when review was already requested`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = true,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldRequestReview waits for enough recorded actions`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = false,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD - 1,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldRequestReview allows request at action threshold`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = false,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD,
            )

        assertTrue(result)
    }
}
