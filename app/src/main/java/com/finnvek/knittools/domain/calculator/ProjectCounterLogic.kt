package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType

object ProjectCounterLogic {
    const val MAX_NAME_LENGTH = 50

    fun canLinkToMainCounter(counterType: ProjectCounterType): Boolean =
        counterType != ProjectCounterType.REPEAT_SECTION

    fun enforceMainCounterLinkRules(counter: ProjectCounter): ProjectCounter =
        if (canLinkToMainCounter(counter.counterType)) {
            counter
        } else {
            counter.copy(linkedToMainCounter = false)
        }

    fun validatedForPersistence(counter: ProjectCounter): ProjectCounter? {
        val name =
            counter.name
                .trim()
                .take(MAX_NAME_LENGTH)
                .let { value -> if (value.lastOrNull()?.isHighSurrogate() == true) value.dropLast(1) else value }
        if (name.isBlank() || counter.count < 0 || counter.stepSize <= 0) return null

        val normalized =
            when (counter.counterType) {
                ProjectCounterType.COUNT_UP -> normalizeCountUp(counter, name)
                ProjectCounterType.REPEATING -> normalizeRepeating(counter, name)
                ProjectCounterType.SHAPING -> normalizeShaping(counter, name)
                ProjectCounterType.REPEAT_SECTION -> normalizeRepeatSection(counter, name)
            } ?: return null
        return enforceMainCounterLinkRules(normalized)
    }

    private fun normalizeCountUp(
        counter: ProjectCounter,
        name: String,
    ): ProjectCounter =
        counter.copy(
            name = name,
            repeatAt = null,
            startingStitches = null,
            stitchChange = null,
            shapeEveryN = null,
            repeatStartRow = null,
            repeatEndRow = null,
            totalRepeats = null,
            currentRepeat = null,
        )

    private fun normalizeRepeating(
        counter: ProjectCounter,
        name: String,
    ): ProjectCounter? {
        if (counter.repeatAt?.let { it > 0 } != true) return null
        return counter.copy(
            name = name,
            startingStitches = null,
            stitchChange = null,
            shapeEveryN = null,
            repeatStartRow = null,
            repeatEndRow = null,
            totalRepeats = null,
            currentRepeat = null,
        )
    }

    private fun normalizeShaping(
        counter: ProjectCounter,
        name: String,
    ): ProjectCounter? {
        if (
            counter.startingStitches?.let { it >= 0 } != true ||
            counter.stitchChange == null ||
            counter.shapeEveryN?.let { it > 0 } != true
        ) {
            return null
        }
        return counter.copy(
            name = name,
            repeatAt = null,
            repeatStartRow = null,
            repeatEndRow = null,
            totalRepeats = null,
            currentRepeat = null,
        )
    }

    private fun normalizeRepeatSection(
        counter: ProjectCounter,
        name: String,
    ): ProjectCounter? {
        val startRow = counter.repeatStartRow ?: return null
        val endRow = counter.repeatEndRow ?: return null
        val totalRepeats = counter.totalRepeats ?: return null
        if (startRow <= 0 || endRow < startRow || totalRepeats <= 0) return null
        return counter.copy(
            name = name,
            repeatAt = null,
            startingStitches = null,
            stitchChange = null,
            shapeEveryN = null,
            linkedToMainCounter = false,
        )
    }

    fun increment(counter: ProjectCounter): ProjectCounter {
        val count = counter.count.coerceAtLeast(0)
        val stepSize = counter.stepSize.coerceAtLeast(1)
        val newCount = count.toLong() + stepSize
        // SHAPING-tyyppi ei resettaa — normaali laskenta
        if (counter.counterType == ProjectCounterType.SHAPING) {
            return counter.copy(count = newCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), stepSize = stepSize)
        }
        if (counter.counterType == ProjectCounterType.REPEAT_SECTION) {
            return counter
        }
        val repeatAt = counter.repeatAt
        return if (
            counter.counterType == ProjectCounterType.REPEATING &&
            repeatAt != null &&
            repeatAt > 0 &&
            newCount >= repeatAt.toLong()
        ) {
            counter.copy(count = (newCount % repeatAt.toLong()).toInt(), stepSize = stepSize)
        } else {
            counter.copy(count = newCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), stepSize = stepSize)
        }
    }

    fun decrement(counter: ProjectCounter): ProjectCounter {
        if (counter.counterType == ProjectCounterType.REPEAT_SECTION) {
            return counter
        }
        val count = counter.count.coerceAtLeast(0)
        val stepSize = counter.stepSize.coerceAtLeast(1)
        val newCount = (count.toLong() - stepSize).coerceAtLeast(0L).toInt()
        return counter.copy(count = newCount, stepSize = stepSize)
    }
}
