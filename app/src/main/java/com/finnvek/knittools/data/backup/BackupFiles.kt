package com.finnvek.knittools.data.backup

import android.content.Context
import androidx.core.net.toUri
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
        val path =
            exported.getOrPut(uri) {
                val parsed = uri.toUri()
                val local = AppFileStorage.resolveAppOwnedFile(context, parsed)
                if (local != null) requireDurable(local)
                BackupFormat.requireValid(local != null || parsed.scheme == "content", BackupError.READ)
                val temporary = File(directory, "file-copy.tmp")
                try {
                    val input =
                        local?.inputStream() ?: context.contentResolver.openInputStream(parsed)
                            ?: throw BackupException(BackupError.READ)
                    input.use { source ->
                        FileOutputStream(temporary).use { output ->
                            BackupFormat.copy(source, output, BackupFormat.MAX_FILE) {
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
