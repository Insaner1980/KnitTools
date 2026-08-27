package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"])],
)
data class ActiveSessionEntity(
    @PrimaryKey
    val singletonId: Int = SINGLETON_ID,
    val sessionToken: String,
    val projectId: Long,
    val startedAtWallMillis: Long,
    val startZoneId: String,
    val startRow: Int,
    val lastObservedRow: Int,
    val trustedLastObservedRow: Int,
    val trustedRowsWorked: Int,
    val pendingRowsWorked: Int,
    val reviewedRowsWorked: Int,
    val reviewedLastObservedRow: Int,
    val unreviewedRowsWorked: Int,
    val checkpointedDurationSeconds: Long,
    val reviewedDurationBaselineSeconds: Long,
    val segmentStartedAtWallMillis: Long,
    val segmentStartedElapsedRealtimeMillis: Long,
    val bootCount: Long?,
    val recoveryReason: String?,
    val recoveryIntervalToken: String?,
    val recoverySuggestedDurationSeconds: Long?,
    val recoveryPromptShown: Boolean,
    val updatedAtWallMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
