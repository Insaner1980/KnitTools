package com.finnvek.knittools.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveSessionMigration21Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate20to21AddsOneGlobalProjectOwnedActiveSession() {
        val testDb = "migration-test-v20-to-v21-active-session"
        helper.createDatabase(testDb, 20).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt
                ) VALUES (41, 'Cardigan', 12, 0, 1, '', 1000, 2000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow,
                    durationMinutes, durationSeconds, rowsWorked, zoneId
                ) VALUES (9, 41, 1000, 61000, 10, 12, 1, 60, 2, 'Europe/Helsinki')
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                21,
                true,
                KnitToolsDatabase.MIGRATION_20_21,
            )

        assertEquals(0, countRows(db, "active_sessions"))
        db.query("SELECT durationSeconds, rowsWorked, zoneId FROM sessions WHERE id = 9").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(60L, cursor.getLong(0))
            assertEquals(2, cursor.getInt(1))
            assertEquals("Europe/Helsinki", cursor.getString(2))
        }

        db.execSQL(
            """
            INSERT INTO active_sessions (
                singletonId, sessionToken, projectId, startedAtWallMillis, startZoneId,
                startRow, lastObservedRow, trustedLastObservedRow,
                trustedRowsWorked, pendingRowsWorked,
                reviewedRowsWorked, reviewedLastObservedRow, unreviewedRowsWorked,
                checkpointedDurationSeconds, reviewedDurationBaselineSeconds,
                segmentStartedAtWallMillis, segmentStartedElapsedRealtimeMillis, bootCount,
                recoveryReason, recoveryIntervalToken, recoverySuggestedDurationSeconds,
                recoveryPromptShown, updatedAtWallMillis
            ) VALUES (
                1, 'session-one', 41, 3000, 'Europe/Helsinki',
                12, 12, 12, 0, 0, 0, 12, 0, 0, 0, 3000, 5000, 7,
                NULL, NULL, NULL, 0, 3000
            )
            """.trimIndent(),
        )

        db.query("SELECT sessionToken, projectId FROM active_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("session-one", cursor.getString(0))
            assertEquals(41L, cursor.getLong(1))
        }
        assertEquals(1, countRows(db, "active_sessions"))
        try {
            db.execSQL(
                """
                INSERT INTO active_sessions (
                    singletonId, sessionToken, projectId, startedAtWallMillis, startZoneId,
                    startRow, lastObservedRow, trustedLastObservedRow,
                    trustedRowsWorked, pendingRowsWorked,
                    reviewedRowsWorked, reviewedLastObservedRow, unreviewedRowsWorked,
                    checkpointedDurationSeconds, reviewedDurationBaselineSeconds,
                    segmentStartedAtWallMillis, segmentStartedElapsedRealtimeMillis,
                    recoveryPromptShown, updatedAtWallMillis
                ) VALUES (
                    2, 'session-two', 41, 3000, 'Europe/Helsinki',
                    12, 12, 12, 0, 0, 0, 12, 0, 0, 0, 3000, 5000, 0, 3000
                )
                """.trimIndent(),
            )
            fail("Non-singleton active session was accepted")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Skeemaraja esti toisen globaalin aktiivisen istunnon.
        }
        db.close()
    }

    private fun countRows(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String,
    ): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
