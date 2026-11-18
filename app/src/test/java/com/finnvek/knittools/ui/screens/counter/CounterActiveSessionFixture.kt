package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.ActiveWorkSession

internal fun activeSession() =
    ActiveWorkSession(
        sessionToken = "existing",
        projectId = 8L,
        startedAtWallMillis = 1_000L,
        startZoneId = "Europe/Helsinki",
        startRow = 0,
        lastObservedRow = 0,
        trustedLastObservedRow = 0,
        trustedRowsWorked = 0,
        pendingRowsWorked = 0,
        reviewedRowsWorked = 0,
        reviewedLastObservedRow = 0,
        unreviewedRowsWorked = 0,
        timingAnchors = ActiveSessionTimingAnchors(1_000L, 1_000L, 1L, 0L, 0L),
        recoveryReason = null,
        recoveryIntervalToken = null,
        recoverySuggestedDurationSeconds = null,
        recoveryPromptShown = false,
    )
