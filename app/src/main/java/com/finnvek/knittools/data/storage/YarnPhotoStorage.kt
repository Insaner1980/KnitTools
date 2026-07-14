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
            val dir = yarnPhotoDir(context, cardId)
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

        fun pruneUnreferencedPhotos(
            context: Context,
            referencedPhotoUris: Set<String>,
        ) {
            val root = yarnPhotoRoot(context)
            if (!root.exists()) return

            val referencedFiles =
                referencedPhotoUris
                    .mapNotNull { uriString ->
                        runCatching {
                            AppFileStorage.resolveAppOwnedFile(context, uriString.toUri())?.canonicalFile
                        }.getOrNull()
                    }.toSet()

            root
                .walkBottomUp()
                .filterNot { file -> file == root }
                .forEach { file ->
                    when {
                        file.isFile && file.safeCanonicalFile() !in referencedFiles -> deleteIfPossible(file)
                        file.isDirectory && file.listFiles()?.isEmpty() == true -> deleteIfPossible(file)
                    }
                }
        }

        private fun yarnPhotoDir(
            context: Context,
            cardId: Long,
        ): File = File(yarnPhotoRoot(context), cardId.toString())

        private fun yarnPhotoRoot(context: Context): File = File(context.filesDir, YARN_PHOTO_ROOT)

        private fun File.safeCanonicalFile(): File? = runCatching { canonicalFile }.getOrNull()

        private fun deleteIfPossible(file: File) {
            runCatching {
                AppFileStorage.deleteFileOrDirectory(
                    file = file,
                    failureMessagePrefix = "Yarn photo file delete failed",
                )
            }
        }

        private companion object {
            const val YARN_PHOTO_ROOT = "yarn_photos"
        }
    }
