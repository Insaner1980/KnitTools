package com.finnvek.knittools.domain.tips

sealed interface TipSignal {
    data class Increment(
        val projectId: Long,
        val monotonicMillis: Long,
    ) : TipSignal

    data class TargetReached(
        val projectId: Long,
    ) : TipSignal

    data class SessionSaved(
        val wasFirstCompletedSession: Boolean,
    ) : TipSignal
}
