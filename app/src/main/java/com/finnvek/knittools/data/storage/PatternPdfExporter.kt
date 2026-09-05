package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.ChartTrackerHighlight
import com.finnvek.knittools.domain.model.PatternAnnotation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class PatternPdfExportProgress(
    val completedPages: Int,
    val totalPages: Int,
)

internal const val PATTERN_PDF_EXPORT_MAX_BITMAP_DIMENSION = 1_800

@Singleton
class PatternPdfExporter
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun export(
            sourceUri: Uri,
            destinationUri: Uri,
            annotations: List<PatternAnnotation>,
            trackerHighlights: Map<Long, ChartTrackerHighlight>,
            style: PatternAnnotationRenderStyle,
            onProgress: (PatternPdfExportProgress) -> Unit,
        ) = withContext(ioDispatcher) {
            val exportDirectory = File(context.cacheDir, EXPORT_TEMP_DIRECTORY).apply { mkdirs() }
            val tempFile = File.createTempFile(EXPORT_TEMP_PREFIX, EXPORT_TEMP_SUFFIX, exportDirectory)
            try {
                renderToTempFile(sourceUri, tempFile, annotations, trackerHighlights, style, onProgress)
                copyToDestination(tempFile, destinationUri)
            } finally {
                if (tempFile.exists() && !tempFile.delete()) tempFile.deleteOnExit()
            }
        }

        private suspend fun renderToTempFile(
            sourceUri: Uri,
            tempFile: File,
            annotations: List<PatternAnnotation>,
            trackerHighlights: Map<Long, ChartTrackerHighlight>,
            style: PatternAnnotationRenderStyle,
            onProgress: (PatternPdfExportProgress) -> Unit,
        ) {
            PdfPageRenderer(context, sourceUri).use { renderer ->
                val document = PdfDocument()
                try {
                    repeat(renderer.pageCount) { pageIndex ->
                        coroutineContext.ensureActive()
                        val bitmap =
                            renderer.renderPage(
                                pageIndex = pageIndex,
                                targetWidth = PATTERN_PDF_EXPORT_MAX_BITMAP_DIMENSION,
                                maxBitmapDimension = PATTERN_PDF_EXPORT_MAX_BITMAP_DIMENSION,
                            )
                        try {
                            val pageInfo =
                                PdfDocument.PageInfo
                                    .Builder(
                                        bitmap.width,
                                        bitmap.height,
                                        pageIndex + 1,
                                    ).create()
                            val page = document.startPage(pageInfo)
                            try {
                                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                PatternAnnotationCanvasRenderer.render(
                                    canvas = page.canvas,
                                    width = bitmap.width.toFloat(),
                                    height = bitmap.height.toFloat(),
                                    annotations = annotations.filter { it.page == pageIndex },
                                    style = style,
                                    trackerHighlights = trackerHighlights,
                                )
                            } finally {
                                // Jokainen aloitettu sivu on suljettava ennen kuin mahdollinen
                                // renderöintipoikkeus etenee, jotta document.close() ei jää
                                // sulkemaan keskeneräistä sivua ja peitä alkuperäistä virhettä.
                                document.finishPage(page)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                        onProgress(PatternPdfExportProgress(pageIndex + 1, renderer.pageCount))
                    }
                    tempFile.outputStream().buffered().use(document::writeTo)
                } finally {
                    document.close()
                }
            }
        }

        private suspend fun copyToDestination(
            tempFile: File,
            destinationUri: Uri,
        ) {
            val output =
                context.contentResolver.openOutputStream(destinationUri, "w")
                    ?: throw IOException("Pattern PDF destination is unavailable")
            output.buffered().use { target ->
                tempFile.inputStream().buffered().use { input ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                    }
                }
            }
        }

        private companion object {
            const val EXPORT_TEMP_DIRECTORY = "pattern_exports"
            const val EXPORT_TEMP_PREFIX = "annotated-pattern-"
            const val EXPORT_TEMP_SUFFIX = ".tmp"
            const val COPY_BUFFER_SIZE = 16 * 1024
        }
    }
