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
            val file = File(dir, "${System.currentTimeMillis()}.jpg")
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
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
            val original = inputStream.use(BitmapFactory::decodeStream) ?: return false

            val scaled = scaleDown(original, MAX_DIMENSION)
            targetFile.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()
            return true
        }

        fun deleteProjectPhotos(
            context: Context,
            projectId: Long,
        ) {
            val dir = File(context.filesDir, "progress_photos/$projectId")
            if (dir.exists()) dir.deleteRecursively()
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
            if (file.exists() && !file.delete()) file.deleteOnExit()
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
