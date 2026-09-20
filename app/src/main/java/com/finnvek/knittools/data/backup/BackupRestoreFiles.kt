package com.finnvek.knittools.data.backup

import android.content.Context
import androidx.core.net.toUri
import com.finnvek.knittools.data.storage.AppFileStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal class BackupRestoreFiles(
    private val context: Context,
    private val directory: File,
) {
    private val token = UUID.randomUUID().toString()
    private val copies = linkedMapOf<File, File>()
    val requiredBytes: Long get() = copies.values.sumOf { it.length() }

    fun rebase(
        table: String,
        row: MutableMap<String, JsonElement>,
    ) {
        val column = BackupFiles.columns[table] ?: return
        val path =
            row[column]
                ?.takeUnless { it == JsonNull }
                ?.jsonPrimitive
                ?.content
                .orEmpty()
        if (path.isBlank()) return
        BackupFormat.requireValid(path.startsWith("files/") && BackupFormat.allowedPath(path))
        val name = path.substringAfter('/').substringBefore('.')
        val target =
            when (table) {
                "progress_photos" -> "progress_photos/${row.getValue(
                    "projectId",
                ).jsonPrimitive.content}/restore-$token-${row.getValue("id").jsonPrimitive.content}-$name.jpg"
                "yarn_cards" -> "yarn_photos/${row.getValue("id").jsonPrimitive.content}/restore-$token-$name.jpg"
                else -> "pattern_pdfs/0/restore-$token-$name.pdf"
            }
        val file = File(context.filesDir, target)
        copies[file] = File(directory, path)
        row[column] = JsonPrimitive(file.toUri().toString())
    }

    fun journal(oldUris: Set<String>): File {
        val oldFiles = oldUris.mapNotNull { AppFileStorage.resolveAppOwnedFile(context, it.toUri()) }
        val paths = (oldFiles + copies.keys).map { it.canonicalPath }.distinct()
        val journal = File(directory, JOURNAL)
        val temporary = File(directory, "$JOURNAL.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(BackupFormat.json.encodeToString(paths).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        BackupFormat.requireValid(temporary.renameTo(journal), BackupError.RESTORE)
        return journal
    }

    fun publish(check: () -> Unit) {
        copies.forEach { (target, source) ->
            check()
            BackupFormat.space(context.filesDir, source.length())
            target.parentFile?.mkdirs()
            BackupFormat.requireValid(!target.exists(), BackupError.RESTORE)
            FileOutputStream(target).use { output ->
                source.inputStream().use { BackupFormat.copy(it, output, source.length(), check) }
                output.fd.sync()
            }
            BackupFormat.requireValid(BackupFormat.digest(target, check) == BackupFormat.digest(source, check))
        }
    }

    companion object {
        const val JOURNAL = "restore-files.json"

        fun cleanup(
            context: Context,
            directory: File,
            referencedUris: Set<String>,
        ): Boolean {
            val journal = File(directory, JOURNAL)
            if (!journal.exists()) return true
            val references =
                referencedUris
                    .mapNotNull {
                        AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.canonicalPath
                    }.toSet()
            val paths = BackupFormat.json.decodeFromString<List<String>>(journal.readText())
            var clean = true
            paths.forEach { path ->
                val file = File(path).canonicalFile
                val root = context.filesDir.canonicalFile
                if (!file.path.startsWith(root.path + File.separator)) return@forEach
                if (file.relativeTo(root).invariantSeparatorsPath.substringBefore('/') !in
                    BackupFiles.roots
                ) {
                    return@forEach
                }
                if (file.path !in references && file.exists() && !file.delete()) clean = false
            }
            if (clean) clean = journal.delete()
            return clean
        }
    }
}
