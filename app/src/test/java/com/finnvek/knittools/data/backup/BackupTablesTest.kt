package com.finnvek.knittools.data.backup

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.finnvek.knittools.domain.model.YARN_CARD_IDS_MAX_CHARACTERS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupTablesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val statement = mockk<SupportSQLiteStatement>(relaxed = true)
    private val columns =
        listOf(
            listOf(0, "id", "INTEGER", 1),
            listOf(1, "name", "TEXT", 1),
            listOf(2, "amount", "REAL", 0),
            listOf(3, "optional", "TEXT", 0),
        )

    private fun database(): SupportSQLiteDatabase =
        mockk(relaxed = true) {
            every { version } returns 25
            every { compileStatement(any()) } returns statement
            every { query(any<String>()) } answers {
                when {
                    firstArg<String>().startsWith("PRAGMA table_info") -> backupCursor(columns)
                    firstArg<String>() == "PRAGMA quick_check" -> backupCursor(listOf(listOf("ok")))
                    firstArg<String>().startsWith("SELECT *") ->
                        backupCursor(listOf(listOf(1L, "Wool", 2.5, null))).also {
                            every { it.getColumnIndexOrThrow(any()) } answers
                                { columns.indexOfFirst { it[1] == firstArg<String>() } }
                        }
                    firstArg<String>().startsWith("SELECT DISTINCT") -> backupCursor(listOf(listOf("files/shared.bin")))
                    else -> backupCursor(emptyList())
                }
            }
        }

    @Test fun exportAndImportPreserveTypesNullsAndTransformations() {
        val db = database()
        val folder = temporary.newFolder()
        BackupTables.export(db, folder, { _, row -> row["name"] = JsonPrimitive("Renamed") }) {}
        assertEquals(
            "[\"amount\",\"id\",\"name\",\"optional\"]\n[2.5,1,\"Renamed\",null]\n",
            File(folder, "tables/sessions.jsonl").readText(),
        )
        BackupTables.import(db, folder)
        verify(exactly = BackupFormat.tables.size) { statement.bindDouble(1, 2.5) }
        verify(exactly = BackupFormat.tables.size) { statement.bindLong(2, 1) }
        verify(exactly = BackupFormat.tables.size) { statement.bindString(3, "Renamed") }
        verify(exactly = BackupFormat.tables.size) { statement.bindNull(4) }
        verify(exactly = BackupFormat.tables.size) { statement.executeInsert() }
        assertEquals(setOf("files/shared.bin"), BackupTables.references(db))
        BackupTables.clear(db)
        verifyOrder {
            db.execSQL("DELETE FROM `project_completions`")
            db.execSQL("DELETE FROM `counter_projects`")
        }
    }

    @Test fun malformedRowsHeadersAndUnterminatedLinesAreRejected() {
        val folder = temporary.newFolder()
        val file = File(folder, "tables/sessions.jsonl").apply { requireNotNull(parentFile).mkdirs() }
        for (text in listOf("", "[\"wrong\"]\n", "[\"id\"]\n[1,2]\n", "[\"id\"]\n[{}]\n", "[\"id\"]\n[1]")) {
            file.writeText(text)
            assertThrows(Exception::class.java) { BackupTables.read(folder, "sessions", listOf("id")) {} }
        }
        file.writeText("[\"id\"]\n[1]\n")
        val rows = mutableListOf<Map<String, JsonElement>>()
        BackupTables.read(folder, "sessions", listOf("id")) { rows.add(it) }
        assertEquals(listOf(mapOf("id" to JsonPrimitive(1))), rows)
    }

    @Test fun importRejectsInvalidColumnValuesBeforeExecutingInsert() {
        val cases =
            listOf(
                Triple("id", "INTEGER", "0"),
                Triple("projectId", "INTEGER", "-1"),
                Triple("isPrimary", "INTEGER", "2"),
                Triple("count", "INTEGER", "2147483648"),
                Triple("count", "INTEGER", "1.5"),
                Triple("count", "INTEGER", "\"1\""),
                Triple("value", "REAL", "\"1.0\""),
                Triple("value", "REAL", "true"),
                Triple("value", "REAL", "1e999"),
                Triple("name", "TEXT", "1"),
                Triple("name", "TEXT", "null"),
                Triple("value", "BLOB", "1"),
            )
        for ((name, type, value) in cases) {
            val db = database()
            every { db.query(match<String> { it.startsWith("PRAGMA table_info") }) } answers {
                backupCursor(listOf(listOf(0, name, type, 1)))
            }
            val folder = temporary.newFolder()
            File(folder, "tables/counter_projects.jsonl").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("[\"$name\"]\n[$value]\n")
            }
            assertThrows(BackupException::class.java) { BackupTables.import(db, folder) }
        }
    }

    @Test fun yarnCardIdsPreflightRejectsOversizedValueBeforeDatabaseMutation() {
        val folder = temporary.newFolder()
        val oversized = "x".repeat(YARN_CARD_IDS_MAX_CHARACTERS + 1)
        File(folder, "tables/counter_projects.jsonl").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("[\"yarnCardIds\"]\n${JsonArray(listOf(JsonPrimitive(oversized)))}\n")
        }

        val failure =
            assertThrows(BackupException::class.java) {
                BackupTables.preflightYarnCardIds(folder, listOf("yarnCardIds"))
            }

        assertEquals(BackupError.VALIDATION, failure.error)
        verify(exactly = 0) { statement.executeInsert() }
    }

    @Test fun timestampsKeepLongPrecisionAndNullableValuesRemainUnknown() {
        val db = database()
        every { db.query(match<String> { it.startsWith("PRAGMA table_info") }) } answers {
            backupCursor(listOf(listOf(0, "timestamp", "INTEGER", 0)))
        }
        val folder = temporary.newFolder()
        BackupFormat.tables.forEach {
            File(folder, "tables/$it.jsonl").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("[\"timestamp\"]\n[9223372036854775807]\n[null]\n")
            }
        }
        BackupTables.import(db, folder)
        verify { statement.bindLong(1, Long.MAX_VALUE) }
        verify { statement.bindNull(1) }
    }

    @Test fun databaseIntegrityAndAnnotationFailuresAreRejected() {
        val failures =
            listOf(
                "PRAGMA foreign_key_check" to listOf(listOf<Any?>(1)),
                "PRAGMA quick_check" to emptyList(),
                "PRAGMA quick_check" to listOf(listOf<Any?>("corrupt")),
                "SELECT projectId FROM project_documents GROUP BY projectId HAVING SUM(isPrimary) != 1" to
                    listOf(listOf<Any?>(1)),
                "SELECT kind, payloadVersion, payloadJson FROM pattern_annotations" to
                    listOf(listOf<Any?>("UNKNOWN", 1, "{}")),
                "SELECT kind, payloadVersion, payloadJson FROM pattern_annotations" to
                    listOf(listOf<Any?>("CHART_TRACKER", 999, "{}")),
            )
        failures.forEach { (query, rows) ->
            val db = database()
            every { db.query(query) } answers { backupCursor(rows) }
            assertThrows(BackupException::class.java) { BackupTables.verify(db) }
        }
    }

    @Test fun unsupportedDatabaseAndBinaryValuesCannotBeExported() {
        val db = database()
        every { db.version } returns 26
        assertThrows(BackupException::class.java) { BackupTables.export(db, temporary.newFolder(), { _, _ -> }) {} }
        every { db.version } returns 25
        every { db.query(match<String> { it.startsWith("SELECT *") }) } answers {
            backupCursor(listOf(listOf(byteArrayOf(1)))).also {
                every { it.getColumnIndexOrThrow(any()) } returns 0
            }
        }
        assertThrows(BackupException::class.java) { BackupTables.export(db, temporary.newFolder(), { _, _ -> }) {} }
    }
}
