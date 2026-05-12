package com.finnvek.knittools.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewManagerTest {
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
