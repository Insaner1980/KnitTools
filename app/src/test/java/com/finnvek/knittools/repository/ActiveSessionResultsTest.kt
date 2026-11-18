package com.finnvek.knittools.repository

import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.ActiveWorkSession
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveSessionResultsTest {
    @Test
    fun `session review results preserve their session and completion identity`() {
        val session = activeSession()

        assertEquals(7L, StopSessionResult.Saved(7).completedSessionId)
        assertEquals(session, StopSessionResult.NeedsRecoveryReview(session).session)
        assertEquals(8L, RecoveryResolutionResult.DiscardedAndStopped(8).completedSessionId)
        assertEquals(9L, RecoveryResolutionResult.EditedAndStopped(9).completedSessionId)
        assertEquals(session, ProjectCompletionResult.NeedsActiveSessionChoice(session).session)
        assertEquals(session, ProjectCompletionResult.NeedsRecoveryReview(session).session)
        assertEquals(session, ProjectDeletionResult.NeedsActiveSessionDiscard(session).session)
    }

    private fun activeSession() =
        ActiveWorkSession(
            sessionToken = "session",
            projectId = 1,
            startedAtWallMillis = 1,
            startZoneId = "Europe/Helsinki",
            startRow = 0,
            lastObservedRow = 0,
            trustedLastObservedRow = 0,
            trustedRowsWorked = 0,
            pendingRowsWorked = 0,
            reviewedRowsWorked = 0,
            reviewedLastObservedRow = 0,
            unreviewedRowsWorked = 0,
            timingAnchors = ActiveSessionTimingAnchors(1, 1, null, 0, 0),
            recoveryReason = null,
            recoveryIntervalToken = null,
            recoverySuggestedDurationSeconds = null,
            recoveryPromptShown = false,
        )
}
