package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.ActiveWorkSession

fun ActiveSessionEntity.toDomain(): ActiveWorkSession =
    ActiveWorkSession(
        sessionToken = sessionToken,
        projectId = projectId,
        startedAtWallMillis = startedAtWallMillis,
        startZoneId = startZoneId,
        startRow = startRow,
        lastObservedRow = lastObservedRow,
        trustedLastObservedRow = trustedLastObservedRow,
        trustedRowsWorked = trustedRowsWorked,
        pendingRowsWorked = pendingRowsWorked,
        reviewedRowsWorked = reviewedRowsWorked,
        reviewedLastObservedRow = reviewedLastObservedRow,
        unreviewedRowsWorked = unreviewedRowsWorked,
        timingAnchors =
            ActiveSessionTimingAnchors(
                segmentStartedAtWallMillis = segmentStartedAtWallMillis,
                segmentStartedElapsedRealtimeMillis = segmentStartedElapsedRealtimeMillis,
                bootCount = bootCount,
                checkpointedDurationSeconds = checkpointedDurationSeconds,
                reviewedDurationBaselineSeconds = reviewedDurationBaselineSeconds,
            ),
        recoveryReason =
            recoveryReason?.let { value ->
                ActiveSessionRecoveryReason.entries.firstOrNull { it.name == value }
                    ?: ActiveSessionRecoveryReason.INVALID_ANCHORS
            },
        recoveryIntervalToken = recoveryIntervalToken,
        recoverySuggestedDurationSeconds = recoverySuggestedDurationSeconds,
        recoveryPromptShown = recoveryPromptShown,
    )
