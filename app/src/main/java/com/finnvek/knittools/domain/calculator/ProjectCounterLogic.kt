package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ProjectCounter

object ProjectCounterLogic {
    fun increment(counter: ProjectCounter): ProjectCounter {
        val newCount = counter.count + counter.stepSize
        // SHAPING-tyyppi ei resettaa — normaali laskenta
        if (counter.counterType == "SHAPING") {
            return counter.copy(count = newCount)
        }
        if (counter.counterType == "REPEAT_SECTION") {
            return counter
        }
        val repeatAt = counter.repeatAt
        return if (repeatAt != null && repeatAt > 0 && newCount >= repeatAt) {
            counter.copy(count = 0)
        } else {
            counter.copy(count = newCount)
        }
    }

    fun decrement(counter: ProjectCounter): ProjectCounter {
        if (counter.counterType == "REPEAT_SECTION") {
            return counter
        }
        val newCount = (counter.count - counter.stepSize).coerceAtLeast(0)
        return counter.copy(count = newCount)
    }
}
