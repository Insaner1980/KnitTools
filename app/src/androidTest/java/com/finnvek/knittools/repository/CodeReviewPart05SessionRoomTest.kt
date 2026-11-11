package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CodeReviewPart05SessionRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private lateinit var repository: CounterRepository
    private var now = SessionTimeSnapshot(1_000L, 5_000L, 1L, "Europe/Helsinki")
    private val clock =
        object : SessionTimeSource {
            override fun snapshot(): SessionTimeSnapshot = now
        }

    @Before
    fun setup() {
        check(context.packageName == "com.finnvek.knittools.activepolicytest")
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .build()
        fixture = RoomCounterTestFixture(database, context)
        repository = fixture.counterRepository(sessionTimeSource = clock)
        runBlocking {
            database.counterProjectDao().insert(CounterProjectEntity(id = 1L, name = "First", count = 12))
            database.counterProjectDao().insert(CounterProjectEntity(id = 2L, name = "Second", count = 3))
        }
    }

    @After
    fun cleanup() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun simultaneousStartsReturnOneOwnerAndOneConflict() =
        runBlocking {
            val start = CompletableDeferred<Unit>()
            val results =
                coroutineScope {
                    listOf(1L, 2L)
                        .map { projectId ->
                            val caller = fixture.counterRepository(sessionTimeSource = clock)
                            async(Dispatchers.IO) {
                                start.await()
                                caller.startSession(projectId)
                            }
                        }.also { start.complete(Unit) }
                        .awaitAll()
                }
            val accepted = results.filterIsInstance<StartSessionResult.Started>().single().session
            val refused = results.filterIsInstance<StartSessionResult.ProjectConflict>().single()
            val active = requireNotNull(database.sessionDao().getActiveSession())

            assertEquals(accepted.sessionToken, active.sessionToken)
            assertEquals(accepted.projectId, active.projectId)
            assertEquals(accepted.sessionToken, refused.activeSession.sessionToken)
            assertTrue(refused.requestedProjectId != active.projectId)
        }

    @Test
    fun simultaneousStopAndReplayInsertExactlyOneCompletedSession() =
        runBlocking {
            val active = (repository.startSession(1L) as StartSessionResult.Started).session
            now = now.copy(wallClockMillis = 66_000L, elapsedRealtimeMillis = 70_000L)
            val start = CompletableDeferred<Unit>()
            val results =
                coroutineScope {
                    List(2) {
                        val caller = fixture.counterRepository(sessionTimeSource = clock)
                        async(Dispatchers.IO) {
                            start.await()
                            caller.stopSession(active.sessionToken, 65L, 0, 12)
                        }
                    }.also { start.complete(Unit) }.awaitAll()
                }

            assertEquals(1, results.filterIsInstance<StopSessionResult.Saved>().size)
            assertEquals(1, results.count { it == StopSessionResult.NoActiveSession })
            assertEquals(StopSessionResult.NoActiveSession, repository.stopSession(active.sessionToken, 65L, 0, 12))
            assertNull(database.sessionDao().getActiveSession())
            val saved =
                database
                    .sessionDao()
                    .getAllSessions(null)
                    .first()
                    .single()
            assertEquals(65L, saved.durationSeconds)
            assertEquals(12, saved.startRow)
            assertEquals(12, saved.endRow)
            assertEquals(0, saved.rowsWorked)
        }

    @Test
    fun recoveryEditAndDiscardCannotBothFinalizeTheSameToken() =
        runBlocking {
            val active = (repository.startSession(1L) as StartSessionResult.Started).session
            now = now.copy(wallClockMillis = 31_000L, elapsedRealtimeMillis = 35_000L)
            repository.checkpointActiveSession()
            now = now.copy(wallClockMillis = 66_000L, elapsedRealtimeMillis = 500L, bootCount = 2L)
            val recovery = requireNotNull(repository.refreshActiveSession())
            val interval = requireNotNull(recovery.recoveryIntervalToken)
            val start = CompletableDeferred<Unit>()
            val results =
                coroutineScope {
                    val edit =
                        async(Dispatchers.IO) {
                            start.await()
                            repository.editRecoveryDurationAndStop(active.sessionToken, interval, 75L)
                        }
                    val discard =
                        async(Dispatchers.IO) {
                            start.await()
                            repository.discardRecoveryInterval(active.sessionToken, interval)
                        }
                    start.complete(Unit)
                    listOf(edit.await(), discard.await())
                }

            assertEquals(1, results.count { it == RecoveryResolutionResult.StaleAction })
            val saved =
                database
                    .sessionDao()
                    .getAllSessions(null)
                    .first()
                    .single()
            val edited = results.any { it is RecoveryResolutionResult.EditedAndStopped }
            assertEquals(if (edited) 75L else 30L, saved.durationSeconds)
            assertEquals(1_000L + saved.durationSeconds * 1_000L, saved.endedAt)
            assertNull(database.sessionDao().getActiveSession())
        }

    @Test
    fun countCommittedBeforeStopRejectsOldSummaryAndPreservesTheCheckpoint() =
        runBlocking {
            withTimeout(15_000L) {
                val active = (repository.startSession(1L) as StartSessionResult.Started).session
                now = now.copy(wallClockMillis = 66_000L, elapsedRealtimeMillis = 70_000L)
                val written = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                coroutineScope {
                    val count =
                        async(Dispatchers.IO) {
                            database.withTransaction {
                                assertTrue(repository.applyMainCounterChange(1L, MainCounterChange.Increment))
                                written.complete(Unit)
                                release.await()
                            }
                        }
                    try {
                        written.await()
                        val stop =
                            async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                                repository.stopSession(active.sessionToken, 65L, 0, 12)
                            }
                        release.complete(Unit)
                        count.await()
                        assertEquals(StopSessionResult.StaleAction, stop.await())
                    } finally {
                        release.complete(Unit)
                    }
                }

                val checkpoint = requireNotNull(database.sessionDao().getActiveSession())
                assertEquals(active.sessionToken, checkpoint.sessionToken)
                assertEquals(65L, checkpoint.checkpointedDurationSeconds)
                assertEquals(1, checkpoint.trustedRowsWorked)
                assertEquals(13, checkpoint.trustedLastObservedRow)
                assertEquals(0L, database.sessionDao().countCompletedSessions())
                assertTrue(repository.stopSession(active.sessionToken, 65L, 1, 13) is StopSessionResult.Saved)
                val saved =
                    database
                        .sessionDao()
                        .getAllSessions(null)
                        .first()
                        .single()
                assertEquals(13, saved.endRow)
                assertEquals(1, saved.rowsWorked)
            }
        }

    @Test
    fun stopCommittedBeforeCountKeepsTheLateCheckpointOutOfTheCompletedSession() =
        runBlocking {
            withTimeout(15_000L) {
                val active = (repository.startSession(1L) as StartSessionResult.Started).session
                now = now.copy(wallClockMillis = 66_000L, elapsedRealtimeMillis = 70_000L)
                val written = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                coroutineScope {
                    val stop =
                        async(Dispatchers.IO) {
                            database.withTransaction {
                                assertTrue(
                                    repository.stopSession(
                                        active.sessionToken,
                                        65L,
                                        0,
                                        12,
                                    ) is StopSessionResult.Saved,
                                )
                                written.complete(Unit)
                                release.await()
                            }
                        }
                    try {
                        written.await()
                        val count =
                            async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                                repository.applyMainCounterChange(1L, MainCounterChange.Increment)
                            }
                        release.complete(Unit)
                        stop.await()
                        assertTrue(count.await())
                    } finally {
                        release.complete(Unit)
                    }
                }

                assertEquals(13, database.counterProjectDao().getProject(1L)?.count)
                assertNull(database.sessionDao().getActiveSession())
                val saved =
                    database
                        .sessionDao()
                        .getAllSessions(null)
                        .first()
                        .single()
                assertEquals(65L, saved.durationSeconds)
                assertEquals(12, saved.endRow)
                assertEquals(0, saved.rowsWorked)
            }
        }
}
