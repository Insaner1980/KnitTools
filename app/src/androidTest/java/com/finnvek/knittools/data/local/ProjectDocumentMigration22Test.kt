package com.finnvek.knittools.data.local

import android.database.SQLException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDocumentMigration22Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate21to22BackfillsOnlyReadableDocumentsAndPreservesOwnedState() {
        val testDb = "migration-test-v21-to-v22-project-documents"
        helper.createDatabase(testDb, 21).apply {
            insertSavedPattern(id = 20, name = "Metadata only", localPdfUri = null)
            insertSavedPattern(id = 21, name = "Saved cardigan", localPdfUri = "content://patterns/cardigan.pdf")
            insertSavedPattern(id = 22, name = "Wrong PDF", localPdfUri = "content://patterns/wrong.pdf")
            insertSavedPattern(id = 23, name = "Camera chart", localPdfUri = "file:///patterns/camera.pdf")
            insertProject(id = 1, name = "No pattern")
            insertProject(id = 2, name = "Metadata", linkedPatternId = 20)
            insertProject(
                id = 3,
                name = "Local",
                patternUri = "content://patterns/local-chart.pdf",
                currentPage = -4,
                rowMapping = "1:0:0.25",
                readingLineEnabled = true,
                readingLineY = 2.5f,
                follow = false,
                verticalEnabled = true,
                verticalX = -3f,
            )
            insertProject(
                id = 4,
                name = "Saved",
                linkedPatternId = 21,
                patternUri = "content://patterns/cardigan.pdf",
                patternName = "  Cardigan chart  ",
                currentPage = 6,
            )
            insertProject(
                id = 5,
                name = "Mismatch",
                linkedPatternId = 22,
                patternUri = "content://patterns/other.pdf",
                patternName = "Other PDF",
                completed = true,
            )
            insertProject(id = 6, name = "Gallery", patternUri = "file:///patterns/gallery.pdf")
            insertProject(
                id = 7,
                name = "Camera",
                linkedPatternId = 23,
                patternUri = "file:///patterns/camera.pdf",
            )
            insertProject(id = 8, name = "SAF", patternUri = "file:///patterns/saf-copy.pdf")
            insertProject(id = 9, name = "Missing", patternUri = "file:///patterns/missing.pdf")
            execSQL(
                "INSERT INTO pattern_annotation_layers " +
                    "(id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) " +
                    "VALUES (400, 4, NULL, 'stable-project-key', 1, 1000, 2000)",
            )
            execSQL(
                "INSERT INTO pattern_annotation_layers " +
                    "(id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) " +
                    "VALUES (401, 4, NULL, 'dormant-key', 0, 1000, 2000)",
            )
            execSQL(
                "INSERT INTO pattern_annotation_layers " +
                    "(id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) " +
                    "VALUES (402, 6, NULL, 'dormant-gallery-key', 0, 1000, 2000)",
            )
            execSQL(
                "INSERT INTO pattern_annotation_layers " +
                    "(id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) " +
                    "VALUES (403, NULL, 23, 'stable-saved-key', 1, 1000, 2000)",
            )
            execSQL(
                "INSERT INTO pattern_annotations " +
                    "(id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt) " +
                    "VALUES (500, 400, 2, 'FREEHAND', 1, '{}', 0, 1000, 1000)",
            )
            execSQL(
                "INSERT INTO pattern_bookmarks " +
                    "(id, projectId, documentKey, name, pageIndex, yFraction, createdAt) " +
                    "VALUES (600, 4, 'stable-project-key', 'Sleeve', 2, 0.4, 1000)",
            )
            insertCompletedSession(projectId = 5)
            insertActiveSession(projectId = 4)
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                22,
                true,
                KnitToolsDatabase.MIGRATION_21_22,
            )

        assertEquals(0, count(db, "SELECT COUNT(*) FROM project_documents WHERE projectId IN (1, 2)"))
        assertDocument(
            db = db,
            projectId = 3,
            savedPatternId = null,
            documentKey = "legacy-project:3",
            label = "local-chart.pdf",
            currentPage = 0,
            readingLineY = 0.95f,
            follow = false,
            verticalX = 0.05f,
        )
        assertDocument(
            db = db,
            projectId = 4,
            savedPatternId = 21,
            documentKey = "stable-project-key",
            label = "Cardigan chart",
            currentPage = 6,
        )
        assertDocument(
            db = db,
            projectId = 5,
            savedPatternId = null,
            documentKey = "legacy-project:5",
            label = "Other PDF",
            currentPage = 0,
        )
        assertEquals(22L, scalarLong(db, "SELECT linkedPatternId FROM counter_projects WHERE id = 5"))
        assertDocument(db, 6, null, "dormant-gallery-key", "gallery.pdf", 0)
        assertDocument(db, 7, 23, "stable-saved-key", "Camera chart", 0)
        assertDocument(db, 8, null, "legacy-project:8", "saf-copy.pdf", 0)
        assertDocument(db, 9, null, "legacy-project:9", "missing.pdf", 0)
        assertEquals(
            "file:///patterns/gallery.pdf",
            scalarString(db, "SELECT localPdfUri FROM project_documents WHERE projectId = 6"),
        )
        assertEquals(
            "file:///patterns/camera.pdf",
            scalarString(db, "SELECT localPdfUri FROM project_documents WHERE projectId = 7"),
        )
        assertEquals(
            "file:///patterns/saf-copy.pdf",
            scalarString(db, "SELECT localPdfUri FROM project_documents WHERE projectId = 8"),
        )
        assertEquals(
            "file:///patterns/missing.pdf",
            scalarString(db, "SELECT localPdfUri FROM project_documents WHERE projectId = 9"),
        )
        assertEquals(1, count(db, "SELECT COUNT(*) FROM sessions WHERE projectId = 5"))
        assertEquals(1, count(db, "SELECT COUNT(*) FROM active_sessions WHERE projectId = 4"))
        assertEquals(2, count(db, "SELECT COUNT(*) FROM pattern_annotation_layers WHERE projectId = 4"))
        assertEquals(1, count(db, "SELECT COUNT(*) FROM pattern_annotations WHERE id = 500"))
        assertEquals(1, count(db, "SELECT COUNT(*) FROM pattern_bookmarks WHERE id = 600"))

        assertPrimaryAndDuplicateConstraints(db)

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM saved_patterns WHERE id = 21")
        assertNull(scalarNullableLong(db, "SELECT savedPatternId FROM project_documents WHERE projectId = 4"))
        assertEquals(
            "content://patterns/cardigan.pdf",
            scalarString(db, "SELECT localPdfUri FROM project_documents WHERE projectId = 4"),
        )

        db.execSQL("DELETE FROM counter_projects WHERE id = 3")
        assertEquals(0, count(db, "SELECT COUNT(*) FROM project_documents WHERE projectId = 3"))
        db.close()
    }

    private fun SupportSQLiteDatabase.insertProject(
        id: Long,
        name: String,
        linkedPatternId: Long? = null,
        patternUri: String? = null,
        patternName: String? = null,
        currentPage: Int = 0,
        rowMapping: String? = null,
        readingLineEnabled: Boolean = false,
        readingLineY: Float = 0.5f,
        follow: Boolean = true,
        verticalEnabled: Boolean = false,
        verticalX: Float = 0.5f,
        completed: Boolean = false,
    ) {
        execSQL(
            """
            INSERT INTO counter_projects (
                id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                linkedPatternId, patternUri, patternName, currentPatternPage, patternRowMapping,
                readingLineEnabled, readingLineYFraction, readingLineFollowCurrentRow,
                verticalReadingGuideEnabled, verticalReadingGuideXFraction, isCompleted
            ) VALUES (?, ?, 0, 0, 1, '', 1000, 2000, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                id,
                name,
                linkedPatternId,
                patternUri,
                patternName,
                currentPage,
                rowMapping,
                if (readingLineEnabled) 1 else 0,
                readingLineY,
                if (follow) 1 else 0,
                if (verticalEnabled) 1 else 0,
                verticalX,
                if (completed) 1 else 0,
            ),
        )
    }

    private fun SupportSQLiteDatabase.insertSavedPattern(
        id: Long,
        name: String,
        localPdfUri: String?,
    ) {
        execSQL(
            """
            INSERT INTO saved_patterns (
                id, source, ravelryPatternId, name, designerName, availability,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            ) VALUES (?, 'LOCAL_PDF', NULL, ?, '', 'unknown', '', '', ?, ?, 1000, 2000, NULL)
            """.trimIndent(),
            arrayOf<Any?>(id, name, localPdfUri, if (localPdfUri == null) 0 else 1),
        )
    }

    private fun SupportSQLiteDatabase.insertCompletedSession(projectId: Long) {
        execSQL(
            """
            INSERT INTO sessions (
                id, projectId, startedAt, endedAt, startRow, endRow,
                durationMinutes, durationSeconds, rowsWorked, zoneId
            ) VALUES (700, ?, 1000, 61000, 0, 5, 1, 60, 5, 'Europe/Helsinki')
            """.trimIndent(),
            arrayOf(projectId),
        )
    }

    private fun SupportSQLiteDatabase.insertActiveSession(projectId: Long) {
        execSQL(
            """
            INSERT INTO active_sessions (
                singletonId, sessionToken, projectId, startedAtWallMillis, startZoneId,
                startRow, lastObservedRow, trustedLastObservedRow, trustedRowsWorked,
                pendingRowsWorked, reviewedRowsWorked, reviewedLastObservedRow,
                unreviewedRowsWorked, checkpointedDurationSeconds,
                reviewedDurationBaselineSeconds, segmentStartedAtWallMillis,
                segmentStartedElapsedRealtimeMillis, bootCount, recoveryReason,
                recoveryIntervalToken, recoverySuggestedDurationSeconds,
                recoveryPromptShown, updatedAtWallMillis
            ) VALUES (
                1, 'active', ?, 1000, 'Europe/Helsinki', 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1000, 1000, 1, NULL, NULL, NULL, 0, 1000
            )
            """.trimIndent(),
            arrayOf(projectId),
        )
    }

    private fun assertDocument(
        db: SupportSQLiteDatabase,
        projectId: Long,
        savedPatternId: Long?,
        documentKey: String,
        label: String,
        currentPage: Int,
        readingLineY: Float = 0.5f,
        follow: Boolean = true,
        verticalX: Float = 0.5f,
    ) {
        db
            .query(
                """
                SELECT savedPatternId, documentKey, label, sortOrder, isPrimary, currentPage,
                    readingLineYFraction, readingLineFollowCurrentRow,
                    verticalReadingGuideXFraction
                FROM project_documents WHERE projectId = ?
                """.trimIndent(),
                arrayOf(projectId),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                if (savedPatternId == null) {
                    assertTrue(cursor.isNull(0))
                } else {
                    assertEquals(savedPatternId, cursor.getLong(0))
                }
                assertEquals(documentKey, cursor.getString(1))
                assertEquals(label, cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals(1, cursor.getInt(4))
                assertEquals(currentPage, cursor.getInt(5))
                assertEquals(readingLineY, cursor.getFloat(6), 0.0001f)
                assertEquals(if (follow) 1 else 0, cursor.getInt(7))
                assertEquals(verticalX, cursor.getFloat(8), 0.0001f)
                assertFalse(cursor.moveToNext())
            }
    }

    private fun assertPrimaryAndDuplicateConstraints(db: SupportSQLiteDatabase) {
        assertSqliteFailure {
            db.execSQL(
                """
                INSERT INTO project_documents (
                    projectId, savedPatternId, documentKey, label, localPdfUri, sortOrder,
                    isPrimary, currentPage, rowMapping, readingLineEnabled,
                    readingLineYFraction, readingLineFollowCurrentRow,
                    verticalReadingGuideEnabled, verticalReadingGuideXFraction,
                    createdAt, updatedAt
                ) VALUES (4, NULL, 'second-primary', 'Second', 'content://second.pdf', 1,
                    1, 0, NULL, 0, 0.5, 1, 0, 0.5, 1000, 1000)
                """.trimIndent(),
            )
        }
        val duplicateCases =
            listOf(
                "4, NULL, 'stable-project-key', 'Key duplicate', 'content://key.pdf'",
                "4, NULL, 'uri-duplicate', 'URI duplicate', 'content://patterns/cardigan.pdf'",
            )
        duplicateCases.forEachIndexed { index, values ->
            assertSqliteFailure {
                db.execSQL(
                    """
                    INSERT INTO project_documents (
                        projectId, savedPatternId, documentKey, label, localPdfUri, sortOrder,
                        isPrimary, currentPage, rowMapping, readingLineEnabled,
                        readingLineYFraction, readingLineFollowCurrentRow,
                        verticalReadingGuideEnabled, verticalReadingGuideXFraction,
                        createdAt, updatedAt
                    ) VALUES ($values, ${index + 1}, 0, 0, NULL, 0, 0.5, 1, 0, 0.5, 1000, 1000)
                    """.trimIndent(),
                )
            }
        }
    }

    private fun assertSqliteFailure(block: () -> Unit) {
        try {
            block()
            fail("SQLite constraint accepted invalid project document")
        } catch (_: SQLException) {
            // Skeemaraja hylkäsi virheellisen tilan odotetusti.
        }
    }

    private fun count(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Int =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun scalarNullableLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long? =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            if (cursor.isNull(0)) null else cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
}
