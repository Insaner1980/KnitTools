package com.finnvek.knittools.data.backup

import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnvek.knittools.domain.model.formatYarnCardIds
import com.finnvek.knittools.domain.model.parseYarnCardIdsWithinLimits
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID

internal class BackupIdentityMap(
    source: SupportSQLiteDatabase,
    destination: SupportSQLiteDatabase,
    limits: BackupLimits = BackupLimits(),
) {
    private val ids = mutableMapOf<String, Map<Long, Long>>()
    private val archiveKeyPrefix = "restored-key:${UUID.randomUUID()}:"

    init {
        val budget = BackupBudget(limits)
        BackupFormat.identityTables.forEach { table ->
            val maximum =
                destination.query("SELECT COALESCE(MAX(id), 0) FROM `$table`").use {
                    it.moveToFirst()
                    it.getLong(0)
                }
            val sequence =
                destination.query("SELECT seq FROM sqlite_sequence WHERE name = ?", arrayOf(table)).use {
                    if (it.moveToFirst()) it.getLong(0) else 0
                }
            var next = maxOf(0L, maximum, sequence)
            val mapping = linkedMapOf<Long, Long>()

            fun retain(sourceId: Long) {
                BackupFormat.requireValid(sourceId > 0L, BackupError.VALIDATION)
                if (sourceId in mapping) return
                budget.addIdentity(table)
                BackupFormat.requireValid(next < Long.MAX_VALUE, BackupError.RESTORE)
                mapping[sourceId] = ++next
            }

            source.query("SELECT id FROM `$table` ORDER BY id").use { cursor ->
                while (cursor.moveToNext()) retain(cursor.getLong(0))
            }
            if (table == "project_counters") {
                source
                    .query(
                        "SELECT payloadJson FROM pattern_annotations WHERE kind = 'CHART_TRACKER' ORDER BY id",
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            parseChartCounterId(cursor.getString(0), budget)?.let(::retain)
                        }
                    }
            }
            ids[table] = mapping
        }
    }

    fun apply(
        table: String,
        row: MutableMap<String, JsonElement>,
    ) {
        val yarnCardIds = validatedYarnCardIds(row)
        if (table in ids) remap(row, "id", table)
        remap(row, "projectId", "counter_projects")
        remap(row, "linkedProjectId", "counter_projects", optional = true)
        remap(row, "savedPatternId", "saved_patterns")
        remap(row, "linkedPatternId", "saved_patterns", optional = true)
        remap(row, "savedYarnCardId", "yarn_cards", optional = true)
        remap(row, "yarnCardId", "yarn_cards")
        remap(row, "projectYarnNoteId", "project_yarn_notes")
        remap(row, "layerId", "pattern_annotation_layers")
        remap(row, "folderId", "project_folders")
        yarnCardIds?.let { sourceIds ->
            row["yarnCardIds"] =
                JsonPrimitive(
                    formatYarnCardIds(
                        sourceIds.mapNotNull {
                            ids["yarn_cards"]?.get(it)
                        },
                    ),
                )
        }
        rebaseDocumentKey(row)
        rebaseAnnotationCounter(table, row)
        if (table == "active_sessions") resetSessionAnchors(row)
    }

    private fun validatedYarnCardIds(row: Map<String, JsonElement>): List<Long>? {
        val value = row["yarnCardIds"] ?: return null
        return parseYarnCardIdsWithinLimits(value.jsonPrimitive.content)
            ?: throw BackupException(BackupError.VALIDATION)
    }

    private fun rebaseDocumentKey(row: MutableMap<String, JsonElement>) {
        row["documentKey"]?.let { value ->
            val key = value.jsonPrimitive.content
            val saved = Regex("saved:([0-9]+):v1").matchEntire(key)
            val legacy = Regex("legacy-project:([0-9]+)").matchEntire(key)
            row["documentKey"] =
                JsonPrimitive(
                    when {
                        saved != null ->
                            ids["saved_patterns"]?.get(saved.groupValues[1].toLong())?.let { "saved:$it:v1" }
                                ?: "$archiveKeyPrefix$key"
                        legacy != null ->
                            ids["counter_projects"]?.get(legacy.groupValues[1].toLong())?.let { "legacy-project:$it" }
                                ?: "$archiveKeyPrefix$key"
                        else -> key
                    },
                )
        }
    }

    private fun rebaseAnnotationCounter(
        table: String,
        row: MutableMap<String, JsonElement>,
    ) {
        if (table == "pattern_annotations" && row["kind"]?.jsonPrimitive?.content == "CHART_TRACKER") {
            val payload =
                BackupFormat.json
                    .parseToJsonElement(
                        row.getValue("payloadJson").jsonPrimitive.content,
                    ).jsonObject
                    .toMutableMap()
            payload["extraCounterId"]?.takeUnless { it == JsonNull }?.let { value ->
                payload["extraCounterId"] =
                    JsonPrimitive(target("project_counters", value.jsonPrimitive.long))
            }
            row["payloadJson"] = JsonPrimitive(JsonObject(payload).toString())
        }
    }

    private fun resetSessionAnchors(row: MutableMap<String, JsonElement>) {
        row["bootCount"] = JsonNull
        row["recoveryReason"] = JsonPrimitive("BOOT_IDENTITY_UNAVAILABLE")
        row["recoverySuggestedDurationSeconds"] = JsonNull
        row["recoveryPromptShown"] = JsonPrimitive(0)
        row["sessionToken"] = JsonPrimitive(UUID.randomUUID().toString())
        row["recoveryIntervalToken"] = JsonPrimitive(UUID.randomUUID().toString())
    }

    private fun remap(
        row: MutableMap<String, JsonElement>,
        column: String,
        table: String,
        optional: Boolean = false,
    ) {
        val value = row[column]?.takeUnless { it == JsonNull } ?: return
        val id = ids[table]?.get(value.jsonPrimitive.long)
        BackupFormat.requireValid(optional || id != null, BackupError.VALIDATION)
        row[column] = id?.let(::JsonPrimitive) ?: JsonNull
    }

    fun reserveIds(db: SupportSQLiteDatabase) {
        ids.forEach { (table, mapping) ->
            mapping.values.maxOrNull()?.let { maximum ->
                db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = ?", arrayOf<Any>(maximum, table))
                db.execSQL(
                    "INSERT INTO sqlite_sequence(name,seq) SELECT ?,? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = ?)",
                    arrayOf<Any>(table, maximum, table),
                )
            }
        }
    }

    private fun target(
        table: String,
        id: Long,
    ): Long = ids[table]?.get(id) ?: throw BackupException(BackupError.VALIDATION)
}
