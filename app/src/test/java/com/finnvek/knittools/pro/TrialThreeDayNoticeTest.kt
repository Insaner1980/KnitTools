package com.finnvek.knittools.pro

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TrialThreeDayNoticeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    private val jobs = mutableListOf<Job>()

    @After
    fun closeStores() = runBlocking { jobs.forEach { it.cancelAndJoin() } }

    @Test
    fun committedClaimCannotPublishAfterANewerTrialRefresh() =
        runBlocking {
            val delegate = store()
            val committed = CompletableDeferred<Unit>()
            val resume = CompletableDeferred<Unit>()
            var pauseNext = false
            val pausedStore =
                object : DataStore<Preferences> {
                    override val data = delegate.data

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                        val result = delegate.updateData(transform)
                        if (pauseNext) {
                            pauseNext = false
                            committed.complete(Unit)
                            resume.await()
                        }
                        return result
                    }
                }
            val time = TrialClock()
            val manager = TrialManager(pausedStore, Dispatchers.Unconfined, time)
            assertEquals(TrialStartResult.Started, manager.startTrial())
            time.advanceDays(11L)
            pauseNext = true
            val claim = async { manager.claimTrialThreeDayNotice() }
            committed.await()
            time.advanceDays(3L)
            val refresh = async(start = CoroutineStart.UNDISPATCHED) { manager.startTrial() }
            assertFalse(refresh.isCompleted)
            resume.complete(Unit)
            assertTrue(claim.await())
            assertEquals(TrialStartResult.AlreadyExpired, refresh.await())
            assertFalse(manager.trialState.value.isActive)
            assertEquals(0, manager.trialState.value.daysRemaining)
        }

    @Test
    fun `concurrent claims have one winner and reopening cannot claim again`() =
        runBlocking {
            val file = File(temporaryFolder.root, "concurrent.preferences_pb")
            val time = TrialClock()
            val manager = TrialManager(store(file), Dispatchers.IO, time)
            assertEquals(TrialStartResult.Started, manager.startTrial())
            time.advanceDays(11L)

            val results = List(4) { async(Dispatchers.IO) { manager.claimTrialThreeDayNotice() } }.awaitAll()

            assertEquals(1, results.count { it })
            assertEquals(3, manager.trialState.value.daysRemaining)
            jobs.last().cancelAndJoin()
            assertFalse(TrialManager(store(file), Dispatchers.IO, time).claimTrialThreeDayNotice())
        }

    @Test
    fun `only exactly three remaining days can claim the fixed three day copy`() =
        runBlocking {
            for (daysRemaining in listOf(14, 4, 2, 1, 0)) {
                val time = TrialClock()
                val manager = TrialManager(store(), Dispatchers.IO, time)
                assertEquals(TrialStartResult.Started, manager.startTrial())
                time.advanceDays(14L - daysRemaining)

                assertFalse("daysRemaining=$daysRemaining", manager.claimTrialThreeDayNotice())
            }
        }

    @Test
    fun `unstarted trial is neither claimed nor started by the notice check`() =
        runBlocking {
            val manager = TrialManager(store(), Dispatchers.IO, TrialClock())

            assertFalse(manager.claimTrialThreeDayNotice())
            assertFalse(manager.trialState.value.hasStarted)
            assertEquals(TrialStartResult.Started, manager.startTrial())
        }

    @Test
    fun `tampered trial cannot claim a notice even at the elapsed three day boundary`() =
        runBlocking {
            val time = TrialClock()
            val manager = TrialManager(store(), Dispatchers.IO, time)
            manager.startTrial()
            time.advanceDays(11L)
            time.wallMillis = START - TimeUnit.HOURS.toMillis(2L)

            assertFalse(manager.claimTrialThreeDayNotice())
            assertTrue(manager.trialState.value.clockTampered)
            assertFalse(manager.trialState.value.isActive)
        }

    @Test
    fun `failure after the claim transform cannot consume the notice and retry can claim`() =
        runBlocking {
            val store = FailingWriteStore(store())
            val time = TrialClock()
            val manager = TrialManager(store, Dispatchers.IO, time)
            manager.startTrial()
            time.advanceDays(11L)
            store.failure = IOException("write failed")

            assertFalse(manager.claimTrialThreeDayNotice())

            store.failure = null
            assertTrue(manager.claimTrialThreeDayNotice())
            assertFalse(manager.claimTrialThreeDayNotice())
        }

    @Test
    fun `cancelled claim propagates cancellation and does not consume the notice`() =
        runBlocking {
            val store = FailingWriteStore(store())
            val time = TrialClock()
            val manager = TrialManager(store, Dispatchers.IO, time)
            manager.startTrial()
            time.advanceDays(11L)
            store.failure = CancellationException("cancelled")

            assertTrue(runCatching { manager.claimTrialThreeDayNotice() }.exceptionOrNull() is CancellationException)

            store.failure = null
            assertTrue(manager.claimTrialThreeDayNotice())
        }

    private fun store(file: File = File(temporaryFolder.newFolder(), "trial.preferences_pb")): DataStore<Preferences> {
        val job = SupervisorJob().also(jobs::add)
        return PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { file.path.toPath() },
                ),
            scope = CoroutineScope(Dispatchers.IO + job),
        )
    }

    private class FailingWriteStore(
        private val delegate: DataStore<Preferences>,
    ) : DataStore<Preferences> {
        var failure: Exception? = null
        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            delegate.updateData {
                val updated = transform(it)
                failure?.let { error -> throw error }
                updated
            }
    }

    private class TrialClock : SessionTimeSource {
        var wallMillis = START
        private var elapsedMillis = 10_000L

        fun advanceDays(days: Long) {
            val millis = TimeUnit.DAYS.toMillis(days)
            wallMillis += millis
            elapsedMillis += millis
        }

        override fun snapshot(): SessionTimeSnapshot =
            SessionTimeSnapshot(
                wallClockMillis = wallMillis,
                elapsedRealtimeMillis = elapsedMillis,
                bootCount = 4L,
                zoneId = "Europe/Helsinki",
            )
    }

    private companion object {
        const val START = 1_700_000_000_000L
    }
}
