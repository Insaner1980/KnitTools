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
class PatternPdfExporter private constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val limits: PatternPdfExportLimits,
    private val cacheSpaceProvider: (File) -> Long,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        context = context,
        ioDispatcher = ioDispatcher,
        limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
        cacheSpaceProvider = File::getUsableSpace,
    )

    internal suspend fun preflight(
        sourceUri: Uri,
        annotations: List<PatternAnnotation>,
        trackerHighlights: Map<Long, ChartTrackerHighlight>,
    ): PatternPdfExportPlan =
        withContext(ioDispatcher) {
            validateInputs(annotations, trackerHighlights)
            PdfPageRenderer(context, sourceUri).use(::planDocument)
        }

    suspend fun export(
        sourceUri: Uri,
        destinationUri: Uri,
        annotations: List<PatternAnnotation>,
        trackerHighlights: Map<Long, ChartTrackerHighlight>,
        style: PatternAnnotationRenderStyle,
        onProgress: (PatternPdfExportProgress) -> Unit,
    ) = withContext(ioDispatcher) {
        requireDifferentDestination(sourceUri, destinationUri)
        IncompleteDestination.writeWithCleanup(context, destinationUri) {
            validateInputs(annotations, trackerHighlights)
            val exportDirectory = File(context.cacheDir, EXPORT_TEMP_DIRECTORY).apply { mkdirs() }
            if (!exportDirectory.isDirectory) throw IOException("Pattern PDF export cache is unavailable")
            val tempFile = File.createTempFile(EXPORT_TEMP_PREFIX, EXPORT_TEMP_SUFFIX, exportDirectory)
            try {
                PdfPageRenderer(context, sourceUri).use { renderer ->
                    val plan = planDocument(renderer)
                    renderToTempFile(renderer, plan, tempFile, annotations, trackerHighlights, style, onProgress)
                }
                coroutineContext.ensureActive()
                copyToDestination(tempFile, destinationUri)
            } finally {
                if (tempFile.exists() && !tempFile.delete()) tempFile.deleteOnExit()
            }
        }
    }

    private fun requireDifferentDestination(
        source: Uri,
        destination: Uri,
    ) {
        fun localFile(uri: Uri): File? =
            AppFileStorage.resolveAppOwnedFile(context, uri)
                ?: uri.path?.takeIf { uri.scheme == "file" }?.let(::File)
        val sourceFile = localFile(source)?.canonicalFile
        val destinationFile = localFile(destination)?.canonicalFile
        if (source == destination || (sourceFile != null && sourceFile == destinationFile)) {
            throw IOException("Pattern PDF destination must not overwrite its source")
        }
    }

    private fun validateInputs(
        annotations: List<PatternAnnotation>,
        trackerHighlights: Map<Long, ChartTrackerHighlight>,
    ) {
        PatternPdfExportBudget.validateAnnotations(annotations, trackerHighlights, limits)
        PatternPdfExportBudget.requireCacheSpace(cacheSpaceProvider(context.cacheDir), limits)
    }

    private fun planDocument(renderer: PdfPageRenderer): PatternPdfExportPlan =
        PatternPdfExportBudget.planDocument(renderer.pageCount, limits) { page ->
            try {
                renderer.pageSize(page)
            } catch (failure: IllegalStateException) {
                throw PatternPdfExportLimitException(PatternPdfExportLimitReason.SOURCE_PAGE_DIMENSIONS, failure)
            }
        }

    private suspend fun renderToTempFile(
        renderer: PdfPageRenderer,
        plan: PatternPdfExportPlan,
        tempFile: File,
        annotations: List<PatternAnnotation>,
        trackerHighlights: Map<Long, ChartTrackerHighlight>,
        style: PatternAnnotationRenderStyle,
        onProgress: (PatternPdfExportProgress) -> Unit,
    ) {
        val annotationsByPage = annotations.groupBy(PatternAnnotation::page)
        val document = PdfDocument()
        try {
            plan.pages.forEachIndexed { pageIndex, pagePlan ->
                coroutineContext.ensureActive()
                val bitmap =
                    renderer.renderPage(
                        pageIndex = pageIndex,
                        targetWidth = limits.maxBitmapDimension,
                        maxBitmapDimension = limits.maxBitmapDimension,
                    )
                try {
                    check(
                        bitmap.width == pagePlan.bitmapSize.width &&
                            bitmap.height == pagePlan.bitmapSize.height,
                    ) { "PDF page dimensions changed during export" }
                    val pageInfo =
                        PdfDocument.PageInfo
                            .Builder(
                                pagePlan.sourceSize.width,
                                pagePlan.sourceSize.height,
                                pageIndex + 1,
                            ).create()
                    val page = document.startPage(pageInfo)
                    try {
                        page.canvas.scale(
                            pagePlan.sourceSize.width.toFloat() / bitmap.width,
                            pagePlan.sourceSize.height.toFloat() / bitmap.height,
                        )
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        PatternAnnotationCanvasRenderer.render(
                            canvas = page.canvas,
                            width = bitmap.width.toFloat(),
                            height = bitmap.height.toFloat(),
                            annotations = annotationsByPage[pageIndex].orEmpty(),
                            style = style,
                            trackerHighlights = trackerHighlights,
                        )
                    } finally {
                        // Jokainen aloitettu sivu suljetaan ennen poikkeuksen etenemistä.
                        document.finishPage(page)
                    }
                } finally {
                    bitmap.recycle()
                }
                onProgress(PatternPdfExportProgress(pageIndex + 1, plan.pages.size))
            }
            coroutineContext.ensureActive()
            writeDocument(document, tempFile)
            coroutineContext.ensureActive()
        } finally {
            document.close()
        }
    }

    private suspend fun writeDocument(
        document: PdfDocument,
        tempFile: File,
    ) {
        val exportContext = coroutineContext
        tempFile.outputStream().use { output ->
            val boundedOutput =
                PatternPdfExportBoundedOutputStream(
                    output = output,
                    maxBytes = limits.maxTemporaryOutputBytes,
                    checkCancelled = exportContext::ensureActive,
                    checkStorage = { additionalBytes ->
                        val available = cacheSpaceProvider(context.cacheDir)
                        if (available < limits.cacheReserveBytes ||
                            additionalBytes.toLong() > available - limits.cacheReserveBytes
                        ) {
                            throw PatternPdfExportLimitException(PatternPdfExportLimitReason.CACHE_SPACE)
                        }
                    },
                )
            try {
                document.writeTo(boundedOutput)
            } finally {
                // Android's native PDF writer can consume an exception from the output stream.
                exportContext.ensureActive()
                boundedOutput.throwIfWriteFailed()
            }
            boundedOutput.flush()
        }
    }

    private suspend fun copyToDestination(
        tempFile: File,
        destinationUri: Uri,
    ) {
        val resolver = context.contentResolver
        val output =
            resolver.openOutputStream(destinationUri, "wt")
                ?: throw IOException("Pattern PDF destination is unavailable")
        var copied = false
        // use säilyttää alkuperäisen virheen tai peruutuksen siivousvirheen rinnalla.
        AutoCloseable {
            if (!copied) IncompleteDestination.delete(context, destinationUri)
        }.use {
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
            copied = true
        }
    }

    internal companion object {
        private const val EXPORT_TEMP_DIRECTORY = "pattern_exports"
        private const val EXPORT_TEMP_PREFIX = "annotated-pattern-"
        private const val EXPORT_TEMP_SUFFIX = ".tmp"
        private const val COPY_BUFFER_SIZE = 16 * 1024

        fun createForTest(
            context: Context,
            ioDispatcher: CoroutineDispatcher,
            limits: PatternPdfExportLimits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
            cacheSpaceProvider: (File) -> Long = File::getUsableSpace,
        ): PatternPdfExporter = PatternPdfExporter(context, ioDispatcher, limits, cacheSpaceProvider)
    }
}
