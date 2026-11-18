package com.finnvek.knittools.data.storage

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import org.junit.Assert.assertTrue
import java.io.File

internal fun writeTestPdf(
    file: File,
    width: Int = 300,
    height: Int = 400,
    color: Int? = Color.WHITE,
) {
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
        color?.let(page.canvas::drawColor)
        document.finishPage(page)
        file.outputStream().use(document::writeTo)
    } finally {
        document.close()
    }
}

internal fun writeTestBitmap(
    file: File,
    size: Int = 8,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100,
) {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    try {
        file.outputStream().use { assertTrue(bitmap.compress(format, quality, it)) }
    } finally {
        bitmap.recycle()
    }
}
