package com.finnvek.knittools.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterRepositoryStopSessionTest : CounterRepositoryActiveSessionFixture() {
    @Test
    fun `background checkpoint after stop review preserves the reviewed duration`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 10L)
            val reviewed = requireNotNull(repository.refreshActiveSession())
            val reviewedDuration = repository.activeSessionDurationSeconds(reviewed)
            timeSource.advance(seconds = 50L)
            repository.checkpointActiveSession()

            val stopped =
                repository.stopSession(
                    sessionToken = started.session.sessionToken,
                    reviewedDurationSeconds = reviewedDuration,
                    reviewedRowsWorked = reviewed.trustedRowsWorked,
                    reviewedEndRow = reviewed.trustedLastObservedRow,
                )

            assertEquals(StopSessionResult.Saved(1L, wasFirstCompletedSession = true), stopped)
            assertEquals(10L, completed.single().durationSeconds)
            assertEquals(11_000L, completed.single().endedAt)
            assertEquals(null, active)
            assertEquals(
                StopSessionResult.NoActiveSession,
                repository.stopSession(started.session.sessionToken, reviewedDuration, 0, 12),
            )
            assertEquals(1, completed.size)
        }

    @Test
    fun `stop rejects durations outside the reviewed recovery interval and changed rows`() =
        runTest {
            val started = repository.startSession(7L) as StartSessionResult.Started
            timeSource.advance(seconds = 10L)
            repository.checkpointActiveSession()
            timeSource.reboot()
            val recovery = requireNotNull(repository.refreshActiveSession())
            repository.addRecoveryInterval(recovery.sessionToken, requireNotNull(recovery.recoveryIntervalToken), 20L)
            timeSource.advance(seconds = 10L)
            repository.checkpointActiveSession()
            val before = active

            val staleSummaries =
                listOf(
                    Triple(-1L, 0, 12),
                    Triple(10L, 0, 12),
                    Triple(41L, 0, 12),
                    Triple(35L, 1, 12),
                    Triple(35L, 0, 13),
                )
            for ((duration, rows, endRow) in staleSummaries) {
                assertEquals(
                    StopSessionResult.StaleAction,
                    repository.stopSession(started.session.sessionToken, duration, rows, endRow),
                )
                assertEquals(before, active)
                assertTrue(completed.isEmpty())
            }
            assertEquals(
                StopSessionResult.Saved(1L, wasFirstCompletedSession = true),
                repository.stopSession(started.session.sessionToken, 35L, 0, 12),
            )
            assertEquals(35L, completed.single().durationSeconds)
        }
}
