package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.domain.model.KnitSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditPart12SessionLimitTest {
    @Test
    fun actualSessionCeilingRejectsStartAndFinalizeWithoutTruncatingHistory() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val db =
                Room
                    .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                    .addCallback(ActiveSessionSchemaConstraints.callback)
                    .build()
            try {
                db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Synthetic session ceiling"))
                db.openHelper.writableDatabase.execSQL(
                    "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<99999) " +
                        "INSERT INTO sessions(id,projectId,startedAt,endedAt,startRow,endRow," +
                        "durationMinutes,durationSeconds,rowsWorked,zoneId) " +
                        "SELECT n,1,1000,61000,0,1,1,60,1,'Europe/Helsinki' FROM seq",
                )
                assertEquals(99_999L, db.sessionDao().countCompletedSessions())
                val repository = RoomCounterTestFixture(db, context).counterRepository()
                val started = repository.startSession(1) as StartSessionResult.Started
                val last =
                    KnitSession(
                        projectId = 1,
                        startedAt = 1000,
                        endedAt = 61000,
                        startRow = 0,
                        endRow = 1,
                        durationMinutes = 1,
                    )
                repository.insertSession(last)
                assertEquals(100_000L, db.sessionDao().countCompletedSessions())
                delay(1_100)
                val stopped = repository.stopSession(started.session.sessionToken, 1, 0, 0)
                if (stopped is StopSessionResult.NeedsRecoveryReview) {
                    assertEquals(
                        RecoveryResolutionResult.HistoryLimitReached,
                        repository.editRecoveryDurationAndStop(
                            started.session.sessionToken,
                            requireNotNull(stopped.session.recoveryIntervalToken),
                            60,
                        ),
                    )
                } else {
                    assertEquals(StopSessionResult.HistoryLimitReached, stopped)
                }
                assertEquals(started.session.sessionToken, db.sessionDao().getActiveSession()?.sessionToken)
                assertTrue(
                    runCatching {
                        repository.insertSession(
                            last,
                        )
                    }.exceptionOrNull() is SessionHistoryLimitReachedException,
                )
                repository.discardActiveSession(started.session.sessionToken)
                assertEquals(StartSessionResult.HistoryLimitReached, repository.startSession(1))
                // A legacy oversized table remains readable; admission must never trim it.
                db.sessionDao().insert(
                    SessionEntity(
                        projectId = 1,
                        startedAt = 1000,
                        endedAt = 61000,
                        startRow = 0,
                        endRow = 1,
                        durationMinutes = 1,
                    ),
                )
                assertEquals(100_001L, db.sessionDao().countCompletedSessions())
                assertEquals(StartSessionResult.HistoryLimitReached, repository.startSession(1))
                assertTrue(
                    runCatching {
                        repository.insertSession(
                            last,
                        )
                    }.exceptionOrNull() is SessionHistoryLimitReachedException,
                )
                assertEquals(100_001L, db.sessionDao().countCompletedSessions())
            } finally {
                db.close()
            }
        }
}
