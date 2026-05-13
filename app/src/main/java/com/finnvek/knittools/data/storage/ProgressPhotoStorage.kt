package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressPhotoStorage
    @Inject
    constructor() {
        fun createPhotoFile(
            context: Context,
            projectId: Long,
        ): Pair<File, Uri> {
            val dir = File(context.filesDir, "progress_photos/$projectId")
            dir.mkdirs()
            val file = createUniquePhotoFile(dir)
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            return file to uri
        }

        fun compressAndSave(
            context: Context,
            sourceUri: Uri,
            targetFile: File,
        ): Boolean {
            val original =
                runCatching {
                    context.contentResolver.openInputStream(sourceUri)?.use(BitmapFactory::decodeStream)
                }.getOrNull() ?: return false

            val scaled = scaleDown(original, MAX_DIMENSION)
            return try {
                runCatching {
                    targetFile.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    }
                }.getOrDefault(false)
            } finally {
                if (scaled !== original) scaled.recycle()
                original.recycle()
            }
        }

        fun deleteProjectPhotos(
            context: Context,
            projectId: Long,
        ) {
            val dir = File(context.filesDir, "progress_photos/$projectId")
            if (dir.exists()) deleteFileOrDirectory(dir)
        }

        private fun createUniquePhotoFile(dir: File): File {
            return StorageFileNames.uniqueTimestampedFile(dir, "", ".jpg")
        }

        fun deletePhoto(photoUri: String) {
            deleteFileUri(photoUri.toUri())
        }

        fun deleteTemporarySource(
            context: Context,
            sourceUri: Uri,
        ) {
            AppFileStorage.deleteIfAppOwned(context, sourceUri)
        }

        private fun deleteFileUri(uri: Uri) {
            val file = File(uri.path ?: return)
            deleteFileOrDirectory(file)
        }

        private fun deleteFileOrDirectory(file: File) {
            if (!file.exists()) return
            val deleted =
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            if (!deleted && file.exists()) {
                scheduleDeleteOnExit(file)
                throw IOException("Progress photo file delete failed: ${file.absolutePath}")
            }
        }

        private fun scheduleDeleteOnExit(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach(::scheduleDeleteOnExit)
            }
            file.deleteOnExit()
        }

        private fun scaleDown(
            bitmap: Bitmap,
            maxDimension: Int,
        ): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= maxDimension && height <= maxDimension) return bitmap

            val ratio = maxDimension.toFloat() / maxOf(width, height)
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            return bitmap.scale(newWidth, newHeight)
        }

        private companion object {
            const val MAX_DIMENSION = 1920
            const val JPEG_QUALITY = 80
        }
    }
