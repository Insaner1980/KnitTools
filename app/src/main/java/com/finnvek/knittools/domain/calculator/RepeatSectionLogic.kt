package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType

object RepeatSectionLogic {
    fun updatePosition(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): ProjectCounter {
        val startRow = counter.repeatStartRow ?: return counter
        val endRow = counter.repeatEndRow ?: return counter
        val totalRepeats = counter.totalRepeats ?: return counter
        if (
            counter.counterType != ProjectCounterType.REPEAT_SECTION ||
            startRow <= 0 ||
            endRow < startRow ||
            totalRepeats <= 0
        ) {
            return counter
        }

        val rowRange = endRow.toLong() - startRow + 1L
        if (mainRowCount < startRow) {
            return counter.copy(count = 0, currentRepeat = 1)
        }

        val maxTrackedRow = startRow.toLong() + rowRange * totalRepeats - 1L
        if (mainRowCount.toLong() >= maxTrackedRow) {
            return counter.copy(
                count = rowRange.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                currentRepeat = totalRepeats,
            )
        }

        val relativeRow = mainRowCount.toLong() - startRow
        val currentRepeat = (relativeRow / rowRange) + 1L
        val rowInRepeat = (relativeRow % rowRange) + 1L
        return counter.copy(count = rowInRepeat.toInt(), currentRepeat = currentRepeat.toInt())
    }

    fun isComplete(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): Boolean {
        val startRow = counter.repeatStartRow ?: return false
        val endRow = counter.repeatEndRow ?: return false
        val totalRepeats = counter.totalRepeats ?: return false
        if (
            counter.counterType != ProjectCounterType.REPEAT_SECTION ||
            startRow <= 0 ||
            endRow < startRow ||
            totalRepeats <= 0
        ) {
            return false
        }

        val rowRange = endRow.toLong() - startRow + 1L
        val finalRow = startRow.toLong() + rowRange * totalRepeats - 1L
        return mainRowCount.toLong() >= finalRow
    }

    fun currentRowInRepeat(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): Int {
        val updated = updatePosition(counter, mainRowCount)
        val startRow = counter.repeatStartRow ?: return updated.count
        val endRow = counter.repeatEndRow ?: return updated.count
        if (counter.counterType != ProjectCounterType.REPEAT_SECTION || startRow <= 0 || endRow < startRow) {
            return updated.count
        }

        val rowRange = (endRow.toLong() - startRow + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return updated.count.coerceIn(1, rowRange)
    }

    fun progress(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): Float {
        val startRow = counter.repeatStartRow ?: return 0f
        val endRow = counter.repeatEndRow ?: return 0f
        val totalRepeats = counter.totalRepeats ?: return 0f
        if (
            counter.counterType != ProjectCounterType.REPEAT_SECTION ||
            startRow <= 0 ||
            endRow < startRow ||
            totalRepeats <= 0
        ) {
            return 0f
        }

        val rowRange = endRow.toLong() - startRow + 1L
        val totalTrackedRows = rowRange * totalRepeats
        val completedRows = (mainRowCount.toLong() - startRow + 1L).coerceIn(0L, totalTrackedRows)
        return completedRows.toFloat() / totalTrackedRows.toFloat()
    }
}
