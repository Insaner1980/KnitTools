package com.finnvek.knittools.repository

import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.calculator.ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterRepositorySessionPresentationTest : CounterRepositoryActiveSessionFixture() {
    @Test
    fun `main presentation reads wait for injected IO and preserve timing and recovery`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val session = (repository.startSession(7L) as StartSessionResult.Started).session
                val initial = timeSource.snapshot()
                var now = initial
                var reads = 0
                val ioScheduler = TestCoroutineScheduler()
                val callerScope = CoroutineScope(coroutineContext.minusKey(TestCoroutineScheduler))
                repository =
                    buildRepository(
                        counterDao = repositoryProjectCounterDao(),
                        ioDispatcher = StandardTestDispatcher(ioScheduler),
                        sessionTimeSource =
                            object : SessionTimeSource {
                                override fun snapshot(): SessionTimeSnapshot {
                                    reads++
                                    return now
                                }
                            },
                    )
                for ((snapshot, seconds, needsRecovery) in presentationScenarios(initial)) {
                    now = snapshot
                    val readsBefore = reads
                    val duration =
                        callerScope.async(Dispatchers.Main) { repository.activeSessionDurationSeconds(session) }
                    val recovery =
                        callerScope.async(Dispatchers.Main) { repository.activeSessionNeedsRecovery(session) }
                    runCurrent()
                    assertEquals(readsBefore, reads)
                    assertFalse(duration.isCompleted)
                    assertFalse(recovery.isCompleted)

                    ioScheduler.runCurrent()
                    runCurrent()

                    assertEquals(readsBefore + 2, reads)
                    assertEquals(seconds, duration.await())
                    assertEquals(needsRecovery, recovery.await())
                }

                val reviewed =
                    session.copy(
                        timingAnchors = session.timingAnchors.copy(checkpointedDurationSeconds = 30L),
                        recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                        recoverySuggestedDurationSeconds = 40L,
                    )
                now = initial.copy(bootCount = null)
                val result =
                    callerScope.async(Dispatchers.Main) {
                        repository.activeSessionDurationSeconds(reviewed) to
                            repository.activeSessionNeedsRecovery(reviewed)
                    }
                repeat(2) {
                    runCurrent()
                    ioScheduler.runCurrent()
                }
                runCurrent()
                assertEquals(70L, result.await().first)
                assertTrue(result.await().second)
                assertEquals(session.sessionToken, active?.sessionToken)
                assertTrue(completed.isEmpty())
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun presentationScenarios(initial: SessionTimeSnapshot): List<Triple<SessionTimeSnapshot, Long, Boolean>> {
        val thresholdMillis = ACTIVE_SESSION_REVIEW_THRESHOLD_SECONDS * 1_000L
        return listOf(
            Triple(
                initial.copy(elapsedRealtimeMillis = initial.elapsedRealtimeMillis + 65_000L),
                65L,
                false,
            ),
            Triple(initial.copy(bootCount = null), 0L, true),
            Triple(initial.copy(bootCount = 5L), 0L, true),
            Triple(initial.copy(elapsedRealtimeMillis = initial.elapsedRealtimeMillis - 1L), 0L, true),
            Triple(
                initial.copy(elapsedRealtimeMillis = initial.elapsedRealtimeMillis + thresholdMillis),
                0L,
                true,
            ),
        )
    }
}
