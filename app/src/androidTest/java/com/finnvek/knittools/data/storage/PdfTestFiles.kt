package com.finnvek.knittools.data.storage

import android.graphics.pdf.PdfDocument
import java.io.File

internal fun writeOnePagePdf(file: File) {
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
        document.finishPage(page)
        file.outputStream().use(document::writeTo)
    } finally {
        document.close()
    }
}
