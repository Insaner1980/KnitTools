package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSessionTimingTest {
    @Test
    fun `same boot uses elapsed realtime and ignores wall clock jumps`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 50L,
                        elapsedRealtimeMillis = 16_500L,
                        bootCount = 4L,
                        zoneId = "Europe/Helsinki",
                    ),
            )

        assertEquals(
            ActiveSessionTimeEvaluation.Trusted(
                totalDurationSeconds = 126L,
                currentSegmentSeconds = 6L,
            ),
            evaluation,
        )
    }

    @Test
    fun `same boot wall clock forward jump and zone change do not alter monotonic duration`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 9_000_000_000L,
                        elapsedRealtimeMillis = 16_500L,
                        bootCount = 4L,
                        zoneId = "America/New_York",
                    ),
            )

        assertEquals(
            ActiveSessionTimeEvaluation.Trusted(
                totalDurationSeconds = 126L,
                currentSegmentSeconds = 6L,
            ),
            evaluation,
        )
    }

    @Test
    fun `reboot requires review and offers credible wall-clock duration`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 70_000L,
                        elapsedRealtimeMillis = 2_000L,
                        bootCount = 5L,
                        zoneId = "Europe/Helsinki",
                    ),
            )

        assertEquals(
            ActiveSessionTimeEvaluation.NeedsReview(
                reason = ActiveSessionRecoveryReason.REBOOTED,
                suggestedPendingDurationSeconds = 60L,
            ),
            evaluation,
        )
    }

    @Test
    fun `missing boot identity requires review without guessing`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(bootCount = null),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 70_000L,
                        elapsedRealtimeMillis = 16_000L,
                        bootCount = null,
                        zoneId = "Europe/Helsinki",
                    ),
            ) as ActiveSessionTimeEvaluation.NeedsReview

        assertEquals(ActiveSessionRecoveryReason.BOOT_IDENTITY_UNAVAILABLE, evaluation.reason)
        assertNull(evaluation.suggestedPendingDurationSeconds)
    }

    @Test
    fun `elapsed realtime rollback requires malformed-anchor review`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 11_000L,
                        elapsedRealtimeMillis = 9_999L,
                        bootCount = 4L,
                        zoneId = "Europe/Helsinki",
                    ),
            )

        assertEquals(
            ActiveSessionTimeEvaluation.NeedsReview(
                reason = ActiveSessionRecoveryReason.INVALID_ANCHORS,
                suggestedPendingDurationSeconds = null,
            ),
            evaluation,
        )
    }

    @Test
    fun `uncheckpointed day requires review instead of capping duration`() {
        val evaluation =
            evaluateActiveSessionTime(
                anchors = anchors(),
                now =
                    SessionTimeSnapshot(
                        wallClockMillis = 100_000_000L,
                        elapsedRealtimeMillis = 10_000L + ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS * 1_000L,
                        bootCount = 4L,
                        zoneId = "Europe/Helsinki",
                    ),
            )

        assertEquals(
            ActiveSessionTimeEvaluation.NeedsReview(
                reason = ActiveSessionRecoveryReason.LONG_RUNNING,
                suggestedPendingDurationSeconds = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS,
            ),
            evaluation,
        )
    }

    @Test
    fun `review threshold includes durable checkpoints since the last review`() {
        val below =
            evaluateActiveSessionTime(
                anchors =
                    anchors().copy(
                        checkpointedDurationSeconds = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS - 2L,
                        reviewedDurationBaselineSeconds = 0L,
                    ),
                now = snapshot(elapsedRealtimeMillis = 11_000L),
            )
        val exact =
            evaluateActiveSessionTime(
                anchors =
                    anchors().copy(
                        checkpointedDurationSeconds = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS - 1L,
                        reviewedDurationBaselineSeconds = 0L,
                    ),
                now = snapshot(elapsedRealtimeMillis = 11_000L),
            )
        val above =
            evaluateActiveSessionTime(
                anchors =
                    anchors().copy(
                        checkpointedDurationSeconds = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS,
                        reviewedDurationBaselineSeconds = 0L,
                    ),
                now = snapshot(elapsedRealtimeMillis = 11_000L),
            )

        assertTrue(below is ActiveSessionTimeEvaluation.Trusted)
        assertEquals(
            ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS,
            (exact as ActiveSessionTimeEvaluation.NeedsReview).suggestedPendingDurationSeconds,
        )
        assertEquals(
            ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS + 1L,
            (above as ActiveSessionTimeEvaluation.NeedsReview).suggestedPendingDurationSeconds,
        )
    }

    @Test
    fun `negative and internally inconsistent anchors require review`() {
        val negative =
            evaluateActiveSessionTime(
                anchors = anchors().copy(checkpointedDurationSeconds = -1L),
                now = snapshot(),
            )
        val inconsistent =
            evaluateActiveSessionTime(
                anchors = anchors().copy(reviewedDurationBaselineSeconds = 121L),
                now = snapshot(),
            )

        assertEquals(
            ActiveSessionRecoveryReason.INVALID_ANCHORS,
            (negative as ActiveSessionTimeEvaluation.NeedsReview).reason,
        )
        assertEquals(
            ActiveSessionRecoveryReason.INVALID_ANCHORS,
            (inconsistent as ActiveSessionTimeEvaluation.NeedsReview).reason,
        )
    }

    @Test
    fun `duration addition saturates instead of overflowing`() {
        assertEquals(Long.MAX_VALUE, saturatingAdd(Long.MAX_VALUE - 5L, 6L))
        assertEquals(0L, saturatingAdd(-1L, 6L))
    }

    private fun snapshot(elapsedRealtimeMillis: Long = 10_000L) =
        SessionTimeSnapshot(
            wallClockMillis = 10_000L,
            elapsedRealtimeMillis = elapsedRealtimeMillis,
            bootCount = 4L,
            zoneId = "Europe/Helsinki",
        )

    private fun anchors(bootCount: Long? = 4L) =
        ActiveSessionTimingAnchors(
            segmentStartedAtWallMillis = 10_000L,
            segmentStartedElapsedRealtimeMillis = 10_000L,
            bootCount = bootCount,
            checkpointedDurationSeconds = 120L,
            reviewedDurationBaselineSeconds = 120L,
        )
}
