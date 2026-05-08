package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
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
            val dir = File(context.filesDir, "patterns/$projectId/captures")
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

        fun convertImageToPdf(
            context: Context,
            projectId: Long,
            imageUri: Uri,
            fileName: String? = null,
        ): Pair<String, String>? {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val original = inputStream.use(BitmapFactory::decodeStream) ?: return null
            val scaled = scaleDown(original, MAX_DIMENSION)
            val safeName = (fileName ?: "pattern-scan-${System.currentTimeMillis()}.pdf").ensurePdfExtension()
            val pdfDir = File(context.filesDir, "patterns/$projectId/pdf")
            pdfDir.mkdirs()
            val pdfFile = File(pdfDir, safeName)

            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(scaled.width, scaled.height, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(scaled, 0f, 0f, null)
                document.finishPage(page)
                pdfFile.outputStream().use(document::writeTo)
            } finally {
                document.close()
                if (scaled !== original) scaled.recycle()
                original.recycle()
            }

            val pdfUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile,
                )
            return pdfUri.toString() to pdfFile.name
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
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
            // Jo sisäinen tiedosto — ei tarvitse kopioida
            if (sourceUri.scheme == "file" &&
                sourceUri.path?.startsWith(context.filesDir.absolutePath) == true
            ) {
                return null
            }

            val safeName = fileName.ensurePdfExtension()
            val pdfDir = File(context.filesDir, "patterns/$projectId/pdf")
            pdfDir.mkdirs()
            val targetFile = File(pdfDir, safeName)

            return try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
                FileProvider
                    .getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile,
                    ).toString()
            } catch (_: Exception) {
                null
            }
        }

        private fun String.ensurePdfExtension(): String = if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"

        private companion object {
            const val MAX_DIMENSION = 1800
        }
    }
