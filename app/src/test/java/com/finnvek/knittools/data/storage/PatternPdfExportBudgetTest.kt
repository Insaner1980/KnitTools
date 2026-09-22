package com.finnvek.knittools.data.storage

import com.finnvek.knittools.domain.calculator.ChartCell
import com.finnvek.knittools.domain.calculator.ChartTrackerHighlight
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class PatternPdfExportBudgetTest {
    @Test
    fun `page count accepts exact boundary and rejects boundary plus one`() {
        val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS

        val plan =
            PatternPdfExportBudget.planDocument(limits.maxPageCount, limits) {
                PatternPdfSourcePageSize(width = limits.maxSourcePageDimension, height = 1)
            }

        assertEquals(limits.maxPageCount, plan.pages.size)
        assertLimit(PatternPdfExportLimitReason.PAGE_COUNT) {
            PatternPdfExportBudget.planDocument(limits.maxPageCount + 1, limits) {
                PatternPdfSourcePageSize(1, 1)
            }
        }
    }

    @Test
    fun `total pixels accept exact boundary and reject the next page`() {
        val pagePixels = 1_800L * 1_800L
        val limits =
            DEFAULT_PATTERN_PDF_EXPORT_LIMITS.copy(
                maxPageCount = 3,
                maxTotalRenderedPixels = pagePixels * 2L,
            )

        val accepted =
            PatternPdfExportBudget.planDocument(2, limits) {
                PatternPdfSourcePageSize(width = 100, height = 100)
            }

        assertEquals(pagePixels * 2L, accepted.totalRenderedPixels)
        assertLimit(PatternPdfExportLimitReason.TOTAL_PIXELS) {
            PatternPdfExportBudget.planDocument(3, limits) {
                PatternPdfSourcePageSize(width = 100, height = 100)
            }
        }
    }

    @Test
    fun `aggregate arithmetic rejects overflow attempts`() {
        assertNull(multiplyWithinLimit(Long.MAX_VALUE, 2L, Long.MAX_VALUE))
        assertNull(addWithinLimit(Long.MAX_VALUE, 1L, Long.MAX_VALUE))
        assertNull(addWithinLimit(0L, Long.MAX_VALUE, Long.MAX_VALUE - 1L))
    }

    @Test
    fun `source page dimensions accept PDF boundary and reject malicious values`() {
        val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS.copy(maxPageCount = 1)

        PatternPdfExportBudget.planDocument(1, limits) {
            PatternPdfSourcePageSize(limits.maxSourcePageDimension, 1)
        }

        assertLimit(PatternPdfExportLimitReason.SOURCE_PAGE_DIMENSIONS) {
            PatternPdfExportBudget.planDocument(1, limits) {
                PatternPdfSourcePageSize(limits.maxSourcePageDimension + 1, 1)
            }
        }
        assertLimit(PatternPdfExportLimitReason.SOURCE_PAGE_DIMENSIONS) {
            PatternPdfExportBudget.planDocument(1, limits) {
                PatternPdfSourcePageSize(Int.MAX_VALUE, 0)
            }
        }
    }

    @Test
    fun `annotation count accepts boundary and rejects boundary plus one`() {
        val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS

        PatternPdfExportBudget.requireAnnotationCount(limits.maxAnnotations, limits)

        assertLimit(PatternPdfExportLimitReason.ANNOTATIONS) {
            PatternPdfExportBudget.requireAnnotationCount(limits.maxAnnotations + 1, limits)
        }
    }

    @Test
    fun `annotation and derived highlight work have exact boundaries`() {
        val annotations = List(3) { index -> lineAnnotation(index.toLong()) }
        val twoCells = setOf(ChartCell(0, 0), ChartCell(0, 1))
        val threeCells = twoCells + ChartCell(0, 2)
        val limits =
            DEFAULT_PATTERN_PDF_EXPORT_LIMITS.copy(
                maxAnnotations = 3,
                maxAnnotationWork = 2L,
                maxTrackerHighlightCells = 2L,
            )

        PatternPdfExportBudget.validateAnnotations(annotations.take(2), emptyMap(), limits)
        assertLimit(PatternPdfExportLimitReason.ANNOTATION_WORK) {
            PatternPdfExportBudget.validateAnnotations(annotations, emptyMap(), limits)
        }
        PatternPdfExportBudget.validateAnnotations(
            annotations = emptyList(),
            trackerHighlights = mapOf(1L to highlight(twoCells)),
            limits = limits,
        )
        assertLimit(PatternPdfExportLimitReason.TRACKER_HIGHLIGHT_WORK) {
            PatternPdfExportBudget.validateAnnotations(
                annotations = emptyList(),
                trackerHighlights = mapOf(1L to highlight(threeCells)),
                limits = limits,
            )
        }
    }

    @Test
    fun `cache check preserves reserve without overflowing`() {
        val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS
        val required = limits.maxTemporaryOutputBytes + limits.cacheReserveBytes

        PatternPdfExportBudget.requireCacheSpace(required, limits)

        assertLimit(PatternPdfExportLimitReason.CACHE_SPACE) {
            PatternPdfExportBudget.requireCacheSpace(required - 1L, limits)
        }
        assertLimit(PatternPdfExportLimitReason.CACHE_SPACE) {
            PatternPdfExportBudget.requireCacheSpace(
                Long.MAX_VALUE,
                limits.copy(maxTemporaryOutputBytes = Long.MAX_VALUE, cacheReserveBytes = 1L),
            )
        }
    }

    @Test
    fun `temporary output accepts exact cap and rejects the next byte`() {
        val target = ByteArrayOutputStream()
        val output = PatternPdfExportBoundedOutputStream(target, maxBytes = 3L)

        output.write(byteArrayOf(1, 2, 3))

        assertArrayEquals(byteArrayOf(1, 2, 3), target.toByteArray())
        assertLimit(PatternPdfExportLimitReason.OUTPUT_BYTES) { output.write(4) }
        assertArrayEquals(byteArrayOf(1, 2, 3), target.toByteArray())
    }

    private fun lineAnnotation(id: Long) =
        PatternAnnotation(
            id = id,
            layerId = 1L,
            page = 0,
            kind = PatternAnnotationKind.LINE,
            payload =
                ShapePayload(
                    start = NormalizedPatternPoint(0.1f, 0.1f),
                    end = NormalizedPatternPoint(0.9f, 0.9f),
                    strokeArgb = 0xFF000000.toInt(),
                    strokeWidth = 2f,
                ),
            zIndex = id,
        )

    private fun highlight(cells: Set<ChartCell>) =
        ChartTrackerHighlight(cells = cells, activeCell = cells.firstOrNull(), counterAvailable = true)

    private fun assertLimit(
        reason: PatternPdfExportLimitReason,
        block: () -> Unit,
    ) {
        val failure = assertThrows(PatternPdfExportLimitException::class.java, block)
        assertEquals(reason, failure.reason)
    }
}
