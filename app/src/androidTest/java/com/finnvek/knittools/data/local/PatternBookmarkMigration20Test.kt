package com.finnvek.knittools.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternBookmarkMigration20Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate19to20PreservesPatternStateAndAddsBookmarkOwnership() {
        val testDb = "migration-test-v19-to-v20-pattern-bookmarks"
        helper.createDatabase(testDb, 19).apply {
            PatternAnnotationSchemaConstraints.create(this)
            execSQL(
                """
                INSERT INTO saved_patterns (
                    id, source, name, designerName, availability, originalUrl, canonicalUrl,
                    isAvailableOffline, savedAt, updatedAt
                ) VALUES (91, 'LOCAL_FILE', 'Pattern', 'Designer', 'free', '', '', 1, 500, 600)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    patternUri, patternName, currentPatternPage, patternRowMapping,
                    readingLineEnabled, readingLineYFraction, linkedPatternId
                ) VALUES (
                    41, 'Gallery pattern', 12, 0, 1, '', 1000, 2000,
                    'content://knittools/pattern.pdf', 'Imported photos', 3,
                    '[{"row":12,"page":3,"yPosition":0.4}]', 1, 0.4, 91
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO pattern_annotation_layers (
                    id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                ) VALUES (51, 41, NULL, 'saved:91:v1', 1, 3000, 3000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO pattern_annotations (
                    id, layerId, page, kind, payloadVersion, payloadJson,
                    zIndex, createdAt, updatedAt
                ) VALUES (61, 51, 3, 'LINE', 1, '{}', 0, 4000, 4000)
                """.trimIndent(),
            )
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                20,
                true,
                KnitToolsDatabase.MIGRATION_19_20,
            )

        db
            .query(
                """
                SELECT patternUri, patternName, currentPatternPage, patternRowMapping,
                    readingLineEnabled, readingLineYFraction,
                    readingLineFollowCurrentRow, verticalReadingGuideEnabled,
                    verticalReadingGuideXFraction, linkedPatternId
                FROM counter_projects WHERE id = 41
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("content://knittools/pattern.pdf", cursor.getString(0))
                assertEquals("Imported photos", cursor.getString(1))
                assertEquals(3, cursor.getInt(2))
                assertTrue(cursor.getString(3).contains("\"row\":12"))
                assertEquals(1, cursor.getInt(4))
                assertEquals(0.4f, cursor.getFloat(5), 0.0001f)
                assertEquals(1, cursor.getInt(6))
                assertEquals(0, cursor.getInt(7))
                assertEquals(0.5f, cursor.getFloat(8), 0.0001f)
                assertEquals(91L, cursor.getLong(9))
            }
        assertEquals(1, count(db, "saved_patterns"))
        assertEquals(1, count(db, "pattern_annotation_layers"))
        assertEquals(1, count(db, "pattern_annotations"))
        assertEquals(4, annotationTriggerCount(db))

        db.execSQL(
            """
            INSERT INTO pattern_bookmarks (
                projectId, documentKey, name, pageIndex, yFraction, createdAt
            ) VALUES (41, 'saved:91:v1', 'Sleeve', 3, 0.4, 5000)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO pattern_bookmarks (
                projectId, documentKey, name, pageIndex, yFraction, createdAt
            ) VALUES (41, 'saved:91:v1', 'Sleeve', 3, 0.4, 5000)
            """.trimIndent(),
        )
        assertEquals(2, count(db, "pattern_bookmarks"))
        assertBookmarkIndex(db)
        assertBookmarkForeignKey(db)

        db.execSQL("PRAGMA foreign_keys=ON")
        db.execSQL("DELETE FROM counter_projects WHERE id = 41")
        assertEquals(0, count(db, "pattern_bookmarks"))
        assertEquals(0, count(db, "pattern_annotation_layers"))
        assertEquals(0, count(db, "pattern_annotations"))
        assertEquals(1, count(db, "saved_patterns"))
        db.close()
    }

    private fun count(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String,
    ): Int =
        db
            .query("SELECT COUNT(*) FROM $table")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private fun annotationTriggerCount(db: androidx.sqlite.db.SupportSQLiteDatabase): Int =
        db
            .query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'trigger' AND name LIKE 'pattern_annotation_layers_%'
                """.trimIndent(),
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private fun assertBookmarkIndex(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.query("PRAGMA index_list('pattern_bookmarks')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == "index_pattern_bookmarks_project_document_position") {
                    assertEquals(0, cursor.getInt(uniqueIndex))
                    found = true
                }
            }
            assertTrue(found)
        }
        db.query("PRAGMA index_info('index_pattern_bookmarks_project_document_position')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val columns =
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(nameIndex))
                    }
                }
            assertEquals(
                listOf("projectId", "documentKey", "pageIndex", "yFraction", "createdAt", "id"),
                columns,
            )
        }
    }

    private fun assertBookmarkForeignKey(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.query("PRAGMA foreign_key_list('pattern_bookmarks')").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("counter_projects", cursor.getString(cursor.getColumnIndexOrThrow("table")))
            assertEquals("projectId", cursor.getString(cursor.getColumnIndexOrThrow("from")))
            assertEquals("id", cursor.getString(cursor.getColumnIndexOrThrow("to")))
            assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
        }
    }
}
