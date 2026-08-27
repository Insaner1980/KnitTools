package com.finnvek.knittools.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveSessionDatabaseTest {
    private lateinit var database: KnitToolsDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).addCallback(ActiveSessionSchemaConstraints.callback)
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun concurrentInsertsLeaveExactlyOneGlobalActiveSession() =
        runTest {
            database.counterProjectDao().insert(CounterProjectEntity(id = 7L, name = "Cardigan"))
            val results =
                coroutineScope {
                    listOf("one", "two")
                        .map { token ->
                            async {
                                runCatching {
                                    database.sessionDao().insertActiveSession(activeSession(token))
                                }.isSuccess
                            }
                        }.awaitAll()
                }

            assertEquals(1, results.count { it })
            assertEquals(7L, database.sessionDao().getActiveSession()?.projectId)
        }

    @Test
    fun activeSessionCrudObservationAndProjectCascadePreserveStableToken() =
        runTest {
            database.counterProjectDao().insert(CounterProjectEntity(id = 7L, name = "Cardigan"))
            val original = activeSession("stable-token")

            database.sessionDao().insertActiveSession(original)
            assertEquals(
                "stable-token",
                database
                    .sessionDao()
                    .observeActiveSession()
                    .first()
                    ?.sessionToken,
            )

            database.sessionDao().updateActiveSession(original.copy(pendingRowsWorked = 2))
            assertEquals(2, database.sessionDao().getActiveSession()?.pendingRowsWorked)
            assertEquals("stable-token", database.sessionDao().getActiveSession()?.sessionToken)

            database.counterProjectDao().delete(7L)
            assertEquals(null, database.sessionDao().getActiveSession())
        }

    @Test
    fun activeSessionForeignKeyRejectsMissingProjectAndDeleteRemovesRow() =
        runTest {
            var rejected = false
            try {
                database.sessionDao().insertActiveSession(activeSession("missing"))
            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                rejected = true
            }
            assertEquals(true, rejected)

            database.counterProjectDao().insert(CounterProjectEntity(id = 7L, name = "Cardigan"))
            database.sessionDao().insertActiveSession(activeSession("delete-me"))
            assertEquals(1, database.sessionDao().deleteActiveSession("delete-me"))
            assertEquals(null, database.sessionDao().getActiveSession())
        }

    private fun activeSession(token: String) =
        ActiveSessionEntity(
            sessionToken = token,
            projectId = 7L,
            startedAtWallMillis = 1_000L,
            startZoneId = "Europe/Helsinki",
            startRow = 0,
            lastObservedRow = 0,
            trustedLastObservedRow = 0,
            trustedRowsWorked = 0,
            pendingRowsWorked = 0,
            reviewedRowsWorked = 0,
            reviewedLastObservedRow = 0,
            unreviewedRowsWorked = 0,
            checkpointedDurationSeconds = 0L,
            reviewedDurationBaselineSeconds = 0L,
            segmentStartedAtWallMillis = 1_000L,
            segmentStartedElapsedRealtimeMillis = 5_000L,
            bootCount = 4L,
            recoveryReason = null,
            recoveryIntervalToken = null,
            recoverySuggestedDurationSeconds = null,
            recoveryPromptShown = false,
            updatedAtWallMillis = 1_000L,
        )
}
