package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
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
            if (dir.exists()) {
                AppFileStorage.deleteFileOrDirectory(
                    file = dir,
                    failureMessagePrefix = "Progress photo file delete failed",
                )
            }
        }

        private fun createUniquePhotoFile(dir: File): File = StorageFileNames.uniqueTimestampedFile(dir, "", ".jpg")

        fun deletePhoto(photoUri: String) {
            deleteFileUri(photoUri.toUri())
        }

        fun isPhotoAvailable(
            context: Context,
            photoUri: String,
        ): Boolean {
            if (photoUri.isBlank()) return false
            val uri = photoUri.toUri()
            val appOwnedFile = AppFileStorage.resolveAppOwnedFile(context, uri)
            if (appOwnedFile != null) return appOwnedFile.exists()
            return when (uri.scheme) {
                "file" -> uri.path?.let(::File)?.exists() == true
                "content" ->
                    runCatching {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                    }.getOrDefault(false)
                else -> false
            }
        }

        fun deleteTemporarySource(
            context: Context,
            sourceUri: Uri,
        ) {
            AppFileStorage.deleteIfAppOwned(context, sourceUri)
        }

        private fun deleteFileUri(uri: Uri) {
            val file = File(uri.path ?: return)
            AppFileStorage.deleteFileOrDirectory(
                file = file,
                failureMessagePrefix = "Progress photo file delete failed",
            )
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
