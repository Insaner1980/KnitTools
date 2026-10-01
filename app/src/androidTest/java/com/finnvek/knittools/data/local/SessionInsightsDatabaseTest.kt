package com.finnvek.knittools.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionInsightsDatabaseTest {
    private lateinit var database: KnitToolsDatabase

    @Before fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).build()
    }

    @After fun close() = database.close()

    @Test fun sessionSnapshotSurvivesProjectDeletionBetweenBatches() = checkConcurrentReplacement(restore = false)

    @Test fun sessionSnapshotSurvivesHistoryReplacementBetweenBatches() = checkConcurrentReplacement(restore = true)

    private fun checkConcurrentReplacement(restore: Boolean) =
        runTest {
            val runner = RoomDatabaseTransactionRunner(database)
            val dao = database.sessionDao()
            runner.run { seedHistory(projectId = 1, seconds = 60) }
            val firstBatchRead = CompletableDeferred<Unit>()
            val finishRead = CompletableDeferred<Unit>()
            val read =
                async {
                    runner.run {
                        assertTrue(dao.hasAnySessions())
                        assertEquals(listOf(SessionProjectActivity(1, 0)), dao.getSessionProjectActivity(null))
                        assertEquals(0L, dao.getFirstSessionStart(null))
                        assertEquals(256, dao.getInsightFirstDateBatch(null, Long.MIN_VALUE, 129600000L).size)
                        val first = dao.getInsightSessionBatch(Long.MIN_VALUE)
                        assertEquals(256, first.size)
                        firstBatchRead.complete(Unit)
                        finishRead.await()
                        val last = dao.getInsightSessionBatch(first.last().id)
                        assertEquals(1, last.size)
                        assertTrue(dao.getInsightSessionBatch(last.last().id).isEmpty())
                        first.sumOf { it.durationSeconds } + last.sumOf { it.durationSeconds }
                    }
                }
            firstBatchRead.await()
            val write =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runner.run {
                        database.counterProjectDao().delete(1)
                        if (restore) seedHistory(projectId = 2, seconds = 120)
                    }
                }
            finishRead.complete(Unit)
            assertEquals(257L * 60, read.await())
            write.await()
            assertEquals(if (restore) 257L else 0L, dao.countCompletedSessions())
            assertEquals(
                if (restore) listOf(SessionProjectActivity(2, 0)) else emptyList<SessionProjectActivity>(),
                dao.getSessionProjectActivity(null),
            )
        }

    @Test fun cancellingSnapshotReleasesRoomTransactionForWaitingWriter() =
        runTest {
            val runner = RoomDatabaseTransactionRunner(database)
            runner.run { seedHistory(projectId = 1, seconds = 60) }
            val firstBatchRead = CompletableDeferred<Unit>()
            val read =
                async {
                    runner.run {
                        database.sessionDao().getInsightSessionBatch(Long.MIN_VALUE)
                        firstBatchRead.complete(Unit)
                        awaitCancellation()
                    }
                }
            firstBatchRead.await()
            val write =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runner.run { database.counterProjectDao().delete(1) }
                }
            read.cancelAndJoin()
            write.await()
            assertEquals(0L, database.sessionDao().countCompletedSessions())
        }

    private suspend fun seedHistory(
        projectId: Long,
        seconds: Long,
    ) {
        database.counterProjectDao().insert(CounterProjectEntity(id = projectId, name = "Synthetic"))
        repeat(257) {
            database.sessionDao().insert(
                SessionEntity(
                    projectId = projectId,
                    startedAt = 0,
                    endedAt = seconds * 1000,
                    startRow = 0,
                    endRow = 1,
                    durationMinutes = (seconds / 60).toInt(),
                    durationSeconds = seconds,
                ),
            )
        }
    }

    @Test fun keysetBatchesCoverAllRowsAndApplyProjectAndEffectiveEndBoundaries() =
        runTest {
            database.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "One"))
            database.counterProjectDao().insert(CounterProjectEntity(id = 2, name = "Two"))
            val dao = database.sessionDao()
            repeat(257) {
                dao.insert(
                    SessionEntity(
                        projectId = 1,
                        startedAt = 0,
                        endedAt = 1000,
                        startRow = 0,
                        endRow = 1,
                        durationMinutes = 0,
                        durationSeconds = 1,
                    ),
                )
            }
            val old =
                dao.insert(
                    SessionEntity(
                        projectId = 2,
                        startedAt = 0,
                        endedAt = 1,
                        startRow = 0,
                        endRow = 0,
                        durationMinutes = 0,
                    ),
                )
            val fallback =
                dao.insert(
                    SessionEntity(
                        projectId = 2,
                        startedAt = 1000,
                        endedAt = 1001,
                        startRow = 0,
                        endRow = 1,
                        durationMinutes = 0,
                        durationSeconds = 10,
                    ),
                )
            assertEquals(256, dao.getInsightSessionBatch(Long.MIN_VALUE).size)
            assertEquals(259L, dao.countCompletedSessions())
            assertEquals(3, dao.getInsightSessionBatch(256).size)
            assertEquals(1, dao.getProjectInsightSessionBatch(1, 256).size)
            assertEquals(listOf(fallback), dao.getInsightSessionBatchSince(Long.MIN_VALUE, 11_000).map { it.id })
            assertEquals(listOf(fallback), dao.getProjectInsightSessionBatchSince(2, old, 11_000).map { it.id })
            assertTrue(dao.getProjectInsightSessionBatchSince(1, Long.MIN_VALUE, 11_000).isEmpty())
            assertTrue(dao.getInsightSessionBatchSince(Long.MIN_VALUE, 11_001).isEmpty())
            assertTrue(dao.hasAnySessions())
            assertEquals(1000L, dao.getSessionProjectActivity(2).single().lastSessionAt)
            dao.deleteById(old)
            assertEquals(258L, dao.countCompletedSessions())
            val sql = database.openHelper.writableDatabase
            sql.query("EXPLAIN QUERY PLAN SELECT * FROM sessions WHERE id > 256 ORDER BY id LIMIT 256").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(3).contains("INTEGER PRIMARY KEY"))
            }
            sql
                .query(
                    "EXPLAIN QUERY PLAN SELECT * FROM sessions WHERE id > ? " +
                        "AND (endedAt >= ? OR endedAt < ? AND startedAt + " +
                        "(CASE WHEN durationSeconds > 0 THEN durationSeconds " +
                        "WHEN durationMinutes > 0 THEN durationMinutes * 60 ELSE 1 END) * 1000 >= ?) " +
                        "ORDER BY id LIMIT 256",
                    arrayOf(256, 1000, 1000, 1000),
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertTrue(cursor.getString(3).contains("INTEGER PRIMARY KEY"))
                }
        }
}
