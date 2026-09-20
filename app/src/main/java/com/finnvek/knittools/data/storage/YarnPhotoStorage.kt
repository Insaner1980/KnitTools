package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
            var copied = false
            try {
                if (!hasYarnPhotoSpace(context)) {
                    throw IOException("Not enough storage for yarn photo")
                }
                val input =
                    context.contentResolver.openInputStream(sourceUri)
                        ?: throw IOException("Unable to open yarn photo")
                input.use { stream ->
                    targetFile.outputStream().use { output ->
                        copyYarnPhotoWithLimit(stream, output)
                    }
                }
                copied = true
                return targetFile.toUri().toString()
            } finally {
                if (!copied) AppFileStorage.deleteIfAppOwned(context, targetFile.toUri())
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

        private fun hasYarnPhotoSpace(context: Context): Boolean =
            try {
                val storageManager = context.getSystemService(StorageManager::class.java)
                val storageUuid = storageManager.getUuidForPath(context.filesDir)
                storageManager.getAllocatableBytes(storageUuid) >=
                    YARN_PHOTO_MAX_BYTES + YARN_PHOTO_FREE_SPACE_RESERVE_BYTES
            } catch (_: IOException) {
                false
            }

        private fun File.safeCanonicalFile(): File? = runCatching { canonicalFile }.getOrNull()

        private fun deleteIfPossible(file: File) {
            runCatching {
                AppFileStorage.deleteFileOrDirectory(
                    file = file,
                    failureMessagePrefix = "Yarn photo file delete failed",
                )
            }
        }

        internal companion object {
            const val YARN_PHOTO_ROOT = "yarn_photos"
            const val YARN_PHOTO_MAX_BYTES = 25L * 1_024L * 1_024L
            const val YARN_PHOTO_FREE_SPACE_RESERVE_BYTES = 32L * 1_024L * 1_024L
        }
    }

internal fun copyYarnPhotoWithLimit(
    input: InputStream,
    output: OutputStream,
    maxBytes: Long = YarnPhotoStorage.YARN_PHOTO_MAX_BYTES,
): Long {
    require(maxBytes >= 0L)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        if (Thread.currentThread().isInterrupted) {
            throw IOException("Yarn photo import cancelled")
        }
        val read = input.read(buffer)
        if (read < 0) return copied
        val nextTotal = copied + read
        if (nextTotal > maxBytes) throw IOException("Yarn photo exceeds size limit")
        output.write(buffer, 0, read)
        copied = nextTotal
    }
}
