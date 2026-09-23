package com.finnvek.knittools.data.backup

import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPageBudget
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal data class BackupLimits(
    val maxArchiveBytes: Long = MAX_ARCHIVE_BYTES,
    val maxExtractedBytes: Long = MAX_EXTRACTED_BYTES,
    val maxTableBytes: Long = MAX_TABLE_BYTES,
    val maxTableTotalBytes: Long = MAX_TABLE_TOTAL_BYTES,
    val maxRowsPerTable: Long = MAX_ROWS_PER_TABLE,
    val maxSessionRows: Long = MAX_SESSION_ROWS,
    val maxTotalRows: Long = MAX_TOTAL_ROWS,
    val maxRowCharacters: Int = MAX_ROW_CHARACTERS,
    val maxFieldCharacters: Int = MAX_FIELD_CHARACTERS,
    val maxDurableFileBytes: Long = MAX_DURABLE_FILE_BYTES,
    val maxDurableFileCount: Long = MAX_DURABLE_FILE_COUNT,
    val maxDurableBytes: Long = MAX_DURABLE_BYTES,
    val maxIdentitiesPerTable: Long = MAX_IDENTITIES_PER_TABLE,
    val maxTotalIdentities: Long = MAX_TOTAL_IDENTITIES,
) {
    companion object {
        const val MAX_ARCHIVE_BYTES = 512L * 1_024L * 1_024L
        const val MAX_EXTRACTED_BYTES = 512L * 1_024L * 1_024L
        const val MAX_TABLE_BYTES = 32L * 1_024L * 1_024L
        const val MAX_TABLE_TOTAL_BYTES = 64L * 1_024L * 1_024L
        const val MAX_ROWS_PER_TABLE = 100_000L
        const val MAX_SESSION_ROWS = 10_000L
        const val MAX_TOTAL_ROWS = 250_000L
        const val MAX_ROW_CHARACTERS = 2 * 1_024 * 1_024
        const val MAX_FIELD_CHARACTERS = 256 * 1_024
        const val MAX_DURABLE_FILE_BYTES = 200L * 1_024L * 1_024L
        const val MAX_DURABLE_FILE_COUNT = 2_000L
        const val MAX_DURABLE_BYTES = 448L * 1_024L * 1_024L
        const val MAX_IDENTITIES_PER_TABLE = 100_000L
        const val MAX_TOTAL_IDENTITIES = 150_000L
    }
}

internal class BackupBudget(
    val limits: BackupLimits = BackupLimits(),
    private val trackEmbeddedIdentities: Boolean = true,
) {
    private var extractedBytes = 0L
    private var tableBytes = 0L
    private var durableFileBytes = 0L
    private var durableFileCount = 0L
    private var totalRows = 0L
    private val rowsByTable = mutableMapOf<String, Long>()
    private var totalIdentities = 0L
    private val identitiesByTable = mutableMapOf<String, Long>()
    private val projectCounterIds = mutableSetOf<Long>()
    private val danglingChartCounterIds = mutableSetOf<Long>()
    private var durableCopyBytes = 0L
    private val durableCopies = mutableSetOf<String>()
    private val annotationPages = mutableMapOf<Pair<Long, Int>, PatternAnnotationPageBudget>()

    fun requireArchiveSize(
        bytes: Long,
        error: BackupError = BackupError.CORRUPT,
    ) {
        BackupFormat.requireValid(bytes in 0..limits.maxArchiveBytes, error)
    }

    fun addArchiveEntry(
        path: String,
        bytes: Long,
    ) {
        BackupFormat.requireValid(bytes >= 0L)
        extractedBytes = BackupFormat.addWithinLimit(extractedBytes, bytes, limits.maxExtractedBytes)
        if (path in BackupFormat.tablePaths) {
            requireTableSize(bytes)
            tableBytes = BackupFormat.addWithinLimit(tableBytes, bytes, limits.maxTableTotalBytes)
        } else {
            BackupFormat.requireValid(bytes <= limits.maxDurableFileBytes)
            durableFileCount = BackupFormat.addWithinLimit(durableFileCount, 1L, limits.maxDurableFileCount)
            durableFileBytes = BackupFormat.addWithinLimit(durableFileBytes, bytes, limits.maxDurableBytes)
        }
    }

    fun addTable(
        table: String,
        bytes: Long,
    ) {
        BackupFormat.requireValid(table in BackupFormat.tables)
        requireTableSize(bytes)
        tableBytes = BackupFormat.addWithinLimit(tableBytes, bytes, limits.maxTableTotalBytes)
    }

    fun requireTableSize(bytes: Long) {
        BackupFormat.requireValid(bytes in 0..limits.maxTableBytes)
    }

    fun addRow(table: String) {
        val rowLimit =
            if (table == "sessions") {
                minOf(limits.maxRowsPerTable, limits.maxSessionRows, BackupLimits.MAX_SESSION_ROWS)
            } else {
                limits.maxRowsPerTable
            }
        val tableRows = BackupFormat.addWithinLimit(rowsByTable[table] ?: 0L, 1L, rowLimit)
        rowsByTable[table] = tableRows
        totalRows = BackupFormat.addWithinLimit(totalRows, 1L, limits.maxTotalRows)
        if (table in BackupFormat.identityTables) addIdentity(table)
    }

    fun requireRowLength(length: Int) {
        BackupFormat.requireValid(length in 0..limits.maxRowCharacters, BackupError.VALIDATION)
    }

    fun requireFieldLength(length: Int) {
        BackupFormat.requireValid(length in 0..limits.maxFieldCharacters, BackupError.VALIDATION)
    }

    fun observeRow(
        table: String,
        row: Map<String, JsonElement>,
    ) {
        if (table == "pattern_annotations") {
            val layerId = row["layerId"]?.jsonPrimitive?.longOrNull ?: 0L
            val page = row["page"]?.jsonPrimitive?.longOrNull ?: -1L
            val version = row["payloadVersion"]?.jsonPrimitive?.longOrNull
            BackupFormat.requireValid(layerId > 0L && page in 0L..Int.MAX_VALUE, BackupError.VALIDATION)
            BackupFormat.requireValid(
                version == PatternAnnotationPayloadCodec.CURRENT_VERSION.toLong(),
                BackupError.VALIDATION,
            )
            observeAnnotation(
                layerId,
                page.toInt(),
                row["kind"]?.jsonPrimitive?.content.orEmpty(),
                row["payloadJson"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
        if (!trackEmbeddedIdentities) return
        if (table == "project_counters") {
            val id = row["id"]?.jsonPrimitive?.longOrNull ?: throw BackupException(BackupError.VALIDATION)
            BackupFormat.requireValid(id > 0L, BackupError.VALIDATION)
            projectCounterIds += id
        }
        if (table == "pattern_annotations" && row["kind"]?.jsonPrimitive?.content == "CHART_TRACKER") {
            val payload = row["payloadJson"]?.jsonPrimitive?.content ?: return
            val id = parseChartCounterId(payload, this) ?: return
            if (id !in projectCounterIds && danglingChartCounterIds.add(id)) {
                val tableTotal = identitiesByTable["project_counters"] ?: 0L
                BackupFormat.addWithinLimit(
                    tableTotal,
                    danglingChartCounterIds.size.toLong(),
                    limits.maxIdentitiesPerTable,
                    BackupError.VALIDATION,
                )
                BackupFormat.addWithinLimit(
                    totalIdentities,
                    danglingChartCounterIds.size.toLong(),
                    limits.maxTotalIdentities,
                    BackupError.VALIDATION,
                )
            }
        }
    }

    fun observeAnnotation(
        layerId: Long,
        page: Int,
        kindName: String,
        payloadJson: String,
    ) {
        BackupFormat.requireJsonDepth(payloadJson, 8)
        val kind = PatternAnnotationKind.entries.firstOrNull { it.name == kindName }
        val payload =
            kind?.let {
                PatternAnnotationPayloadCodec.decode(it, PatternAnnotationPayloadCodec.CURRENT_VERSION, payloadJson)
            }
                ?: throw BackupException(BackupError.VALIDATION)
        try {
            annotationPages
                .getOrPut(layerId to page) { PatternAnnotationPageBudget() }
                .add(payload, payloadJson.encodeToByteArray().size.toLong())
        } catch (failure: PatternAnnotationPageLimitException) {
            throw BackupException(BackupError.VALIDATION, failure)
        }
    }

    fun complete() {
        if (!trackEmbeddedIdentities) return
        danglingChartCounterIds.forEach { addIdentity("project_counters") }
        danglingChartCounterIds.clear()
        projectCounterIds.clear()
    }

    fun addIdentity(table: String) {
        val tableIdentities =
            BackupFormat.addWithinLimit(
                identitiesByTable[table] ?: 0L,
                1L,
                limits.maxIdentitiesPerTable,
                BackupError.VALIDATION,
            )
        identitiesByTable[table] = tableIdentities
        totalIdentities =
            BackupFormat.addWithinLimit(
                totalIdentities,
                1L,
                limits.maxTotalIdentities,
                BackupError.VALIDATION,
            )
    }

    fun addDurableCopy(
        key: String,
        bytes: Long,
    ) {
        BackupFormat.requireValid(bytes in 0..limits.maxDurableFileBytes, BackupError.VALIDATION)
        if (!durableCopies.add(key)) return
        BackupFormat.requireValid(
            durableCopies.size.toLong() <= limits.maxDurableFileCount,
            BackupError.VALIDATION,
        )
        durableCopyBytes =
            BackupFormat.addWithinLimit(
                durableCopyBytes,
                bytes,
                limits.maxDurableBytes,
                BackupError.VALIDATION,
            )
    }

    val requiredDurableCopyBytes: Long
        get() = durableCopyBytes
}

internal fun parseChartCounterId(
    payloadJson: String,
    budget: BackupBudget,
): Long? {
    budget.requireFieldLength(payloadJson.length)
    BackupFormat.requireJsonDepth(payloadJson, 8)
    val payload =
        runCatching { BackupFormat.json.parseToJsonElement(payloadJson).jsonObject }
            .getOrElse { throw BackupException(BackupError.VALIDATION, it) }
    val value = payload["extraCounterId"]?.takeUnless { it == JsonNull } ?: return null
    val id = value.jsonPrimitive.longOrNull
    BackupFormat.requireValid(id != null && id > 0L, BackupError.VALIDATION)
    return id
}
