package com.finnvek.knittools.data.local

import androidx.room.testing.MigrationTestHelper
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
 * Testaa Room-migraatiot v1→v9.
 * v1→v3: AutoMigration. v3→v9: manuaaliset muutokset.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
        )

    private val allMigrations =
        arrayOf(
            KnitToolsDatabase.MIGRATION_3_4,
            KnitToolsDatabase.MIGRATION_4_5,
            KnitToolsDatabase.MIGRATION_5_6,
            KnitToolsDatabase.MIGRATION_6_7,
            KnitToolsDatabase.MIGRATION_7_8,
            KnitToolsDatabase.MIGRATION_8_9,
        )

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

        // Varmista backfill: projekti 1 (secondaryCount=4) tuotti project_counters-rivin
        val counterCursor =
            db.query(
                "SELECT projectId, name, count, stepSize, repeatAt, sortOrder FROM project_counters ORDER BY projectId",
            )
        assertTrue(counterCursor.moveToFirst())
        assertEquals(1L, counterCursor.getLong(0)) // projectId
        assertEquals("Pattern repeat", counterCursor.getString(1)) // name
        assertEquals(4, counterCursor.getInt(2)) // count = secondaryCount
        assertEquals(8, counterCursor.getInt(3)) // stepSize
        assertEquals(8, counterCursor.getInt(4)) // repeatAt = stepSize
        assertEquals(0, counterCursor.getInt(5)) // sortOrder
        // Ei toista riviä — projekti 2 (secondaryCount=0) ei tuottanut backfilliä
        assertFalse(counterCursor.moveToNext())
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

        // Backfill tuotti laskurin
        val counterCursor =
            db.query("SELECT name, count, repeatAt FROM project_counters WHERE projectId = 1")
        assertTrue(counterCursor.moveToFirst())
        assertEquals("Pattern repeat", counterCursor.getString(0))
        assertEquals(3, counterCursor.getInt(1))
        assertEquals(4, counterCursor.getInt(2))
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

        // Backfill v3→v4 tuotti laskurin, v5→v6 asetti sen REPEATING-tyypiksi
        val counterCursor =
            db.query("SELECT name, count, counterType FROM project_counters WHERE projectId = 1")
        assertTrue(counterCursor.moveToFirst())
        assertEquals("Pattern repeat", counterCursor.getString(0))
        assertEquals(3, counterCursor.getInt(1))
        assertEquals("REPEATING", counterCursor.getString(2))
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
        assertTrue(counterCursor.isNull(3))
        counterCursor.close()

        val annotationCursor = db.query("SELECT COUNT(*) FROM pattern_annotations")
        assertTrue(annotationCursor.moveToFirst())
        assertEquals(0, annotationCursor.getInt(0))
        annotationCursor.close()

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
        assertTrue(counterCursor.moveToFirst())
        assertEquals("Pattern repeat", counterCursor.getString(0))
        assertEquals(3, counterCursor.getInt(1))
        assertEquals("REPEATING", counterCursor.getString(2))
        assertTrue(counterCursor.isNull(3))
        assertTrue(counterCursor.isNull(4))
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
                    0, 0, NULL
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

        val indexCursor = db.query("PRAGMA index_list('sessions')")
        var hasStartedAtIndex = false
        while (indexCursor.moveToNext()) {
            if (indexCursor.getString(1) == "index_sessions_startedAt") {
                hasStartedAtIndex = true
            }
        }
        indexCursor.close()
        assertTrue(hasStartedAtIndex)

        db.close()
    }

    @Test
    fun migrate1to9() {
        val testDb = "migration-test-v1-to-v9"

        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Chain v9', 50, 3, 4, 'test', 1000, 2000)",
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                9,
                true,
                *allMigrations,
            )

        val projectCursor = db.query("SELECT name, targetRows FROM counter_projects WHERE id = 1")
        assertTrue(projectCursor.moveToFirst())
        assertEquals("Full Chain v9", projectCursor.getString(0))
        assertTrue(projectCursor.isNull(1))
        projectCursor.close()

        val indexCursor = db.query("PRAGMA index_list('sessions')")
        var hasStartedAtIndex = false
        while (indexCursor.moveToNext()) {
            if (indexCursor.getString(1) == "index_sessions_startedAt") {
                hasStartedAtIndex = true
            }
        }
        indexCursor.close()
        assertTrue(hasStartedAtIndex)

        db.close()
    }
}
