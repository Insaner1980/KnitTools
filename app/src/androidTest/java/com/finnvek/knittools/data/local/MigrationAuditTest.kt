package com.finnvek.knittools.data.local

import android.database.Cursor
import android.os.Bundle
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MigrationAuditTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KnitToolsDatabase::class.java)

    @Test
    fun everyAdjacentMigrationPreservesAllExistingColumnsAndMatchesFreshConstraints() {
        for (start in 1 until KNITTOOLS_DATABASE_VERSION) {
            val name = "audit-adjacent-$start-${UUID.randomUUID()}"
            val before =
                helper.createDatabase(name, start).use { db ->
                    seed(db, start)
                    rows(db)
                }
            helper.runMigrationsAndValidate(name, start + 1, true, *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS).use { db ->
                assertPreserved(before, rows(db), start, start + 1)
                helper.createDatabase("audit-fresh-$start-${UUID.randomUUID()}", start + 1).use { fresh ->
                    constraints(fresh, start + 1)
                    val tables = tables(fresh)
                    assertEquals(tables, tables(db))
                    tables.forEach { table ->
                        assertEquals("$start->$table", TableInfo.read(fresh, table), TableInfo.read(db, table))
                    }
                    assertEquals(triggers(fresh), triggers(db))
                    seed(fresh, start + 1)
                    probeRequiredColumns(fresh)
                    probeForeignKey(fresh)
                }
                probeForeignKey(db)
                probeRequiredColumns(db)
                receipt("adjacent:$start->${start + 1}:all-columns+fresh-schema+triggers+fk")
            }
        }
    }

    @Test
    fun everySupportedPopulatedStartPreservesExistingColumnsThroughTheCurrentChain() {
        for (start in 1 until KNITTOOLS_DATABASE_VERSION) {
            val name = "audit-chain-$start-${UUID.randomUUID()}"
            val before =
                helper.createDatabase(name, start).use { db ->
                    seed(db, start)
                    rows(db)
                }
            helper
                .runMigrationsAndValidate(
                    name,
                    KNITTOOLS_DATABASE_VERSION,
                    true,
                    *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
                ).use { db ->
                    assertPreserved(before, rows(db), start, KNITTOOLS_DATABASE_VERSION)
                    probeForeignKey(db)
                    receipt("chain:$start->$KNITTOOLS_DATABASE_VERSION:all-columns+fk")
                }
        }
    }

    @Test
    fun completionBackfillIncludesOnlyValidCurrentCompletionsAndPreservesEarlierTables() {
        val name = "audit-completions-${UUID.randomUUID()}"
        val before =
            helper.createDatabase(name, 24).use { db ->
                seed(db, 24)
                db.execSQL(
                    """
                    INSERT INTO counter_projects
                        (id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt, isCompleted, completedAt)
                    VALUES
                        (43, 'Valid', 8, 2, 1, 'kept', 1000, 2000, 1, 2000),
                        (44, 'Zero', 9, 3, 1, 'kept', 1000, 2000, 1, 0),
                        (45, 'Before creation', 10, 4, 1, 'kept', 1000, 2000, 1, 999),
                        (46, 'Future', 11, 5, 1, 'kept', 1000, 2000, 1, 9223372036854775807)
                    """.trimIndent(),
                )
                rows(db)
            }
        helper.runMigrationsAndValidate(name, 25, true, KnitToolsDatabase.MIGRATION_24_25).use { db ->
            val after = rows(db)
            before.forEach { (table, values) -> assertEquals(table, values, after.getValue(table)) }
            val events = after.getValue("project_completions")
            assertEquals(listOf("43", "46"), events.map { it["projectId"] })
            assertEquals(listOf("2000", Long.MAX_VALUE.toString()), events.map { it["completedAt"] })
            assertTrue(events.all { it["zoneId"] == null })
            assertEquals(2, events.map { it["id"] }.distinct().size)
        }
    }

    @Test
    fun failureAfterEachMigrationRollsBackSchemaRowsAndVersionThenAllowsRetry() {
        val autoMigrations =
            listOf(KnitToolsDatabase_AutoMigration_1_2_Impl(), KnitToolsDatabase_AutoMigration_2_3_Impl())
        val migrations = autoMigrations + KnitToolsDatabase.ALL_MANUAL_MIGRATIONS
        for (migration in migrations) {
            val start = migration.startVersion
            val name = "audit-rollback-$start-${UUID.randomUUID()}"
            lateinit var beforeRows: Map<String, List<Map<String, String?>>>
            lateinit var beforeSchema: List<List<String?>>
            helper.createDatabase(name, start).use { db ->
                seed(db, start)
                beforeRows = rows(db)
                beforeSchema = schema(db)
            }
            val marker = "audit-failure-$start"
            val failing =
                object : Migration(start, migration.endVersion) {
                    override fun migrate(connection: SQLiteConnection) {
                        migration.migrate(connection)
                        error(marker)
                    }
                }
            try {
                helper.runMigrationsAndValidate(name, migration.endVersion, true, failing).close()
                fail("Expected injected migration failure: $start")
            } catch (failure: IllegalStateException) {
                assertTrue(failure.message.orEmpty().contains(marker))
            }
            helper.runMigrationsAndValidate(name, start, true).use { db ->
                assertEquals(start, db.version)
                assertEquals(beforeSchema, schema(db))
                assertEquals(beforeRows, rows(db))
            }
            helper
                .runMigrationsAndValidate(
                    name,
                    KNITTOOLS_DATABASE_VERSION,
                    true,
                    *KnitToolsDatabase.ALL_MANUAL_MIGRATIONS,
                ).use { db ->
                    assertPreserved(beforeRows, rows(db), start, KNITTOOLS_DATABASE_VERSION)
                    probeForeignKey(db)
                }
            receipt("rollback:$start->${migration.endVersion}:schema+rows+version+retry-to-$KNITTOOLS_DATABASE_VERSION")
        }
    }

    private fun constraints(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        if (version >= 17) PatternAnnotationSchemaConstraints.create(db)
        if (version >= 21) ActiveSessionSchemaConstraints.create(db)
        if (version >= 22) ProjectDocumentSchemaConstraints.create(db)
    }

    private fun seed(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        constraints(db, version)
        val pending = tables(db).toMutableSet()
        while (pending.isNotEmpty()) {
            val table =
                pending.first { candidate ->
                    db.query("PRAGMA foreign_key_list(`$candidate`)").use { cursor ->
                        buildList { while (cursor.moveToNext()) add(cursor.getString(2)) }.none { it in pending }
                    }
                }
            val columns =
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                Triple(
                                    cursor.getString(1),
                                    cursor.getString(2),
                                    cursor.getInt(3) == 1,
                                ),
                            )
                        }
                    }
                }
            val ids = if (table == "active_sessions") listOf(41L) else listOf(41L, 42L)
            ids.forEach { id ->
                val values = columns.map { (column, type, required) -> fixtureValue(table, column, type, required, id) }
                db.execSQL(
                    "INSERT INTO `$table` (${columns.joinToString(
                        ",",
                    ) { "`${it.first}`" }}) VALUES (${columns.joinToString(",") { "?" }})",
                    values.toTypedArray(),
                )
            }
            pending.remove(table)
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse("Invalid fixture: $version", it.moveToFirst()) }
    }

    private fun fixtureValue(
        table: String,
        column: String,
        type: String,
        required: Boolean,
        id: Long,
    ): Any? {
        if (table == "pattern_annotation_layers" && column == "savedPatternId") return null
        if (table == "pattern_annotation_layers" && column == "projectId") return id
        if (!required && id == 42L) return null
        return when (column) {
            "singletonId" -> 1L
            "id", "projectId", "yarnCardId", "projectYarnNoteId", "linkedProjectId",
            "savedYarnCardId", "savedPatternId", "linkedPatternId", "folderId", "layerId",
            -> id
            "createdAt", "savedAt", "startedAt" -> 1000L + id
            "updatedAt", "timestamp", "completedAt", "endedAt" -> 2000L + id
            "isCompleted" -> if (id == 42L) 1L else 0L
            "isActive", "isPrimary", "isFree", "isAvailableOffline", "readingLineEnabled",
            "readingLineFollowCurrentRow",
            "verticalReadingGuideEnabled", "stitchTrackingEnabled", "linkedToMainCounter", "recoveryPromptShown",
            -> 1L
            "secondaryCounterUsed", "notesCreated" -> 0L
            "payloadVersion" -> 1L
            "craftType" -> "CROCHET"
            "mainCounterLabelType" -> "ROUNDS"
            "counterType" -> "COUNT_UP"
            "status" -> "IN_STASH"
            "source" -> "LOCAL_FILE"
            "availability" -> "unknown"
            "kind" -> "FREEHAND"
            "color" -> "#FF0000"
            "pathData" -> "M 0 0 L 1 1"
            "payloadJson" -> "{\"legacyPathData\":\"M 0 0 L 1 1\"}"
            "patternUri", "patternUrl", "localPdfUri" -> "content://audit/$id.pdf"
            "yarnCardIds" -> id.toString()
            "zoneId", "startZoneId" -> "Europe/Helsinki"
            "documentKey" -> "doc:$id"
            "readingLineYFraction", "verticalReadingGuideXFraction", "yFraction" -> 0.375
            else ->
                when (type) {
                    "TEXT" -> "Ä-$table-$column-$id\ntext"
                    "REAL" -> id + 0.125
                    else -> id
                }
        }
    }

    private fun assertPreserved(
        before: Map<String, List<Map<String, String?>>>,
        after: Map<String, List<Map<String, String?>>>,
        start: Int,
        end: Int,
    ) {
        before.forEach { (table, originalRows) ->
            val current = after.getValue(table)
            assertEquals("$start->$end/$table count", originalRows.size, current.size)
            originalRows.zip(current).forEach { (original, row) ->
                original.forEach { (column, value) ->
                    if (column !in row) {
                        val allowed =
                            when (table) {
                                "saved_patterns" -> setOf("ravelryId", "patternUrl", "isFree")
                                "pattern_annotations" -> setOf("projectId", "pathData", "color", "strokeWidth")
                                else -> emptySet()
                            }
                        assertTrue("Unexpected removed column: $table.$column", column in allowed)
                    } else {
                        assertEquals("$start->$end/$table.$column", value, row[column])
                    }
                }
            }
        }
    }

    private fun tables(db: SupportSQLiteDatabase): Set<String> =
        db
            .query(
                """
                SELECT name FROM sqlite_master
                WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
                    AND name NOT IN ('android_metadata', 'room_master_table')
                ORDER BY name
                """.trimIndent(),
            ).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }

    private fun rows(db: SupportSQLiteDatabase): Map<String, List<Map<String, String?>>> =
        tables(db).associateWith { table ->
            db.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            cursor.columnNames.withIndex().associate { (index, name) ->
                                name to
                                    if (cursor.isNull(index)) null else cursor.getString(index)
                            },
                        )
                    }
                }
            }
        }

    private fun schema(db: SupportSQLiteDatabase): List<List<String?>> =
        db.query("SELECT type, name, tbl_name, sql FROM sqlite_master ORDER BY type, name").use(::cursorRows)

    private fun triggers(db: SupportSQLiteDatabase): List<List<String?>> =
        db.query("SELECT name, sql FROM sqlite_master WHERE type = 'trigger' ORDER BY name").use(::cursorRows)

    private fun cursorRows(cursor: Cursor): List<List<String?>> =
        buildList {
            while (cursor.moveToNext()) {
                add(
                    List(cursor.columnCount) { if (cursor.isNull(it)) null else cursor.getString(it) },
                )
            }
        }

    private fun probeForeignKey(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON")
        try {
            db.execSQL("UPDATE counter_history SET projectId = 999999 WHERE id = 41")
            fail("Expected foreign key rejection")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            assertEquals("41", rows(db).getValue("counter_history").first()["projectId"])
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }

    private fun probeRequiredColumns(db: SupportSQLiteDatabase) {
        val before = rows(db)
        tables(db).forEach { table ->
            if (before.getValue(table).isEmpty()) return@forEach
            val column =
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    generateSequence { if (cursor.moveToNext()) cursor else null }
                        .firstOrNull { it.getInt(3) == 1 && it.getInt(5) == 0 }
                        ?.getString(1)
                } ?: return@forEach
            try {
                db.execSQL("UPDATE `$table` SET `$column` = NULL WHERE rowid = (SELECT MIN(rowid) FROM `$table`)")
                fail("Expected required-column rejection: $table.$column")
            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                assertEquals("Rejected write changed rows: $table.$column", before, rows(db))
            }
        }
    }

    private fun receipt(case: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("audit_case", case) })
    }
}
