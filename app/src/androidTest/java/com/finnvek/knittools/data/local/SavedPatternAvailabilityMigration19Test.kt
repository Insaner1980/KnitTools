package com.finnvek.knittools.data.local

import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedPatternAvailabilityMigration19Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
        )

    @Test
    fun migrate18to19PreservesSavedPatternsLinksAndAnnotationOwnership() {
        val testDb = "migration-test-v18-to-v19-pattern-availability"

        helper.createDatabase(testDb, 18).apply {
            PatternAnnotationSchemaConstraints.create(this)
            insertLegacySavedPatterns()
            insertLinkedProject()
            insertAnnotationLayersAndAnnotations()
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                19,
                true,
                KnitToolsDatabase.MIGRATION_18_19,
            )

        assertSingleRow(
            db,
            "SELECT availability FROM saved_patterns WHERE id = 10",
        ) {
            assertEquals("free", getString(0))
        }
        assertSingleRow(
            db,
            """
            SELECT source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, availability,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns WHERE id = 20
            """.trimIndent(),
        ) {
            assertEquals("RAVELRY", getString(0))
            assertEquals(4321, getInt(1))
            assertEquals("Unknown Cardigan", getString(2))
            assertEquals("Test Designer", getString(3))
            assertEquals("https://images.example/cardigan.jpg", getString(4))
            assertEquals(4.25, getDouble(5), 0.0)
            assertEquals(22.0, getDouble(6), 0.0)
            assertEquals(30.0, getDouble(7), 0.0)
            assertEquals("4 mm", getString(8))
            assertEquals("DK", getString(9))
            assertEquals(900, getInt(10))
            assertEquals("unknown", getString(11))
            assertEquals("https://example.test/shared-cardigan", getString(12))
            assertEquals("https://www.ravelry.com/patterns/library/unknown-cardigan", getString(13))
            assertEquals("content://patterns/unknown-cardigan.pdf", getString(14))
            assertEquals(1, getInt(15))
            assertEquals(1_000L, getLong(16))
            assertEquals(2_000L, getLong(17))
            assertEquals(3_000L, getLong(18))
        }
        assertSingleRow(
            db,
            """
            SELECT name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, linkedPatternId
            FROM counter_projects WHERE id = 1
            """.trimIndent(),
        ) {
            assertEquals("Linked project", getString(0))
            assertEquals(5, getInt(1))
            assertEquals(0, getInt(2))
            assertEquals(1, getInt(3))
            assertEquals("", getString(4))
            assertEquals(100L, getLong(5))
            assertEquals(200L, getLong(6))
            assertEquals(20L, getLong(7))
        }
        assertSingleRow(db, "SELECT COUNT(*) FROM counter_projects") {
            assertEquals(1, getInt(0))
        }
        insertAndAssertPaidPattern(db)
        assertStableAvailabilityValues(db)
        assertLayerAndAnnotationRows(db)
        assertSavedPatternIndexes(db)
        assertAvailabilityReplacesIsFree(db)
        assertAnnotationTriggersRemain(db)
        assertAnnotationTriggersFunction(db)
        db.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }

        db.close()
    }

    private fun SupportSQLiteDatabase.insertLegacySavedPatterns() {
        execSQL(
            """
            INSERT INTO saved_patterns (
                id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, isFree,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            ) VALUES (
                10, 'RAVELRY', 123, 'Free Hat', 'Designer', NULL, NULL,
                NULL, NULL, NULL, NULL, NULL, 1,
                'https://example.test/free-hat', 'https://www.ravelry.com/patterns/library/free-hat',
                NULL, 0, 100, 200, NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO saved_patterns (
                id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, isFree,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            ) VALUES (
                20, 'RAVELRY', 4321, 'Unknown Cardigan', 'Test Designer',
                'https://images.example/cardigan.jpg', 4.25,
                22.0, 30.0, '4 mm', 'DK', 900, 0,
                'https://example.test/shared-cardigan',
                'https://www.ravelry.com/patterns/library/unknown-cardigan',
                'content://patterns/unknown-cardigan.pdf', 1, 1000, 2000, 3000
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.insertLinkedProject() {
        execSQL(
            """
            INSERT INTO counter_projects (
                id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                linkedPatternId
            ) VALUES (1, 'Linked project', 5, 0, 1, '', 100, 200, 20)
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.insertAnnotationLayersAndAnnotations() {
        execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (100, NULL, 20, 'saved:20:v1', 1, 4000, 5000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (101, 1, NULL, 'saved:20:v1', 1, 6000, 7000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO pattern_annotations (
                id, layerId, page, kind, payloadVersion, payloadJson,
                zIndex, createdAt, updatedAt
            ) VALUES (200, 100, 2, 'FREEHAND', 1, '{"points":[]}', 3, 8000, 9000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO pattern_annotations (
                id, layerId, page, kind, payloadVersion, payloadJson,
                zIndex, createdAt, updatedAt
            ) VALUES (201, 101, 4, 'HIGHLIGHTER', 1, '{"points":[]}', 5, 10000, 11000)
            """.trimIndent(),
        )
    }

    private fun assertLayerAndAnnotationRows(db: SupportSQLiteDatabase) {
        assertSingleRow(
            db,
            """
            SELECT projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            FROM pattern_annotation_layers WHERE id = 100
            """.trimIndent(),
        ) {
            assertTrue(isNull(0))
            assertEquals(20L, getLong(1))
            assertEquals("saved:20:v1", getString(2))
            assertEquals(1, getInt(3))
            assertEquals(4_000L, getLong(4))
            assertEquals(5_000L, getLong(5))
        }
        assertSingleRow(
            db,
            "SELECT projectId, savedPatternId FROM pattern_annotation_layers WHERE id = 101",
        ) {
            assertEquals(1L, getLong(0))
            assertTrue(isNull(1))
        }
        assertSingleRow(
            db,
            """
            SELECT layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            FROM pattern_annotations WHERE id = 200
            """.trimIndent(),
        ) {
            assertEquals(100L, getLong(0))
            assertEquals(2, getInt(1))
            assertEquals("FREEHAND", getString(2))
            assertEquals(1, getInt(3))
            assertEquals("{\"points\":[]}", getString(4))
            assertEquals(3L, getLong(5))
            assertEquals(8_000L, getLong(6))
            assertEquals(9_000L, getLong(7))
        }
        assertSingleRow(
            db,
            "SELECT layerId, page, kind FROM pattern_annotations WHERE id = 201",
        ) {
            assertEquals(101L, getLong(0))
            assertEquals(4, getInt(1))
            assertEquals("HIGHLIGHTER", getString(2))
        }
    }

    private fun insertAndAssertPaidPattern(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO saved_patterns (
                id, source, ravelryPatternId, name, designerName, availability,
                originalUrl, canonicalUrl, isAvailableOffline, savedAt, updatedAt
            ) VALUES (
                30, 'RAVELRY', 9876, 'Paid Scarf', 'Paid Designer', 'paid',
                'https://example.test/paid-scarf',
                'https://www.ravelry.com/patterns/library/paid-scarf',
                0, 12000, 13000
            )
            """.trimIndent(),
        )
        assertSingleRow(
            db,
            "SELECT availability FROM saved_patterns WHERE id = 30",
        ) {
            assertEquals("paid", getString(0))
        }
    }

    private fun assertStableAvailabilityValues(db: SupportSQLiteDatabase) {
        val values = mutableListOf<String>()
        db.query("SELECT DISTINCT availability FROM saved_patterns ORDER BY availability").use { cursor ->
            while (cursor.moveToNext()) {
                values += cursor.getString(0)
            }
        }
        assertEquals(listOf("free", "paid", "unknown"), values)
    }

    private fun assertSavedPatternIndexes(db: SupportSQLiteDatabase) {
        listOf(
            "index_saved_patterns_ravelryPatternId",
            "index_saved_patterns_canonicalUrl",
            "index_saved_patterns_originalUrl",
            "index_saved_patterns_localPdfUri",
        ).forEach { indexName ->
            db.query("PRAGMA index_list('saved_patterns')").use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == indexName) found = true
                }
                assertTrue(indexName, found)
            }
        }
    }

    private fun assertAvailabilityReplacesIsFree(db: SupportSQLiteDatabase) {
        val columns = mutableMapOf<String, String>()
        db.query("PRAGMA table_info('saved_patterns')").use { cursor ->
            while (cursor.moveToNext()) {
                columns[cursor.getString(1)] = cursor.getString(2)
            }
        }
        assertFalse(columns.containsKey("isFree"))
        assertEquals("TEXT", columns["availability"])
    }

    private fun assertAnnotationTriggersRemain(db: SupportSQLiteDatabase) {
        listOf(
            "pattern_annotation_layers_owner_insert",
            "pattern_annotation_layers_owner_update",
            "pattern_annotation_layers_active_insert",
            "pattern_annotation_layers_active_update",
        ).forEach { triggerName ->
            assertSingleRow(
                db,
                "SELECT name FROM sqlite_master WHERE type = 'trigger' AND name = '$triggerName'",
            ) {
                assertEquals(triggerName, getString(0))
            }
        }
    }

    private fun assertAnnotationTriggersFunction(db: SupportSQLiteDatabase) {
        assertSqliteConstraint(
            db,
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (300, NULL, NULL, 'invalid-owner', 0, 1, 1)
            """.trimIndent(),
        )
        assertSqliteConstraint(
            db,
            "UPDATE pattern_annotation_layers SET savedPatternId = 20 WHERE id = 101",
        )
        assertSqliteConstraint(
            db,
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (301, 1, NULL, 'second-active-insert', 1, 1, 1)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            ) VALUES (302, 1, NULL, 'second-active-update', 0, 1, 1)
            """.trimIndent(),
        )
        assertSqliteConstraint(
            db,
            "UPDATE pattern_annotation_layers SET isActive = 1 WHERE id = 302",
        )
    }

    private fun assertSqliteConstraint(
        db: SupportSQLiteDatabase,
        sql: String,
    ) {
        try {
            db.execSQL(sql)
            fail("Expected SQLiteConstraintException")
        } catch (_: SQLiteConstraintException) {
            // Odotettu triggerin torjunta todistaa, että migraation jälkeinen constraint toimii.
        }
    }

    private inline fun assertSingleRow(
        db: SupportSQLiteDatabase,
        sql: String,
        verify: Cursor.() -> Unit,
    ) {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.verify()
            assertFalse(cursor.moveToNext())
        }
    }
}
