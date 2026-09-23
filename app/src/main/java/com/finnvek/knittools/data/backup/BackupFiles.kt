package com.finnvek.knittools.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnvek.knittools.data.storage.AppFileStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

internal class BackupFiles(
    private val context: Context,
    private val directory: File,
    private val externalFiles: Map<String, File> = emptyMap(),
    private val budget: BackupBudget = BackupBudget(),
) {
    private val exported = mutableMapOf<String, String>()

    fun export(
        table: String,
        row: MutableMap<String, JsonElement>,
        check: () -> Unit,
    ) {
        val column = columns[table] ?: return
        val uri =
            row[column]
                ?.takeUnless { it == JsonNull }
                ?.jsonPrimitive
                ?.content
                .orEmpty()
        if (uri.isBlank()) return
        if (uri !in exported) {
            BackupFormat.requireValid(
                exported.size.toLong() < budget.limits.maxDurableFileCount,
                BackupError.WRITE,
            )
        }
        val path =
            exported.getOrPut(uri) {
                val parsed = uri.toUri()
                val local = AppFileStorage.resolveAppOwnedFile(context, parsed)
                if (local != null) requireDurable(local)
                BackupFormat.requireValid(local != null || parsed.scheme == "content", BackupError.READ)
                val sourceFile = local ?: externalFiles[uri]
                BackupFormat.requireValid(sourceFile?.isFile == true, BackupError.READ)
                BackupFormat.requireValid(
                    checkNotNull(sourceFile).length() <= BackupFormat.durableFileLimit(table),
                    BackupError.WRITE,
                )
                val temporary = File(directory, "file-copy.tmp")
                try {
                    sourceFile.inputStream().use { source ->
                        FileOutputStream(temporary).use { output ->
                            BackupFormat.copy(source, output, BackupFormat.durableFileLimit(table)) {
                                check()
                                BackupFormat.space(directory, 64 * 1024)
                            }
                        }
                    }
                    val path = "files/${BackupFormat.digest(temporary, check)}.bin"
                    val target = File(directory, path)
                    target.parentFile?.mkdirs()
                    if (!target.exists()) check(temporary.renameTo(target))
                    path
                } finally {
                    deleteTemporaryFile(temporary)
                }
            }
        BackupFormat.requireValid(
            File(directory, path).length() <= BackupFormat.durableFileLimit(table),
            BackupError.WRITE,
        )
        val copyKey =
            when (table) {
                "progress_photos", "yarn_cards" -> "$table:${row["id"]?.jsonPrimitive?.content}"
                else -> "pdf:$path"
            }
        budget.addDurableCopy(copyKey, File(directory, path).length())
        row[column] = JsonPrimitive(path)
    }

    fun requireDurable(file: File) {
        val root = context.filesDir.canonicalFile
        val canonical = file.canonicalFile
        BackupFormat.requireValid(canonical.path.startsWith(root.path + File.separator), BackupError.READ)
        val relative = canonical.relativeTo(root).invariantSeparatorsPath
        BackupFormat.requireValid(relative.substringBefore('/') in roots && canonical.isFile, BackupError.READ)
    }

    companion object {
        fun boundedReferences(db: SupportSQLiteDatabase): Map<String, Long> =
            buildMap {
                columns.forEach { (table, column) ->
                    db
                        .query(
                            "SELECT DISTINCT `$column` FROM `$table` WHERE `$column` IS NOT NULL AND `$column` != ''",
                        ).use { cursor ->
                            while (cursor.moveToNext()) {
                                val reference = cursor.getString(0)
                                val limit = BackupFormat.durableFileLimit(table)
                                put(reference, minOf(get(reference) ?: limit, limit))
                                BackupFormat.requireValid(
                                    size.toLong() <= BackupLimits.MAX_DURABLE_FILE_COUNT,
                                    BackupError.WRITE,
                                )
                            }
                        }
                }
            }

        fun deleteTemporaryFile(file: File) {
            try {
                Files.deleteIfExists(file.toPath())
            } catch (_: IOException) {
                // The operation directory is cleaned again after use or at the next startup.
            }
        }

        val columns =
            mapOf(
                "counter_projects" to "patternUri",
                "saved_patterns" to "localPdfUri",
                "project_documents" to "localPdfUri",
                "progress_photos" to "photoUri",
                "yarn_cards" to "photoUri",
            )
        val roots = setOf("pattern_pdfs", "patterns", "pattern_captures", "progress_photos", "yarn_photos")
    }
}
