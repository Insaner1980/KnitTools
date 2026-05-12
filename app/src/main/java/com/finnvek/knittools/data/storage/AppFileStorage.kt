package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

object AppFileStorage {
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
    ): Boolean =
        when (uri.scheme) {
            "content" -> uri.authority == "${context.packageName}.fileprovider"
            "file" -> uri.path?.startsWith(context.filesDir.absolutePath) == true
            else -> false
        }

    private fun deleteFile(file: File) {
        if (file.exists() && !file.delete()) {
            file.deleteOnExit()
        }
    }
}
