package com.finnvek.knittools.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProStateTest {
    @Test
    fun `trial active is pro`() {
        val state = ProState(status = ProStatus.TRIAL_ACTIVE, trialDaysRemaining = 5)
        assertTrue(state.isPro)
    }

    @Test
    fun `pro purchased is pro`() {
        val state = ProState(status = ProStatus.PRO_PURCHASED)
        assertTrue(state.isPro)
    }

    @Test
    fun `trial expired is not pro`() {
        val state = ProState(status = ProStatus.TRIAL_EXPIRED)
        assertFalse(state.isPro)
    }

    @Test
    fun `default state is not pro`() {
        val state = ProState()
        assertFalse(state.isPro)
    }

    @Test
    fun `hasFeature returns true when pro purchased`() {
        val state = ProState(status = ProStatus.PRO_PURCHASED)
        ProFeature.entries.forEach { feature ->
            assertTrue("$feature pitäisi olla käytössä Pro:lla", state.hasFeature(feature))
        }
    }

    @Test
    fun `hasFeature returns true when trial active`() {
        val state = ProState(status = ProStatus.TRIAL_ACTIVE, trialDaysRemaining = 3)
        ProFeature.entries.forEach { feature ->
            assertTrue("$feature pitäisi olla käytössä trialissa", state.hasFeature(feature))
        }
    }

    @Test
    fun `hasFeature returns false when trial expired`() {
        val state = ProState(status = ProStatus.TRIAL_EXPIRED)
        ProFeature.entries.forEach { feature ->
            assertFalse("$feature ei pitäisi olla käytössä expiredillä", state.hasFeature(feature))
        }
    }
}
