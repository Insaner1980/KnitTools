package com.finnvek.knittools.ui.screens.counter

import java.util.concurrent.TimeUnit

internal object SessionProgress {
    fun adjustRowsWorked(
        currentRowsWorked: Int,
        action: String,
        previousValue: Int,
        newValue: Int,
    ): Int {
        val delta =
            when (action) {
                "increment" -> (newValue - previousValue).coerceAtLeast(0)
                "decrement", "reset" -> -(previousValue - newValue).coerceAtLeast(0)
                "undo" -> if (newValue < previousValue) -(previousValue - newValue) else 0
                else -> 0
            }
        return (currentRowsWorked + delta).coerceAtLeast(0)
    }

    fun resolveDurationSeconds(
        recordedSeconds: Long,
        startedAt: Long,
        nowMillis: Long,
    ): Long {
        val elapsedMillis = (nowMillis - startedAt).coerceAtLeast(0L)
        val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        return maxOf(recordedSeconds, elapsedSeconds)
    }
}
