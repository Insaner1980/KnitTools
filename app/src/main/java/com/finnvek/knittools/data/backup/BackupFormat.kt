package com.finnvek.knittools.data.backup

import android.annotation.SuppressLint
import com.finnvek.knittools.data.storage.YarnPhotoStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

enum class BackupError {
    UNSUPPORTED,
    INVALID,
    CORRUPT,
    SPACE,
    READ,
    WRITE,
    VALIDATION,
    RESTORE,
}

class BackupException(
    val error: BackupError,
    cause: Throwable? = null,
) : IOException(error.name, cause)

@Serializable
data class BackupEntry(
    val path: String,
    val size: Long,
    val sha256: String,
)

@Serializable
data class BackupManifest(
    val format: String = "KnitTools",
    val formatVersion: Int = 1,
    val dataVersion: Int = 1,
    val schemaVersion: Int = 25,
    val createdAt: Long,
    val versionCode: Long,
    val versionName: String,
    val entries: List<BackupEntry>,
)

data class BackupPreview(
    val createdAt: Long,
    val versionName: String,
    val projects: Int,
    val patterns: Int,
    val yarnCards: Int,
    val files: Int,
)

internal object BackupFormat {
    const val EXTENSION = "knittools-backup"
    const val MIME = "application/octet-stream"
    const val MANIFEST = "manifest.json"
    const val MAX_ENTRIES = 2_019
    const val MAX_FILE = BackupLimits.MAX_DURABLE_FILE_BYTES
    const val MAX_TOTAL = BackupLimits.MAX_ARCHIVE_BYTES
    const val MAX_TABLE = BackupLimits.MAX_TABLE_BYTES
    const val MAX_MANIFEST = 512L * 1_024L
    const val MAX_ROW = BackupLimits.MAX_ROW_CHARACTERS
    const val MAX_ROWS = BackupLimits.MAX_ROWS_PER_TABLE
    const val RESERVE = 32L * 1024 * 1024
    val json = Json { encodeDefaults = true }
    val hashPattern = Regex("[0-9a-f]{64}")

    // Viitatut taulut tulevat ennen niihin viittaavia tauluja; poisto käyttää käänteistä järjestystä.
    val tables =
        listOf(
            "counter_projects",
            "saved_patterns",
            "yarn_cards",
            "project_folders",
            "counter_history",
            "sessions",
            "active_sessions",
            "row_reminders",
            "progress_photos",
            "project_counters",
            "project_yarn_notes",
            "pattern_annotation_layers",
            "pattern_annotations",
            "pattern_bookmarks",
            "project_documents",
            "project_folder_assignments",
            "project_yarn_usage",
            "project_completions",
        )
    val tablePaths = tables.map { "tables/$it.jsonl" }.toSet()
    val identityTables = tables - setOf("active_sessions", "project_folder_assignments")
    private val filePath = Regex("files/[0-9a-f]{64}\\.bin")

    fun allowedPath(path: String): Boolean = path in tablePaths || filePath.matches(path)

    fun limit(path: String): Long = if (path in tablePaths) MAX_TABLE else MAX_FILE

    fun durableFileLimit(table: String): Long =
        if (table == "yarn_cards") YarnPhotoStorage.YARN_PHOTO_MAX_BYTES else MAX_FILE

    fun requireValid(
        value: Boolean,
        error: BackupError = BackupError.CORRUPT,
    ) {
        if (!value) throw BackupException(error)
    }

    fun requireJsonDepth(
        text: String,
        maximum: Int,
    ) {
        var depth = 0
        var quoted = false
        var escaped = false
        text.forEach { character ->
            if (quoted) {
                if (escaped) {
                    escaped = false
                } else {
                    when (character) {
                        '\\' -> escaped = true
                        '"' -> quoted = false
                    }
                }
            } else {
                when (character) {
                    '"' -> quoted = true
                    '[', '{' -> {
                        depth++
                        requireValid(depth <= maximum)
                    }
                    ']', '}' -> {
                        depth--
                        requireValid(depth >= 0)
                    }
                }
            }
        }
        requireValid(depth == 0 && !quoted)
    }

    // Count only currently free space; reclaimable cache must not satisfy the backup reserve.
    @SuppressLint("UsableSpace")
    fun space(
        directory: File,
        bytes: Long,
    ) {
        if (bytes < 0 || bytes > Long.MAX_VALUE - RESERVE || directory.usableSpace < bytes + RESERVE) {
            throw BackupException(BackupError.SPACE)
        }
    }

    fun digest(
        file: File,
        check: () -> Unit = {},
    ): String = file.inputStream().use { digest(it, check) }

    private fun digest(
        input: InputStream,
        check: () -> Unit,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            check()
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun copy(
        input: InputStream,
        output: OutputStream,
        maxBytes: Long,
        check: () -> Unit = {},
    ): Long {
        requireValid(maxBytes >= 0L)
        var total = 0L
        val buffer = ByteArray(64 * 1024)
        while (true) {
            check()
            val count = input.read(buffer)
            if (count < 0) return total
            requireValid(total <= maxBytes && count.toLong() <= maxBytes - total)
            total += count
            output.write(buffer, 0, count)
        }
    }

    fun addWithinLimit(
        current: Long,
        additional: Long,
        limit: Long,
        error: BackupError = BackupError.CORRUPT,
    ): Long {
        requireValid(current >= 0L && additional >= 0L && limit >= 0L, error)
        requireValid(current <= limit && additional <= limit - current, error)
        return current + additional
    }

    fun multiplyWithinLimit(
        value: Long,
        multiplier: Long,
        limit: Long = Long.MAX_VALUE,
        error: BackupError = BackupError.SPACE,
    ): Long {
        requireValid(value >= 0L && multiplier >= 0L && limit >= 0L, error)
        requireValid(value == 0L || multiplier <= limit / value, error)
        return value * multiplier
    }
}
