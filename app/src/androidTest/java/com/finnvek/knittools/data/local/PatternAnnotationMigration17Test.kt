package com.finnvek.knittools.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternAnnotationMigration17Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
        )

    @Test
    fun migrate16to17PreservesLegacyAnnotationInVersionedProjectLayer() {
        val testDb = "migration-test-v16-to-v17-pattern-annotation"
        helper.createDatabase(testDb, 16).apply {
            insertProject(id = 7L, linkedPatternId = 12L, patternUri = "content://pattern.pdf")
            insertSavedPattern(id = 12L)
            execSQL(
                """
                INSERT INTO pattern_annotations (
                    id, projectId, page, pathData, color, strokeWidth, createdAt
                ) VALUES (
                    81, 7, 3, 'M 0 0 L 10 10', '#FFAA00', 4.5, 1700000701
                )
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                17,
                true,
                KnitToolsDatabase.MIGRATION_16_17,
            )

        db
            .query(
                """
                SELECT projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                FROM pattern_annotation_layers
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7L, cursor.getLong(0))
                assertTrue(cursor.isNull(1))
                assertEquals("saved:12:v1", cursor.getString(2))
                assertEquals(1, cursor.getInt(3))
                assertEquals(1_700_000_701L, cursor.getLong(4))
                assertEquals(1_700_000_701L, cursor.getLong(5))
                assertFalse(cursor.moveToNext())
            }
        db
            .query(
                """
                SELECT id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
                FROM pattern_annotations
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(81L, cursor.getLong(0))
                assertMigratedLegacyFreehand(
                    layerId = cursor.getLong(1),
                    page = cursor.getInt(2),
                    kind = cursor.getString(3),
                    payloadVersion = cursor.getInt(4),
                    payloadJson = cursor.getString(5),
                    zIndex = cursor.getLong(6),
                    createdAt = cursor.getLong(7),
                    updatedAt = cursor.getLong(8),
                    expectedPathData = "M 0 0 L 10 10",
                    expectedColor = "#FFAA00",
                    expectedStrokeWidth = 4.5f,
                )
                assertFalse(cursor.moveToNext())
            }
        db.close()
    }

    @Test
    fun migrate16to17UsesStableLegacyKeyWhenSavedPatternIsUnknown() {
        val testDb = "migration-test-v16-to-v17-legacy-key"
        helper.createDatabase(testDb, 16).apply {
            insertProject(id = 9L, linkedPatternId = null, patternUri = null)
            execSQL(
                """
                INSERT INTO pattern_annotations (
                    id, projectId, page, pathData, color, strokeWidth, createdAt
                ) VALUES (1, 9, 0, 'invalid legacy path', 'not-a-color', 2.0, 5000)
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                17,
                true,
                KnitToolsDatabase.MIGRATION_16_17,
            )

        assertSingleString(db, "SELECT documentKey FROM pattern_annotation_layers", "legacy-project:9")
        assertEquals(1, countRows(db, "pattern_annotations"))
        db.close()
    }

    @Test
    fun migrate16to17CreatesActiveLayerForAttachedPdfWithoutLegacyAnnotations() {
        val testDb = "migration-test-v16-to-v17-attached-pdf-without-annotations"
        helper.createDatabase(testDb, 16).apply {
            insertProject(id = 10L, linkedPatternId = 12L, patternUri = "content://pattern.pdf")
            insertSavedPattern(id = 12L)
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                17,
                true,
                KnitToolsDatabase.MIGRATION_16_17,
            )

        db
            .query(
                """
                SELECT projectId, documentKey, isActive, createdAt, updatedAt
                FROM pattern_annotation_layers
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(10L, cursor.getLong(0))
                assertEquals("saved:12:v1", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(1_000L, cursor.getLong(3))
                assertEquals(1_000L, cursor.getLong(4))
                assertFalse(cursor.moveToNext())
            }
        assertEquals(0, countRows(db, "pattern_annotations"))
        db.close()
    }

    @Test
    fun schema17EnforcesOwnersUniqueDocumentsActiveProjectAndCascadeCleanup() {
        val testDb = "migration-test-v17-pattern-invariants"
        helper.createDatabase(testDb, 16).close()
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                17,
                true,
                KnitToolsDatabase.MIGRATION_16_17,
            )
        db.execSQL("PRAGMA foreign_keys = ON")
        db.insertProject(id = 7L, linkedPatternId = null, patternUri = null)
        db.insertSavedPattern(id = 12L)
        db.execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (1, 7, NULL, 'legacy-project:7', 1, 1000, 1000)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO pattern_annotations (
                id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            ) VALUES (1, 1, 0, 'FREEHAND', 1, '{}', 0, 1000, 1000)
            """.trimIndent(),
        )

        assertConstraintViolation {
            db.execSQL(
                """
                INSERT INTO pattern_annotation_layers (
                    projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                ) VALUES (7, NULL, 'legacy-project:7', 0, 1000, 1000)
                """.trimIndent(),
            )
        }
        assertConstraintViolation {
            db.execSQL(
                """
                INSERT INTO pattern_annotation_layers (
                    projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                ) VALUES (7, NULL, 'another-document', 1, 1000, 1000)
                """.trimIndent(),
            )
        }
        assertConstraintViolation {
            db.execSQL(
                """
                INSERT INTO pattern_annotation_layers (
                    projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                ) VALUES (NULL, NULL, 'ownerless', 0, 1000, 1000)
                """.trimIndent(),
            )
        }
        assertConstraintViolation {
            db.execSQL(
                """
                INSERT INTO pattern_annotation_layers (
                    projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                ) VALUES (7, 12, 'two-owners', 0, 1000, 1000)
                """.trimIndent(),
            )
        }

        db.execSQL("DELETE FROM counter_projects WHERE id = 7")
        assertEquals(0, countRows(db, "pattern_annotation_layers"))
        assertEquals(0, countRows(db, "pattern_annotations"))

        db.execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (2, NULL, 12, 'saved:12:v1', 1, 1000, 1000)
            """.trimIndent(),
        )
        db.execSQL("DELETE FROM saved_patterns WHERE id = 12")
        assertEquals(0, countRows(db, "pattern_annotation_layers"))
        db.close()
    }

    @Test
    fun migrationChainsFromVersionsOneAndSixReachSchema17() {
        listOf(1, 6).forEach { startVersion ->
            val testDb = "migration-test-v$startVersion-to-v17-pattern-schema"
            helper.createDatabase(testDb, startVersion).close()

            val db =
                helper.runMigrationsAndValidate(
                    testDb,
                    17,
                    true,
                    *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
                )

            assertEquals(0, countRows(db, "pattern_annotation_layers"))
            assertEquals(0, countRows(db, "pattern_annotations"))
            db.close()
        }
    }

    private fun SupportSQLiteDatabase.insertProject(
        id: Long,
        linkedPatternId: Long?,
        patternUri: String?,
    ) {
        execSQL(
            """
            INSERT INTO counter_projects (
                id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                linkedPatternId, patternUri
            ) VALUES (?, 'Project', 0, 0, 1, '', 1000, 1000, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(id, linkedPatternId, patternUri),
        )
    }

    private fun SupportSQLiteDatabase.insertSavedPattern(id: Long) {
        execSQL(
            """
            INSERT INTO saved_patterns (
                id, source, ravelryPatternId, name, designerName, thumbnailUrl,
                difficulty, gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage,
                isFree, originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            ) VALUES (
                ?, 'LOCAL_FILE', NULL, 'Pattern', '', NULL,
                NULL, NULL, NULL, NULL, NULL, NULL,
                1, 'content://pattern.pdf', '', 'content://pattern.pdf', 1,
                1000, 1000, NULL
            )
            """.trimIndent(),
            arrayOf(id),
        )
    }

    private fun countRows(
        db: SupportSQLiteDatabase,
        table: String,
    ): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private fun assertSingleString(
        db: SupportSQLiteDatabase,
        sql: String,
        expected: String,
    ) {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getString(0))
            assertFalse(cursor.moveToNext())
        }
    }

    private fun assertConstraintViolation(block: () -> Unit) {
        val exception = runCatching(block).exceptionOrNull()
        assertTrue(exception is SQLiteConstraintException)
    }
}
