package com.finnvek.knittools.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrialManagerTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val baseTime = 1_700_000_000_000L

    @Test
    fun `first launch keeps trial not started`() {
        val state = TrialManager.calculateTrialState(baseTime, 0L, 0L)

        assertFalse(state.isActive)
        assertFalse(state.hasStarted)
        assertEquals(0, state.daysRemaining)
        assertFalse(state.clockTampered)
    }

    @Test
    fun `confirmed trial begins with 14 days remaining`() {
        val state = TrialManager.calculateTrialState(baseTime, baseTime, baseTime)

        assertTrue(state.isActive)
        assertTrue(state.hasStarted)
        assertEquals(14, state.daysRemaining)
    }

    @Test
    fun `day 3 shows 11 days remaining`() {
        val state = TrialManager.calculateTrialState(baseTime + 3 * day, baseTime, baseTime + 2 * day)

        assertTrue(state.isActive)
        assertEquals(11, state.daysRemaining)
    }

    @Test
    fun `day 13 shows 1 day remaining`() {
        val state = TrialManager.calculateTrialState(baseTime + 13 * day, baseTime, baseTime + 12 * day)

        assertTrue(state.isActive)
        assertEquals(1, state.daysRemaining)
    }

    @Test
    fun `fractional final day is shown as one day remaining`() {
        val state =
            TrialManager.calculateTrialState(
                baseTime + 13 * day + 23 * hour,
                baseTime,
                baseTime + 13 * day,
            )

        assertTrue(state.isActive)
        assertEquals(1, state.daysRemaining)
    }

    @Test
    fun `day 14 expires trial`() {
        val state = TrialManager.calculateTrialState(baseTime + 14 * day, baseTime, baseTime + 13 * day)

        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
    }

    @Test
    fun `day 30 still shows 0 not negative`() {
        val state = TrialManager.calculateTrialState(baseTime + 30 * day, baseTime, baseTime + 29 * day)

        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
    }

    @Test
    fun `resume after expiry stays expired`() {
        val state = TrialManager.calculateTrialState(baseTime + 20 * day, baseTime, baseTime + 19 * day)

        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
        assertTrue(state.hasStarted)
    }

    @Test
    fun `clock set back more than 1h triggers tampering`() {
        val state =
            TrialManager.calculateTrialState(
                baseTime + 3 * day,
                baseTime,
                baseTime + 3 * day + 2 * hour,
            )

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(11, state.daysRemaining)
    }

    @Test
    fun `clock drift within 1h tolerance is fine`() {
        val state =
            TrialManager.calculateTrialState(
                baseTime + 3 * day,
                baseTime,
                baseTime + 3 * day + 30 * 60_000L,
            )

        assertFalse(state.clockTampered)
        assertTrue(state.isActive)
    }

    @Test
    fun `clock set forward then back kills trial`() {
        val state = TrialManager.calculateTrialState(baseTime + 3 * day, baseTime, baseTime + 10 * day)

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }

    @Test
    fun `persisted clock tamper keeps trial blocked after clock catches up`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 11 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 10 * day,
                clockTamperedAlready = true,
            )

        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(3, state.daysRemaining)
    }

    @Test
    fun `last known timestamp does not move backwards`() {
        val futureLastKnown = baseTime + 10 * day

        val nextLastKnown =
            TrialManager.calculateNextLastKnownTimestamp(
                now = baseTime + 3 * day,
                lastKnownTimestamp = futureLastKnown,
            )

        assertEquals(futureLastKnown, nextLastKnown)
    }

    @Test
    fun `trial refresh waits only until next day boundary when it is sooner than poll interval`() {
        val delayMillis =
            TrialManager.calculateTrialRefreshDelayMillis(
                now = baseTime + 13 * day + 23 * hour + 59 * 60_000L,
                startTimestamp = baseTime,
            )

        assertEquals(60_000L, delayMillis)
    }

    @Test
    fun `trial refresh is capped by regular poll interval before next day boundary`() {
        val delayMillis =
            TrialManager.calculateTrialRefreshDelayMillis(
                now = baseTime + 3 * day,
                startTimestamp = baseTime,
            )

        assertEquals(TimeUnit.MINUTES.toMillis(15), delayMillis)
    }
}
