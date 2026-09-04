package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min

internal const val PDF_RENDER_MAX_BITMAP_DIMENSION = 4096

internal data class PdfRenderBitmapSize(
    val width: Int,
    val height: Int,
)

internal fun calculatePdfRenderBitmapSize(
    pageWidth: Int,
    pageHeight: Int,
    targetWidth: Int,
    maxBitmapDimension: Int = PDF_RENDER_MAX_BITMAP_DIMENSION,
): PdfRenderBitmapSize {
    val safePageWidth = pageWidth.coerceAtLeast(1).toDouble()
    val safePageHeight = pageHeight.coerceAtLeast(1).toDouble()
    val safeMaxBitmapDimension = maxBitmapDimension.coerceAtLeast(1)
    val targetScale = targetWidth.coerceAtLeast(1).toDouble() / safePageWidth
    val maxScale = safeMaxBitmapDimension.toDouble() / max(safePageWidth, safePageHeight)
    val scale = min(targetScale, maxScale)
    return PdfRenderBitmapSize(
        width = (safePageWidth * scale).toInt().coerceIn(1, safeMaxBitmapDimension),
        height = (safePageHeight * scale).toInt().coerceIn(1, safeMaxBitmapDimension),
    )
}

class PdfPageRenderer(
    context: Context,
    uri: Uri,
) : Closeable {
    private val fileDescriptor: ParcelFileDescriptor =
        requireNotNull(AppFileStorage.openReadDescriptor(context, uri)) {
            "PDF could not be opened"
        }
    private val renderer = createRenderer(fileDescriptor)

    val pageCount: Int
        get() = renderer.pageCount

    @Synchronized
    fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
        maxBitmapDimension: Int = PDF_RENDER_MAX_BITMAP_DIMENSION,
    ): Bitmap {
        val safeIndex = pageIndex.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
        renderer.openPage(safeIndex).use { page ->
            val (width, height) =
                calculatePdfRenderBitmapSize(
                    pageWidth = page.width,
                    pageHeight = page.height,
                    targetWidth = targetWidth,
                    maxBitmapDimension = maxBitmapDimension,
                )
            val bitmap = createBitmap(width, height)
            return runCatching {
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }.onFailure { bitmap.recycle() }
                .getOrThrow()
        }
    }

    @Synchronized
    override fun close() {
        try {
            renderer.close()
        } finally {
            fileDescriptor.close()
        }
    }

    private fun createRenderer(fileDescriptor: ParcelFileDescriptor): PdfRenderer =
        runCatching { PdfRenderer(fileDescriptor) }
            .getOrElse { failure ->
                runCatching { fileDescriptor.close() }
                throw failure
            }
}
