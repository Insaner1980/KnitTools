package com.finnvek.knittools.data.backup

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.File

internal data class BackupColumn(
    val name: String,
    val type: String,
    val required: Boolean,
)

internal object BackupTables {
    fun columns(
        db: SupportSQLiteDatabase,
        table: String,
    ): List<BackupColumn> {
        require(table in BackupFormat.tables)
        return db.query("PRAGMA table_info(`$table`)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(BackupColumn(cursor.getString(1), cursor.getString(2), cursor.getInt(3) == 1))
                }
            }.sortedBy { it.name }
        }
    }

    fun export(
        db: SupportSQLiteDatabase,
        directory: File,
        transform: (String, MutableMap<String, JsonElement>) -> Unit,
        check: () -> Unit,
    ) {
        BackupFormat.requireValid(db.version == 25, BackupError.UNSUPPORTED)
        BackupFormat.tables.forEach { table ->
            val columns = columns(db, table)
            val file = File(directory, "tables/$table.jsonl")
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { output ->
                output.appendLine(JsonArray(columns.map { JsonPrimitive(it.name) }).toString())
                db.query("SELECT * FROM `$table`").use { cursor ->
                    var count = 0
                    while (cursor.moveToNext()) {
                        check()
                        BackupFormat.requireValid(++count <= BackupFormat.MAX_ROWS)
                        val row =
                            columns
                                .associate {
                                    it.name to
                                        cursor.value(
                                            cursor.getColumnIndexOrThrow(it.name),
                                        )
                                }.toMutableMap()
                        transform(table, row)
                        val line = JsonArray(columns.map { row.getValue(it.name) }).toString()
                        BackupFormat.requireValid(line.length <= BackupFormat.MAX_ROW)
                        output.appendLine(line)
                        BackupFormat.requireValid(file.length() <= BackupFormat.MAX_TABLE)
                    }
                }
            }
        }
    }

    fun import(
        db: SupportSQLiteDatabase,
        directory: File,
        transform: (String, MutableMap<String, JsonElement>) -> Unit = { _, _ -> },
        check: () -> Unit = {},
    ) {
        BackupFormat.tables.forEach { table ->
            val columns = columns(db, table)
            val names = columns.joinToString(",") { "`${it.name}`" }
            val placeholders = columns.joinToString(",") { "?" }
            db.compileStatement("INSERT INTO `$table` ($names) VALUES ($placeholders)").use { statement ->
                read(directory, table, columns.map { it.name }, check) { row ->
                    transform(table, row)
                    statement.clearBindings()
                    columns.forEachIndexed { index, column ->
                        val value = row.getValue(column.name)
                        when {
                            value == JsonNull -> {
                                BackupFormat.requireValid(!column.required, BackupError.VALIDATION)
                                statement.bindNull(index + 1)
                            }
                            column.type == "TEXT" -> {
                                BackupFormat.requireValid(value.jsonPrimitive.isString, BackupError.VALIDATION)
                                statement.bindString(index + 1, value.jsonPrimitive.content)
                            }
                            column.type == "INTEGER" -> {
                                BackupFormat.requireValid(!value.jsonPrimitive.isString, BackupError.VALIDATION)
                                val number =
                                    value.jsonPrimitive.longOrNull ?: invalidValue()
                                validateInteger(column.name, number)
                                statement.bindLong(index + 1, number)
                            }
                            column.type == "REAL" -> {
                                BackupFormat.requireValid(!value.jsonPrimitive.isString, BackupError.VALIDATION)
                                val number =
                                    value.jsonPrimitive.doubleOrNull ?: invalidValue()
                                BackupFormat.requireValid(number.isFinite(), BackupError.VALIDATION)
                                statement.bindDouble(index + 1, number)
                            }
                            else -> throw BackupException(BackupError.VALIDATION)
                        }
                    }
                    statement.executeInsert()
                }
            }
        }
        verify(db)
    }

    fun read(
        directory: File,
        table: String,
        columns: List<String>,
        check: () -> Unit = {},
        consume: (MutableMap<String, JsonElement>) -> Unit,
    ) {
        File(directory, "tables/$table.jsonl").bufferedReader().use { input ->
            val headerText = input.boundedLine() ?: ""
            BackupFormat.requireJsonDepth(headerText, 1)
            val header = BackupFormat.json.parseToJsonElement(headerText).jsonArray
            BackupFormat.requireValid(header == JsonArray(columns.map(::JsonPrimitive)), BackupError.VALIDATION)
            var count = 0
            while (true) {
                check()
                val line = input.boundedLine() ?: break
                BackupFormat.requireValid(++count <= BackupFormat.MAX_ROWS, BackupError.VALIDATION)
                BackupFormat.requireJsonDepth(line, 1)
                val values = BackupFormat.json.parseToJsonElement(line).jsonArray
                BackupFormat.requireValid(
                    values.size == columns.size && values.all { it is JsonPrimitive },
                    BackupError.VALIDATION,
                )
                consume(columns.zip(values).toMap().toMutableMap())
            }
        }
    }

    fun clear(db: SupportSQLiteDatabase) {
        BackupFormat.tables.reversed().forEach { db.execSQL("DELETE FROM `$it`") }
    }

    fun verify(db: SupportSQLiteDatabase) {
        db
            .query(
                "PRAGMA foreign_key_check",
            ).use { BackupFormat.requireValid(!it.moveToFirst(), BackupError.VALIDATION) }
        db.query("PRAGMA quick_check").use {
            BackupFormat.requireValid(it.moveToFirst() && it.getString(0) == "ok", BackupError.VALIDATION)
        }
        db.query("SELECT projectId FROM project_documents GROUP BY projectId HAVING SUM(isPrimary) != 1").use {
            BackupFormat.requireValid(!it.moveToFirst(), BackupError.VALIDATION)
        }
        db.query("SELECT kind, payloadVersion, payloadJson FROM pattern_annotations").use { cursor ->
            while (cursor.moveToNext()) {
                val kind =
                    PatternAnnotationKind.entries.firstOrNull { it.name == cursor.getString(0) }
                        ?: invalidValue()
                BackupFormat.requireJsonDepth(cursor.getString(2), 8)
                BackupFormat.requireValid(
                    PatternAnnotationPayloadCodec.decode(kind, cursor.getInt(1), cursor.getString(2)) != null,
                    BackupError.VALIDATION,
                )
            }
        }
    }

    fun references(db: SupportSQLiteDatabase): Set<String> =
        buildSet {
            BackupFiles.columns.forEach { (table, column) ->
                db
                    .query(
                        "SELECT DISTINCT `$column` FROM `$table` WHERE `$column` IS NOT NULL AND `$column` != ''",
                    ).use { cursor ->
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
            }
        }

    private fun invalidValue(): Nothing = throw BackupException(BackupError.VALIDATION)

    private fun validateInteger(
        column: String,
        number: Long,
    ) {
        if (column in booleanColumns) BackupFormat.requireValid(number in 0..1, BackupError.VALIDATION)
        val identity = column == "id" || column.endsWith("Id")
        if (identity) BackupFormat.requireValid(number > 0, BackupError.VALIDATION)
        if (!identity && column !in longColumns) {
            BackupFormat.requireValid(number in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong(), BackupError.VALIDATION)
        }
    }

    private val booleanColumns =
        setOf(
            "readingLineEnabled",
            "readingLineFollowCurrentRow",
            "verticalReadingGuideEnabled",
            "secondaryCounterUsed",
            "notesCreated",
            "isCompleted",
            "stitchTrackingEnabled",
            "linkedToMainCounter",
            "isActive",
            "isPrimary",
            "isAvailableOffline",
            "recoveryPromptShown",
        )
    private val longColumns =
        setOf(
            "careSymbols",
            "zIndex",
            "timestamp",
            "createdAt",
            "updatedAt",
            "completedAt",
            "lastSyncedAt",
            "savedAt",
            "startedAt",
            "endedAt",
            "durationSeconds",
            "checkpointedDurationSeconds",
            "reviewedDurationBaselineSeconds",
            "startedAtWallMillis",
            "segmentStartedAtWallMillis",
            "segmentStartedElapsedRealtimeMillis",
            "updatedAtWallMillis",
            "bootCount",
            "recoverySuggestedDurationSeconds",
        )

    private fun Cursor.value(index: Int): JsonElement =
        when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> JsonNull
            Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(index))
            Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(index))
            Cursor.FIELD_TYPE_STRING -> JsonPrimitive(getString(index))
            else -> throw BackupException(BackupError.VALIDATION)
        }

    private fun BufferedReader.boundedLine(): String? {
        val line = StringBuilder()
        while (true) {
            val value = read()
            if (value == -1) {
                BackupFormat.requireValid(line.isEmpty())
                return null
            }
            if (value == '\n'.code) return line.toString()
            BackupFormat.requireValid(line.length < BackupFormat.MAX_ROW)
            line.append(value.toChar())
        }
    }
}
