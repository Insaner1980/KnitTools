package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternDocumentStorage
    @Inject
    constructor() {
        fun createCaptureImageFile(
            context: Context,
            projectId: Long,
        ): Pair<File, Uri> {
            val dir = File(context.filesDir, "pattern_captures/$projectId")
            dir.mkdirs()
            val file = StorageFileNames.uniqueTimestampedFile(dir, "", ".jpg")
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            return file to uri
        }

        fun convertImageToPdf(
            context: Context,
            projectId: Long,
            imageUri: Uri,
            fileName: String? = null,
        ): Pair<String, String>? {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val original = inputStream.use(BitmapFactory::decodeStream) ?: return null
            val scaled = scaleDown(original, MAX_DIMENSION)
            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(scaled.width, scaled.height, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(scaled, 0f, 0f, null)
                document.finishPage(page)
                val pdfDir = File(context.filesDir, "pattern_pdfs/$projectId")
                val pdfFile =
                    PatternDocumentFiles.writeUniquePdf(
                        directory = pdfDir,
                        fileName = fileName ?: "pattern-scan-${System.currentTimeMillis()}.pdf",
                    ) { targetFile ->
                        targetFile.outputStream().use(document::writeTo)
                    } ?: return null
                return pdfFile.toUri().toString() to pdfFile.name
            } finally {
                document.close()
                if (scaled !== original) scaled.recycle()
                original.recycle()
            }
        }

        private fun scaleDown(
            bitmap: Bitmap,
            maxDimension: Int,
        ): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= maxDimension && height <= maxDimension) return bitmap

            val ratio = maxDimension.toFloat() / maxOf(width, height)
            val newWidth = (width * ratio).toInt().coerceAtLeast(1)
            val newHeight = (height * ratio).toInt().coerceAtLeast(1)
            return bitmap.scale(newWidth, newHeight)
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
            const val MAX_DIMENSION = 1800
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
    ): File? {
        if (!directory.exists() && !directory.mkdirs()) return null
        val safeFileName = safePdfFileName(fileName)
        val tempFile = createTempFile(directory, safeFileName)
        return try {
            write(tempFile)
            moveToUniquePdf(tempFile, directory, safeFileName)
        } catch (_: Exception) {
            null
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
            fileName.substringBeforeLast(PDF_EXTENSION)
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
