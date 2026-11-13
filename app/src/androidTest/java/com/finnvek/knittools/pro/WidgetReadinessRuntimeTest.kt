package com.finnvek.knittools.pro

import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.billing.BillingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Run each method in a fresh instrumentation process; billing inputs remain synthetic. */
class WidgetReadinessRuntimeTest {
    @org.junit.After fun resetControl() {
        if (ProPolicyTestControl.isActive) ProPolicyTestControl.reset()
    }

    @Test fun delayedPurchaseIsNotRejectedFromDefaultState() = verifyDelayed(purchased = true, trial = false)

    @Test fun delayedTrialIsNotRejectedFromDefaultState() = verifyDelayed(purchased = false, trial = true)

    @Test fun loadedFreeStateDoesNotGrantWidget() = verifyDelayed(purchased = false, trial = false)

    private fun verifyDelayed(
        purchased: Boolean,
        trial: Boolean,
    ) = runBlocking(Dispatchers.IO) {
        org.junit.Assume.assumeTrue(ProPolicyTestControl.isActive)
        val manager = controlledManager()
        ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
        assertFalse(manager.initialStateReady.value)
        val start = SystemClock.elapsedRealtime()
        val load =
            launch {
                delay(300)
                ProPolicyTestControl.setTrialState(
                    TrialState(hasStarted = true, isActive = trial, daysRemaining = if (trial) 7 else 0),
                )
                ProPolicyTestControl.setPurchased(purchased)
            }
        val allowed = manager.hasFeatureAfterInitialLoad(ProFeature.WIDGET)
        val elapsed = SystemClock.elapsedRealtime() - start
        load.join()
        assertTrue(elapsed >= 250)
        assertTrue(allowed == (purchased || trial))
        assertTrue(manager.initialStateReady.value)
    }

    @Test fun readinessTimeoutDoesNotInventPurchasedAccess() =
        runBlocking(Dispatchers.IO) {
            org.junit.Assume.assumeTrue(ProPolicyTestControl.isActive)
            val manager = controlledManager()
            ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
            val start = SystemClock.elapsedRealtime()
            assertFalse(manager.hasFeatureAfterInitialLoad(ProFeature.WIDGET))
            val elapsed = SystemClock.elapsedRealtime() - start
            assertTrue(elapsed >= 3_800 && elapsed < 10_000)
            assertFalse(manager.initialStateReady.value)
            assertFalse(manager.proState.value.status == ProStatus.PRO_PURCHASED)
        }

    private fun controlledManager(): ProManager {
        ProPolicyTestControl.reset()
        val app = ApplicationProvider.getApplicationContext<App>()
        // Do not initialize a BillingClient: its real response would race the synthetic delay.
        return ProManager(TrialManager(app, Dispatchers.IO), BillingManager(app)).also { it.initialize() }
    }
}
