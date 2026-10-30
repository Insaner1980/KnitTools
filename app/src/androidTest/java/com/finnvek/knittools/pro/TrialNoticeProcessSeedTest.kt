package com.finnvek.knittools.pro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.time.AndroidSessionTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Seed only; tools/trial-notice-process-test.py owns the standalone process-restoration assertions. */
class TrialNoticeProcessSeedTest {
    @Test
    fun prepareExpiredTrial() =
        runBlocking<Unit>(Dispatchers.IO) {
            assumeTrue(InstrumentationRegistry.getArguments().getString("trialProcessSeed") == "true")
            val app = ApplicationProvider.getApplicationContext<App>()
            check(app.packageName == "com.finnvek.knittools.activepolicytest" && ProPolicyTestControl.isActive)
            ProPolicyTestControl.setPurchased(false)
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .initialStateReady
                    .first { it }
            }
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            val timeSource = AndroidSessionTimeSource(app)
            val now = timeSource.snapshot()
            val elapsed = TimeUnit.DAYS.toMillis(15)
            val store = trialDataStore(app)
            store.seedTrialClock(now, elapsed)
            val manager = TrialManager(app, Dispatchers.IO, timeSource)
            manager.initialize()
            val state = manager.trialState.value
            assertTrue(state.hasStarted)
            assertFalse(state.isActive)
            assertFalse(state.clockTampered)
            assertEquals(0, state.daysRemaining)
            assertTrue(state.elapsedDurationMillis >= elapsed)
            assertFalse(store.data.first()[booleanPreferencesKey("trial_end_notice_shown")] == true)
            File(app.filesDir, "trial-notice-process-test").writeText("synthetic-unpurchased")
        }

    @Suppress("UNCHECKED_CAST")
    private fun trialDataStore(context: Context): DataStore<Preferences> =
        TrialManager.Companion::class.java
            .getDeclaredMethod("getTrialDataStore", Context::class.java)
            .apply { isAccessible = true }
            .invoke(TrialManager.Companion, context) as DataStore<Preferences>
}
