package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.storage.StorageManager
import androidx.core.net.toUri
import com.finnvek.knittools.ui.screens.pattern.PatternImageImportLimits
import com.finnvek.knittools.ui.screens.pattern.PatternImageSelection
import com.finnvek.knittools.ui.screens.pattern.StagedPatternPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

private const val PATTERN_CAPTURE_ROOT = "pattern_captures"

internal enum class PatternImageFailureReason {
    UNSUPPORTED,
    ANIMATED,
    INVALID_DIMENSIONS,
}

internal class PatternImageValidationException(
    val reason: PatternImageFailureReason,
    cause: Throwable? = null,
) : Exception(cause)

internal data class PatternImageInfo(
    val width: Int,
    val height: Int,
)

internal enum class PatternImageStageFailure {
    PAGE_LIMIT,
    IMAGE_TOO_LARGE,
    TOTAL_TOO_LARGE,
    UNREADABLE,
    UNSUPPORTED,
    ANIMATED,
}

internal class PatternImageStageException(
    val reason: PatternImageStageFailure,
    cause: Throwable? = null,
) : Exception(cause)

internal data class PatternImageStageBatch(
    val pages: List<StagedPatternPage>,
    val duplicatesIgnored: Int,
)

@Singleton
class PatternDocumentStorage
    @Inject
    constructor() {
        fun createCaptureImageFile(
            context: Context,
            projectId: Long,
            sessionId: String? = null,
        ): Pair<File, Uri> {
            val dir =
                sessionId?.let { PatternImageStagingFiles.sessionDirectory(context.filesDir, projectId, it) }
                    ?: patternCaptureDir(context, projectId)
            dir.mkdirs()
            val file = StorageFileNames.uniqueTimestampedFile(dir, "", ".jpg")
            val uri = AppFileStorage.fileProviderUri(context, file)
            return file to uri
        }

        fun deleteProjectCaptureImages(
            context: Context,
            projectId: Long,
        ) {
            val dir = patternCaptureDir(context, projectId)
            if (dir.exists()) {
                deleteCaptureFileIfPossible(dir)
            }
        }

        fun pruneStaleCaptureImages(
            context: Context,
            nowMillis: Long = System.currentTimeMillis(),
            maxAgeMillis: Long = STALE_CAPTURE_MAX_AGE_MILLIS,
        ) {
            val root = patternCaptureRoot(context)
            if (!root.exists()) return
            val staleBeforeMillis = nowMillis - maxAgeMillis

            root
                .walkBottomUp()
                .filterNot { file -> file == root }
                .forEach { file ->
                    when {
                        file.isFile && file.lastModified() <= staleBeforeMillis -> deleteCaptureFileIfPossible(file)
                        file.isDirectory && file.listFiles()?.isEmpty() == true -> deleteCaptureFileIfPossible(file)
                    }
                }
        }

        internal suspend fun stageSelectedImages(
            context: Context,
            projectId: Long,
            sessionId: String,
            existingSelection: PatternImageSelection,
            sourceUris: List<Uri>,
        ): PatternImageStageBatch {
            val (uniqueUris, duplicatesIgnored) = distinctSourceUris(existingSelection, sourceUris)
            if (existingSelection.pages.size + uniqueUris.size > PatternImageImportLimits.MAX_PAGES) {
                stageFailure(PatternImageStageFailure.PAGE_LIMIT)
            }
            if (uniqueUris.isEmpty()) return PatternImageStageBatch(emptyList(), duplicatesIgnored)

            val sessionDirectory = PatternImageStagingFiles.sessionDirectory(context.filesDir, projectId, sessionId)
            val stagedFiles = mutableListOf<File>()
            val stagedPages = mutableListOf<StagedPatternPage>()
            val stagingContext = coroutineContext
            var batchBytes = 0L
            var completed = false
            try {
                uniqueUris.forEach { sourceUri ->
                    stagingContext.ensureActive()
                    val stagedFile = File(sessionDirectory, "page-${UUID.randomUUID()}.img")
                    stagedFiles += stagedFile
                    val copiedBytes = copySelectedImage(context, sourceUri, stagedFile)
                    batchBytes += copiedBytes
                    if (existingSelection.totalBytes + batchBytes > PatternImageImportLimits.MAX_TOTAL_BYTES) {
                        stageFailure(PatternImageStageFailure.TOTAL_TOO_LARGE)
                    }
                    val info = validateStagedImage(stagedFile)
                    stagedPages +=
                        StagedPatternPage(
                            id = UUID.randomUUID().toString(),
                            sourceUri = sourceUri.toString(),
                            stagedPath = stagedFile.absolutePath,
                            byteCount = copiedBytes,
                            width = info.width,
                            height = info.height,
                        )
                    stagingContext.ensureActive()
                }
                completed = true
                return PatternImageStageBatch(stagedPages, duplicatesIgnored)
            } finally {
                if (!completed) {
                    stagedFiles.forEach(::deleteCaptureFileIfPossible)
                    deleteEmptySessionDirectory(sessionDirectory)
                }
            }
        }

        private fun distinctSourceUris(
            existingSelection: PatternImageSelection,
            sourceUris: List<Uri>,
        ): Pair<List<Uri>, Int> {
            val seen = existingSelection.pages.mapTo(linkedSetOf(), StagedPatternPage::sourceUri)
            var duplicatesIgnored = 0
            val uniqueUris =
                sourceUris.filter { uri ->
                    val isNew = seen.add(uri.toString())
                    if (!isNew) duplicatesIgnored += 1
                    isNew
                }
            return uniqueUris to duplicatesIgnored
        }

        private fun copySelectedImage(
            context: Context,
            sourceUri: Uri,
            stagedFile: File,
        ): Long {
            val input =
                try {
                    context.contentResolver.openInputStream(sourceUri)
                } catch (failure: IOException) {
                    stageFailure(PatternImageStageFailure.UNREADABLE, failure)
                } catch (failure: SecurityException) {
                    stageFailure(PatternImageStageFailure.UNREADABLE, failure)
                } ?: stageFailure(PatternImageStageFailure.UNREADABLE)
            return try {
                input.use {
                    PatternImageStagingFiles.copyBounded(
                        input = it,
                        target = stagedFile,
                        maxBytes = PatternImageImportLimits.MAX_BYTES_PER_IMAGE,
                    )
                }
            } catch (failure: PatternImageTooLargeException) {
                stageFailure(PatternImageStageFailure.IMAGE_TOO_LARGE, failure)
            } catch (failure: IOException) {
                stageFailure(PatternImageStageFailure.UNREADABLE, failure)
            }
        }

        private fun validateStagedImage(stagedFile: File): PatternImageInfo =
            try {
                inspectStagedImage(stagedFile)
            } catch (failure: PatternImageValidationException) {
                val reason =
                    if (failure.reason == PatternImageFailureReason.ANIMATED) {
                        PatternImageStageFailure.ANIMATED
                    } else {
                        PatternImageStageFailure.UNSUPPORTED
                    }
                stageFailure(reason, failure)
            }

        internal fun deleteImportSession(
            context: Context,
            projectId: Long,
            sessionId: String,
        ) {
            val sessionDirectory =
                PatternImageStagingFiles.sessionDirectory(
                    context.filesDir,
                    projectId,
                    sessionId,
                )
            deleteCaptureFileIfPossible(sessionDirectory)
        }

        internal fun deleteStagedPage(page: StagedPatternPage) {
            val file = File(page.stagedPath)
            deleteCaptureFileIfPossible(file)
            file.parentFile?.let(::deleteEmptySessionDirectory)
        }

        internal fun deleteGeneratedPdf(
            context: Context,
            uri: String,
        ) {
            AppFileStorage.deleteIfAppOwned(context, uri)
        }

        internal fun hasCreationSpace(
            context: Context,
            stagedBytes: Long,
        ): Boolean =
            try {
                val storageManager = context.getSystemService(StorageManager::class.java)
                val storageUuid = storageManager.getUuidForPath(context.filesDir)
                storageManager.getAllocatableBytes(storageUuid) >=
                    stagedBytes + PatternImageImportLimits.MIN_FREE_SPACE_RESERVE_BYTES
            } catch (_: IOException) {
                false
            }

        suspend fun convertImageToPdf(
            context: Context,
            projectId: Long,
            imageUri: Uri,
            fileName: String? = null,
        ): Pair<String, String>? {
            val imageFile = AppFileStorage.resolveAppOwnedFile(context, imageUri) ?: return null
            val info = runCatching { inspectStagedImage(imageFile) }.getOrNull() ?: return null
            val page =
                StagedPatternPage(
                    id = imageFile.name,
                    sourceUri = imageUri.toString(),
                    stagedPath = imageFile.absolutePath,
                    byteCount = imageFile.length(),
                    width = info.width,
                    height = info.height,
                )
            return runCatching {
                convertImagesToPdf(
                    context = context,
                    projectId = projectId,
                    pages = listOf(page),
                    fileName = fileName ?: "pattern-photo-${System.currentTimeMillis()}.pdf",
                ) { _, _ -> }
            }.getOrNull()
        }

        internal fun inspectStagedImage(file: File): PatternImageInfo {
            val bitmap = decodeBoundedBitmap(file)
            return try {
                PatternImageInfo(bitmap.width, bitmap.height)
            } finally {
                bitmap.recycle()
            }
        }

        internal suspend fun convertImagesToPdf(
            context: Context,
            projectId: Long,
            pages: List<StagedPatternPage>,
            fileName: String,
            onProgress: (currentPage: Int, totalPages: Int) -> Unit,
        ): Pair<String, String> {
            require(pages.isNotEmpty())
            val document = PdfDocument()
            try {
                pages.forEachIndexed { index, stagedPage ->
                    coroutineContext.ensureActive()
                    val bitmap = decodeBoundedBitmap(File(stagedPage.stagedPath))
                    try {
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val pdfPage = document.startPage(pageInfo)
                        try {
                            pdfPage.canvas.drawColor(Color.WHITE)
                            pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        } finally {
                            document.finishPage(pdfPage)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    coroutineContext.ensureActive()
                    onProgress(index + 1, pages.size)
                }
                val conversionContext = coroutineContext
                val pdfFile =
                    PatternDocumentFiles.writeUniquePdfOrThrow(
                        directory = File(context.filesDir, "pattern_pdfs/$projectId"),
                        fileName = fileName,
                    ) { targetFile ->
                        conversionContext.ensureActive()
                        targetFile.outputStream().use(document::writeTo)
                        conversionContext.ensureActive()
                    }
                return pdfFile.toUri().toString() to pdfFile.name
            } finally {
                document.close()
            }
        }

        private fun decodeBoundedBitmap(file: File): Bitmap {
            if (!file.isFile || file.length() <= 0L) {
                unsupportedImage()
            }
            return try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    configureImageDecoder(decoder, info)
                }
            } catch (failure: IOException) {
                unsupportedImage(failure)
            } catch (failure: IllegalArgumentException) {
                unsupportedImage(failure)
            }
        }

        private fun configureImageDecoder(
            decoder: ImageDecoder,
            info: ImageDecoder.ImageInfo,
        ) {
            if (info.isAnimated) {
                throw PatternImageValidationException(PatternImageFailureReason.ANIMATED)
            }
            val width = info.size.width
            val height = info.size.height
            if (width <= 0 || height <= 0) {
                throw PatternImageValidationException(PatternImageFailureReason.INVALID_DIMENSIONS)
            }
            val longEdge = maxOf(width, height)
            if (longEdge > PatternImageImportLimits.MAX_LONG_EDGE_PIXELS) {
                val ratio = PatternImageImportLimits.MAX_LONG_EDGE_PIXELS.toFloat() / longEdge
                decoder.setTargetSize(
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        /**
         * Kopioi ulkoisen content URI:n PDF sovelluksen sisäiseen tallennustilaan.
         * Palauttaa sisäisen content URI:n tai null jos kopiointi epäonnistuu.
         */
        fun copyPdfToInternal(
            context: Context,
            projectId: Long,
            sourceUri: Uri,
            fileName: String,
        ): String? {
            if (AppFileStorage.isAppOwnedUri(context, sourceUri)) return null

            return try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val copiedFile =
                        PatternDocumentFiles.writeUniquePdf(
                            directory = File(context.filesDir, "pattern_pdfs/$projectId"),
                            fileName = fileName,
                        ) { targetFile ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    copiedFile?.toUri()?.toString()
                }
            } catch (_: Exception) {
                null
            }
        }

        private companion object {
            const val STALE_CAPTURE_MAX_AGE_MILLIS = 7L * 24L * 60L * 60L * 1000L
        }
    }

private fun stageFailure(
    reason: PatternImageStageFailure,
    cause: Throwable? = null,
): Nothing = throw PatternImageStageException(reason, cause)

private fun unsupportedImage(cause: Throwable? = null): Nothing =
    throw PatternImageValidationException(PatternImageFailureReason.UNSUPPORTED, cause)

private fun patternCaptureDir(
    context: Context,
    projectId: Long,
): File = File(patternCaptureRoot(context), projectId.toString())

private fun patternCaptureRoot(context: Context): File = File(context.filesDir, PATTERN_CAPTURE_ROOT)

private fun deleteCaptureFileIfPossible(file: File) {
    runCatching {
        AppFileStorage.deleteFileOrDirectory(
            file = file,
            failureMessagePrefix = "Pattern capture file delete failed",
        )
    }
}

private fun deleteEmptySessionDirectory(directory: File) {
    if (directory.isDirectory && directory.listFiles()?.isEmpty() == true) {
        deleteCaptureFileIfPossible(directory)
    }
}

internal object PatternDocumentFiles {
    fun safePdfFileName(
        fileName: String?,
        fallbackName: String = FALLBACK_NAME,
    ): String {
        val baseName =
            fileName
                .orEmpty()
                .trim()
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .replace(CONTROL_CHARS, "")
                .replace(UNSAFE_CHARS, "_")
                .replace(WHITESPACE, " ")
                .replace(REPEATED_DOTS, ".")
                .trim(' ', '.')
                .ifBlank { fallbackName }
        val stem =
            if (baseName.endsWith(PDF_EXTENSION, ignoreCase = true)) {
                baseName.dropLast(PDF_EXTENSION.length)
            } else {
                baseName
            }
        val pdfName = "$stem$PDF_EXTENSION".avoidReservedDeviceName()
        return pdfName.truncatePdfFileName()
    }

    fun writeUniquePdf(
        directory: File,
        fileName: String,
        write: (File) -> Unit,
    ): File? =
        try {
            writeUniquePdfOrThrow(directory, fileName, write)
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            null
        }

    fun writeUniquePdfOrThrow(
        directory: File,
        fileName: String,
        write: (File) -> Unit,
    ): File {
        if (!directory.exists() && !directory.mkdirs()) {
            throw java.io.IOException("Pattern PDF directory creation failed")
        }
        val safeFileName = safePdfFileName(fileName)
        val tempFile = createTempFile(directory, safeFileName)
        return try {
            write(tempFile)
            moveToUniquePdf(tempFile, directory, safeFileName)
                ?: throw java.io.IOException("Pattern PDF publish failed")
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit()
            }
        }
    }

    private fun moveToUniquePdf(
        sourceFile: File,
        directory: File,
        fileName: String,
    ): File? {
        var index = 1
        while (true) {
            val candidate = pdfCandidateFile(directory, fileName, index)
            try {
                Files.move(sourceFile.toPath(), candidate.toPath())
                return candidate
            } catch (_: FileAlreadyExistsException) {
                index += 1
            } catch (_: Exception) {
                return null
            }
        }
    }

    private fun createTempFile(
        directory: File,
        fileName: String,
    ): File {
        val prefix =
            fileName
                .substringBeforeLast(PDF_EXTENSION)
                .take(16)
                .ifBlank { FALLBACK_NAME }
                .padEnd(3, '_')
        return File.createTempFile(prefix, ".tmp", directory)
    }

    private fun pdfCandidateFile(
        directory: File,
        fileName: String,
        index: Int,
    ): File {
        val suffix = if (index == 1) "" else "-${index - 1}"
        val stem =
            fileName
                .substringBeforeLast(PDF_EXTENSION)
                .take(MAX_PDF_FILE_NAME_LENGTH - PDF_EXTENSION.length - suffix.length)
                .trimEnd(' ', '.')
                .ifBlank { FALLBACK_NAME }
        return File(directory, "$stem$suffix$PDF_EXTENSION")
    }

    private const val FALLBACK_NAME = "pattern"
    private const val MAX_PDF_FILE_NAME_LENGTH = 180
    private const val PDF_EXTENSION = ".pdf"
    private val CONTROL_CHARS = Regex("\\p{Cntrl}+")
    private val UNSAFE_CHARS = Regex("[^A-Za-z0-9._ -]")
    private val WHITESPACE = Regex("\\s+")
    private val REPEATED_DOTS = Regex("\\.{2,}")
    private val WINDOWS_RESERVED_DEVICE_NAMES =
        setOf(
            "CON",
            "PRN",
            "AUX",
            "NUL",
            "COM1",
            "COM2",
            "COM3",
            "COM4",
            "COM5",
            "COM6",
            "COM7",
            "COM8",
            "COM9",
            "LPT1",
            "LPT2",
            "LPT3",
            "LPT4",
            "LPT5",
            "LPT6",
            "LPT7",
            "LPT8",
            "LPT9",
        )

    private fun String.truncatePdfFileName(): String {
        if (length <= MAX_PDF_FILE_NAME_LENGTH) return this

        val stem =
            if (endsWith(PDF_EXTENSION, ignoreCase = true)) {
                dropLast(PDF_EXTENSION.length)
            } else {
                this
            }
        val truncatedStem =
            stem
                .take(MAX_PDF_FILE_NAME_LENGTH - PDF_EXTENSION.length)
                .trimEnd(' ', '.')
                .ifBlank { FALLBACK_NAME }
        return "$truncatedStem$PDF_EXTENSION"
    }

    private fun String.avoidReservedDeviceName(): String {
        val stem = substringBeforeLast(PDF_EXTENSION)
        val deviceName = stem.substringBefore('.').uppercase(Locale.US)
        if (deviceName !in WINDOWS_RESERVED_DEVICE_NAMES) return this
        return "${stem}_$PDF_EXTENSION"
    }
}
