package com.finnvek.knittools.ui.screens.insights

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.MAX_COMPLETED_SESSIONS
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.RoomCounterTestFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Device measurements use a private synthetic database, never the application's database. */
class InsightsDeviceBudgetTest {
    @Test
    fun legacyHistoryAboveWriterCeilingFoldsEveryRowOnceWithoutMutation(): Unit =
        runBlocking(Dispatchers.IO) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            val count = 100_001
            assertEquals(100_000L, MAX_COMPLETED_SESSIONS)
            val directory = File(context.cacheDir, "insights-legacy-rw03d-${UUID.randomUUID()}").apply { mkdirs() }
            val database =
                Room
                    .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "fixture.db").absolutePath)
                    .build()
            try {
                withTimeout(180_000) {
                    val runner = RoomDatabaseTransactionRunner(database)
                    runner.run {
                        database.counterProjectDao().insert(
                            CounterProjectEntity(id = 1, name = "Synthetic legacy history"),
                        )
                        // Only fixture setup bypasses writer admission; null zones exercise legacy UTC fallback.
                        database.openHelper.writableDatabase.execSQL(
                            """
                            WITH RECURSIVE seq(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM seq WHERE n<100000)
                            INSERT INTO sessions(id,projectId,startedAt,endedAt,startRow,endRow,
                                durationMinutes,durationSeconds,rowsWorked,zoneId)
                            SELECT 11+2*n,1,1577836800000+n*1800000,
                                1577836800000+n*1800000+(61+n%3)*1000,20,21+n%5,2,61+n%3,1+n%5,NULL
                            FROM seq
                            """.trimIndent(),
                        )
                    }

                    fun sourceFingerprint(): ByteArray {
                        val digest = MessageDigest.getInstance("SHA-256")
                        database.openHelper.readableDatabase
                            .query(
                                "SELECT id,projectId,startedAt,endedAt,startRow,endRow," +
                                    "durationMinutes,durationSeconds,rowsWorked,zoneId FROM sessions ORDER BY id",
                            ).use { cursor ->
                                var rows = 0
                                while (cursor.moveToNext()) {
                                    val line =
                                        (0 until cursor.columnCount).joinToString("|") {
                                            if (cursor.isNull(it)) "NULL" else cursor.getString(it)
                                        } + "\n"
                                    digest.update(line.toByteArray(Charsets.UTF_8))
                                    rows++
                                }
                                assertEquals(count, rows)
                            }
                        return digest.digest()
                    }
                    val before = sourceFingerprint()
                    val repository = RoomCounterTestFixture(database, context).counterRepository()
                    val params =
                        InsightsQueryParams(
                            timeRange = TimeRange.ALL_TIME,
                            currentDate = LocalDate.of(2026, 10, 27),
                            zone = ZoneOffset.UTC,
                        )
                    var processed = 0
                    val batchSizes = mutableListOf<Int>()
                    val result =
                        repository
                            .observeSessionsForInsights(
                                projectId = null,
                                start = null,
                                zone = ZoneOffset.UTC,
                                create = { InsightsSessionAccumulator(params, it) },
                                accumulate = { accumulator, batch ->
                                    batchSizes.add(batch.size)
                                    batch.forEach { session ->
                                        yield()
                                        val index = processed
                                        val start = 1_577_836_800_000L + index * 1_800_000L
                                        val seconds = 61L + index % 3
                                        val progress = 1 + index % 5
                                        assertEquals(
                                            "Legacy row at index $index",
                                            KnitSession(
                                                id = 11L + 2L * index,
                                                projectId = 1,
                                                startedAt = start,
                                                endedAt = start + seconds * 1_000L,
                                                startRow = 20,
                                                endRow = 20 + progress,
                                                durationMinutes = 2,
                                                durationSeconds = seconds,
                                                rowsWorked = progress,
                                                zoneId = null,
                                            ),
                                            session,
                                        )
                                        accumulator.add(session)
                                        processed++
                                    }
                                },
                            ).first()
                    assertEquals(count, processed)
                    assertEquals(List(390) { 256 } + 161, batchSizes)
                    assertEquals(count, result.summary.sessionCount)
                    // 33,333 * (61+62+63) + 61+62; 20,000 * (1+2+3+4+5) + 1.
                    assertEquals(6_200_061L, result.summary.totalSeconds)
                    assertEquals(300_001L, result.summary.totalRows)
                    assertEquals(LocalDate.of(2020, 1, 1), result.firstDate)
                    assertArrayEquals("Reading must preserve every source column and row", before, sourceFingerprint())
                    Log.i(
                        "RW03D",
                        "PASS sessions=$processed batches=${batchSizes.size} finalBatch=${batchSizes.last()} " +
                            "seconds=${result.summary.totalSeconds} neutralProgress=${result.summary.totalRows} " +
                            "sourceSha256=${before.joinToString("") { "%02x".format(it) }}",
                    )
                }
            } finally {
                database.close()
                assertTrue("Remove only the test-owned database directory", directory.deleteRecursively())
            }
        }

    @Test
    fun completeHistoryFoldMeasuresSampledMemoryAndQueuedWriter() =
        runBlocking(Dispatchers.IO) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val directory = File(context.cacheDir, "insights-budget-${UUID.randomUUID()}").apply { mkdirs() }
            val database =
                Room
                    .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "fixture.db").absolutePath)
                    .build()
            val receipt = File(context.getExternalFilesDir(null), "insights-device-budget.csv")
            receipt.writeText(
                "sessions,seed_ms,fold_ms,writer_wait_ms,max_sampled_pss_kib,max_sampled_java_bytes,batches,total_seconds,total_rows\n",
            )
            try {
                val runner = RoomDatabaseTransactionRunner(database)
                val repository = RoomCounterTestFixture(database, context).counterRepository()
                runner.run {
                    repeat(
                        10,
                    ) {
                        database.counterProjectDao().insert(
                            CounterProjectEntity(id = it + 1L, name = "Synthetic $it"),
                        )
                    }
                }
                var seeded = 0
                for (count in listOf(1_000, 10_000, 100_000)) {
                    withTimeout(180_000) {
                        val seedStart = SystemClock.elapsedRealtime()
                        runner.run {
                            val sql =
                                database.openHelper.writableDatabase.compileStatement(
                                    "INSERT INTO sessions (projectId,startedAt,endedAt,startRow,endRow,durationMinutes,durationSeconds,rowsWorked,zoneId) VALUES (?,?,?,?,?,?,?,?,?)",
                                )
                            sql.use {
                                for (i in seeded until count) {
                                    val start = 1_580_000_000_000L + i * 1_800_000L
                                    it.bindLong(1, i % 10 + 1L)
                                    it.bindLong(2, start)
                                    it.bindLong(3, start + 61_000L)
                                    it.bindLong(4, 0)
                                    it.bindLong(5, 10)
                                    it.bindLong(6, 2)
                                    it.bindLong(7, 61)
                                    it.bindLong(8, 10)
                                    it.bindString(9, "Europe/Helsinki")
                                    it.executeInsert()
                                }
                            }
                        }
                        seeded = count
                        val seedMs = SystemClock.elapsedRealtime() - seedStart
                        val firstBatch = CompletableDeferred<Unit>()
                        val writerAttempted = CompletableDeferred<Unit>()
                        val writer =
                            async(Dispatchers.IO) {
                                firstBatch.await()
                                val start = SystemClock.elapsedRealtime()
                                writerAttempted.complete(Unit)
                                runner.run { database.counterProjectDao().updateName(1, "Synthetic writer", 1L) }
                                SystemClock.elapsedRealtime() - start
                            }
                        var batches = 0
                        var maxJava = 0L
                        var maxPss = 0
                        val params =
                            InsightsQueryParams(
                                timeRange = TimeRange.ALL_TIME,
                                currentDate = LocalDate.of(2026, 10, 13),
                                zone = ZoneOffset.UTC,
                            )
                        val start = SystemClock.elapsedRealtime()
                        val result =
                            repository
                                .observeSessionsForInsights(
                                    projectId = null,
                                    start = null,
                                    zone = ZoneOffset.UTC,
                                    create = { InsightsSessionAccumulator(params, it) },
                                    accumulate = { accumulator, batch ->
                                        assertTrue(batch.size <= 256)
                                        if (batches == 0) {
                                            firstBatch.complete(Unit)
                                            writerAttempted.await()
                                        }
                                        batch.forEach {
                                            yield()
                                            accumulator.add(it)
                                        }
                                        val runtime = Runtime.getRuntime()
                                        maxJava = maxOf(maxJava, runtime.totalMemory() - runtime.freeMemory())
                                        if (batches++ % 32 == 0) {
                                            val memory = Debug.MemoryInfo()
                                            Debug.getMemoryInfo(memory)
                                            maxPss = maxOf(maxPss, memory.totalPss)
                                        }
                                    },
                                ).first()
                        val foldMs = SystemClock.elapsedRealtime() - start
                        val writerMs = writer.await()
                        assertEquals(count, result.summary.sessionCount)
                        assertEquals(count * 61L, result.summary.totalSeconds)
                        assertEquals(count * 10L, result.summary.totalRows)
                        assertEquals((count + 255) / 256, batches)
                        assertEquals("Synthetic writer", database.counterProjectDao().getProject(1)?.name)
                        receipt.appendText(
                            "$count,$seedMs,$foldMs,$writerMs,$maxPss,$maxJava,$batches,${result.summary.totalSeconds},${result.summary.totalRows}\n",
                        )
                    }
                }
            } finally {
                database.close()
                directory.deleteRecursively()
            }
        }
}
