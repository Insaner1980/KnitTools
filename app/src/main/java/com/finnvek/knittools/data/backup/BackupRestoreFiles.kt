package com.finnvek.knittools.data.backup

import android.content.Context
import android.system.Os
import android.system.OsConstants
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
    private val budget: BackupBudget = BackupBudget(),
    private val dataVersion: Int = 1,
) {
    private val token = UUID.randomUUID().toString()
    private val copies = linkedMapOf<File, File>()
    private val referencedArchivePaths = mutableSetOf<String>()
    val requiredBytes: Long get() = budget.requiredDurableCopyBytes
    val archivePaths: Set<String> get() = referencedArchivePaths

    fun rebase(
        table: String,
        row: MutableMap<String, JsonElement>,
    ) {
        val column = BackupFiles.columns[table] ?: return
        val reference =
            row[column]
                ?.takeUnless { it == JsonNull }
                ?.jsonPrimitive
                ?.content
                .orEmpty()
        if (reference.isBlank()) return
        val path = reference.substringBefore('#')
        BackupFormat.requireValid(path.startsWith("files/") && BackupFormat.allowedPath(path))
        val hash = path.substringAfter('/').substringBefore('.')
        val name =
            if (reference == path) {
                hash
            } else {
                BackupFormat.requireValid(dataVersion == 2 && column != "photoUri")
                val suffix = reference.substringAfter('#')
                val copyNumber = suffix.toLongOrNull()
                BackupFormat.requireValid(
                    copyNumber != null &&
                        copyNumber in 2..BackupLimits.MAX_DURABLE_FILE_COUNT &&
                        suffix == copyNumber.toString(),
                )
                "$hash-$copyNumber"
            }
        val target =
            when (table) {
                "progress_photos" -> "progress_photos/${row.getValue(
                    "projectId",
                ).jsonPrimitive.content}/restore-$token-${row.getValue("id").jsonPrimitive.content}-$name.jpg"

                "yarn_cards" -> "yarn_photos/${row.getValue("id").jsonPrimitive.content}/restore-$token-$name.jpg"

                else -> "pattern_pdfs/0/restore-$token-$name.pdf"
            }
        val file = File(context.filesDir, target)
        val source = File(directory, path)
        BackupFormat.requireValid(source.isFile, BackupError.VALIDATION)
        BackupFormat.requireValid(
            source.length() <= BackupFormat.durableFileLimit(table),
            BackupError.VALIDATION,
        )
        referencedArchivePaths += path
        budget.addDurableCopy(file.canonicalPath, source.length())
        copies[file] = source
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
        syncDirectories(listOf(directory), context.noBackupFilesDir)
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
        syncDirectories(copies.keys.mapNotNull(File::getParentFile), context.filesDir, check)
    }

    private fun syncDirectories(
        directories: List<File>,
        root: File,
        check: () -> Unit = {},
    ) {
        val boundary = root.canonicalFile
        val parentDirectories = mutableSetOf<File>()
        directories.forEach { directory ->
            var current = directory.canonicalFile
            BackupFormat.requireValid(current.path.startsWith(boundary.path + File.separator), BackupError.RESTORE)
            while (parentDirectories.add(current) && current != boundary) {
                current = checkNotNull(current.parentFile)
            }
        }
        parentDirectories.forEach { directory ->
            check()
            val fd = Os.open(directory.path, OsConstants.O_RDONLY, 0)
            try {
                Os.fsync(fd)
            } finally {
                Os.close(fd)
            }
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
