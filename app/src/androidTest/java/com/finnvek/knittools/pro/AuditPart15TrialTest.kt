package com.finnvek.knittools.pro

import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditPart15TrialTest {
    @Test fun unstartedMalformedAndExactFinalMillisecondFollowTrialPolicyOnAndroid() {
        val start = 1_800_000_000_000L
        val now = SessionTimeSnapshot(start, 100_000, 1, "Europe/Helsinki")
        assertFalse(TrialManager.evaluateTrialTiming(now, 0, 0).state.hasStarted)
        val malformed =
            TrialManager
                .evaluateTrialTiming(
                    now,
                    start,
                    start,
                    storedTiming = StoredTrialTiming.Malformed,
                ).state
        assertFalse(malformed.isActive)
        assertTrue(malformed.clockTampered)
        val duration = 14 * 86_400_000L
        for (elapsed in listOf(0L, duration - 1, duration)) {
            val state =
                TrialManager
                    .evaluateTrialTiming(
                        now.copy(wallClockMillis = start + elapsed, elapsedRealtimeMillis = 100_000 + elapsed),
                        start,
                        start,
                        storedTiming = StoredTrialTiming.Anchored(0, start, 100_000, 1),
                    ).state
            assertEquals(
                if (elapsed == 0L) {
                    14
                } else if (elapsed < duration) {
                    1
                } else {
                    0
                },
                state.daysRemaining,
            )
            assertEquals(elapsed < duration, state.isActive)
        }
    }
}
