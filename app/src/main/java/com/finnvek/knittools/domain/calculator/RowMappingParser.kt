package com.finnvek.knittools.domain.calculator

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
        .sortedWith(compareBy<RowMarker> { it.page }.thenBy { it.row })
}

fun serializeMapping(markers: List<RowMarker>): String =
    rowMappingJson.encodeToString(
        markers
            .distinctBy { it.row to it.page }
            .sortedWith(compareBy<RowMarker> { it.page }.thenBy { it.row }),
    )

fun interpolateYPosition(
    markers: List<RowMarker>,
    targetRow: Int,
    page: Int,
): Float? {
    val pageMarkers = markers.filter { it.page == page }.sortedBy { it.row }
    if (pageMarkers.isEmpty()) return null

    pageMarkers.firstOrNull { it.row == targetRow }?.let { return it.yPosition }

    val previous = pageMarkers.lastOrNull { it.row < targetRow }
    val next = pageMarkers.firstOrNull { it.row > targetRow }

    if (previous != null && next != null && previous.row != next.row) {
        val progress = (targetRow - previous.row).toFloat() / (next.row - previous.row).toFloat()
        return previous.yPosition + ((next.yPosition - previous.yPosition) * progress)
    }

    return previous?.yPosition ?: next?.yPosition
}
