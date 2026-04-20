package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable

class PdfPageRenderer(
    context: Context,
    uri: Uri,
) : Closeable {
    private val fileDescriptor: ParcelFileDescriptor =
        requireNotNull(context.contentResolver.openFileDescriptor(uri, "r")) {
            "PDF could not be opened"
        }
    private val renderer = PdfRenderer(fileDescriptor)

    val pageCount: Int
        get() = renderer.pageCount

    fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
    ): Bitmap {
        val safeIndex = pageIndex.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
        renderer.openPage(safeIndex).use { page ->
            val width = targetWidth.coerceAtLeast(1)
            val ratio = page.height.toFloat() / page.width.toFloat()
            val height = (width * ratio).toInt().coerceAtLeast(1)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    override fun close() {
        renderer.close()
        fileDescriptor.close()
    }
}
