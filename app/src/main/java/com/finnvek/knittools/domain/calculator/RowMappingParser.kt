package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.advanceReadingLineForRowDelta
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

fun parseMapping(json: String?): List<RowMarker> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        rowMappingJson.decodeFromString<List<RowMarker>>(json)
    }.getOrDefault(emptyList())
        .filter(RowMarker::isValid)
        .sortedWith(compareBy<RowMarker> { it.page }.thenBy { it.row })
}

fun serializeMapping(markers: List<RowMarker>): String =
    rowMappingJson.encodeToString(
        markers
            .distinctBy { it.row to it.page }
            .sortedWith(compareBy<RowMarker> { it.page }.thenBy { it.row }),
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
    ).sortedWith(compareBy<RowMarker> { it.page }.thenBy { it.row })
}

fun interpolateYPosition(
    markers: List<RowMarker>,
    targetRow: Int,
    page: Int,
): Float? {
    var previous: RowMarker? = null
    var next: RowMarker? = null

    markers.forEach { marker ->
        if (marker.page != page) return@forEach

        when {
            marker.row == targetRow -> return marker.yPosition
            marker.row < targetRow -> {
                val currentPrevious = previous
                if (currentPrevious == null || marker.row >= currentPrevious.row) {
                    previous = marker
                }
            }
            marker.row > targetRow -> {
                val currentNext = next
                if (currentNext == null || marker.row < currentNext.row) {
                    next = marker
                }
            }
        }
    }

    if (previous != null && next != null && previous.row != next.row) {
        val progress = (targetRow - previous.row).toFloat() / (next.row - previous.row).toFloat()
        return previous.yPosition + ((next.yPosition - previous.yPosition) * progress)
    }

    return null
}

fun resolveReadingLineYFraction(
    markers: List<RowMarker>,
    currentRow: Int,
    currentPage: Int,
    currentYFraction: Float,
    rowDelta: Int,
): Float? {
    val mappedYFraction =
        interpolateYPosition(
            markers = markers,
            targetRow = currentRow,
            page = currentPage,
        )
    if (mappedYFraction != null) {
        return sanitizeReadingLineYFraction(mappedYFraction)
    }

    if (rowDelta == 0) return null

    return advanceReadingLineForRowDelta(
        yFraction = currentYFraction,
        rowDelta = rowDelta,
    )
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

    val interpolationCandidates = validMarkers.interpolationCandidates(newRow)
    val interpolation =
        interpolationCandidates.firstOrNull { it.page == safeCurrentPage }
            ?: interpolationCandidates.singleOrNull()
    if (!exactMatch.ambiguous && interpolation != null) {
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
