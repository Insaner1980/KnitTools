package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType

object RepeatSectionLogic {
    fun updatePosition(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): ProjectCounter {
        val section = validRepeatSection(counter) ?: return counter
        if (mainRowCount < section.startRow) {
            return counter.copy(count = 0, currentRepeat = 1)
        }

        if (mainRowCount.toLong() >= section.finalRow) {
            return counter.copy(
                count = section.rowRange.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                currentRepeat = section.totalRepeats,
            )
        }

        val relativeRow = mainRowCount.toLong() - section.startRow
        val currentRepeat = (relativeRow / section.rowRange) + 1L
        val rowInRepeat = (relativeRow % section.rowRange) + 1L
        return counter.copy(count = rowInRepeat.toInt(), currentRepeat = currentRepeat.toInt())
    }

    fun isComplete(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): Boolean {
        val section = validRepeatSection(counter) ?: return false
        return mainRowCount.toLong() >= section.finalRow
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
        val section = validRepeatSection(counter) ?: return 0f
        val totalTrackedRows = section.rowRange * section.totalRepeats
        val completedRows = (mainRowCount.toLong() - section.startRow + 1L).coerceIn(0L, totalTrackedRows)
        return completedRows.toFloat() / totalTrackedRows.toFloat()
    }

    private class RepeatSectionBounds(
        val startRow: Int,
        endRow: Int,
        val totalRepeats: Int,
    ) {
        val rowRange: Long = endRow.toLong() - startRow + 1L
        val finalRow: Long = startRow.toLong() + rowRange * totalRepeats - 1L
    }

    /** Palauttaa toistojakson rajat vain kokonaan ja johdonmukaisesti määritetylle REPEAT_SECTION-laskurille. */
    private fun validRepeatSection(counter: ProjectCounter): RepeatSectionBounds? {
        val startRow = counter.repeatStartRow ?: return null
        val endRow = counter.repeatEndRow ?: return null
        val totalRepeats = counter.totalRepeats ?: return null
        if (
            counter.counterType != ProjectCounterType.REPEAT_SECTION ||
            startRow <= 0 ||
            endRow < startRow ||
            totalRepeats <= 0
        ) {
            return null
        }
        return RepeatSectionBounds(startRow, endRow, totalRepeats)
    }
}
