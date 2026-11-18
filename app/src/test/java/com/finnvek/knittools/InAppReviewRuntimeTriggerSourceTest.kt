package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewRuntimeTriggerSourceTest {
    @Test
    fun `review request reacts to persisted eligibility changes`() {
        val manager = ProjectSourceFiles.read(IN_APP_REVIEW_MANAGER)
        val activity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(manager.contains("val reviewEligibility: Flow<Boolean> ="))
        assertTrue(manager.contains("context.reviewDataStore.safePreferencesData"))
        assertTrue(
            activity.contains(
                "inAppReviewManager.reviewEligibility.collectAsStateWithLifecycle(initialValue = false)",
            ),
        )
        assertTrue(
            activity.contains("InAppReviewRequestEffect(reviewEligible && lifecycleState == Lifecycle.State.RESUMED)"),
        )
        assertTrue(activity.contains("snapshotFlow { currentEligibility }.filter { it }.collect"))
        assertFalse(activity.contains("LaunchedEffect(reviewEligible)"))
    }

    private companion object {
        private const val IN_APP_REVIEW_MANAGER =
            "app/src/main/java/com/finnvek/knittools/pro/InAppReviewManager.kt"
        private const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
    }
}
