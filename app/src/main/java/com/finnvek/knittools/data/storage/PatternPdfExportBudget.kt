package com.finnvek.knittools.data.storage

import com.finnvek.knittools.domain.calculator.ChartTrackerHighlight
import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import java.io.IOException
import java.io.OutputStream

internal const val PATTERN_PDF_EXPORT_MAX_ANNOTATIONS = 2_000
internal const val PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES = 16L * 1_024L * 1_024L

internal data class PatternPdfExportLimits(
    val maxPageCount: Int = 100,
    val maxSourcePageDimension: Int = 14_400,
    val maxBitmapDimension: Int = PATTERN_PDF_EXPORT_MAX_BITMAP_DIMENSION,
    val maxRenderedPixelsPerPage: Long = 3_240_000L,
    val maxTotalRenderedPixels: Long = 48_600_000L,
    val maxAnnotations: Int = PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
    val maxAnnotationPayloadBytes: Long = PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
    val maxAnnotationWork: Long = 250_000L,
    val maxTrackerHighlightCells: Long = 20_000L,
    val maxTemporaryOutputBytes: Long = 200L * 1_024L * 1_024L,
    val cacheReserveBytes: Long = 32L * 1_024L * 1_024L,
)

internal val DEFAULT_PATTERN_PDF_EXPORT_LIMITS = PatternPdfExportLimits()

internal enum class PatternPdfExportLimitReason {
    PAGE_COUNT,
    SOURCE_PAGE_DIMENSIONS,
    PAGE_PIXELS,
    TOTAL_PIXELS,
    ANNOTATIONS,
    ANNOTATION_WORK,
    TRACKER_HIGHLIGHT_WORK,
    CACHE_SPACE,
    OUTPUT_BYTES,
}

internal class PatternPdfExportLimitException(
    val reason: PatternPdfExportLimitReason,
) : IOException("Pattern PDF export resource limit exceeded: $reason")

internal data class PatternPdfSourcePageSize(
    val width: Int,
    val height: Int,
)

internal data class PatternPdfExportPagePlan(
    val bitmapSize: PdfRenderBitmapSize,
    val renderedPixels: Long,
)

internal data class PatternPdfExportPlan(
    val pages: List<PatternPdfExportPagePlan>,
    val totalRenderedPixels: Long,
)

internal object PatternPdfExportBudget {
    /*
     * Annotated export keeps one PdfDocument alive until writeTo. The total-pixel
     * cap therefore bounds cumulative raster work independently of the 100-page
     * metadata cap. A 14 400-unit source side matches PDF's normal 200-inch page
     * boundary, while the existing 1 800-pixel raster edge remains unchanged.
     */
    fun planDocument(
        pageCount: Int,
        limits: PatternPdfExportLimits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
        pageSizeAt: (Int) -> PatternPdfSourcePageSize,
    ): PatternPdfExportPlan {
        if (pageCount !in 1..limits.maxPageCount) fail(PatternPdfExportLimitReason.PAGE_COUNT)
        val pages = ArrayList<PatternPdfExportPagePlan>(pageCount)
        var totalPixels = 0L
        repeat(pageCount) { pageIndex ->
            val sourceSize = pageSizeAt(pageIndex)
            if (sourceSize.width !in 1..limits.maxSourcePageDimension ||
                sourceSize.height !in 1..limits.maxSourcePageDimension
            ) {
                fail(PatternPdfExportLimitReason.SOURCE_PAGE_DIMENSIONS)
            }
            val bitmapSize =
                calculatePdfRenderBitmapSize(
                    pageWidth = sourceSize.width,
                    pageHeight = sourceSize.height,
                    targetWidth = limits.maxBitmapDimension,
                    maxBitmapDimension = limits.maxBitmapDimension,
                )
            val pagePixels =
                multiplyWithinLimit(
                    bitmapSize.width.toLong(),
                    bitmapSize.height.toLong(),
                    limits.maxRenderedPixelsPerPage,
                ) ?: fail(PatternPdfExportLimitReason.PAGE_PIXELS)
            totalPixels =
                addWithinLimit(totalPixels, pagePixels, limits.maxTotalRenderedPixels)
                    ?: fail(PatternPdfExportLimitReason.TOTAL_PIXELS)
            pages += PatternPdfExportPagePlan(bitmapSize, pagePixels)
        }
        return PatternPdfExportPlan(pages, totalPixels)
    }

    fun validateAnnotations(
        annotations: List<PatternAnnotation>,
        trackerHighlights: Map<Long, ChartTrackerHighlight>,
        limits: PatternPdfExportLimits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
    ) {
        requireAnnotationCount(annotations.size, limits)
        var annotationWork = 0L
        annotations.forEach { annotation ->
            annotationWork =
                addWithinLimit(annotationWork, annotation.renderWork(), limits.maxAnnotationWork)
                    ?: fail(PatternPdfExportLimitReason.ANNOTATION_WORK)
        }
        if (trackerHighlights.size > limits.maxAnnotations) fail(PatternPdfExportLimitReason.TRACKER_HIGHLIGHT_WORK)
        var highlightCells = 0L
        trackerHighlights.values.forEach { highlight ->
            highlightCells =
                addWithinLimit(
                    highlightCells,
                    highlight.cells.size.toLong(),
                    limits.maxTrackerHighlightCells,
                ) ?: fail(PatternPdfExportLimitReason.TRACKER_HIGHLIGHT_WORK)
        }
    }

    fun requireAnnotationCount(
        annotationCount: Int,
        limits: PatternPdfExportLimits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
    ) {
        if (annotationCount !in 0..limits.maxAnnotations) fail(PatternPdfExportLimitReason.ANNOTATIONS)
    }

    fun requireCacheSpace(
        availableBytes: Long,
        limits: PatternPdfExportLimits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS,
    ) {
        val hasSpace =
            availableBytes >= 0L &&
                limits.cacheReserveBytes >= 0L &&
                limits.maxTemporaryOutputBytes >= 0L &&
                availableBytes >= limits.cacheReserveBytes &&
                limits.maxTemporaryOutputBytes <= availableBytes - limits.cacheReserveBytes
        if (!hasSpace) fail(PatternPdfExportLimitReason.CACHE_SPACE)
    }
}

internal class PatternPdfExportBoundedOutputStream(
    private val output: OutputStream,
    private val maxBytes: Long,
    private val checkCancelled: () -> Unit = {},
) : OutputStream() {
    private var writtenBytes = 0L
    private var limitExceeded = false

    init {
        require(maxBytes >= 0L)
    }

    override fun write(value: Int) {
        requireCapacity(1)
        checkCancelled()
        output.write(value)
        writtenBytes += 1L
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ) {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        requireCapacity(length)
        checkCancelled()
        output.write(buffer, offset, length)
        writtenBytes += length.toLong()
    }

    override fun flush() = output.flush()

    fun throwIfLimitExceeded() {
        if (limitExceeded) fail(PatternPdfExportLimitReason.OUTPUT_BYTES)
    }

    private fun requireCapacity(additionalBytes: Int) {
        if (additionalBytes.toLong() > maxBytes - writtenBytes) limitExceeded = true
        throwIfLimitExceeded()
    }
}

internal fun multiplyWithinLimit(
    left: Long,
    right: Long,
    limit: Long,
): Long? {
    if (left < 0L || right < 0L || limit < 0L) return null
    if (left != 0L && right > limit / left) return null
    return left * right
}

internal fun addWithinLimit(
    current: Long,
    additional: Long,
    limit: Long,
): Long? {
    if (current < 0L || additional < 0L || limit < 0L || current > limit) return null
    if (additional > limit - current) return null
    return current + additional
}

private fun PatternAnnotation.renderWork(): Long =
    when (val annotationPayload = payload) {
        is FreehandPayload ->
            if (annotationPayload.points.isNotEmpty()) {
                annotationPayload.points.size.toLong()
            } else {
                annotationPayload.legacyPathData
                    .orEmpty()
                    .length
                    .toLong()
            }
        is ShapePayload -> 1L
        is TextBoxPayload -> annotationPayload.text.length.toLong()
        is CalloutPayload -> annotationPayload.title.length.toLong() + annotationPayload.description.length.toLong()
        is ChartRegionPayload -> annotationPayload.rows.toLong() + annotationPayload.columns.toLong()
        is ChartTrackerPayload -> annotationPayload.region.rows.toLong() + annotationPayload.region.columns.toLong()
    }.coerceAtLeast(1L)

private fun fail(reason: PatternPdfExportLimitReason): Nothing = throw PatternPdfExportLimitException(reason)
