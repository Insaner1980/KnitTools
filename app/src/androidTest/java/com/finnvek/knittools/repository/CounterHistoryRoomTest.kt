package com.finnvek.knittools.repository

import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.domain.model.CounterHistory
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.domain.model.MainCounterChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CounterHistoryRoomTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val databaseName = "history-retention-${UUID.randomUUID()}.db"

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.databaseBuilder(context, KnitToolsDatabase::class.java, databaseName).build()
        repository = counterHistoryTestRepository(database, context)
    }

    @After fun teardown() {
        scope.cancel()
        database.close()
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(databaseName)
    }

    @Test fun canonicalAndWidgetChangesUpdateScopedObservationAndUndoRemovesLatest() =
        runBlocking {
            val dao = database.counterProjectDao()
            val id = dao.insert(CounterProjectEntity(name = "Cardigan", stepSize = 3))
            val other = dao.insert(CounterProjectEntity(name = "Other"))
            val snapshots = Channel<List<CounterHistory>>(Channel.UNLIMITED)
            scope.launch { repository.observeCounterHistory(id).collect { snapshots.send(it) } }

            suspend fun awaitSize(size: Int): List<CounterHistory> =
                withTimeout(10_000) {
                    var rows = snapshots.receive()
                    while (rows.size != size) rows = snapshots.receive()
                    rows
                }
            awaitSize(0)
            assertFalse(repository.applyMainCounterChange(id, MainCounterChange.Decrement))
            assertFalse(repository.applyMainCounterChange(id, MainCounterChange.Reset))
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
            assertEquals(0 to 3, awaitSize(1).first().let { it.previousValue to it.newValue })
            assertTrue(repository.applyWidgetCountChange(id, true))
            assertEquals(3 to 6, awaitSize(2).first().let { it.previousValue to it.newValue })
            assertTrue(repository.applyWidgetCountChange(id, false))
            val decreased = awaitSize(3).first()
            assertEquals(CounterHistoryAction.DECREASE, decreased.action)
            assertEquals(6 to 3, decreased.previousValue to decreased.newValue)
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Reset))
            val reset = awaitSize(4).first()
            assertEquals(CounterHistoryAction.RESET, reset.action)
            assertEquals(3 to 0, reset.previousValue to reset.newValue)
            assertTrue(repository.applyMainCounterChange(other, MainCounterChange.Increment))
            assertEquals(4, repository.observeCounterHistory(id).first().size)
            assertEquals(1, repository.observeCounterHistory(other).first().size)
            repository.undoLastChange(id)
            val afterUndo = awaitSize(3)
            assertFalse(afterUndo.any { it.id == reset.id })
            assertEquals(decreased.id, afterUndo.first().id)
        }

    @Test fun equalTimestampsUnknownActionsAndThousandsOfRowsKeepStableNewestFirstOrder() =
        runBlocking {
            val dao = database.counterProjectDao()
            val id = dao.insert(CounterProjectEntity(name = "History"))

            fun pragma(name: String): Long =
                database.openHelper.writableDatabase.query("PRAGMA $name").use {
                    check(it.moveToFirst())
                    it.getLong(0)
                }
            val bytesBefore = pragma("page_count") * pragma("page_size")
            (1L..2000L).forEach { row ->
                dao.insertHistory(CounterHistoryEntity(row, id, "future", 56, 58, 100L))
            }
            dao.insertHistory(CounterHistoryEntity(2001, id, "increment", 0, 1, 99L))
            val rows = repository.observeCounterHistory(id).first()
            assertEquals((2000L downTo 1L).toList() + 2001L, rows.map { it.id })
            assertEquals(CounterHistoryAction.CHANGED, rows.first().action)
            val addedBytes = pragma("page_count") * pragma("page_size") - bytesBefore
            Log.i("HistoryRetentionTest", "2001 history rows added $addedBytes SQLite bytes including index")
            assertEquals(56 to 58, rows.first().let { it.previousValue to it.newValue })
        }

    @Test fun completionAndReactivationRetainHistoryAndDeletionCascadesOnlyTarget() =
        runBlocking {
            var dao = database.counterProjectDao()
            val id = dao.insert(CounterProjectEntity(name = "Cardigan"))
            val other = dao.insert(CounterProjectEntity(name = "Socks"))
            val now = System.currentTimeMillis()
            listOf(180L, 7L, 2L).forEach { days ->
                dao.insertHistory(
                    CounterHistoryEntity(
                        projectId = id,
                        action = "reset",
                        previousValue = 1,
                        newValue = 0,
                        timestamp = now - days * 86_400_000L,
                    ),
                )
            }
            repository.applyMainCounterChange(id, MainCounterChange.Increment)
            repository.applyMainCounterChange(other, MainCounterChange.Increment)
            val before = repository.observeCounterHistory(id).first()
            repository.archiveProject(id, 100)
            assertEquals(before, repository.observeCounterHistory(id).first())
            database.close()
            setup()
            dao = database.counterProjectDao()
            assertEquals(before, repository.observeCounterHistory(id).first())
            assertEquals(ProjectReactivationResult.Reactivated, repository.reactivateProject(id, 100))
            assertEquals(before, repository.observeCounterHistory(id).first())
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
            assertEquals(before, repository.observeCounterHistory(id).first().drop(1))
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Undo))
            assertEquals(before, repository.observeCounterHistory(id).first())
            val invalidEntries =
                listOf(
                    CounterHistoryEntity(projectId = id, action = "increment", previousValue = 5, newValue = 6),
                    CounterHistoryEntity(projectId = id, action = "unknown", previousValue = 0, newValue = 1),
                )
            for (entry in invalidEntries) {
                dao.insertHistory(entry)
                val invalidLatest = repository.observeCounterHistory(id).first()
                assertFalse(repository.applyMainCounterChange(id, MainCounterChange.Undo))
                assertEquals(invalidLatest, repository.observeCounterHistory(id).first())
                assertEquals(1, dao.getProject(id)?.count)
                dao.deleteHistoryById(invalidLatest.first().id)
            }
            repository.archiveProject(id, 100)
            assertEquals(before, repository.observeCounterHistory(id).first())
            repository.deleteProject(id)
            assertTrue(repository.observeCounterHistory(id).first().isEmpty())
            assertEquals(1, repository.observeCounterHistory(other).first().size)
        }
}
