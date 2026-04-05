package com.finnvek.knittools.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testaa Room AutoMigration v1→v2→v3.
 * Varmistaa ettei migraatio kaada tietokantaa olemassa olevalla datalla.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
        )

    @Test
    fun migrate1to2() {
        // Luo v1-tietokanta testidatalla
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Test', 42, 0, 1, '', 1000, 2000)",
            )
            close()
        }

        // Migroi v2:een — AutoMigration
        helper.runMigrationsAndValidate(testDb, 2, true)
    }

    @Test
    fun migrate2to3() {
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
                "SELECT sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds FROM counter_projects WHERE id = 1",
            )
        cursor.moveToFirst()
        assert(cursor.isNull(0)) // sectionName = NULL
        assert(cursor.isNull(1)) // stitchCount = NULL
        assert(cursor.getInt(2) == 0) // isCompleted = 0
        assert(cursor.isNull(3)) // totalRows = NULL
        assert(cursor.isNull(4)) // completedAt = NULL
        assert(cursor.getString(5) == "") // yarnCardIds = ""
        cursor.close()

        // Varmista sessions-taulu luotiin
        val sessionCursor = db.query("SELECT COUNT(*) FROM sessions")
        sessionCursor.moveToFirst()
        assert(sessionCursor.getInt(0) == 0) // tyhjä, koska ei lisätty sessioita
        sessionCursor.close()

        db.close()
    }

    @Test
    fun migrate1to3() {
        // Testaa koko migraatioketju v1→v2→v3
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt) VALUES (1, 'Full Migration Test', 100, 5, 2, 'test notes', 1000, 2000)",
            )
            close()
        }

        helper.runMigrationsAndValidate(testDb, 3, true).close()
    }
}
