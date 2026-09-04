package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PatternDocumentStoragePdfImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = PatternDocumentStorage()
    private val sourceDirectory = File(context.cacheDir, "pattern-pdf-import-test")
    private val targetDirectory = File(context.filesDir, "pattern_pdfs/$PROJECT_ID")

    @Before
    fun setUp() {
        sourceDirectory.deleteRecursively()
        targetDirectory.deleteRecursively()
        sourceDirectory.mkdirs()
    }

    @After
    fun tearDown() {
        sourceDirectory.deleteRecursively()
        targetDirectory.deleteRecursively()
    }

    @Test
    fun validPdfIsPublishedOnlyAfterItCanBeOpened() =
        runTest {
            val source = File(sourceDirectory, "valid.pdf")
            writeOnePagePdf(source)

            val imported = storage.copyPdfToInternal(context, PROJECT_ID, source.toUri(), "chart.pdf")

            assertNotNull(imported)
            PdfPageRenderer(context, requireNotNull(imported).toUri()).use { renderer ->
                assertEquals(1, renderer.pageCount)
            }
            assertTrue(source.isFile)
        }

    @Test
    fun invalidPdfIsNotPublishedAndSourceIsPreserved() =
        runTest {
            val source = File(sourceDirectory, "invalid.pdf").apply { writeText("not a PDF") }

            val imported = storage.copyPdfToInternal(context, PROJECT_ID, source.toUri(), "invalid.pdf")

            assertNull(imported)
            assertTrue(targetDirectory.listFiles().orEmpty().isEmpty())
            assertEquals("not a PDF", source.readText())
        }

    private fun writeOnePagePdf(file: File) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
            document.finishPage(page)
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
    }

    private companion object {
        const val PROJECT_ID = 93_400L
    }
}
