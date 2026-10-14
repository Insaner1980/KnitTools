package com.finnvek.knittools.ui.screens.insights

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.repository.RoomCounterTestFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Device measurements use a private synthetic database, never the application's database. */
class InsightsDeviceBudgetTest {
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
