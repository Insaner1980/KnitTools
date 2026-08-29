package com.finnvek.knittools.data.local

import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.repository.ProjectYarnUsageRepository
import com.finnvek.knittools.repository.YarnUsageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectFolderMigration23Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate23to24PreservesEveryTableAndBothActiveSessionStatesWithoutBackfill() {
        listOf(false, true).forEach { recovery ->
            val testDb = "migration-test-yarn-usage-v24-$recovery"
            lateinit var before: DatabaseSnapshot
            helper.createDatabase(testDb, 23).apply {
                ActiveSessionSchemaConstraints.create(this)
                PatternAnnotationSchemaConstraints.create(this)
                ProjectDocumentSchemaConstraints.create(this)
                insertProject(1, "Active", updatedAt = 101)
                insertProject(2, "Completed", isCompleted = true, updatedAt = 202)
                insertSavedPattern(30)
                insertCompletedSession(2)
                insertActiveSession(1, recovery)
                insertProjectDocument(1)
                insertAnnotationAndBookmark(1)
                insertProjectOwnedRows(1)
                execSQL("UPDATE counter_projects SET yarnCardIds = '61' WHERE id = 1")
                execSQL(
                    "INSERT INTO project_folders (id, name, normalizedName, sortOrder) VALUES (90, 'Gifts', 'gifts', 0)",
                )
                execSQL("INSERT INTO project_folder_assignments (projectId, folderId) VALUES (1, 90)")
                execSQL(
                    "INSERT INTO counter_history (id, projectId, action, previousValue, newValue, timestamp) VALUES (80, 1, 'INCREMENT', 3, 4, 100)",
                )
                before = snapshotExistingState(this)
                close()
            }
            helper.runMigrationsAndValidate(testDb, 24, true, KnitToolsDatabase.MIGRATION_23_24).use { db ->
                assertEquals(before, snapshotExistingState(db, before.tables.keys))
                assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_yarn_usage"))
                assertTrue(db.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
            }
            val database =
                Room
                    .databaseBuilder(
                        InstrumentationRegistry.getInstrumentation().targetContext,
                        KnitToolsDatabase::class.java,
                        testDb,
                    ).addMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)
                    .build()
            try {
                val repository =
                    ProjectYarnUsageRepository(
                        database.projectYarnUsageDao(),
                        database.projectYarnNoteDao(),
                        database.yarnCardDao(),
                        RoomDatabaseTransactionRunner(database),
                        Dispatchers.IO,
                    )
                runBlocking {
                    val created =
                        repository.create(
                            1,
                            YarnUsageSource(projectYarnNoteId = 62),
                            YarnUsageAmounts(1200.0, 600.0, 350.0, 200.0, 100.0),
                            "Fallback",
                        ) as YarnUsageResult.Created
                    assertEquals(YarnUsageSource(61, 62), created.usage.source)
                    val updated =
                        repository.update(
                            1,
                            created.usage.id,
                            created.usage.updatedAt,
                            created.usage.amounts.copy(usedMeters = 700.0),
                        ) as YarnUsageResult.Updated
                    assertEquals(
                        before,
                        snapshotExistingState(database.openHelper.writableDatabase, before.tables.keys),
                    )
                    assertEquals(
                        YarnUsageResult.Deleted,
                        repository.delete(1, updated.usage.id, updated.usage.updatedAt),
                    )
                }
                assertEquals(before, snapshotExistingState(database.openHelper.writableDatabase, before.tables.keys))
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun migrate22to23CreatesEmptyFolderStateAndPreservesProjectOwnedData() {
        val testDb = "migration-test-v22-to-v23-project-folders"
        lateinit var schema22Snapshot: DatabaseSnapshot
        helper.createDatabase(testDb, 22).apply {
            ActiveSessionSchemaConstraints.create(this)
            PatternAnnotationSchemaConstraints.create(this)
            ProjectDocumentSchemaConstraints.create(this)
            insertProject(1, "Active", updatedAt = 101L)
            insertProject(2, "Completed", isCompleted = true, updatedAt = 202L)
            insertSavedPattern(30)
            insertCompletedSession(2)
            insertActiveSession(1, recoveryRequired = true)
            insertProjectDocument(1)
            insertAnnotationAndBookmark(1)
            insertProjectOwnedRows(1)
            schema22Snapshot = snapshotExistingState(this)
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                23,
                true,
                *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
            )

        assertEquals(
            schema22Snapshot,
            snapshotExistingState(db, schema22Snapshot.tables.keys),
        )
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folders"))
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments"))
        assertProjectState(db, projectId = 1, expectedName = "Active", expectedCompleted = 0, expectedUpdatedAt = 101L)
        assertProjectState(
            db,
            projectId = 2,
            expectedName = "Completed",
            expectedCompleted = 1,
            expectedUpdatedAt = 202L,
        )
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM sessions WHERE projectId = 2"))
        assertEquals(
            1L,
            scalarLong(db, "SELECT COUNT(*) FROM active_sessions WHERE projectId = 1 AND recoveryReason = 'REBOOT'"),
        )
        assertEquals(
            1L,
            scalarLong(
                db,
                "SELECT COUNT(*) FROM project_documents WHERE projectId = 1 AND documentKey = 'document-key'",
            ),
        )
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM pattern_annotation_layers WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM pattern_annotations WHERE id = 51"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM pattern_bookmarks WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM progress_photos WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM yarn_cards WHERE linkedProjectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM project_yarn_notes WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM row_reminders WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM project_counters WHERE projectId = 1"))
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM saved_patterns WHERE id = 30"))
        assertIndexExists(db, "project_folders", "index_project_folders_normalizedName")
        assertIndexExists(db, "project_folder_assignments", "index_project_folder_assignments_folderId")
        assertEquals(
            3L,
            scalarLong(
                db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'trigger' AND name IN ('active_sessions_require_singleton_id_insert', 'pattern_annotation_layers_owner_insert', 'project_documents_primary_insert')",
            ),
        )
        assertFolderOperationsWorkAfterMigration(db)
        assertTrue(db.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
        db.close()
    }

    @Test
    fun migrate22to23PreservesNormalActiveSession() {
        val testDb = "migration-test-v22-to-v23-normal-active-session"
        helper.createDatabase(testDb, 22).apply {
            ActiveSessionSchemaConstraints.create(this)
            insertProject(1, "Active", updatedAt = 101L)
            insertActiveSession(1, recoveryRequired = false)
            close()
        }

        val db =
            helper.runMigrationsAndValidate(
                testDb,
                23,
                true,
                *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
            )

        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM active_sessions WHERE projectId = 1"))
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM active_sessions WHERE recoveryReason IS NOT NULL"))
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments"))
        db.close()
    }

    @Test
    fun dedicatedMigrationEntrypointsContinueTo23WithAnUnfiledProject() {
        listOf(16, 18, 19, 20, 21).forEach { startVersion ->
            val testDb = "migration-test-v$startVersion-to-v23-folder-entrypoint"
            helper.createDatabase(testDb, startVersion).apply {
                execSQL(
                    "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, " +
                        "createdAt, updatedAt) " +
                        "VALUES (1, 'Legacy $startVersion', 4, 0, 1, 'note', 100, ${startVersion}00)",
                )
                close()
            }

            val db =
                helper.runMigrationsAndValidate(
                    testDb,
                    23,
                    true,
                    *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
                )

            assertProjectState(
                db,
                projectId = 1,
                expectedName = "Legacy $startVersion",
                expectedCompleted = 0,
                expectedUpdatedAt = startVersion * 100L,
            )
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folders"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM counter_projects WHERE id = 1"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments WHERE projectId = 1"))
            assertTrue(db.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
            db.close()
        }
    }

    private fun SupportSQLiteDatabase.insertProject(
        id: Long,
        name: String,
        isCompleted: Boolean = false,
        updatedAt: Long,
    ) {
        execSQL(
            "INSERT INTO counter_projects (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, isCompleted, completedAt) VALUES (?, ?, 4, 0, 1, 'note', 100, ?, ?, ?)",
            arrayOf<Any?>(id, name, updatedAt, if (isCompleted) 1 else 0, if (isCompleted) 300L else null),
        )
    }

    private fun SupportSQLiteDatabase.insertSavedPattern(id: Long) {
        execSQL(
            "INSERT INTO saved_patterns (id, source, ravelryPatternId, name, designerName, availability, originalUrl, canonicalUrl, localPdfUri, isAvailableOffline, savedAt, updatedAt, lastSyncedAt) VALUES (?, 'LOCAL_FILE', NULL, 'Saved', '', 'free', '', '', 'file:///saved.pdf', 1, 100, 100, NULL)",
            arrayOf(id),
        )
    }

    private fun SupportSQLiteDatabase.insertCompletedSession(projectId: Long) {
        execSQL(
            "INSERT INTO sessions (id, projectId, startedAt, endedAt, startRow, endRow, durationMinutes, durationSeconds, rowsWorked, zoneId) VALUES (10, ?, 1, 61, 0, 4, 1, 60, 4, 'Europe/Helsinki')",
            arrayOf(projectId),
        )
    }

    private fun SupportSQLiteDatabase.insertActiveSession(
        projectId: Long,
        recoveryRequired: Boolean,
    ) {
        execSQL(
            "INSERT INTO active_sessions (singletonId, sessionToken, projectId, startedAtWallMillis, startZoneId, startRow, lastObservedRow, trustedLastObservedRow, trustedRowsWorked, pendingRowsWorked, reviewedRowsWorked, reviewedLastObservedRow, unreviewedRowsWorked, checkpointedDurationSeconds, reviewedDurationBaselineSeconds, segmentStartedAtWallMillis, segmentStartedElapsedRealtimeMillis, bootCount, recoveryReason, recoveryIntervalToken, recoverySuggestedDurationSeconds, recoveryPromptShown, updatedAtWallMillis) VALUES (1, 'active', ?, 1, 'Europe/Helsinki', 0, 4, 3, 3, ?, 0, 3, ?, 60, 0, 1, 1, 2, ?, ?, ?, 0, 100)",
            arrayOf<Any?>(
                projectId,
                if (recoveryRequired) 1 else 0,
                if (recoveryRequired) 1 else 0,
                if (recoveryRequired) "REBOOT" else null,
                if (recoveryRequired) "recovery" else null,
                if (recoveryRequired) 60 else null,
            ),
        )
    }

    private fun SupportSQLiteDatabase.insertProjectDocument(projectId: Long) {
        execSQL(
            "INSERT INTO project_documents (id, projectId, savedPatternId, documentKey, label, localPdfUri, sortOrder, isPrimary, currentPage, rowMapping, readingLineEnabled, readingLineYFraction, readingLineFollowCurrentRow, verticalReadingGuideEnabled, verticalReadingGuideXFraction, createdAt, updatedAt) VALUES (40, ?, 30, 'document-key', 'Document', 'file:///document.pdf', 0, 1, 2, '4:2:0.4', 1, 0.4, 1, 1, 0.6, 100, 101)",
            arrayOf(projectId),
        )
    }

    private fun SupportSQLiteDatabase.insertAnnotationAndBookmark(projectId: Long) {
        execSQL(
            "INSERT INTO pattern_annotation_layers (id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) VALUES (50, ?, NULL, 'document-key', 1, 100, 100)",
            arrayOf(projectId),
        )
        execSQL(
            "INSERT INTO pattern_annotations (id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt) VALUES (51, 50, 2, 'FREEHAND', 1, '{}', 0, 100, 100)",
        )
        execSQL(
            "INSERT INTO pattern_bookmarks (id, projectId, documentKey, name, pageIndex, yFraction, createdAt) VALUES (52, ?, 'document-key', 'Row 4', 2, 0.4, 100)",
            arrayOf(projectId),
        )
    }

    private fun SupportSQLiteDatabase.insertProjectOwnedRows(projectId: Long) {
        execSQL(
            "INSERT INTO progress_photos (id, projectId, photoUri, rowNumber, note, createdAt) VALUES (60, ?, 'file:///progress.jpg', 4, 'Progress', 100)",
            arrayOf(projectId),
        )
        execSQL(
            "INSERT INTO yarn_cards (id, brand, yarnName, fiberContent, weightGrams, lengthMeters, needleSize, gaugeInfo, colorName, colorNumber, dyeLot, weightCategory, careSymbols, photoUri, createdAt, quantityInStash, status, linkedProjectId) VALUES (61, '', 'Yarn', '', '', '', '', '', '', '', '', '', 0, '', 100, 1, 'IN_STASH', ?)",
            arrayOf(projectId),
        )
        execSQL(
            "INSERT INTO project_yarn_notes (id, projectId, name, description, quantity, notes, savedYarnCardId, createdAt, updatedAt) VALUES (62, ?, 'Yarn', '', 1, '', 61, 100, 100)",
            arrayOf(projectId),
        )
        execSQL(
            "INSERT INTO row_reminders (id, projectId, targetRow, repeatInterval, message, isCompleted, createdAt) VALUES (63, ?, 5, NULL, 'Reminder', 0, 100)",
            arrayOf(projectId),
        )
        execSQL(
            "INSERT INTO project_counters (id, projectId, name, count, stepSize, repeatAt, sortOrder, createdAt, counterType, linkedToMainCounter) VALUES (64, ?, 'Counter', 1, 1, NULL, 0, 100, 'COUNT_UP', 0)",
            arrayOf(projectId),
        )
    }

    private fun assertProjectState(
        db: SupportSQLiteDatabase,
        projectId: Long,
        expectedName: String,
        expectedCompleted: Int,
        expectedUpdatedAt: Long,
    ) {
        db.query("SELECT name, isCompleted, updatedAt FROM counter_projects WHERE id = $projectId").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expectedName, cursor.getString(0))
            assertEquals(expectedCompleted, cursor.getInt(1))
            assertEquals(expectedUpdatedAt, cursor.getLong(2))
        }
    }

    private fun assertIndexExists(
        db: SupportSQLiteDatabase,
        table: String,
        indexName: String,
    ) {
        db.query("PRAGMA index_list('$table')").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == indexName) {
                    found = true
                }
            }
            assertTrue("Missing index $indexName", found)
        }
    }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        query: String,
    ): Long =
        db.query(query).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun assertFolderOperationsWorkAfterMigration(db: SupportSQLiteDatabase) {
        assertEquals(0L, scalarLong(db, "PRAGMA foreign_keys"))
        db.execSQL("PRAGMA foreign_keys = ON")
        assertEquals(1L, scalarLong(db, "PRAGMA foreign_keys"))
        db.execSQL(
            "INSERT INTO project_folders (id, name, normalizedName, sortOrder) VALUES (90, 'Gifts', 'gifts', 0)",
        )
        db.execSQL(
            "INSERT INTO project_folders (id, name, normalizedName, sortOrder) VALUES (91, 'Personal', 'personal', 1)",
        )
        db.execSQL("INSERT INTO project_folder_assignments (projectId, folderId) VALUES (1, 90)")
        db.execSQL("INSERT OR REPLACE INTO project_folder_assignments (projectId, folderId) VALUES (1, 91)")
        assertEquals(91L, scalarLong(db, "SELECT folderId FROM project_folder_assignments WHERE projectId = 1"))
        db.execSQL("DELETE FROM project_folder_assignments WHERE projectId = 1")
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments WHERE projectId = 1"))
        db.execSQL("INSERT INTO project_folder_assignments (projectId, folderId) VALUES (1, 90)")
        db.execSQL("DELETE FROM project_folders WHERE id = 90")
        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM counter_projects WHERE id = 1"))
        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM project_folder_assignments WHERE projectId = 1"))
    }

    private fun snapshotExistingState(
        db: SupportSQLiteDatabase,
        tableNames: Set<String>? = null,
    ): DatabaseSnapshot {
        val tables =
            tableNames
                ?: db
                    .query(
                        "SELECT name FROM sqlite_master " +
                            "WHERE type = 'table' AND name NOT IN ('android_metadata', 'room_master_table') " +
                            "AND name NOT LIKE 'sqlite_%' ORDER BY name",
                    ).use { cursor ->
                        buildSet {
                            while (cursor.moveToNext()) {
                                add(cursor.getString(0))
                            }
                        }
                    }
        return DatabaseSnapshot(
            tables = tables.associateWith { table -> snapshotTable(db, table) },
            triggers = snapshotTriggers(db, tableNames),
        )
    }

    private fun snapshotTable(
        db: SupportSQLiteDatabase,
        table: String,
    ): TableSnapshot =
        TableSnapshot(
            columns = pragmaRows(db, "table_info", table),
            indexes = pragmaRows(db, "index_list", table),
            foreignKeys = pragmaRows(db, "foreign_key_list", table),
            rows =
                db.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(cursor.values())
                        }
                    }
                },
        )

    private fun pragmaRows(
        db: SupportSQLiteDatabase,
        pragma: String,
        table: String,
    ): List<List<String?>> =
        db.query("PRAGMA $pragma(`$table`)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.values())
                }
            }
        }

    private fun snapshotTriggers(
        db: SupportSQLiteDatabase,
        tableNames: Set<String>?,
    ): Map<String, String> {
        val selection =
            tableNames
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { "'${it.replace("'", "''")}'" }
                ?.let { " AND tbl_name IN ($it)" }
                .orEmpty()
        return db
            .query(
                "SELECT name, sql FROM sqlite_master WHERE type = 'trigger'$selection ORDER BY name",
            ).use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(0), cursor.getString(1))
                    }
                }
            }
    }

    private fun Cursor.values(): List<String?> =
        List(columnCount) { index ->
            if (isNull(index)) null else getString(index)
        }

    private data class DatabaseSnapshot(
        val tables: Map<String, TableSnapshot>,
        val triggers: Map<String, String>,
    )

    private data class TableSnapshot(
        val columns: List<List<String?>>,
        val indexes: List<List<String?>>,
        val foreignKeys: List<List<String?>>,
        val rows: List<List<String?>>,
    )
}
