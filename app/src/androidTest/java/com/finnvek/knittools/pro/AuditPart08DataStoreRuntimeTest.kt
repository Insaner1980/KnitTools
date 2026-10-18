package com.finnvek.knittools.pro

import android.system.Os
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.time.AndroidSessionTimeSource
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Run each method in a fresh, cleared isolated package. Persistence is real Android DataStore. */
class AuditPart08DataStoreRuntimeTest {
    private fun app(): App =
        ApplicationProvider.getApplicationContext<App>().also {
            check(it.packageName == "com.finnvek.knittools.activepolicytest")
            ProPolicyTestControl.setPurchased(false)
        }

    @Test
    fun overlappingStartAndRefreshCannotPersistFalseClockTamper() =
        runBlocking {
            val app = app()
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .initialStateReady
                    .first { it }
            }
            val base = AndroidSessionTimeSource(app).snapshot()
            val firstEntered = CountDownLatch(1)
            val releaseFirst = CountDownLatch(1)
            val secondEntered = CountDownLatch(1)
            val samples = AtomicInteger()
            val clock =
                object : SessionTimeSource {
                    override fun snapshot(): SessionTimeSnapshot {
                        val index = samples.incrementAndGet()
                        val sample =
                            base.copy(
                                wallClockMillis = base.wallClockMillis + index,
                                elapsedRealtimeMillis = base.elapsedRealtimeMillis + index,
                            )
                        if (index == 1) {
                            firstEntered.countDown()
                            check(releaseFirst.await(10, TimeUnit.SECONDS))
                        } else {
                            secondEntered.countDown()
                        }
                        return sample
                    }
                }
            val manager = TrialManager(app, Dispatchers.IO, clock)
            val first = async(Dispatchers.IO) { manager.startTrial() }
            try {
                assertTrue(firstEntered.await(10, TimeUnit.SECONDS))
                val competingStarted = CompletableDeferred<Unit>()
                val second =
                    async(Dispatchers.IO) {
                        competingStarted.complete(Unit)
                        manager.startTrial()
                    }
                competingStarted.await()
                val refresh = async(Dispatchers.IO) { manager.initialize() }
                // A clock read inside the first transform prevents a later transform sampling ahead of it.
                assertFalse(secondEntered.await(500, TimeUnit.MILLISECONDS))
                releaseFirst.countDown()
                val results = withTimeout(10_000) { awaitAll(first, second) }
                withTimeout(10_000) { refresh.await() }
                assertEquals(1, results.count { it == TrialStartResult.Started })
                assertEquals(1, results.count { it == TrialStartResult.AlreadyActive })
                assertTrue(manager.trialState.value.isActive)
                assertFalse(manager.trialState.value.clockTampered)
                val reopened = TrialManager(app, Dispatchers.IO, clock)
                assertEquals(TrialStartResult.AlreadyActive, reopened.startTrial())
                assertEquals(manager.trialState.value.startTimestamp, reopened.trialState.value.startTimestamp)
                assertFalse(reopened.trialState.value.clockTampered)
            } finally {
                releaseFirst.countDown()
                first.cancel()
            }
        }

    @Test
    fun failedTrialWriteDoesNotAdmitStartAndRetryPersistsOnce() =
        runBlocking {
            val app = app()
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .initialStateReady
                    .first { it }
            }
            val manager = TrialManager(app, Dispatchers.IO, AndroidSessionTimeSource(app))
            val directory = File(app.filesDir, "datastore")
            check(directory.isDirectory)
            val mode = Os.stat(directory.path).st_mode
            try {
                Os.chmod(directory.path, 320) // Owner read/execute only: a real temp-file write must fail.
                assertEquals(TrialStartResult.Failed, manager.startTrial())
                assertFalse(manager.trialState.value.hasStarted)
            } finally {
                Os.chmod(directory.path, mode)
            }
            assertEquals(TrialStartResult.Started, manager.startTrial())
            val start = manager.trialState.value.startTimestamp
            assertEquals(TrialStartResult.AlreadyActive, manager.startTrial())
            assertEquals(start, manager.trialState.value.startTimestamp)
            assertFalse(manager.trialState.value.clockTampered)
        }

    @Test
    fun analyticsConsentPersistsAndFailedWriteDoesNotPublishSuccess() =
        runBlocking {
            val app = app()
            val preferences = app.preferencesManager.get()
            assertTrue(preferences.setUsageAnalyticsEnabled(false))
            assertFalse(preferences.preferences.first().usageAnalyticsEnabled)
            val directory = File(app.filesDir, "datastore")
            val mode = Os.stat(directory.path).st_mode
            try {
                Os.chmod(directory.path, 320)
                assertFalse(preferences.setUsageAnalyticsEnabled(true))
                assertFalse(preferences.preferences.first().usageAnalyticsEnabled)
            } finally {
                Os.chmod(directory.path, mode)
            }
            repeat(2) { assertTrue(preferences.setUsageAnalyticsEnabled(true)) }
            assertTrue(PreferencesManager(app).preferences.first().usageAnalyticsEnabled)
            repeat(2) { assertTrue(preferences.setUsageAnalyticsEnabled(false)) }
            assertFalse(PreferencesManager(app).preferences.first().usageAnalyticsEnabled)
        }
}
