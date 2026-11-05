package com.finnvek.knittools.repository

import android.content.Context
import android.os.Process
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ActiveSessionProcessTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory = File(context.cacheDir, "audit-process-session")
    private val expectedRow = File(directory, "expected-row.txt")
    private val seedPid = File(directory, "seed-pid.txt")
    private var now = SessionTimeSnapshot(1_700_000_000_000L, 5_000L, 7L, "Europe/Helsinki")

    private fun open(): KnitToolsDatabase {
        check(context.packageName.endsWith(".activepolicytest"))
        directory.mkdirs()
        return Room
            .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "live.db").absolutePath)
            .addCallback(ActiveSessionSchemaConstraints.callback)
            .build()
    }

    private fun repository(database: KnitToolsDatabase): CounterRepository {
        val fixture = RoomCounterTestFixture(database, context)
        return CounterRepository(
            dao = database.counterProjectDao(),
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(Dispatchers.IO),
            context = context,
            yarnCardRepository = fixture.yarnCardRepository,
            savedPatternRepository = fixture.savedPatternRepository,
            projectDocumentRepository = fixture.projectDocumentRepository,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = fixture.transactionRunner,
            ioDispatcher = Dispatchers.IO,
            sessionTimeSource =
                object : SessionTimeSource {
                    override fun snapshot() = now
                },
        )
    }

    @Test fun seedSessionBeforeSeparateProcessRestart() =
        runBlocking {
            assumeTrue(InstrumentationRegistry.getArguments().getString("activeSessionProcessStage") == "seed")
            check(!expectedRow.exists()) { "Previous process fixture must be resolved before seeding" }
            val database = open()
            var seeded = false
            try {
                val project =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = "Process session", count = 10, stepSize = 2),
                    )
                val repository = repository(database)
                assertTrue(repository.startSession(project) is StartSessionResult.Started)
                now = now.copy(wallClockMillis = now.wallClockMillis + 61_000L, elapsedRealtimeMillis = 66_000L)
                assertTrue(repository.applyMainCounterChange(project, MainCounterChange.Increment))
                now = now.copy(wallClockMillis = now.wallClockMillis + 62_000L, elapsedRealtimeMillis = 128_000L)
                assertTrue(repository.applyWidgetCountChange(project, true))
                val active = checkNotNull(database.sessionDao().getActiveSession())
                assertEquals(123L, active.checkpointedDurationSeconds)
                assertEquals(4, active.trustedRowsWorked)
                assertEquals(14, active.trustedLastObservedRow)
                expectedRow.writeText(active.toString())
                seedPid.writeText(Process.myPid().toString())
                seeded = true
            } finally {
                database.close()
                if (!seeded) directory.deleteRecursively()
            }
        }

    @Test fun restartedProcessKeepsEveryAnchorAndRecoveryFinalizesAtMostOnce() =
        runBlocking {
            assumeTrue(InstrumentationRegistry.getArguments().getString("activeSessionProcessStage") == "verify")
            check(expectedRow.isFile) { "Run seed, stop its isolated process, then run verification" }
            val database = open()
            try {
                val original = checkNotNull(database.sessionDao().getActiveSession())
                assertFalse(seedPid.readText().toInt() == Process.myPid())
                assertEquals(expectedRow.readText(), original.toString())
                assertEquals(14, database.counterProjectDao().getProject(original.projectId)?.count)
                val repository = repository(database)
                now = now.copy(wallClockMillis = 1_700_000_200_000L, elapsedRealtimeMillis = 2_000L, bootCount = 8L)
                val recovery = checkNotNull(repository.refreshActiveSession())
                assertEquals(ActiveSessionRecoveryReason.REBOOTED, recovery.recoveryReason)
                assertEquals(original.sessionToken, recovery.sessionToken)
                val interval = checkNotNull(recovery.recoveryIntervalToken)
                assertEquals(true, repository.markRecoveryPromptShown(recovery.sessionToken, interval))
                assertTrue(
                    repository.addRecoveryInterval(
                        recovery.sessionToken,
                        interval,
                        60L,
                    ) is RecoveryResolutionResult.Continued,
                )
                assertEquals(
                    RecoveryResolutionResult.StaleAction,
                    repository.addRecoveryInterval(recovery.sessionToken, interval, 60L),
                )
                now = now.copy(wallClockMillis = now.wallClockMillis + 61_000L, elapsedRealtimeMillis = 63_000L)
                val final = checkNotNull(repository.refreshActiveSession())
                assertTrue(repository.stopSession(final.sessionToken, 244L, 4, 14) is StopSessionResult.Saved)
                assertEquals(StopSessionResult.NoActiveSession, repository.stopSession(final.sessionToken, 244L, 4, 14))
                val completed = repository.getSessionsForProject(original.projectId).first().single()
                assertEquals(244L, completed.durationSeconds)
                assertEquals(4, completed.rowsWorked)
                assertEquals(10, completed.startRow)
                assertEquals(14, completed.endRow)
                assertEquals("Europe/Helsinki", completed.zoneId)
                assertEquals(null, database.sessionDao().getActiveSession())
            } finally {
                database.close()
                directory.deleteRecursively()
            }
        }
}
