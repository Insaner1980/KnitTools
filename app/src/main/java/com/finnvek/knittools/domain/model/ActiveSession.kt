package com.finnvek.knittools.domain.model

data class SessionTimeSnapshot(
    val wallClockMillis: Long,
    val elapsedRealtimeMillis: Long,
    val bootCount: Long?,
    val zoneId: String,
)

data class ActiveSessionTimingAnchors(
    val segmentStartedAtWallMillis: Long,
    val segmentStartedElapsedRealtimeMillis: Long,
    val bootCount: Long?,
    val checkpointedDurationSeconds: Long,
    val reviewedDurationBaselineSeconds: Long,
)

enum class ActiveSessionRecoveryReason {
    REBOOTED,
    BOOT_IDENTITY_UNAVAILABLE,
    INVALID_ANCHORS,
    LONG_RUNNING,
}

sealed interface ActiveSessionTimeEvaluation {
    data class Trusted(
        val totalDurationSeconds: Long,
        val currentSegmentSeconds: Long,
    ) : ActiveSessionTimeEvaluation

    data class NeedsReview(
        val reason: ActiveSessionRecoveryReason,
        val suggestedPendingDurationSeconds: Long?,
    ) : ActiveSessionTimeEvaluation
}

data class ActiveWorkSession(
    val sessionToken: String,
    val projectId: Long,
    val startedAtWallMillis: Long,
    val startZoneId: String,
    val startRow: Int,
    val lastObservedRow: Int,
    val trustedLastObservedRow: Int,
    val trustedRowsWorked: Int,
    val pendingRowsWorked: Int,
    val reviewedRowsWorked: Int,
    val reviewedLastObservedRow: Int,
    val unreviewedRowsWorked: Int,
    val timingAnchors: ActiveSessionTimingAnchors,
    val recoveryReason: ActiveSessionRecoveryReason?,
    val recoveryIntervalToken: String?,
    val recoverySuggestedDurationSeconds: Long?,
    val recoveryPromptShown: Boolean,
) {
    val needsRecoveryReview: Boolean
        get() = recoveryReason != null
}
