package com.finnvek.knittools.ui.components

import com.finnvek.knittools.pro.ProStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProBadgeTest {
    @Test
    fun `badge distinguishes locked trial and purchased states`() {
        assertEquals(ProBadgeState.Locked, proBadgeState(ProStatus.TRIAL_NOT_STARTED))
        assertEquals(ProBadgeState.Trial, proBadgeState(ProStatus.TRIAL_ACTIVE))
        assertEquals(ProBadgeState.Locked, proBadgeState(ProStatus.TRIAL_EXPIRED))
        assertEquals(ProBadgeState.Hidden, proBadgeState(ProStatus.PRO_PURCHASED))
    }
}
