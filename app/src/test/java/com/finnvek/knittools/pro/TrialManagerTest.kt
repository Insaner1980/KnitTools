package com.finnvek.knittools.pro

import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrialManagerTest {
    private val day = TimeUnit.DAYS.toMillis(1L)
    private val hour = TimeUnit.HOURS.toMillis(1L)
    private val quarterHour = TimeUnit.MINUTES.toMillis(15L)
    private val baseTime = 1_700_000_000_000L
    private val baseElapsed = 10_000L

    @Test
    fun `first launch keeps trial not started`() {
        val evaluation =
            TrialManager.evaluateTrialTiming(
                now = snapshot(),
                startTimestamp = 0L,
                lastKnownTimestamp = 0L,
            )

        assertFalse(evaluation.state.isActive)
        assertFalse(evaluation.state.hasStarted)
        assertEquals(0, evaluation.state.daysRemaining)
        assertFalse(evaluation.state.clockTampered)
        assertEquals(null, evaluation.anchors)
    }

    @Test
    fun `unavailable boot identity before trial start preserves unstarted state`() {
        val evaluation =
            TrialManager.evaluateTrialTiming(
                now = snapshot(bootCount = null),
                startTimestamp = 0L,
                lastKnownTimestamp = 0L,
            )

        assertFalse(evaluation.state.clockTampered)
        assertFalse(evaluation.state.hasStarted)
        assertFalse(evaluation.state.isActive)
        assertEquals(null, evaluation.anchors)
    }

    @Test
    fun `confirmed trial begins with 14 days remaining`() {
        val state = evaluate(now = snapshot(), storedTiming = anchors()).state

        assertTrue(state.isActive)
        assertTrue(state.hasStarted)
        assertEquals(14, state.daysRemaining)
        assertEquals(0L, state.elapsedDurationMillis)
    }

    @Test
    fun `repeated trial start distinguishes active expired and tampered state`() {
        assertEquals(
            TrialStartResult.AlreadyActive,
            TrialManager.classifyExistingTrial(TrialState(isActive = true, hasStarted = true)),
        )
        assertEquals(
            TrialStartResult.AlreadyExpired,
            TrialManager.classifyExistingTrial(TrialState(hasStarted = true)),
        )
        assertEquals(
            TrialStartResult.AlreadyTampered,
            TrialManager.classifyExistingTrial(TrialState(hasStarted = true, clockTampered = true)),
        )
    }

    @Test
    fun `normal forward progress uses elapsed realtime and preserves day presentation`() {
        val dayThree =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 3 * day, elapsedRealtimeMillis = baseElapsed + 3 * day),
                storedTiming = anchors(),
            ).state
        val fractionalFinalDay =
            evaluate(
                now =
                    snapshot(
                        wallClockMillis = baseTime + 13 * day + 23 * hour,
                        elapsedRealtimeMillis = baseElapsed + 13 * day + 23 * hour,
                    ),
                storedTiming = anchors(),
            ).state

        assertTrue(dayThree.isActive)
        assertEquals(11, dayThree.daysRemaining)
        assertTrue(fractionalFinalDay.isActive)
        assertEquals(1, fractionalFinalDay.daysRemaining)
    }

    @Test
    fun `same boot process restart continues from persisted elapsed anchor`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 3 * day, elapsedRealtimeMillis = baseElapsed + 3 * day),
                storedTiming =
                    anchors(
                        elapsedDurationMillis = 2 * day,
                        anchorWallClockMillis = baseTime + 2 * day,
                        anchorElapsedRealtimeMillis = baseElapsed + 2 * day,
                    ),
                lastKnownTimestamp = baseTime + 2 * day,
            ).state

        assertEquals(3 * day, state.elapsedDurationMillis)
        assertEquals(11, state.daysRemaining)
        assertTrue(state.isActive)
    }

    @Test
    fun `repeated tolerated rollbacks still consume monotonic trial time`() {
        var storedTiming = anchors()
        var lastKnownTimestamp = baseTime
        var evaluation = evaluate(now = snapshot(), storedTiming = storedTiming)

        repeat(2 * 24 * 4) { interval ->
            evaluation =
                evaluate(
                    now =
                        snapshot(
                            wallClockMillis = baseTime - 59 * 60_000L,
                            elapsedRealtimeMillis = baseElapsed + (interval + 1L) * quarterHour,
                        ),
                    storedTiming = storedTiming,
                    lastKnownTimestamp = lastKnownTimestamp,
                )
            storedTiming = checkNotNull(evaluation.anchors)
            lastKnownTimestamp = evaluation.lastKnownTimestamp
        }

        assertFalse(evaluation.state.clockTampered)
        assertTrue(evaluation.state.isActive)
        assertEquals(2 * day, evaluation.state.elapsedDurationMillis)
        assertEquals(12, evaluation.state.daysRemaining)
    }

    @Test
    fun `trial expires after 14 monotonic days despite repeated tolerated rollbacks`() {
        var storedTiming = anchors()
        var lastKnownTimestamp = baseTime
        var evaluation = evaluate(now = snapshot(), storedTiming = storedTiming)

        repeat(14 * 24 * 4) { interval ->
            evaluation =
                evaluate(
                    now =
                        snapshot(
                            wallClockMillis = baseTime - 59 * 60_000L,
                            elapsedRealtimeMillis = baseElapsed + (interval + 1L) * quarterHour,
                        ),
                    storedTiming = storedTiming,
                    lastKnownTimestamp = lastKnownTimestamp,
                )
            storedTiming = checkNotNull(evaluation.anchors)
            lastKnownTimestamp = evaluation.lastKnownTimestamp
        }

        assertFalse(evaluation.state.clockTampered)
        assertFalse(evaluation.state.isActive)
        assertEquals(14 * day, evaluation.state.elapsedDurationMillis)
        assertEquals(0, evaluation.state.daysRemaining)
    }

    @Test
    fun `reboot includes credible forward wall clock time without resetting age`() {
        val evaluation =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 3 * day, elapsedRealtimeMillis = hour, bootCount = 8L),
                storedTiming = anchors(elapsedDurationMillis = 2 * day),
            )

        assertFalse(evaluation.state.clockTampered)
        assertEquals(5 * day + hour, evaluation.state.elapsedDurationMillis)
        assertEquals(9, evaluation.state.daysRemaining)
    }

    @Test
    fun `repeated tolerated rollback before reboot cannot extend trial past fourteen days`() {
        val minute = TimeUnit.MINUTES.toMillis(1L)
        val cycleDuration = 10 * minute
        var actualElapsedDuration = 0L
        var wallClockMillis = baseTime
        var storedTiming = anchors()
        var lastKnownTimestamp = baseTime
        var clockTampered = false
        var evaluation = evaluate(now = snapshot(), storedTiming = storedTiming)

        repeat(14 * 24 * 6 + 1) { reboot ->
            // Jokaisessa bootissa kuluu kymmenen minuuttia, sitten kelloa siirretään yhdeksän minuuttia taakse.
            actualElapsedDuration += cycleDuration
            wallClockMillis += cycleDuration
            wallClockMillis -= 9 * minute
            evaluation =
                evaluate(
                    now =
                        snapshot(
                            wallClockMillis = wallClockMillis,
                            elapsedRealtimeMillis = minute,
                            bootCount = 8L + reboot,
                        ),
                    storedTiming = storedTiming,
                    lastKnownTimestamp = lastKnownTimestamp,
                    clockTamperedAlready = clockTampered,
                )
            storedTiming = checkNotNull(evaluation.anchors)
            lastKnownTimestamp = evaluation.lastKnownTimestamp
            clockTampered = evaluation.state.clockTampered
            assertTrue(evaluation.state.elapsedDurationMillis >= actualElapsedDuration)
        }

        assertTrue(actualElapsedDuration > 14 * day)
        assertFalse(evaluation.state.isActive)
    }

    @Test
    fun `reboot with wall clock rollback fails closed`() {
        val state =
            evaluate(
                now =
                    snapshot(
                        wallClockMillis = baseTime - TimeUnit.MINUTES.toMillis(30L),
                        elapsedRealtimeMillis = hour,
                        bootCount = 8L,
                    ),
                storedTiming = anchors(elapsedDurationMillis = 2 * day),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(2 * day, state.elapsedDurationMillis)
    }

    @Test
    fun `unavailable stored boot identity fails closed despite increasing elapsed time`() {
        val state =
            evaluate(
                now =
                    snapshot(
                        wallClockMillis = baseTime - TimeUnit.MINUTES.toMillis(30L),
                        elapsedRealtimeMillis = baseElapsed + 2 * day,
                        bootCount = null,
                    ),
                storedTiming = anchors(anchorBootCount = null),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(0L, state.elapsedDurationMillis)
    }

    @Test
    fun `unavailable current boot identity fails closed despite forward wall clock`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 2 * day, elapsedRealtimeMillis = hour, bootCount = null),
                storedTiming = anchors(anchorElapsedRealtimeMillis = 3 * day, anchorBootCount = 7L),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(0L, state.elapsedDurationMillis)
    }

    @Test
    fun `multiple missed reboots each consume rollback allowance`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + hour, elapsedRealtimeMillis = hour, bootCount = 10L),
                storedTiming = anchors(),
            ).state

        assertFalse(state.clockTampered)
        assertEquals(4 * hour, state.elapsedDurationMillis)
    }

    @Test
    fun `reboot fails closed when current uptime exceeds compensated wall time`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + hour, elapsedRealtimeMillis = 3 * hour, bootCount = 8L),
                storedTiming = anchors(),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `unrepresentable reboot allowance fails closed`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + hour, bootCount = Long.MAX_VALUE),
                storedTiming = anchors(),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `expired anchored trial remains expired after reboot`() {
        val state =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + day, bootCount = 8L),
                storedTiming = anchors(elapsedDurationMillis = 14 * day),
            ).state

        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
    }

    @Test
    fun `elapsed realtime rollback on same boot fails closed`() {
        val state =
            evaluate(
                now = snapshot(elapsedRealtimeMillis = baseElapsed - 1L),
                storedTiming = anchors(),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `decreasing boot count fails closed`() {
        val state =
            evaluate(
                now = snapshot(bootCount = 6L),
                storedTiming = anchors(anchorBootCount = 7L),
            ).state

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `malformed partial negative inconsistent and overflowing anchors fail closed`() {
        val malformed = evaluate(now = snapshot(), storedTiming = StoredTrialTiming.Malformed).state
        val negative = evaluate(now = snapshot(), storedTiming = anchors(elapsedDurationMillis = -1L)).state
        val inconsistent =
            evaluate(
                now = snapshot(),
                storedTiming = anchors(anchorWallClockMillis = baseTime + hour),
            ).state
        val overflowing =
            evaluate(
                now = snapshot(elapsedRealtimeMillis = baseElapsed + 1L),
                storedTiming = anchors(elapsedDurationMillis = Long.MAX_VALUE),
            ).state

        listOf(malformed, negative, inconsistent, overflowing).forEach { state ->
            assertTrue(state.clockTampered)
            assertFalse(state.isActive)
        }
    }

    @Test
    fun `legacy timestamp only trial retains age but fails closed without boot continuity`() {
        val evaluation =
            TrialManager.evaluateTrialTiming(
                now = snapshot(wallClockMillis = baseTime + 3 * day, elapsedRealtimeMillis = baseElapsed + hour),
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 2 * day,
            )

        assertTrue(evaluation.state.clockTampered)
        assertFalse(evaluation.state.isActive)
        assertEquals(3 * day, evaluation.state.elapsedDurationMillis)
        assertEquals(11, evaluation.state.daysRemaining)
        assertNotNull(evaluation.anchors)
    }

    @Test
    fun `already expired legacy trial stays expired during migration`() {
        val evaluation =
            TrialManager.evaluateTrialTiming(
                now = snapshot(wallClockMillis = baseTime + 20 * day, elapsedRealtimeMillis = baseElapsed + hour),
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 19 * day,
            )

        assertFalse(evaluation.state.isActive)
        assertTrue(evaluation.state.clockTampered)
        assertEquals(0, evaluation.state.daysRemaining)
        assertEquals(20 * day, evaluation.state.elapsedDurationMillis)
    }

    @Test
    fun `large rollback keeps permanent tamper behavior after clock catches up`() {
        val first =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 3 * day, elapsedRealtimeMillis = baseElapsed + 3 * day),
                storedTiming = anchors(),
                lastKnownTimestamp = baseTime + 3 * day + 2 * hour,
            )
        val caughtUp =
            evaluate(
                now = snapshot(wallClockMillis = baseTime + 4 * day, elapsedRealtimeMillis = baseElapsed + 4 * day),
                storedTiming = checkNotNull(first.anchors),
                lastKnownTimestamp = first.lastKnownTimestamp,
                clockTamperedAlready = first.state.clockTampered,
            ).state

        assertTrue(first.state.clockTampered)
        assertFalse(first.state.isActive)
        assertTrue(caughtUp.clockTampered)
        assertFalse(caughtUp.isActive)
    }

    @Test
    fun `malformed negative trial start cannot become a fresh trial`() {
        val state =
            TrialManager
                .evaluateTrialTiming(
                    now = snapshot(),
                    startTimestamp = -1L,
                    lastKnownTimestamp = baseTime,
                ).state

        assertTrue(state.hasStarted)
        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `persisted tamper flag without a start cannot become a fresh trial`() {
        val state =
            TrialManager
                .evaluateTrialTiming(
                    now = snapshot(),
                    startTimestamp = 0L,
                    lastKnownTimestamp = baseTime,
                    clockTamperedAlready = true,
                ).state

        assertTrue(state.hasStarted)
        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `trial refresh waits only until next day boundary when sooner than poll interval`() {
        val delayMillis =
            TrialManager.calculateTrialRefreshDelayMillis(
                elapsedDurationMillis = 13 * day + 23 * hour + 59 * 60_000L,
            )

        assertEquals(60_000L, delayMillis)
    }

    @Test
    fun `trial refresh is capped by regular poll interval before next day boundary`() {
        val delayMillis =
            TrialManager.calculateTrialRefreshDelayMillis(
                elapsedDurationMillis = 3 * day,
            )

        assertEquals(quarterHour, delayMillis)
    }

    @Test
    fun `expired or overflowing duration keeps normal refresh interval`() {
        assertEquals(quarterHour, TrialManager.calculateTrialRefreshDelayMillis(14 * day))
        assertEquals(quarterHour, TrialManager.calculateTrialRefreshDelayMillis(Long.MAX_VALUE))
    }

    private fun evaluate(
        now: SessionTimeSnapshot,
        storedTiming: StoredTrialTiming,
        lastKnownTimestamp: Long = baseTime,
        clockTamperedAlready: Boolean = false,
    ): TrialTimingEvaluation =
        TrialManager.evaluateTrialTiming(
            now = now,
            startTimestamp = baseTime,
            lastKnownTimestamp = lastKnownTimestamp,
            clockTamperedAlready = clockTamperedAlready,
            storedTiming = storedTiming,
        )

    private fun snapshot(
        wallClockMillis: Long = baseTime,
        elapsedRealtimeMillis: Long = baseElapsed,
        bootCount: Long? = 7L,
    ) = SessionTimeSnapshot(
        wallClockMillis = wallClockMillis,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
        bootCount = bootCount,
        zoneId = "Europe/Helsinki",
    )

    private fun anchors(
        elapsedDurationMillis: Long = 0L,
        anchorWallClockMillis: Long = baseTime,
        anchorElapsedRealtimeMillis: Long = baseElapsed,
        anchorBootCount: Long? = 7L,
    ) = StoredTrialTiming.Anchored(
        elapsedDurationMillis = elapsedDurationMillis,
        anchorWallClockMillis = anchorWallClockMillis,
        anchorElapsedRealtimeMillis = anchorElapsedRealtimeMillis,
        anchorBootCount = anchorBootCount,
    )
}
