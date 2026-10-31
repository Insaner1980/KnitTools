package com.finnvek.knittools.pro

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.longPreferencesKey
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TrialManagerDispatcherTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `main callers sample on injected IO inside real datastore transforms`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val ioScheduler = TestCoroutineScheduler()
                val clock = TrialClock()
                val manager = TrialManager(store(), StandardTestDispatcher(ioScheduler), clock)

                runSnapshotOperation(ioScheduler, clock, 1) { manager.initialize() }
                assertFalse(manager.trialState.value.hasStarted)
                assertEquals(
                    TrialStartResult.Started,
                    runSnapshotOperation(ioScheduler, clock, 2) { manager.startTrial() },
                )
                val startedAt = manager.trialState.value.startTimestamp
                assertEquals(14, manager.trialState.value.daysRemaining)

                val elapsed = TimeUnit.DAYS.toMillis(11)
                clock.now =
                    clock.now.copy(
                        wallClockMillis = clock.now.wallClockMillis + elapsed,
                        elapsedRealtimeMillis = clock.now.elapsedRealtimeMillis + elapsed,
                    )
                assertTrue(runSnapshotOperation(ioScheduler, clock, 1) { manager.claimTrialThreeDayNotice() })
                assertEquals(3, manager.trialState.value.daysRemaining)
                assertEquals(elapsed, manager.trialState.value.elapsedDurationMillis)
                assertEquals(
                    TrialStartResult.AlreadyActive,
                    runSnapshotOperation(ioScheduler, clock, 2) { manager.startTrial() },
                )
                assertEquals(startedAt, manager.trialState.value.startTimestamp)

                clock.now = clock.now.copy(bootCount = null)
                assertFalse(runSnapshotOperation(ioScheduler, clock, 1) { manager.claimTrialThreeDayNotice() })
                assertTrue(manager.trialState.value.clockTampered)
                assertEquals(elapsed, manager.trialState.value.elapsedDurationMillis)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `cancelling while snapshot awaits IO leaves trial unstarted`() =
        runTest {
            val ioScheduler = TestCoroutineScheduler()
            val clock = TrialClock()
            val dataStore = store()
            val manager = TrialManager(dataStore, StandardTestDispatcher(ioScheduler), clock)
            val start = CoroutineScope(coroutineContext.minusKey(TestCoroutineScheduler)).async { manager.startTrial() }
            runCurrent()
            assertEquals(0, clock.reads)

            start.cancel()
            ioScheduler.runCurrent()
            runCurrent()
            start.join()

            assertEquals(0, clock.reads)
            assertNull(dataStore.data.first()[longPreferencesKey("trial_start_timestamp")])
            assertFalse(manager.trialState.value.hasStarted)
        }

    private suspend fun <T> TestScope.runSnapshotOperation(
        ioScheduler: TestCoroutineScheduler,
        clock: TrialClock,
        expectedReads: Int,
        operation: suspend () -> T,
    ): T {
        val readsBefore = clock.reads
        // Drive Main and IO independently without inheriting the test scope's scheduler key.
        val callerScope = CoroutineScope(coroutineContext.minusKey(TestCoroutineScheduler))
        val result = callerScope.async(Dispatchers.Main) { operation() }
        runCurrent()
        assertEquals(readsBefore, clock.reads)
        assertFalse(result.isCompleted)
        repeat(expectedReads) {
            ioScheduler.runCurrent()
            runCurrent()
        }
        assertTrue(result.isCompleted)
        assertEquals(readsBefore + expectedReads, clock.reads)
        return result.await()
    }

    private fun TestScope.store() =
        PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { File(temporaryFolder.root, "trial.preferences_pb").path.toPath() },
                ),
            scope = CoroutineScope(backgroundScope.coroutineContext.minusKey(TestCoroutineScheduler)),
        )

    private class TrialClock : SessionTimeSource {
        var now = SessionTimeSnapshot(1_700_000_000_000L, 10_000L, 4L, "Europe/Helsinki")
        var reads = 0

        override fun snapshot(): SessionTimeSnapshot {
            reads++
            return now
        }
    }
}
