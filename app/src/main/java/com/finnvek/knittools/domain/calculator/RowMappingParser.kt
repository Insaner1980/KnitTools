package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.advanceReadingLineForRowDelta
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

internal const val ROW_MAPPING_MAX_MARKERS = 4_096
internal const val ROW_MAPPING_MAX_JSON_LENGTH = 256 * 1_024

@Serializable
data class RowMarker(
    val row: Int,
    val page: Int,
    val yPosition: Float,
)

enum class ReadingLineResolutionKind {
    EXACT_MARKER,
    SAME_PAGE_INTERPOLATION,
    ROW_DELTA_FALLBACK,
    UNCHANGED,
    AMBIGUOUS_FALLBACK,
}

data class ReadingLineLocationResolution(
    val targetPage: Int,
    val targetYFraction: Float,
    val kind: ReadingLineResolutionKind,
    val automaticPageChange: Boolean,
)

private val rowMappingJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

private val rowMarkerOrder: Comparator<RowMarker> = compareBy<RowMarker> { it.page }.thenBy { it.row }

fun parseMapping(json: String?): List<RowMarker> {
    if (json == null || json.length > ROW_MAPPING_MAX_JSON_LENGTH || json.isBlank()) return emptyList()
    if (json.encodeToByteArray().size > ROW_MAPPING_MAX_JSON_LENGTH) return emptyList()
    return runCatching {
        (rowMappingJson.parseToJsonElement(json) as? JsonArray)
            ?.asSequence()
            ?.mapNotNull { element ->
                runCatching { rowMappingJson.decodeFromJsonElement<RowMarker>(element) }.getOrNull()
            }?.filter(RowMarker::isValid)
            ?.distinctBy { marker -> marker.row to marker.page }
            ?.take(ROW_MAPPING_MAX_MARKERS)
            ?.sortedWith(rowMarkerOrder)
            ?.toList()
            .orEmpty()
    }.getOrDefault(emptyList())
}

fun serializeMapping(markers: List<RowMarker>): String =
    rowMappingJson.encodeToString(
        markers
            .filter(RowMarker::isValid)
            .distinctBy { it.row to it.page }
            .take(ROW_MAPPING_MAX_MARKERS)
            .sortedWith(rowMarkerOrder),
    )

fun createCalibrationRowMarkers(
    firstRow: Int,
    firstPage: Int,
    firstYPosition: Float,
    lastRow: Int,
    lastPage: Int,
    lastYPosition: Float,
): List<RowMarker>? {
    if (firstRow == lastRow) return null
    if (firstRow < 0 || lastRow < 0 || firstPage < 0 || lastPage < 0) return null
    if (!firstYPosition.isFinite() || !lastYPosition.isFinite()) return null

    return listOf(
        RowMarker(
            row = firstRow,
            page = firstPage,
            yPosition = sanitizeReadingLineYFraction(firstYPosition),
        ),
        RowMarker(
            row = lastRow,
            page = lastPage,
            yPosition = sanitizeReadingLineYFraction(lastYPosition),
        ),
    ).sortedWith(rowMarkerOrder)
}

fun resolveReadingLineLocation(
    markers: List<RowMarker>,
    previousRow: Int,
    newRow: Int,
    currentPage: Int,
    currentYFraction: Float,
    pageCount: Int? = null,
): ReadingLineLocationResolution {
    val validPageCount = pageCount?.takeIf { it > 0 }
    val safeCurrentPage = currentPage.coerceToPageCount(validPageCount)
    val safeCurrentY = sanitizeReadingLineYFraction(currentYFraction)
    val validMarkers = markers.validForPageCount(validPageCount)

    val exactMatch = validMarkers.findExactMarker(newRow, safeCurrentPage)
    if (exactMatch.marker != null) {
        return resolvedLocation(
            page = exactMatch.marker.page,
            yFraction = exactMatch.marker.yPosition,
            kind = ReadingLineResolutionKind.EXACT_MARKER,
            currentPage = currentPage,
        )
    }

    val interpolationCandidates =
        if (exactMatch.ambiguous) emptyList() else validMarkers.interpolationCandidates(newRow)
    val interpolation =
        interpolationCandidates.firstOrNull { it.page == safeCurrentPage }
            ?: interpolationCandidates.singleOrNull()
    if (interpolation != null) {
        return resolvedLocation(
            page = interpolation.page,
            yFraction = interpolation.yFraction,
            kind = ReadingLineResolutionKind.SAME_PAGE_INTERPOLATION,
            currentPage = currentPage,
        )
    }

    return fallbackLocation(
        previousRow = previousRow,
        newRow = newRow,
        currentPage = currentPage,
        safeCurrentPage = safeCurrentPage,
        safeCurrentY = safeCurrentY,
        ambiguous = exactMatch.ambiguous || interpolationCandidates.size > 1,
    )
}

private data class ExactMarkerMatch(
    val marker: RowMarker?,
    val ambiguous: Boolean,
)

private fun List<RowMarker>.findExactMarker(
    targetRow: Int,
    currentPage: Int,
): ExactMarkerMatch {
    val matches = filter { it.row == targetRow }
    val marker =
        when {
            matches.size == 1 -> matches.single()
            matches.size > 1 -> matches.singleOrNull { it.page == currentPage }
            else -> null
        }
    return ExactMarkerMatch(marker = marker, ambiguous = marker == null && matches.size > 1)
}

private fun List<RowMarker>.validForPageCount(pageCount: Int?): List<RowMarker> =
    filter { marker -> marker.isValid() && (pageCount == null || marker.page < pageCount) }

private fun List<RowMarker>.interpolationCandidates(targetRow: Int): List<InterpolationCandidate> =
    groupBy(RowMarker::page).mapNotNull { (page, pageMarkers) ->
        interpolationCandidate(page, pageMarkers, targetRow)
    }

private fun Int.coerceToPageCount(pageCount: Int?): Int = pageCount?.let { coerceIn(0, it - 1) } ?: coerceAtLeast(0)

private fun fallbackLocation(
    previousRow: Int,
    newRow: Int,
    currentPage: Int,
    safeCurrentPage: Int,
    safeCurrentY: Float,
    ambiguous: Boolean,
): ReadingLineLocationResolution {
    val rowDelta =
        (newRow.toLong() - previousRow.toLong())
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            .toInt()
    val targetY = if (rowDelta == 0) safeCurrentY else advanceReadingLineForRowDelta(safeCurrentY, rowDelta)
    val kind =
        when {
            ambiguous -> ReadingLineResolutionKind.AMBIGUOUS_FALLBACK
            rowDelta == 0 -> ReadingLineResolutionKind.UNCHANGED
            else -> ReadingLineResolutionKind.ROW_DELTA_FALLBACK
        }
    return ReadingLineLocationResolution(
        targetPage = safeCurrentPage,
        targetYFraction = targetY,
        kind = kind,
        automaticPageChange = safeCurrentPage != currentPage,
    )
}

private data class InterpolationCandidate(
    val page: Int,
    val yFraction: Float,
)

private fun interpolationCandidate(
    page: Int,
    markers: List<RowMarker>,
    targetRow: Int,
): InterpolationCandidate? {
    val lowerRow = markers.filter { it.row < targetRow }.maxOfOrNull(RowMarker::row) ?: return null
    val upperRow = markers.filter { it.row > targetRow }.minOfOrNull(RowMarker::row) ?: return null
    val lower = markers.singleOrNull { it.row == lowerRow } ?: return null
    val upper = markers.singleOrNull { it.row == upperRow } ?: return null
    val progress = (targetRow - lower.row).toFloat() / (upper.row - lower.row).toFloat()
    return InterpolationCandidate(
        page = page,
        yFraction = lower.yPosition + ((upper.yPosition - lower.yPosition) * progress),
    )
}

private fun RowMarker.isValid(): Boolean = row >= 0 && page >= 0 && yPosition.isFinite() && yPosition in 0f..1f

private fun resolvedLocation(
    page: Int,
    yFraction: Float,
    kind: ReadingLineResolutionKind,
    currentPage: Int,
): ReadingLineLocationResolution =
    ReadingLineLocationResolution(
        targetPage = page,
        targetYFraction = sanitizeReadingLineYFraction(yFraction),
        kind = kind,
        automaticPageChange = page != currentPage,
    )
