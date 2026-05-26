package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnPhotoStorage
    @Inject
    constructor() {
        fun copyPhoto(
            context: Context,
            cardId: Long,
            sourceUri: Uri,
        ): String {
            val dir = File(context.filesDir, "yarn_photos/$cardId")
            dir.mkdirs()
            val targetFile = StorageFileNames.uniqueTimestampedFile(dir, "yarn-", ".jpg")
            return try {
                val input =
                    context.contentResolver.openInputStream(sourceUri)
                        ?: throw IOException("Unable to open yarn photo")
                input.use { stream ->
                    targetFile.outputStream().use(stream::copyTo)
                }
                targetFile.toUri().toString()
            } catch (failure: IOException) {
                AppFileStorage.deleteIfAppOwned(context, targetFile.toUri())
                throw failure
            }
        }
    }
