package com.finnvek.knittools.data.local

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testaa Room-migraatiot v1->v24.
 * v1->v3: AutoMigration. v3->v24: manuaaliset muutokset.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
        )

    private val allMigrations = KnitToolsDatabase.ALL_MANUAL_MIGRATIONS

    private val latestVersion = KNITTOOLS_DATABASE_VERSION

    private fun migrateToLatest(testDb: String): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(
            testDb,
            latestVersion,
            true,
            *allMigrations,
        )

    private inline fun assertSingleRow(
        db: SupportSQLiteDatabase,
        sql: String,
        verify: Cursor.() -> Unit,
    ) {
        val cursor = db.query(sql)
        try {
            assertTrue(cursor.moveToFirst())
            cursor.verify()
            assertFalse(cursor.moveToNext())
        } finally {
            cursor.close()
        }
    }

    private fun assertStartedAtIndexExists(db: SupportSQLiteDatabase) {
        assertIndexExists(db, "sessions", "index_sessions_startedAt")
    }

    private fun assertProjectYarnNoteIndexExists(db: SupportSQLiteDatabase) {
        assertIndexExists(db, "project_yarn_notes", "index_project_yarn_notes_projectId")
    }

    private fun assertLinkedCleanupIndexesExist(db: SupportSQLiteDatabase) {
        assertIndexExists(db, "counter_projects", "index_counter_projects_linkedPatternId")
        assertIndexExists(db, "yarn_cards", "index_yarn_cards_linkedProjectId")
    }

    private fun assertIndexExists(
        db: SupportSQLiteDatabase,
        table: String,
        indexName: String,
    ) {
        val indexCursor = db.query("PRAGMA index_list('$table')")
        var hasIndex = false
        try {
            while (indexCursor.moveToNext()) {
                if (indexCursor.getString(1) == indexName) {
                    hasIndex = true
                }
            }
        } finally {
            indexCursor.close()
        }
        assertTrue(hasIndex)
    }

    @Test
    fun migrate14to15AddsLinkedCleanupIndexes() {
        val testDb = "migration-test-v14-to-v15-linked-cleanup-indexes"

        helper.createDatabase(testDb, 14).close()

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                15,
                true,
                KnitToolsDatabase.MIGRATION_14_15,
            )

        assertLinkedCleanupIndexesExist(db)
        db.close()
    }

    @Test
    fun migrate15to16AddsNullableSessionZoneId() {
        val testDb = "migration-test-v15-to-v16-session-zone"

        helper.createDatabase(testDb, 15).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt
                ) VALUES (1, 'Test', 0, 0, 1, '', 1000, 1000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow,
                    durationMinutes, durationSeconds, rowsWorked
                ) VALUES (1, 1, 1000, 2000, 0, 1, 1, 60, 1)
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                16,
                true,
                KnitToolsDatabase.MIGRATION_15_16,
            )

        assertSingleRow(db, "SELECT zoneId FROM sessions WHERE id = 1") {
            assertNull(getString(0))
        }
        db.close()
    }

    @Test
    fun migrate17to18PreservesProjectsAndUnlocksPreviouslyCreatedContent() {
        val testDb = "migration-test-v17-to-v18-pro-content-flags"

        helper.createDatabase(testDb, 17).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt
                ) VALUES (1, 'Existing project', 0, 0, 1, '', 1000, 2000)
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                18,
                true,
                KnitToolsDatabase.MIGRATION_17_18,
            )

        assertSingleRow(
            db,
            "SELECT name, secondaryCounterUsed, notesCreated FROM counter_projects WHERE id = 1",
        ) {
            assertEquals("Existing project", getString(0))
            assertEquals(1, getInt(1))
            assertEquals(1, getInt(2))
        }

        db.execSQL(
            """
            INSERT INTO counter_projects (
                id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt
            ) VALUES (2, 'New project', 0, 0, 1, '', 3000, 3000)
            """.trimIndent(),
        )
        assertSingleRow(
            db,
            "SELECT secondaryCounterUsed, notesCreated FROM counter_projects WHERE id = 2",
        ) {
            assertEquals(0, getInt(0))
            assertEquals(0, getInt(1))
        }
        db.close()
    }

    @Test
    fun migrate1to2() {
        val testDb = "migration-test-v1-to-v2"

        // Luo v1-tietokanta testidatalla
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Test', 42, 0, 1, '', 1000, 2000)",
            )
            execSQL(
                "INSERT INTO counter_history (id, projectId, action, previousValue, newValue, timestamp) VALUES (1, 1, 'increment', 41, 42, 3000)",
            )
            close()
        }

        // Migroi v2:een — AutoMigration
        val db = helper.runMigrationsAndValidate(testDb, 2, true)

        val projectCursor =
            db.query(
                "SELECT name, count, secondaryCount, stepSize, notes, createdAt, updatedAt FROM counter_projects WHERE id = 1",
            )
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Test", projectCursor.getString(0))
        assertEquals(42, projectCursor.getInt(1))
        assertEquals(0, projectCursor.getInt(2))
        assertEquals(1, projectCursor.getInt(3))
        assertEquals("", projectCursor.getString(4))
        assertEquals(1000L, projectCursor.getLong(5))
        assertEquals(2000L, projectCursor.getLong(6))
        assertFalse(projectCursor.moveToNext())
        // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
        projectCursor.close()

        val historyCursor =
            db.query(
                "SELECT projectId, action, previousValue, newValue, timestamp FROM counter_history WHERE id = 1",
            )
        assertTrue(historyCursor.moveToFirst())
        assertEquals(1L, historyCursor.getLong(0))
        assertEquals("increment", historyCursor.getString(1))
        assertEquals(41, historyCursor.getInt(2))
        assertEquals(42, historyCursor.getInt(3))
        assertEquals(3000L, historyCursor.getLong(4))
        assertFalse(historyCursor.moveToNext())
        historyCursor.close()

        val yarnCardsCursor = db.query("SELECT COUNT(*) FROM yarn_cards")
        assertTrue(yarnCardsCursor.moveToFirst())
        assertEquals(0, yarnCardsCursor.getInt(0))
        yarnCardsCursor.close()
        // CPD-ON

        db.close()
    }

    @Test
    fun migrate2to3() {
        val testDb = "migration-test-v2-to-v3"

        // Luo v2-tietokanta
        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Test', 42, 0, 1, 'notes', 1000, 2000)",
            )
            execSQL(
                "INSERT INTO counter_history (id, projectId, action, previousValue, newValue, timestamp) VALUES (1, 1, 'increment', 41, 42, 3000)",
            )
            close()
        }

        // Migroi v3:een — lisää uudet kentät ja sessions-taulu
        val db = helper.runMigrationsAndValidate(testDb, 3, true)

        // Varmista uudet kentät ovat olemassa ja oletusarvot toimivat
        val cursor =
            db.query(
                "SELECT name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds FROM counter_projects WHERE id = 1",
            )
        assertTrue(cursor.moveToFirst())
        assertEquals("Test", cursor.getString(0))
        assertEquals(42, cursor.getInt(1))
        assertEquals(0, cursor.getInt(2))
        assertEquals(1, cursor.getInt(3))
        assertEquals("notes", cursor.getString(4))
        assertEquals(1000L, cursor.getLong(5))
        assertEquals(2000L, cursor.getLong(6))
        assertNull(cursor.getString(7)) // sectionName = NULL
        assertTrue(cursor.isNull(8)) // stitchCount = NULL
        assertEquals(0, cursor.getInt(9)) // isCompleted = 0
        assertTrue(cursor.isNull(10)) // totalRows = NULL
        assertTrue(cursor.isNull(11)) // completedAt = NULL
        assertEquals("", cursor.getString(12)) // yarnCardIds = ""
        assertFalse(cursor.moveToNext())
        cursor.close()

        val historyCursor =
            db.query(
                "SELECT projectId, action, previousValue, newValue, timestamp FROM counter_history WHERE id = 1",
            )
        assertTrue(historyCursor.moveToFirst())
        assertEquals(1L, historyCursor.getLong(0))
        assertEquals("increment", historyCursor.getString(1))
        assertEquals(41, historyCursor.getInt(2))
        assertEquals(42, historyCursor.getInt(3))
        assertEquals(3000L, historyCursor.getLong(4))
        assertFalse(historyCursor.moveToNext())
        historyCursor.close()

        // Varmista sessions-taulu luotiin
        val sessionCursor = db.query("SELECT COUNT(*) FROM sessions")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals(0, sessionCursor.getInt(0)) // tyhjä, koska ei lisätty sessioita
        sessionCursor.close()

        db.close()
    }

    @Test
    fun migrate1to3() {
        val testDb = "migration-test-v1-to-v3"

        // Testaa koko migraatioketju v1→v2→v3
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Migration Test', 100, 5, 2, 'test notes', 1000, 2000)",
            )
            execSQL(
                "INSERT INTO counter_history (id, projectId, action, previousValue, newValue, timestamp) VALUES (1, 1, 'decrement', 101, 100, 4000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 3, true)

        val projectCursor =
            db.query(
                "SELECT name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds FROM counter_projects WHERE id = 1",
            )
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Migration Test", projectCursor.getString(0))
        assertEquals(100, projectCursor.getInt(1))
        assertEquals(5, projectCursor.getInt(2))
        assertEquals(2, projectCursor.getInt(3))
        assertEquals("test notes", projectCursor.getString(4))
        assertEquals(1000L, projectCursor.getLong(5))
        assertEquals(2000L, projectCursor.getLong(6))
        assertNull(projectCursor.getString(7))
        assertTrue(projectCursor.isNull(8))
        assertEquals(0, projectCursor.getInt(9))
        assertTrue(projectCursor.isNull(10))
        assertTrue(projectCursor.isNull(11))
        assertEquals("", projectCursor.getString(12))
        assertFalse(projectCursor.moveToNext())
        projectCursor.close()

        val historyCursor =
            db.query(
                "SELECT projectId, action, previousValue, newValue, timestamp FROM counter_history WHERE id = 1",
            )
        assertTrue(historyCursor.moveToFirst())
        assertEquals(1L, historyCursor.getLong(0))
        assertEquals("decrement", historyCursor.getString(1))
        assertEquals(101, historyCursor.getInt(2))
        assertEquals(100, historyCursor.getInt(3))
        assertEquals(4000L, historyCursor.getLong(4))
        assertFalse(historyCursor.moveToNext())
        historyCursor.close()

        val yarnCardsCursor = db.query("SELECT COUNT(*) FROM yarn_cards")
        assertTrue(yarnCardsCursor.moveToFirst())
        assertEquals(0, yarnCardsCursor.getInt(0))
        yarnCardsCursor.close()

        val sessionCursor = db.query("SELECT COUNT(*) FROM sessions")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals(0, sessionCursor.getInt(0))
        sessionCursor.close()

        db.close()
    }

    @Test
    fun migrate3to4() {
        val testDb = "migration-test-v3-to-v4"

        // Luo v3-tietokanta: projekti jolla secondaryCount > 0 ja toinen jolla 0
        helper.createDatabase(testDb, 3).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds) VALUES (1, 'With Secondary', 42, 4, 8, '', 1000, 2000, NULL, NULL, 0, NULL, NULL, '')",
            )
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds) VALUES (2, 'No Secondary', 10, 0, 1, '', 1000, 2000, NULL, NULL, 0, NULL, NULL, '')",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                4,
                true,
                KnitToolsDatabase.MIGRATION_3_4,
                // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            )

        // Varmista uudet taulut luotiin
        val reminderCursor = db.query("SELECT COUNT(*) FROM row_reminders")
        assertTrue(reminderCursor.moveToFirst())
        assertEquals(0, reminderCursor.getInt(0))
        reminderCursor.close()

        val photoCursor = db.query("SELECT COUNT(*) FROM progress_photos")
        assertTrue(photoCursor.moveToFirst())
        assertEquals(0, photoCursor.getInt(0))
        photoCursor.close()
        // CPD-ON

        // Legacy secondaryCount säilyy vain counter_projects-taulussa.
        val counterCursor =
            db.query(
                "SELECT projectId, name, count, stepSize, repeatAt, sortOrder FROM project_counters ORDER BY projectId",
            )
        assertFalse(counterCursor.moveToFirst())
        counterCursor.close()

        // Alkuperäinen data säilyi
        val projectCursor =
            db.query("SELECT name, count, secondaryCount FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("With Secondary", projectCursor.getString(0))
        assertEquals(42, projectCursor.getInt(1))
        assertEquals(4, projectCursor.getInt(2))
        projectCursor.close()

        db.close()
    }

    @Test
    fun migrate1to4() {
        val testDb = "migration-test-v1-to-v4"

        // Koko migraatioketju v1→v2→v3→v4
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Chain', 50, 3, 4, 'test', 1000, 2000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                4,
                true,
                *allMigrations,
            )

        // Projektidata säilyi koko ketjun läpi
        val projectCursor =
            db.query("SELECT name, count, secondaryCount, stepSize, yarnCardIds FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Chain", projectCursor.getString(0))
        assertEquals(50, projectCursor.getInt(1))
        assertEquals(3, projectCursor.getInt(2))
        assertEquals(4, projectCursor.getInt(3))
        assertEquals("", projectCursor.getString(4))
        projectCursor.close()

        // Legacy secondaryCount ei tuota erillistä project_counters-riviä.
        val counterCursor =
            db.query("SELECT name, count, repeatAt FROM project_counters WHERE projectId = 1")
        assertFalse(counterCursor.moveToFirst())
        counterCursor.close()

        // Kaikki uudet taulut olemassa
        val reminderCursor = db.query("SELECT COUNT(*) FROM row_reminders")
        assertTrue(reminderCursor.moveToFirst())
        assertEquals(0, reminderCursor.getInt(0))
        reminderCursor.close()

        val photoCursor = db.query("SELECT COUNT(*) FROM progress_photos")
        assertTrue(photoCursor.moveToFirst())
        assertEquals(0, photoCursor.getInt(0))
        photoCursor.close()

        db.close()
    }

    @Test
    fun migrate4to5() {
        val testDb = "migration-test-v4-to-v5"

        helper.createDatabase(testDb, 4).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds
                ) VALUES (
                    1, 'Pattern link project', 42, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, ''
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                5,
                true,
                KnitToolsDatabase.MIGRATION_4_5,
            )

        val patternCursor = db.query("SELECT COUNT(*) FROM saved_patterns")
        assertTrue(patternCursor.moveToFirst())
        assertEquals(0, patternCursor.getInt(0))
        patternCursor.close()

        val projectCursor = db.query("SELECT name, linkedPatternId FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Pattern link project", projectCursor.getString(0))
        assertTrue(projectCursor.isNull(1))
        projectCursor.close()

        db.close()
    }

    @Test
    fun migrate5to6() {
        val testDb = "migration-test-v5-to-v6"

        // Luo v5-tietokanta: yarn card + project counter (jolla repeatAt)
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Test Project', 42, 0, 1, '', 1000, 2000)",
            )
            execSQL(
                "INSERT INTO yarn_cards (id, brand, yarnName, fiberContent, weightGrams, lengthMeters, needleSize, gaugeInfo, colorName, colorNumber, dyeLot, weightCategory, careSymbols, photoUri, createdAt) VALUES (1, 'Novita', '7 Veljestä', '75% wool', '100', '200', '3.5', '', 'Red', '123', '', 'DK', 0, '', 1000)",
            )
            execSQL(
                "INSERT INTO project_counters (id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt) VALUES (1, 1, 'Pattern repeat', 4, 1, 8, 0, 1000)",
            )
            execSQL(
                "INSERT INTO project_counters (id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt) VALUES (2, 1, 'Row counter', 10, 1, NULL, 1, 1000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                6,
                true,
                KnitToolsDatabase.MIGRATION_5_6,
            )

        // Varmista yarn_cards uudet kentät ja oletusarvot
        val yarnCursor =
            db.query("SELECT brand, yarnName, quantityInStash, status, linkedProjectId FROM yarn_cards WHERE id = 1")
        assertTrue(yarnCursor.moveToFirst())
        assertEquals("Novita", yarnCursor.getString(0))
        assertEquals("7 Veljestä", yarnCursor.getString(1))
        assertEquals(1, yarnCursor.getInt(2)) // quantityInStash oletusarvo
        assertEquals("IN_STASH", yarnCursor.getString(3)) // status oletusarvo
        assertTrue(yarnCursor.isNull(4)) // linkedProjectId = NULL
        assertFalse(yarnCursor.moveToNext())
        yarnCursor.close()

        // Varmista backfill: repeatAt-laskuri → REPEATING, muut → COUNT_UP
        val counterCursor =
            db.query(
                "SELECT id, name, counterType, startingStitches, stitchChange, shapeEveryN FROM project_counters ORDER BY id",
            )
        assertTrue(counterCursor.moveToFirst())
        assertEquals(1L, counterCursor.getLong(0))
        assertEquals("Pattern repeat", counterCursor.getString(1))
        assertEquals("REPEATING", counterCursor.getString(2)) // backfill toimi
        assertTrue(counterCursor.isNull(3)) // startingStitches = NULL
        assertTrue(counterCursor.isNull(4)) // stitchChange = NULL
        assertTrue(counterCursor.isNull(5)) // shapeEveryN = NULL

        assertTrue(counterCursor.moveToNext())
        assertEquals(2L, counterCursor.getLong(0))
        assertEquals("Row counter", counterCursor.getString(1))
        assertEquals("COUNT_UP", counterCursor.getString(2)) // oletusarvo
        assertFalse(counterCursor.moveToNext())
        counterCursor.close()

        db.close()
    }

    @Test
    fun migrate1to6() {
        val testDb = "migration-test-v1-to-v6"

        // Koko migraatioketju v1→v6
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Chain v6', 50, 3, 4, 'test', 1000, 2000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                6,
                true,
                *allMigrations,
            )

        // Projektidata säilyi
        val projectCursor =
            db.query("SELECT name, count, linkedPatternId FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Chain v6", projectCursor.getString(0))
        assertEquals(50, projectCursor.getInt(1))
        assertTrue(projectCursor.isNull(2)) // linkedPatternId = NULL (v4→v5)
        projectCursor.close()

        // Legacy secondaryCount ei tuota erillistä project_counters-riviä.
        val counterCursor =
            db.query("SELECT name, count, counterType FROM project_counters WHERE projectId = 1")
        assertFalse(counterCursor.moveToFirst())
        counterCursor.close()

        // yarn_cards uudet kentät olemassa
        val yarnCursor = db.query("SELECT COUNT(*) FROM yarn_cards")
        assertTrue(yarnCursor.moveToFirst())
        assertEquals(0, yarnCursor.getInt(0))
        yarnCursor.close()

        // saved_patterns taulu olemassa (v4→v5)
        val patternCursor = db.query("SELECT COUNT(*) FROM saved_patterns")
        assertTrue(patternCursor.moveToFirst())
        assertEquals(0, patternCursor.getInt(0))
        patternCursor.close()

        db.close()
    }

    @Test
    fun migrate6to7() {
        val testDb = "migration-test-v6-to-v7"

        helper.createDatabase(testDb, 6).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds, linkedPatternId
                ) VALUES (
                    1, 'Pattern project', 42, 2, 1, 'notes', 1000, 2000,
                    'Sleeve', 120, 0, NULL, NULL, '', NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt,
                    counterType, startingStitches, stitchChange, shapeEveryN
                ) VALUES (
                    1, 1, 'Sleeve repeat', 3, 1, NULL, 0, 1000,
                    'SHAPING', 80, -2, 4
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                7,
                true,
                KnitToolsDatabase.MIGRATION_6_7,
            )

        val projectCursor =
            db.query(
                """
                SELECT
                    patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch
                FROM counter_projects WHERE id = 1
                """.trimIndent(),
            )
        assertTrue(projectCursor.moveToFirst())
        assertTrue(projectCursor.isNull(0))
        assertTrue(projectCursor.isNull(1))
        assertEquals(0, projectCursor.getInt(2))
        assertTrue(projectCursor.isNull(3))
        assertEquals(0, projectCursor.getInt(4))
        assertEquals(0, projectCursor.getInt(5))
        projectCursor.close()

        val counterCursor =
            db.query(
                """
                SELECT repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
                FROM project_counters WHERE id = 1
                """.trimIndent(),
            )
        assertTrue(counterCursor.moveToFirst())
        assertTrue(counterCursor.isNull(0))
        assertTrue(counterCursor.isNull(1))
        assertTrue(counterCursor.isNull(2))
        // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
        assertTrue(counterCursor.isNull(3))
        counterCursor.close()

        val annotationCursor = db.query("SELECT COUNT(*) FROM pattern_annotations")
        assertTrue(annotationCursor.moveToFirst())
        assertEquals(0, annotationCursor.getInt(0))
        annotationCursor.close()
        // CPD-ON

        db.close()
    }

    @Test
    fun migrate1to7() {
        val testDb = "migration-test-v1-to-v7"

        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Chain v7', 50, 3, 4, 'test', 1000, 2000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                7,
                true,
                *allMigrations,
            )

        val projectCursor =
            db.query(
                """
                SELECT name, count, linkedPatternId, patternUri, stitchTrackingEnabled, currentStitch
                FROM counter_projects WHERE id = 1
                """.trimIndent(),
            )
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Chain v7", projectCursor.getString(0))
        assertEquals(50, projectCursor.getInt(1))
        assertTrue(projectCursor.isNull(2))
        assertTrue(projectCursor.isNull(3))
        assertEquals(0, projectCursor.getInt(4))
        assertEquals(0, projectCursor.getInt(5))
        projectCursor.close()

        val counterCursor =
            db.query(
                """
                SELECT name, count, counterType, repeatStartRow, currentRepeat
                FROM project_counters WHERE projectId = 1
                """.trimIndent(),
            )
        assertFalse(counterCursor.moveToFirst())
        counterCursor.close()

        val annotationCursor = db.query("SELECT COUNT(*) FROM pattern_annotations")
        assertTrue(annotationCursor.moveToFirst())
        assertEquals(0, annotationCursor.getInt(0))
        annotationCursor.close()

        db.close()
    }

    @Test
    fun migrate7to8() {
        val testDb = "migration-test-v7-to-v8"

        helper.createDatabase(testDb, 7).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch
                ) VALUES (
                    1, 'Target row project', 10, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '',
                    NULL, NULL, NULL, 0, NULL,
                    0, 0
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                8,
                true,
                KnitToolsDatabase.MIGRATION_7_8,
            )

        val projectCursor = db.query("SELECT name, targetRows FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Target row project", projectCursor.getString(0))
        assertTrue(projectCursor.isNull(1))
        projectCursor.close()

        db.close()
    }

    @Test
    fun migrate8to9() {
        val testDb = "migration-test-v8-to-v9"

        helper.createDatabase(testDb, 8).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch, targetRows
                ) VALUES (
                    1, 'Indexed project', 10, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '',
                    NULL, NULL, NULL, 0, NULL,
                    0, 0, 72
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow, durationMinutes
                ) VALUES (
                    1, 1, 1000, 2000, 1, 5, 30
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                9,
                true,
                KnitToolsDatabase.MIGRATION_8_9,
            )

        assertSingleRow(
            db,
            "SELECT name, targetRows FROM counter_projects WHERE id = 1",
        ) {
            assertEquals("Indexed project", getString(0))
            assertEquals(72, getInt(1))
        }
        assertSingleRow(
            db,
            "SELECT projectId, startedAt, endedAt, startRow, endRow, durationMinutes FROM sessions WHERE id = 1",
        ) {
            assertEquals(1L, getLong(0))
            assertEquals(1000L, getLong(1))
            assertEquals(2000L, getLong(2))
            assertEquals(1, getInt(3))
            assertEquals(5, getInt(4))
            assertEquals(30, getInt(5))
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate9to10BackfillsSessionSecondsAndRowsWorked() {
        val testDb = "migration-test-v9-to-v10"

        helper.createDatabase(testDb, 9).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch, targetRows
                ) VALUES (
                    1, 'Session timing project', 10, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '',
                    NULL, NULL, NULL, 0, NULL,
                    0, 0, 72
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow, durationMinutes
                ) VALUES (
                    1, 1, 1000, 2000, 4, 9, 12
                )
                """.trimIndent(),
            )
            close()
        }

        // CPD-OFF: Migraation versionkohtainen asetelma ja tarkistus pidetaan yhdessa.
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                10,
                true,
                KnitToolsDatabase.MIGRATION_9_10,
            )

        assertSingleRow(
            db,
            "SELECT durationSeconds, rowsWorked FROM sessions WHERE id = 1",
        ) {
            assertEquals(720L, getLong(0))
            assertEquals(5, getInt(1))
            // CPD-ON
        }

        db.close()
    }

    @Test
    fun migrate9to10ClampsRowsWorkedOverflowToIntMax() {
        val testDb = "migration-test-v9-to-v10-rows-overflow"

        helper.createDatabase(testDb, 9).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch, targetRows
                ) VALUES (
                    1, 'Overflow session project', 10, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '',
                    NULL, NULL, NULL, 0, NULL,
                    0, 0, 72
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow, durationMinutes
                ) VALUES (
                    1, 1, 1000, 2000, ${Int.MIN_VALUE}, ${Int.MAX_VALUE}, 12
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                10,
                true,
                KnitToolsDatabase.MIGRATION_9_10,
            )

        assertSingleRow(
            db,
            "SELECT durationSeconds, rowsWorked FROM sessions WHERE id = 1",
        ) {
            assertEquals(720L, getLong(0))
            assertEquals(Int.MAX_VALUE, getInt(1))
        }

        db.close()
    }

    @Test
    fun migrate2toLatestPreservesYarnCards() {
        val testDb = "migration-test-v2-to-latest"

        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Yarn project', 12, 0, 1, 'notes', 1000, 2000)",
            )
            execSQL(
                "INSERT INTO counter_history (id, projectId, action, previousValue, newValue, timestamp) VALUES (1, 1, 'increment', 11, 12, 3000)",
            )
            execSQL(
                """
                INSERT INTO yarn_cards (
                    id, brand, yarnName, fiberContent, weightGrams, lengthMeters,
                    needleSize, gaugeInfo, colorName, colorNumber, dyeLot,
                    weightCategory, careSymbols, photoUri, createdAt
                ) VALUES (
                    1, 'Novita', 'Nalle', '75% wool 25% polyamide', '100', '260',
                    '3.0', '22 sts', 'Blue', '170', 'A1',
                    'Sport', 5, 'content://yarn/1', 1234
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            """
            SELECT brand, yarnName, fiberContent, weightGrams, lengthMeters,
                needleSize, gaugeInfo, colorName, colorNumber, dyeLot,
                weightCategory, careSymbols, photoUri, createdAt,
                quantityInStash, status, linkedProjectId
            FROM yarn_cards WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("Novita", getString(0))
            assertEquals("Nalle", getString(1))
            assertEquals("75% wool 25% polyamide", getString(2))
            assertEquals("100", getString(3))
            assertEquals("260", getString(4))
            assertEquals("3.0", getString(5))
            assertEquals("22 sts", getString(6))
            assertEquals("Blue", getString(7))
            assertEquals("170", getString(8))
            assertEquals("A1", getString(9))
            assertEquals("Sport", getString(10))
            assertEquals(5L, getLong(11))
            assertEquals("content://yarn/1", getString(12))
            assertEquals(1234L, getLong(13))
            assertEquals(1, getInt(14))
            assertEquals("IN_STASH", getString(15))
            assertTrue(isNull(16))
        }
        assertSingleRow(
            db,
            "SELECT action, previousValue, newValue, timestamp FROM counter_history WHERE id = 1",
        ) {
            assertEquals("increment", getString(0))
            assertEquals(11, getInt(1))
            assertEquals(12, getInt(2))
            assertEquals(3000L, getLong(3))
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate3toLatestPreservesSessionsAndLegacySecondaryCounter() {
        val testDb = "migration-test-v3-to-latest"

        helper.createDatabase(testDb, 3).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds
                ) VALUES (
                    1, 'Session project', 44, 2, 6, 'session notes', 1000, 2000,
                    'Body', 88, 1, 120, 3000, '4,5'
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, projectId, startedAt, endedAt, startRow, endRow, durationMinutes
                ) VALUES (
                    1, 1, 4000, 4600, 12, 18, 10
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            """
            SELECT sectionName, stitchCount, isCompleted, totalRows, completedAt,
                yarnCardIds, targetRows
            FROM counter_projects WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("Body", getString(0))
            assertEquals(88, getInt(1))
            assertEquals(1, getInt(2))
            assertEquals(120, getInt(3))
            assertEquals(3000L, getLong(4))
            assertEquals("4,5", getString(5))
            assertTrue(isNull(6))
        }
        assertSingleRow(
            db,
            """
            SELECT projectId, startedAt, endedAt, startRow, endRow, durationMinutes,
                durationSeconds, rowsWorked
            FROM sessions WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals(1L, getLong(0))
            assertEquals(4000L, getLong(1))
            assertEquals(4600L, getLong(2))
            assertEquals(12, getInt(3))
            assertEquals(18, getInt(4))
            assertEquals(10, getInt(5))
            assertEquals(600L, getLong(6))
            assertEquals(6, getInt(7))
        }
        val counterCursor =
            db.query(
                "SELECT name, count, stepSize, repeatAt, counterType FROM project_counters WHERE projectId = 1",
            )
        assertFalse(counterCursor.moveToFirst())
        counterCursor.close()
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate4toLatestPreservesReminderPhotoAndCounterRows() {
        val testDb = "migration-test-v4-to-latest"

        helper.createDatabase(testDb, 4).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds
                ) VALUES (
                    1, 'Feature project', 20, 0, 2, 'feature notes', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, ''
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO row_reminders (
                    id, projectId, targetRow, repeatInterval, message, isCompleted, createdAt
                ) VALUES (
                    1, 1, 25, 5, 'Check cable', 0, 3000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO progress_photos (
                    id, projectId, photoUri, rowNumber, note, createdAt
                ) VALUES (
                    1, 1, 'content://photos/1', 24, 'Front panel', 4000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt
                ) VALUES (
                    1, 1, 'Sleeve repeats', 6, 2, 12, 3, 5000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            "SELECT targetRow, repeatInterval, message, isCompleted, createdAt FROM row_reminders WHERE id = 1",
        ) {
            assertEquals(25, getInt(0))
            assertEquals(5, getInt(1))
            assertEquals("Check cable", getString(2))
            assertEquals(0, getInt(3))
            assertEquals(3000L, getLong(4))
        }
        assertSingleRow(
            db,
            "SELECT photoUri, rowNumber, note, createdAt FROM progress_photos WHERE id = 1",
        ) {
            assertEquals("content://photos/1", getString(0))
            assertEquals(24, getInt(1))
            assertEquals("Front panel", getString(2))
            assertEquals(4000L, getLong(3))
        }
        assertSingleRow(
            db,
            """
            SELECT name, count, stepSize, repeatAt, sortOrder, createdAt,
                counterType, startingStitches, stitchChange, shapeEveryN
            FROM project_counters WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("Sleeve repeats", getString(0))
            assertEquals(6, getInt(1))
            assertEquals(2, getInt(2))
            assertEquals(12, getInt(3))
            assertEquals(3, getInt(4))
            assertEquals(5000L, getLong(5))
            assertEquals("REPEATING", getString(6))
            assertTrue(isNull(7))
            assertTrue(isNull(8))
            assertTrue(isNull(9))
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate5toLatestPreservesSavedPatternsAndProjectLinks() {
        val testDb = "migration-test-v5-to-latest"

        helper.createDatabase(testDb, 5).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt,
                    yarnCardIds, linkedPatternId
                ) VALUES (
                    1, 'Saved pattern project', 30, 0, 1, 'pattern notes', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '', 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO saved_patterns (
                    id, ravelryId, name, designerName, thumbnailUrl, difficulty,
                    gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                    isFree, patternUrl, savedAt
                ) VALUES (
                    1, 9001, 'Cable Socks', 'Test Designer', 'https://example.test/thumb.jpg', 3.5,
                    28.0, 36.0, '2.5 mm', 'Fingering', 420,
                    0, 'https://example.test/pattern', 6000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            "SELECT name, linkedPatternId FROM counter_projects WHERE id = 1",
        ) {
            assertEquals("Saved pattern project", getString(0))
            assertEquals(1L, getLong(1))
        }
        assertSingleRow(
            db,
            """
            SELECT source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                availability, originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("RAVELRY", getString(0))
            assertEquals(9001, getInt(1))
            assertEquals("Cable Socks", getString(2))
            assertEquals("Test Designer", getString(3))
            assertEquals("https://example.test/thumb.jpg", getString(4))
            assertEquals(3.5, getDouble(5), 0.0)
            assertEquals(28.0, getDouble(6), 0.0)
            assertEquals(36.0, getDouble(7), 0.0)
            assertEquals("2.5 mm", getString(8))
            assertEquals("Fingering", getString(9))
            assertEquals(420, getInt(10))
            assertEquals("unknown", getString(11))
            assertEquals("https://example.test/pattern", getString(12))
            assertEquals("https://example.test/pattern", getString(13))
            assertTrue(isNull(14))
            assertEquals(0, getInt(15))
            assertEquals(6000L, getLong(16))
            assertEquals(6000L, getLong(17))
            assertTrue(isNull(18))
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate13toLatestBackfillsSavedPatternSourceMetadata() {
        val testDb = "migration-test-v13-to-latest"

        helper.createDatabase(testDb, 13).apply {
            execSQL(
                """
                INSERT INTO saved_patterns (
                    id, ravelryId, name, designerName, thumbnailUrl, difficulty,
                    gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                    isFree, patternUrl, savedAt
                ) VALUES (
                    10, 9001, 'Ravelry pattern', 'Designer', NULL, NULL,
                    NULL, NULL, NULL, NULL, NULL, 1, 'https://www.ravelry.com/patterns/library/test', 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO saved_patterns (
                    id, ravelryId, name, designerName, thumbnailUrl, difficulty,
                    gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                    isFree, patternUrl, savedAt
                ) VALUES (
                    11, 0, 'Local pattern', 'Imported', NULL, NULL,
                    NULL, NULL, NULL, NULL, NULL, 1, 'content://patterns/local.pdf', 2000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO saved_patterns (
                    id, ravelryId, name, designerName, thumbnailUrl, difficulty,
                    gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                    isFree, patternUrl, savedAt
                ) VALUES (
                    12, 0, 'Other pattern', 'Unknown', NULL, NULL,
                    NULL, NULL, NULL, NULL, NULL, 1, 'https://example.test/other', 3000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            """
            SELECT id, source, ravelryPatternId, originalUrl, canonicalUrl, localPdfUri,
                isAvailableOffline, savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns WHERE id = 10
            """.trimIndent(),
        ) {
            assertEquals(10L, getLong(0))
            assertEquals("RAVELRY", getString(1))
            assertEquals(9001, getInt(2))
            assertEquals("https://www.ravelry.com/patterns/library/test", getString(3))
            assertEquals("https://www.ravelry.com/patterns/library/test", getString(4))
            assertTrue(isNull(5))
            assertEquals(0, getInt(6))
            assertEquals(1000L, getLong(7))
            assertEquals(1000L, getLong(8))
            assertTrue(isNull(9))
        }
        assertSingleRow(
            db,
            """
            SELECT id, source, ravelryPatternId, originalUrl, canonicalUrl, localPdfUri,
                isAvailableOffline, savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns WHERE id = 11
            """.trimIndent(),
        ) {
            assertEquals(11L, getLong(0))
            assertEquals("LOCAL_FILE", getString(1))
            assertTrue(isNull(2))
            assertEquals("content://patterns/local.pdf", getString(3))
            assertEquals("", getString(4))
            assertEquals("content://patterns/local.pdf", getString(5))
            assertEquals(1, getInt(6))
            assertEquals(2000L, getLong(7))
            assertEquals(2000L, getLong(8))
            assertTrue(isNull(9))
        }
        assertSingleRow(
            db,
            """
            SELECT id, source, ravelryPatternId, originalUrl, canonicalUrl, localPdfUri,
                isAvailableOffline, savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns WHERE id = 12
            """.trimIndent(),
        ) {
            assertEquals(12L, getLong(0))
            assertEquals("OTHER", getString(1))
            assertTrue(isNull(2))
            assertEquals("https://example.test/other", getString(3))
            assertEquals("", getString(4))
            assertTrue(isNull(5))
            assertEquals(0, getInt(6))
            assertEquals(3000L, getLong(7))
            assertEquals(3000L, getLong(8))
            assertTrue(isNull(9))
        }

        db.close()
    }

    @Test
    fun migrate6toLatestPreservesStashAndShapingValues() {
        val testDb = "migration-test-v6-to-latest"

        helper.createDatabase(testDb, 6).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt,
                    yarnCardIds, linkedPatternId
                ) VALUES (
                    1, 'Stash project', 18, 0, 1, '', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '', NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO yarn_cards (
                    id, brand, yarnName, fiberContent, weightGrams, lengthMeters,
                    needleSize, gaugeInfo, colorName, colorNumber, dyeLot,
                    weightCategory, careSymbols, photoUri, createdAt,
                    quantityInStash, status, linkedProjectId
                ) VALUES (
                    1, 'Istex', 'Lettlopi', '100% wool', '50', '100',
                    '4.5', '18 sts', 'Moss', '9423', 'D2',
                    'Aran', 9, 'content://yarn/6', 3000,
                    7, 'ASSIGNED', 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt,
                    counterType, startingStitches, stitchChange, shapeEveryN
                ) VALUES (
                    1, 1, 'Waist shaping', 4, 1, NULL, 0, 4000,
                    'SHAPING', 100, -2, 6
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            "SELECT quantityInStash, status, linkedProjectId FROM yarn_cards WHERE id = 1",
        ) {
            assertEquals(7, getInt(0))
            assertEquals("ASSIGNED", getString(1))
            assertEquals(1L, getLong(2))
        }
        assertSingleRow(
            db,
            """
            SELECT counterType, startingStitches, stitchChange, shapeEveryN,
                repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
            FROM project_counters WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("SHAPING", getString(0))
            assertEquals(100, getInt(1))
            assertEquals(-2, getInt(2))
            assertEquals(6, getInt(3))
            assertTrue(isNull(4))
            assertTrue(isNull(5))
            assertTrue(isNull(6))
            assertTrue(isNull(7))
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate11to12AddsProjectYarnNotesWithoutChangingExistingProjectData() {
        val testDb = "migration-test-v11-to-v12"

        helper.createDatabase(testDb, 11).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch, targetRows
                ) VALUES (
                    1, 'Project yarn migration', 22, 2, 1, 'notes', 1000, 2000,
                    'Sleeve', 64, 0, NULL, NULL, '1',
                    NULL, 'content://pattern.pdf', 'Pattern.pdf', 4, '{"10":2}',
                    1, 12, 80
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO yarn_cards (
                    id, brand, yarnName, fiberContent, weightGrams, lengthMeters,
                    needleSize, gaugeInfo, colorName, colorNumber, dyeLot,
                    weightCategory, careSymbols, photoUri, createdAt,
                    quantityInStash, status, linkedProjectId
                ) VALUES (
                    1, 'Istex', 'Lettlopi', '100% wool', '50', '100',
                    '4.5', '18 sts', 'Moss', '9423', 'D2',
                    'Aran', 9, 'content://yarn/11', 3000,
                    7, 'IN_USE', 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt,
                    counterType, startingStitches, stitchChange, shapeEveryN,
                    repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
                ) VALUES (
                    1, 1, 'Sleeve repeats', 4, 1, NULL, 0, 4000,
                    'REPEAT_SECTION', NULL, NULL, NULL,
                    10, 18, 6, 2
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO row_reminders (
                    id, projectId, targetRow, repeatInterval, message, isCompleted, createdAt
                ) VALUES (
                    1, 1, 25, NULL, 'Try on', 0, 5000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO progress_photos (
                    id, projectId, photoUri, rowNumber, note, createdAt
                ) VALUES (
                    1, 1, 'content://photo/11', 20, 'Halfway', 6000
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                12,
                true,
                KnitToolsDatabase.MIGRATION_11_12,
            )

        assertSingleRow(
            db,
            """
            SELECT name, count, secondaryCount, sectionName, stitchCount, yarnCardIds,
                patternName, currentPatternPage, stitchTrackingEnabled, currentStitch, targetRows
            FROM counter_projects WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("Project yarn migration", getString(0))
            assertEquals(22, getInt(1))
            assertEquals(2, getInt(2))
            assertEquals("Sleeve", getString(3))
            assertEquals(64, getInt(4))
            assertEquals("1", getString(5))
            assertEquals("Pattern.pdf", getString(6))
            assertEquals(4, getInt(7))
            assertEquals(1, getInt(8))
            assertEquals(12, getInt(9))
            assertEquals(80, getInt(10))
        }
        assertSingleRow(
            db,
            "SELECT quantityInStash, status, linkedProjectId FROM yarn_cards WHERE id = 1",
        ) {
            assertEquals(7, getInt(0))
            assertEquals("IN_USE", getString(1))
            assertEquals(1L, getLong(2))
        }
        assertSingleRow(db, "SELECT COUNT(*) FROM project_counters WHERE projectId = 1") {
            assertEquals(1, getInt(0))
        }
        assertSingleRow(db, "SELECT COUNT(*) FROM row_reminders WHERE projectId = 1") {
            assertEquals(1, getInt(0))
        }
        assertSingleRow(db, "SELECT COUNT(*) FROM progress_photos WHERE projectId = 1") {
            assertEquals(1, getInt(0))
        }
        assertSingleRow(db, "SELECT COUNT(*) FROM project_yarn_notes") {
            assertEquals(0, getInt(0))
        }
        assertProjectYarnNoteIndexExists(db)

        db.close()
    }

    @Test
    fun migrate12to13AddsFeatureDecisionColumnsWithDefaults() {
        val testDb = "migration-test-v12-to-v13"

        helper.createDatabase(testDb, 12).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch, targetRows
                ) VALUES (
                    1, 'Legacy rows project', 22, 2, 1, 'notes', 1000, 2000,
                    'Sleeve', 64, 0, NULL, NULL, '1',
                    NULL, 'content://pattern.pdf', 'Pattern.pdf', 4, '{"10":2}',
                    1, 12, 80
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt,
                    counterType, startingStitches, stitchChange, shapeEveryN,
                    repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
                ) VALUES (
                    1, 1, 'Sleeve repeats', 4, 1, NULL, 0, 4000,
                    'COUNT_UP', NULL, NULL, NULL,
                    NULL, NULL, NULL, NULL
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                13,
                true,
                KnitToolsDatabase.MIGRATION_12_13,
            )

        assertSingleRow(
            db,
            """
            SELECT craftType, mainCounterLabelType, mainCounterCustomLabel,
                readingLineEnabled, readingLineYFraction
            FROM counter_projects WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("KNITTING", getString(0))
            assertEquals("ROWS", getString(1))
            assertTrue(isNull(2))
            assertEquals(0, getInt(3))
            assertEquals(0.5, getDouble(4), 0.0)
        }
        assertSingleRow(db, "SELECT linkedToMainCounter FROM project_counters WHERE id = 1") {
            assertEquals(0, getInt(0))
        }

        db.close()
    }

    @Test
    fun migrate7toLatestPreservesPatternViewerData() {
        val testDb = "migration-test-v7-to-latest"

        helper.createDatabase(testDb, 7).apply {
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds,
                    linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                    stitchTrackingEnabled, currentStitch
                ) VALUES (
                    1, 'Pattern viewer project', 52, 0, 1, 'viewer notes', 1000, 2000,
                    NULL, NULL, 0, NULL, NULL, '',
                    NULL, 'content://patterns/socks.pdf', 'Socks.pdf', 3, '{"10":2}',
                    1, 17
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO project_counters (
                    id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt,
                    counterType, startingStitches, stitchChange, shapeEveryN,
                    repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
                ) VALUES (
                    1, 1, 'Chart repeat', 5, 1, NULL, 0, 3000,
                    'REPEATING', NULL, NULL, NULL,
                    10, 20, 4, 2
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO pattern_annotations (
                    id, projectId, page, pathData, color, strokeWidth, createdAt
                ) VALUES (
                    1, 1, 3, 'M 0 0 L 10 10', '#FF0000', 2.5, 4000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = migrateToLatest(testDb)

        assertSingleRow(
            db,
            """
            SELECT patternUri, patternName, currentPatternPage, patternRowMapping,
                stitchTrackingEnabled, currentStitch, targetRows
            FROM counter_projects WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("content://patterns/socks.pdf", getString(0))
            assertEquals("Socks.pdf", getString(1))
            assertEquals(3, getInt(2))
            assertEquals("""{"10":2}""", getString(3))
            assertEquals(1, getInt(4))
            assertEquals(17, getInt(5))
            assertTrue(isNull(6))
        }
        assertSingleRow(
            db,
            """
            SELECT repeatStartRow, repeatEndRow, totalRepeats, currentRepeat
            FROM project_counters WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals(10, getInt(0))
            assertEquals(20, getInt(1))
            assertEquals(4, getInt(2))
            assertEquals(2, getInt(3))
        }
        assertSingleRow(
            db,
            """
            SELECT layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            FROM pattern_annotations WHERE id = 1
            """.trimIndent(),
        ) {
            assertMigratedLegacyFreehand(
                layerId = getLong(0),
                page = getInt(1),
                kind = getString(2),
                payloadVersion = getInt(3),
                payloadJson = getString(4),
                zIndex = getLong(5),
                createdAt = getLong(6),
                updatedAt = getLong(7),
                expectedPathData = "M 0 0 L 10 10",
                expectedColor = "#FF0000",
                expectedStrokeWidth = 2.5f,
            )
        }
        assertStartedAtIndexExists(db)

        db.close()
    }

    @Test
    fun migrate1toLatest() {
        val testDb = "migration-test-v1-to-latest"

        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Chain latest', 50, 3, 4, 'test', 1000, 2000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                latestVersion,
                true,
                *allMigrations,
            )

        val projectCursor = db.query("SELECT name, targetRows FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Chain latest", projectCursor.getString(0))
        assertTrue(projectCursor.isNull(1))
        projectCursor.close()

        assertStartedAtIndexExists(db)

        db.close()
    }
}
