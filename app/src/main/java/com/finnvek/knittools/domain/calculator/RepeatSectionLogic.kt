package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.data.local.ProjectCounterEntity

object RepeatSectionLogic {
    fun updatePosition(
        counter: ProjectCounterEntity,
        mainRowCount: Int,
    ): ProjectCounterEntity {
        val startRow = counter.repeatStartRow ?: return counter
        val endRow = counter.repeatEndRow ?: return counter
        val totalRepeats = counter.totalRepeats ?: return counter
        if (counter.counterType != "REPEAT_SECTION" || endRow < startRow || totalRepeats <= 0) {
            return counter
        }

        val rowRange = endRow - startRow + 1
        if (mainRowCount < startRow) {
            return counter.copy(count = 0, currentRepeat = 1)
        }

        val maxTrackedRow = startRow + (rowRange * totalRepeats) - 1
        if (mainRowCount >= maxTrackedRow) {
            return counter.copy(count = rowRange, currentRepeat = totalRepeats)
        }

        val relativeRow = mainRowCount - startRow
        val currentRepeat = (relativeRow / rowRange) + 1
        val rowInRepeat = (relativeRow % rowRange) + 1
        return counter.copy(count = rowInRepeat, currentRepeat = currentRepeat)
    }

    fun isComplete(
        counter: ProjectCounterEntity,
        mainRowCount: Int,
    ): Boolean {
        val startRow = counter.repeatStartRow ?: return false
        val endRow = counter.repeatEndRow ?: return false
        val totalRepeats = counter.totalRepeats ?: return false
        if (counter.counterType != "REPEAT_SECTION" || endRow < startRow || totalRepeats <= 0) {
            return false
        }

        val rowRange = endRow - startRow + 1
        val finalRow = startRow + (rowRange * totalRepeats) - 1
        return mainRowCount >= finalRow
    }

    fun currentRowInRepeat(
        counter: ProjectCounterEntity,
        mainRowCount: Int,
    ): Int = updatePosition(counter, mainRowCount).count

    fun progress(
        counter: ProjectCounterEntity,
        mainRowCount: Int,
    ): Float {
        val startRow = counter.repeatStartRow ?: return 0f
        val endRow = counter.repeatEndRow ?: return 0f
        val totalRepeats = counter.totalRepeats ?: return 0f
        if (counter.counterType != "REPEAT_SECTION" || endRow < startRow || totalRepeats <= 0) {
            return 0f
        }

        val rowRange = endRow - startRow + 1
        val totalTrackedRows = rowRange * totalRepeats
        val completedRows = (mainRowCount - startRow + 1).coerceIn(0, totalTrackedRows)
        return completedRows.toFloat() / totalTrackedRows.toFloat()
    }
}
