package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCorner
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.ChartTrackingMode
import com.finnvek.knittools.domain.model.PatternAnnotationLimits
import java.lang.Math.floorMod

data class ChartCell(
    val row: Int,
    val column: Int,
)

data class ChartTrackerHighlight(
    val cells: Set<ChartCell>,
    val activeCell: ChartCell?,
    val counterAvailable: Boolean,
)

fun resolveChartTrackerHighlight(
    tracker: ChartTrackerPayload,
    counterValue: Int?,
): ChartTrackerHighlight {
    if (counterValue == null) return ChartTrackerHighlight(emptySet(), null, counterAvailable = false)
    if (!PatternAnnotationLimits.isValidChartDimensions(tracker.region.rows, tracker.region.columns)) {
        return ChartTrackerHighlight(emptySet(), null, counterAvailable = false)
    }
    val totalCells = tracker.region.rows * tracker.region.columns
    val rawIndex = counterValue.toLong() - tracker.counterStartValue.toLong() + tracker.gridStartIndex.toLong()
    val index =
        if (tracker.wrapAtEnd) {
            floorMod(rawIndex, totalCells.toLong()).toInt()
        } else {
            rawIndex.coerceIn(0L, (totalCells - 1).toLong()).toInt()
        }
    val activeCell = tracker.cellAt(index)
    val cells =
        when (tracker.trackingMode) {
            ChartTrackingMode.ACTIVE_ROW ->
                (0 until tracker.region.columns).mapTo(linkedSetOf()) { column ->
                    ChartCell(activeCell.row, column)
                }
            ChartTrackingMode.ACTIVE_COLUMN ->
                (0 until tracker.region.rows).mapTo(linkedSetOf()) { row ->
                    ChartCell(row, activeCell.column)
                }
            ChartTrackingMode.CROSSHAIR ->
                buildSet {
                    repeat(tracker.region.columns) { column -> add(ChartCell(activeCell.row, column)) }
                    repeat(tracker.region.rows) { row -> add(ChartCell(row, activeCell.column)) }
                }
            ChartTrackingMode.COMPLETED_CELLS ->
                (0..index).mapTo(linkedSetOf()) { completedIndex -> tracker.cellAt(completedIndex) }
            ChartTrackingMode.C2C_DIAGONAL -> setOf(activeCell)
        }
    return ChartTrackerHighlight(cells, activeCell, counterAvailable = true)
}

private fun ChartTrackerPayload.cellAt(index: Int): ChartCell =
    if (trackingMode == ChartTrackingMode.C2C_DIAGONAL) c2cCellAt(index) else standardCellAt(index)

private fun ChartTrackerPayload.standardCellAt(index: Int): ChartCell {
    val logicalRow = index / region.columns
    val logicalColumn = index % region.columns
    return ChartCell(
        row = toVisualRow(logicalRow),
        column = toVisualColumn(logicalColumn, logicalRow),
    )
}

@Suppress("kotlin:S3776") // C2C-läpikäynti pitää diagonaalin rajat näkyvinä yhdessä laskennassa.
private fun ChartTrackerPayload.c2cCellAt(index: Int): ChartCell {
    var remaining = index
    for (diagonal in 0 until region.rows + region.columns - 1) {
        val firstRow = maxOf(0, diagonal - region.columns + 1)
        val lastRow = minOf(region.rows - 1, diagonal)
        val diagonalSize = lastRow - firstRow + 1
        if (remaining < diagonalSize) {
            val row = firstRow + remaining
            val column = diagonal - row
            return ChartCell(
                row = if (c2cOrigin.isBottom) region.rows - 1 - row else row,
                column = if (c2cOrigin.isRight) region.columns - 1 - column else column,
            )
        }
        remaining -= diagonalSize
    }
    error("Chart tracker index outside validated grid")
}

private fun ChartTrackerPayload.toVisualRow(logicalRow: Int): Int =
    when (region.rowDirection) {
        ChartRowDirection.TOP_TO_BOTTOM -> logicalRow
        ChartRowDirection.BOTTOM_TO_TOP -> region.rows - 1 - logicalRow
    }

private fun ChartTrackerPayload.toVisualColumn(
    logicalColumn: Int,
    logicalRow: Int,
): Int =
    when (region.columnDirection) {
        ChartColumnDirection.LEFT_TO_RIGHT -> logicalColumn
        ChartColumnDirection.RIGHT_TO_LEFT -> region.columns - 1 - logicalColumn
        ChartColumnDirection.ALTERNATING ->
            if (logicalRow % 2 == 0) logicalColumn else region.columns - 1 - logicalColumn
    }

private val ChartCorner.isBottom: Boolean
    get() = this == ChartCorner.BOTTOM_LEFT || this == ChartCorner.BOTTOM_RIGHT

private val ChartCorner.isRight: Boolean
    get() = this == ChartCorner.TOP_RIGHT || this == ChartCorner.BOTTOM_RIGHT
