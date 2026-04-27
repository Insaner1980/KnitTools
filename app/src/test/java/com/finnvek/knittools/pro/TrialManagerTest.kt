package com.finnvek.knittools.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrialManagerTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val baseTime = 1_700_000_000_000L // kiinteä alkuhetki

    @Test
    fun `first launch starts 14 day trial`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime,
                startTimestamp = baseTime,
                lastKnownTimestamp = 0L,
                isFirstLaunch = true,
            )
        assertTrue(state.isActive)
        assertTrue(state.isFirstLaunch)
        assertEquals(14, state.daysRemaining)
        assertFalse(state.clockTampered)
    }

    @Test
    fun `day 3 shows 11 days remaining`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 3 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 2 * day,
                isFirstLaunch = false,
            )
        assertTrue(state.isActive)
        assertEquals(11, state.daysRemaining)
    }

    @Test
    fun `day 13 shows 1 day remaining`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 13 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 12 * day,
                isFirstLaunch = false,
            )
        assertTrue(state.isActive)
        assertEquals(1, state.daysRemaining)
    }

    @Test
    fun `day 14 expires trial`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 14 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 13 * day,
                isFirstLaunch = false,
            )
        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
    }

    @Test
    fun `day 30 still shows 0 not negative`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 30 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 29 * day,
                isFirstLaunch = false,
            )
        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
    }

    @Test
    fun `resume after expiry stays expired`() {
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 20 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 19 * day,
                isFirstLaunch = false,
            )
        assertFalse(state.isActive)
        assertEquals(0, state.daysRemaining)
        assertFalse(state.isFirstLaunch)
    }

    @Test
    fun `clock set back more than 1h triggers tampering`() {
        // Viimeisin tunnettu aika on 2h edellä nykyhetkeä
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 3 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 3 * day + 2 * hour,
                isFirstLaunch = false,
            )
        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
        assertEquals(11, state.daysRemaining) // päiviä jäljellä mutta trial kuitenkin estetty
    }

    @Test
    fun `clock drift within 1h tolerance is fine`() {
        // 30 min takaisinkelaus — normaali kellodrifti
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 3 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 3 * day + 30 * 60_000L,
                isFirstLaunch = false,
            )
        assertFalse(state.clockTampered)
        assertTrue(state.isActive)
    }

    @Test
    fun `clock set forward then back kills trial`() {
        // Käyttäjä kelaa kellon eteenpäin (lastKnown = +10d), sitten palauttaa
        val state =
            TrialManager.calculateTrialState(
                now = baseTime + 3 * day,
                startTimestamp = baseTime,
                lastKnownTimestamp = baseTime + 10 * day,
                isFirstLaunch = false,
            )
        assertTrue(state.clockTampered)
        assertFalse(state.isActive)
    }
}
