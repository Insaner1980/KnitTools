package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.TextBoxPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PatternPdfExporterTest {
    private lateinit var context: Context
    private lateinit var exporter: PatternPdfExporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        exporter = PatternPdfExporter(context, Dispatchers.IO)
        exportTempDirectory().deleteRecursively()
    }

    @Test
    fun exportWritesEveryPageAndUnicodeAnnotation() =
        runBlocking {
            val source = createPdf("source-multi.pdf", pageCount = 2)
            val destination = File(context.cacheDir, "annotated-multi.pdf")
            val progress = mutableListOf<PatternPdfExportProgress>()

            exporter.export(
                sourceUri = source.toUri(),
                destinationUri = destination.toUri(),
                annotations = listOf(unicodeTextAnnotation()),
                trackerHighlights = emptyMap(),
                style = renderStyle(),
                onProgress = progress::add,
            )

            PdfPageRenderer(context, destination.toUri()).use { rendered -> assertEquals(2, rendered.pageCount) }
            assertEquals(PatternPdfExportProgress(2, 2), progress.last())
            assertTrue(destination.length() > 0L)
            assertTempDirectoryEmpty()
        }

    @Test
    fun cancellationCleansTemporaryFile() =
        runBlocking {
            val source = createPdf("source-cancel.pdf", pageCount = 2)
            val destination = File(context.cacheDir, "annotated-cancel.pdf")

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = destination.toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                    ) { progress ->
                        if (progress.completedPages == 1) throw CancellationException("test cancellation")
                    }
                }.exceptionOrNull()

            assertTrue(failure is CancellationException)
            assertFalse(destination.exists())
            assertTempDirectoryEmpty()
        }

    @Test
    fun destinationWriteFailureCleansTemporaryFile() =
        runBlocking {
            val source = createPdf("source-error.pdf", pageCount = 1)

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = "content://invalid/export.pdf".toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = {},
                    )
                }.exceptionOrNull()

            assertTrue(failure != null)
            assertTempDirectoryEmpty()
        }

    private fun createPdf(
        name: String,
        pageCount: Int,
    ): File {
        val file = File(context.cacheDir, name)
        val document = PdfDocument()
        try {
            repeat(pageCount) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                document.finishPage(page)
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        return file
    }

    private fun unicodeTextAnnotation() =
        PatternAnnotation(
            layerId = 1L,
            page = 0,
            kind = PatternAnnotationKind.TEXT_BOX,
            payload =
                TextBoxPayload(
                    bounds = NormalizedPatternBounds(0.1f, 0.1f, 0.9f, 0.3f),
                    text = "Neulehuomautus: ääkköset ✓",
                    textSizeSp = 24f,
                    textArgb = Color.BLACK,
                ),
            zIndex = 0L,
        )

    private fun renderStyle() =
        PatternAnnotationRenderStyle(
            referencePageSize = 1_000f,
            highlighterDefaultAlpha = 96,
            calloutBackgroundAlpha = 0.16f,
            calloutTextSize = 24f,
            calloutCornerRadius = 12f,
            chartGridArgb = Color.DKGRAY,
            chartGridWidth = 2f,
            arrowHeadMultiplier = 4f,
            arrowHeadAngle = 0.55f,
            minimumPressureScale = 0.15f,
        )

    private fun exportTempDirectory(): File = File(context.cacheDir, "pattern_exports")

    private fun assertTempDirectoryEmpty() {
        assertTrue(exportTempDirectory().listFiles().isNullOrEmpty())
    }
}
