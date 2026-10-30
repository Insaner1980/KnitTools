package com.finnvek.knittools.ui.layout

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/** Keep two compact columns; add columns before a thumbnail exceeds the shared media size. */
internal object PhotoGridCells : GridCells {
    private val maximumCellWidth = 220.dp

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val columns =
            ceil((availableSize + spacing).toFloat() / (maximumCellWidth.roundToPx() + spacing))
                .toInt()
                .coerceAtLeast(2)
        val cellSpace = (availableSize - spacing * (columns - 1)).coerceAtLeast(0)
        return List(columns) { index -> cellSpace / columns + if (index < cellSpace % columns) 1 else 0 }
    }
}
