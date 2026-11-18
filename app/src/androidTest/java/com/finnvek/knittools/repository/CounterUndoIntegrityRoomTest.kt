package com.finnvek.knittools.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CounterUndoIntegrityRoomTest {
    private lateinit var context: Context
    private lateinit var database: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private val databaseName = "counter-undo-${UUID.randomUUID()}.db"
    private var now = SessionTimeSnapshot(1_700_000_000_000L, 5_000L, 7L, "Europe/Helsinki")
    private val time =
        object : SessionTimeSource {
            override fun snapshot() = now
        }

    @Before
    fun setup() {
        context = isolatedRoomTestContext()
        open()
    }

    private fun open() {
        database = Room.databaseBuilder(context, KnitToolsDatabase::class.java, databaseName).build()
        fixture = RoomCounterTestFixture(database, context)
    }

    @After
    fun teardown() {
        if (::database.isInitialized) database.close()
        if (::context.isInitialized) context.deleteDatabase(databaseName)
    }

    @Test
    fun backwardClockCannotDisableUndoOrSelectAnOlderMatchingCount() =
        runBlocking {
            for (includeDecrement in listOf(false, true)) {
                val id = seed(0, 1)
                val repository = repository()
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                timestampLatest(id, 100)
                assertTrue(repository.applyWidgetCountChange(id, true))
                timestampLatest(id, 90)
                if (includeDecrement) {
                    assertTrue(repository.applyWidgetCountChange(id, false))
                    timestampLatest(id, 91)
                }
                val latest =
                    database
                        .counterProjectDao()
                        .observeCounterHistory(id)
                        .first()
                        .maxBy { it.id }
                assertTrue(repository.observeCanUndo(id).first())
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                val expected = if (includeDecrement) 2 else 1
                assertEquals(expected, repository.getProject(id)?.count)
                assertEquals(
                    5 + expected,
                    database
                        .projectCounterDao()
                        .getCountersForProject(id)
                        .first()
                        .single()
                        .count,
                )
                assertFalse(repository.observeCounterHistory(id).first().any { it.id == latest.id })
            }
        }

    @Test
    fun clampedSessionUndoSurvivesDatabaseReopenAndSavesNoInventedWork() =
        runBlocking {
            for ((count, step) in listOf(12 to 1, 12 to 5, 2 to 5)) {
                val id = seed(count, step)
                var repository = repository()
                assertTrue(repository.startSession(id) is StartSessionResult.Started)
                now =
                    now.copy(
                        wallClockMillis = now.wallClockMillis + 1_000,
                        elapsedRealtimeMillis =
                            now.elapsedRealtimeMillis + 1_000,
                    )
                assertTrue(repository.applyWidgetCountChange(id, false))
                database.close()
                open()
                repository = repository()
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                val active = requireNotNull(database.sessionDao().getActiveSession())
                assertEquals(count, repository.getProject(id)?.count)
                assertEquals(
                    5,
                    database
                        .projectCounterDao()
                        .getCountersForProject(id)
                        .first()
                        .single()
                        .count,
                )
                assertEquals(0, active.trustedRowsWorked)
                assertEquals(0, active.pendingRowsWorked)
                assertEquals(0, active.unreviewedRowsWorked)
                assertEquals(count, active.lastObservedRow)
                assertTrue(repository.observeCounterHistory(id).first().isEmpty())
                assertTrue(repository.stopSession(active.sessionToken, 1, 0, count) is StopSessionResult.Saved)
                assertEquals(
                    0,
                    database
                        .sessionDao()
                        .getSessionsForProject(id)
                        .first()
                        .single()
                        .rowsWorked,
                )
            }
        }

    @Test
    fun concurrentUndosConsumeDistinctHistoryRowsAndEmptyUndoDoesNothing() =
        runBlocking {
            withTimeout(10_000) {
                val id = seed(7, 5)
                val repository = repository()
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                assertTrue(repository.applyWidgetCountChange(id, true))
                val release = CompletableDeferred<Unit>()
                var admitted = 0
                val gated =
                    repository(
                        object : DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T {
                                admitted++
                                release.await()
                                return fixture.transactionRunner.run(block)
                            }
                        },
                    )
                val undos =
                    List(2) {
                        async(
                            start = CoroutineStart.UNDISPATCHED,
                        ) { gated.applyMainCounterChange(id, MainCounterChange.Undo) }
                    }
                assertEquals(2, admitted)
                release.complete(Unit)
                assertTrue(undos.awaitAll().all { it })
                assertEquals(7, repository.getProject(id)?.count)
                assertEquals(
                    5,
                    database
                        .projectCounterDao()
                        .getCountersForProject(id)
                        .first()
                        .single()
                        .count,
                )
                assertTrue(repository.observeCounterHistory(id).first().isEmpty())
                val before = database.auditSnapshot()
                assertFalse(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(before, database.auditSnapshot())
            }
        }

    @Test
    fun queuedUndoConsumesTheInterveningWidgetChangeAfterReset() =
        runBlocking {
            withTimeout(10_000) {
                val id = seed(12, 5)
                val repository = repository()
                assertTrue(repository.startSession(id) is StartSessionResult.Started)
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Decrement))
                assertTrue(repository.applyWidgetCountChange(id, false))
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Reset))
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(2, repository.getProject(id)?.count)
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val queued = repository(fixture.transactionRunner.pauseBeforeTransaction(entered, release))
                val undo = async { queued.applyMainCounterChange(id, MainCounterChange.Undo) }
                entered.await()
                assertTrue(repository.applyWidgetCountChange(id, true))
                assertEquals(7, repository.getProject(id)?.count)
                release.complete(Unit)
                assertTrue(undo.await())
                assertEquals(2, repository.getProject(id)?.count)
                assertEquals(0, database.sessionDao().getActiveSession()?.trustedRowsWorked)
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(7, repository.getProject(id)?.count)
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(12, repository.getProject(id)?.count)
                assertEquals(5, repository.getProject(id)?.stepSize)
                assertEquals(0, repository.getProject(id)?.currentStitch)
                assertEquals(
                    5,
                    database
                        .projectCounterDao()
                        .getCountersForProject(id)
                        .first()
                        .single()
                        .count,
                )
                assertEquals(0, database.sessionDao().getActiveSession()?.trustedRowsWorked)
                assertEquals(0, database.sessionDao().getActiveSession()?.unreviewedRowsWorked)
                assertTrue(repository.observeCounterHistory(id).first().isEmpty())
            }
        }

    @Test
    fun integerLimitNoOpAndUndoPreserveOneCoherentHistoryStep() =
        runBlocking {
            for (step in listOf(1, 99)) {
                val id = seed(Int.MAX_VALUE - 1, step)
                val repository = repository()
                assertTrue(repository.startSession(id) is StartSessionResult.Started)
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                assertEquals(Int.MAX_VALUE, repository.getProject(id)?.count)
                assertEquals(1, database.sessionDao().getActiveSession()?.trustedRowsWorked)
                val before = database.auditSnapshot()
                assertFalse(repository.applyWidgetCountChange(id, true))
                assertEquals(before, database.auditSnapshot())
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(Int.MAX_VALUE - 1, repository.getProject(id)?.count)
                assertEquals(0, database.sessionDao().getActiveSession()?.trustedRowsWorked)
                assertEquals(
                    5,
                    database
                        .projectCounterDao()
                        .getCountersForProject(id)
                        .first()
                        .single()
                        .count,
                )
                repository.discardActiveSession(requireNotNull(database.sessionDao().getActiveSession()).sessionToken)
            }
        }

    @Test
    fun failuresAtHistoryDocumentAndSessionWritesRollbackEveryParticipatingTable() =
        runBlocking {
            val id = seed(7, 5)
            val repository = repository()
            assertTrue(repository.startSession(id) is StartSessionResult.Started)
            for (change in listOf(MainCounterChange.Increment, MainCounterChange.Undo)) {
                if (change ==
                    MainCounterChange.Undo
                ) {
                    assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                }
                val historyOperation = if (change == MainCounterChange.Undo) "DELETE" else "INSERT"
                val targets =
                    listOf(
                        "$historyOperation ON counter_history",
                        "UPDATE ON project_documents",
                        "UPDATE ON active_sessions",
                    ) +
                        if (change == MainCounterChange.Increment) listOf("UPDATE ON counter_history") else emptyList()
                for (target in targets) {
                    val before = database.auditSnapshot()
                    val sql = database.openHelper.writableDatabase
                    sql.execSQL(
                        "CREATE TRIGGER reject_counter_write BEFORE $target BEGIN SELECT RAISE(ABORT, 'counter undo test'); END",
                    )
                    try {
                        val failure = runCatching { repository.applyMainCounterChange(id, change) }.exceptionOrNull()
                        assertTrue("$change $target: $failure", failure is SQLiteException)
                        assertEquals(before, database.auditSnapshot())
                    } finally {
                        sql.execSQL("DROP TRIGGER reject_counter_write")
                    }
                }
            }
        }

    @Test
    fun cancellationBeforeCommitRollsBackAndAfterCommitPreservesTheChange() =
        runBlocking {
            withTimeout(10_000) {
                val id = seed(7, 5)
                assertTrue(repository().startSession(id) is StartSessionResult.Started)
                for (change in listOf(MainCounterChange.Increment, MainCounterChange.Undo)) {
                    for (afterCommit in listOf(false, true)) {
                        val entered = CompletableDeferred<Unit>()
                        val release = CompletableDeferred<Unit>()
                        val before = database.auditSnapshot()
                        val gated =
                            repository(
                                object : DatabaseTransactionRunner {
                                    override suspend fun <T> run(block: suspend () -> T): T {
                                        val result =
                                            fixture.transactionRunner.run {
                                                val value = block()
                                                if (!afterCommit) {
                                                    entered.complete(Unit)
                                                    release.await()
                                                }
                                                value
                                            }
                                        if (afterCommit) {
                                            entered.complete(Unit)
                                            release.await()
                                        }
                                        return result
                                    }
                                },
                            )
                        val job = async { gated.applyMainCounterChange(id, change) }
                        entered.await()
                        val committed = if (afterCommit) database.auditSnapshot() else null
                        job.cancelAndJoin()
                        assertTrue(job.isCancelled)
                        if (afterCommit) {
                            assertEquals(committed, database.auditSnapshot())
                            val expected = if (change == MainCounterChange.Increment) 12 else 7
                            assertEquals(expected, repository().getProject(id)?.count)
                            assertEquals(
                                if (change ==
                                    MainCounterChange.Increment
                                ) {
                                    1
                                } else {
                                    0
                                },
                                repository().observeCounterHistory(id).first().size,
                            )
                            assertEquals(
                                if (change ==
                                    MainCounterChange.Increment
                                ) {
                                    10
                                } else {
                                    5
                                },
                                database
                                    .projectCounterDao()
                                    .getCountersForProject(id)
                                    .first()
                                    .single()
                                    .count,
                            )
                            assertEquals(
                                if (change ==
                                    MainCounterChange.Increment
                                ) {
                                    5
                                } else {
                                    0
                                },
                                database.sessionDao().getActiveSession()?.trustedRowsWorked,
                            )
                            assertEquals(0, repository().getProject(id)?.currentStitch)
                        } else {
                            assertEquals(before, database.auditSnapshot())
                        }
                    }
                }
            }
        }

    private fun repository(transactions: DatabaseTransactionRunner = fixture.transactionRunner) =
        fixture.counterRepository(transactions = transactions, sessionTimeSource = time)

    private suspend fun timestampLatest(
        id: Long,
        timestamp: Long,
    ) {
        val latest =
            database
                .counterProjectDao()
                .observeCounterHistory(id)
                .first()
                .maxOf { it.id }
        database.openHelper.writableDatabase.execSQL(
            "UPDATE counter_history SET timestamp = ? WHERE id = ?",
            arrayOf(timestamp, latest),
        )
    }

    private suspend fun seed(
        count: Int,
        step: Int,
    ): Long {
        val id =
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    name = "Counter integrity",
                    count = count,
                    stepSize = step,
                    stitchTrackingEnabled = true,
                    stitchCount = 30,
                    currentStitch = 9,
                ),
            )
        database.projectCounterDao().insert(
            ProjectCounterEntity(projectId = id, name = "Linked", count = 5, linkedToMainCounter = true),
        )
        database.projectDocumentDao().insert(
            ProjectDocumentEntity(
                projectId = id,
                savedPatternId = null,
                documentKey = "counter-integrity:$id",
                label = "Pattern",
                localPdfUri = "content://counter-integrity/$id",
                sortOrder = 0,
                isPrimary = true,
                currentPage = 0,
                rowMapping = null,
                readingLineEnabled = true,
                readingLineYFraction = 0.5f,
                readingLineFollowCurrentRow = true,
                verticalReadingGuideEnabled = false,
                verticalReadingGuideXFraction = 0.5f,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        return id
    }
}
