package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.SessionTimeSnapshot

const val ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS = 24L * 60L * 60L

fun evaluateActiveSessionTime(
    anchors: ActiveSessionTimingAnchors,
    now: SessionTimeSnapshot,
): ActiveSessionTimeEvaluation {
    if (hasInvalidTimingAnchors(anchors)) {
        return ActiveSessionTimeEvaluation.NeedsReview(
            reason = ActiveSessionRecoveryReason.INVALID_ANCHORS,
            suggestedPendingDurationSeconds = null,
        )
    }
    val storedBootCount = anchors.bootCount
    val currentBootCount = now.bootCount
    val bootCountRecoveryReason = bootCountRecoveryReason(storedBootCount, currentBootCount)
    if (bootCountRecoveryReason != null) {
        return ActiveSessionTimeEvaluation.NeedsReview(
            reason = bootCountRecoveryReason,
            suggestedPendingDurationSeconds = null,
        )
    }

    checkNotNull(storedBootCount)
    checkNotNull(currentBootCount)
    if (storedBootCount != currentBootCount) {
        return ActiveSessionTimeEvaluation.NeedsReview(
            reason = ActiveSessionRecoveryReason.REBOOTED,
            suggestedPendingDurationSeconds =
                credibleWallClockDurationSeconds(
                    startedAtMillis = anchors.segmentStartedAtWallMillis,
                    nowMillis = now.wallClockMillis,
                ),
        )
    }

    if (now.elapsedRealtimeMillis < anchors.segmentStartedElapsedRealtimeMillis) {
        return ActiveSessionTimeEvaluation.NeedsReview(
            reason = ActiveSessionRecoveryReason.INVALID_ANCHORS,
            suggestedPendingDurationSeconds = null,
        )
    }
    val elapsedMillis = now.elapsedRealtimeMillis - anchors.segmentStartedElapsedRealtimeMillis
    val segmentSeconds = elapsedMillis / 1_000L
    val totalDurationSeconds = saturatingAdd(anchors.checkpointedDurationSeconds, segmentSeconds)
    val unreviewedDurationSeconds = totalDurationSeconds - anchors.reviewedDurationBaselineSeconds
    if (unreviewedDurationSeconds >= ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS) {
        return ActiveSessionTimeEvaluation.NeedsReview(
            reason = ActiveSessionRecoveryReason.LONG_RUNNING,
            suggestedPendingDurationSeconds = unreviewedDurationSeconds,
        )
    }
    return ActiveSessionTimeEvaluation.Trusted(
        totalDurationSeconds = totalDurationSeconds,
        currentSegmentSeconds = segmentSeconds,
    )
}

private fun hasInvalidTimingAnchors(anchors: ActiveSessionTimingAnchors): Boolean =
    listOf(
        anchors.segmentStartedAtWallMillis < 0L,
        anchors.segmentStartedElapsedRealtimeMillis < 0L,
        anchors.checkpointedDurationSeconds < 0L,
        anchors.reviewedDurationBaselineSeconds < 0L,
        anchors.reviewedDurationBaselineSeconds > anchors.checkpointedDurationSeconds,
    ).any { it }

private fun bootCountRecoveryReason(
    storedBootCount: Long?,
    currentBootCount: Long?,
): ActiveSessionRecoveryReason? =
    when {
        storedBootCount == null || currentBootCount == null -> ActiveSessionRecoveryReason.BOOT_IDENTITY_UNAVAILABLE
        storedBootCount < 0L || currentBootCount < 0L -> ActiveSessionRecoveryReason.INVALID_ANCHORS
        else -> null
    }

private fun credibleWallClockDurationSeconds(
    startedAtMillis: Long,
    nowMillis: Long,
): Long? {
    if (startedAtMillis < 0L || nowMillis < startedAtMillis) return null
    return (nowMillis - startedAtMillis) / 1_000L
}

internal fun saturatingAdd(
    first: Long,
    second: Long,
): Long =
    when {
        first < 0L || second < 0L -> 0L
        Long.MAX_VALUE - first < second -> Long.MAX_VALUE
        else -> first + second
    }
