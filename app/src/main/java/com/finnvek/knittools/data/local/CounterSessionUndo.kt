package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Row accounting before a counter change; elapsed time is never rewound by Undo. */
@Serializable
internal data class CounterSessionUndo(
    val sessionToken: String,
    val recoveryIntervalToken: String?,
    val reviewedDurationBaselineSeconds: Long,
    val reviewedRowsWorked: Int,
    val reviewedLastObservedRow: Int,
    val trustedLastObservedRow: Int,
    val trustedRowsWorked: Int,
    val unreviewedRowsWorked: Int,
    val pendingRowsWorked: Int,
) {
    fun encode(): String = Json.encodeToString(this)

    fun restoreRows(active: ActiveSessionEntity): ActiveSessionEntity? {
        // A new session or explicit recovery review starts a different accounting interval.
        if (sessionToken != active.sessionToken ||
            reviewedDurationBaselineSeconds != active.reviewedDurationBaselineSeconds ||
            reviewedRowsWorked != active.reviewedRowsWorked ||
            reviewedLastObservedRow != active.reviewedLastObservedRow
        ) {
            return null
        }
        if (recoveryIntervalToken == active.recoveryIntervalToken) {
            return active.copy(
                trustedLastObservedRow = trustedLastObservedRow,
                trustedRowsWorked = trustedRowsWorked,
                unreviewedRowsWorked = unreviewedRowsWorked,
                pendingRowsWorked = pendingRowsWorked,
            )
        }
        if (recoveryIntervalToken != null || active.recoveryReason == null) return null
        // Synchronization may have moved the unreviewed interval into recovery since the change.
        return if (active.recoveryReason == ActiveSessionRecoveryReason.LONG_RUNNING.name) {
            active.copy(
                pendingRowsWorked =
                    (pendingRowsWorked.toLong() + unreviewedRowsWorked)
                        .coerceAtMost(
                            Int.MAX_VALUE.toLong(),
                        ).toInt(),
            )
        } else {
            active.copy(
                trustedLastObservedRow = trustedLastObservedRow,
                trustedRowsWorked = trustedRowsWorked,
                unreviewedRowsWorked = unreviewedRowsWorked,
                pendingRowsWorked = pendingRowsWorked,
            )
        }
    }

    companion object {
        fun capture(active: ActiveSessionEntity): CounterSessionUndo =
            CounterSessionUndo(
                sessionToken = active.sessionToken,
                recoveryIntervalToken = active.recoveryIntervalToken,
                reviewedDurationBaselineSeconds = active.reviewedDurationBaselineSeconds,
                reviewedRowsWorked = active.reviewedRowsWorked,
                reviewedLastObservedRow = active.reviewedLastObservedRow,
                trustedLastObservedRow = active.trustedLastObservedRow,
                trustedRowsWorked = active.trustedRowsWorked,
                unreviewedRowsWorked = active.unreviewedRowsWorked,
                pendingRowsWorked = active.pendingRowsWorked,
            )

        fun decode(value: String): CounterSessionUndo =
            Json.decodeFromString<CounterSessionUndo>(value).also {
                require(it.sessionToken.isNotBlank())
                require(it.recoveryIntervalToken == null || it.recoveryIntervalToken.isNotBlank())
                require(it.reviewedDurationBaselineSeconds >= 0L)
                require(it.reviewedRowsWorked >= 0 && it.reviewedLastObservedRow >= 0 && it.trustedLastObservedRow >= 0)
                require(it.trustedRowsWorked >= 0 && it.unreviewedRowsWorked >= 0 && it.pendingRowsWorked >= 0)
            }
    }
}
