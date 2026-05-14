package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

object AppFileStorage {
    fun openReadDescriptor(
        context: Context,
        uri: Uri,
    ): ParcelFileDescriptor? =
        resolveAppOwnedFile(context, uri)
            ?.takeIf(File::exists)
            ?.let { file -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }
            ?: context.contentResolver.openFileDescriptor(uri, "r")

    fun deleteUri(
        context: Context,
        uriString: String,
    ) {
        if (uriString.isBlank()) return
        deleteUri(context, uriString.toUri())
    }

    fun deleteUri(
        context: Context,
        uri: Uri,
    ) {
        runCatching {
            resolveAppOwnedFile(context, uri)?.let { file ->
                deleteFile(file)
                return@runCatching
            }
            when (uri.scheme) {
                "file" -> deleteFile(File(uri.path ?: return))
                "content" -> context.contentResolver.delete(uri, null, null)
            }
        }
    }

    fun deleteIfAppOwned(
        context: Context,
        uriString: String,
    ) {
        if (uriString.isBlank()) return
        deleteIfAppOwned(context, uriString.toUri())
    }

    fun deleteIfAppOwned(
        context: Context,
        uri: Uri,
    ) {
        if (isAppOwnedUri(context, uri)) {
            deleteUri(context, uri)
        }
    }

    fun isAppOwnedUri(
        context: Context,
        uri: Uri,
    ): Boolean = resolveAppOwnedFile(context, uri) != null

    internal fun deleteFileOrDirectory(
        file: File,
        failureMessagePrefix: String,
    ) {
        if (!file.exists()) return
        val deleted =
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        if (!deleted && file.exists()) {
            scheduleDeleteOnExit(file)
            throw IOException("$failureMessagePrefix: ${file.absolutePath}")
        }
    }

    fun resolveAppOwnedFile(
        context: Context,
        uri: Uri,
    ): File? =
        runCatching {
            when (uri.scheme) {
                "file" -> uri.path?.let(::File)?.takeIf { it.isInside(context.filesDir) }
                "content" -> resolveFileProviderUri(context, uri)
                else -> null
            }
        }.getOrNull()

    private fun resolveFileProviderUri(
        context: Context,
        uri: Uri,
    ): File? {
        if (uri.authority != "${context.packageName}.fileprovider") return null
        val segments: List<String> = uri.pathSegments
        val root = segments.firstOrNull() ?: return null
        val rootDir =
            when (root) {
                "yarn_photos" -> File(context.filesDir, "yarn_photos")
                "progress_photos" -> File(context.filesDir, "progress_photos")
                "pattern_captures" -> File(context.filesDir, "pattern_captures")
                "pattern_pdfs" -> File(context.filesDir, "pattern_pdfs")
                "patterns" -> File(context.filesDir, "patterns")
                else -> return null
            }
        val file = segments.drop(1).fold(rootDir) { parent, segment -> File(parent, segment) }
        return file.takeIf { it.isInside(rootDir) && it.isInside(context.filesDir) }
    }

    private fun deleteFile(file: File) {
        if (file.exists() && !file.delete()) {
            file.deleteOnExit()
        }
    }

    private fun scheduleDeleteOnExit(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach(::scheduleDeleteOnExit)
        }
        file.deleteOnExit()
    }

    private fun File.isInside(root: File): Boolean {
        val canonicalFile = canonicalFile
        val canonicalRoot = root.canonicalFile
        return canonicalFile == canonicalRoot ||
            canonicalFile.path.startsWith(canonicalRoot.path + File.separator)
    }
}
