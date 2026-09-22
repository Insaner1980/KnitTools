package com.finnvek.knittools.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
            assertEquals(3, dao.getInsightSessionBatch(256).size)
            assertEquals(1, dao.getProjectInsightSessionBatch(1, 256).size)
            assertEquals(listOf(fallback), dao.getInsightSessionBatchSince(Long.MIN_VALUE, 11_000).map { it.id })
            assertEquals(listOf(fallback), dao.getProjectInsightSessionBatchSince(2, old, 11_000).map { it.id })
            assertTrue(dao.getProjectInsightSessionBatchSince(1, Long.MIN_VALUE, 11_000).isEmpty())
            assertTrue(dao.getInsightSessionBatchSince(Long.MIN_VALUE, 11_001).isEmpty())
            assertTrue(dao.hasAnySessions())
            assertEquals(1000L, dao.getSessionProjectActivity(2).single().lastSessionAt)
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
