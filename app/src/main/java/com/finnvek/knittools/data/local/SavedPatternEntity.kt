package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_patterns",
    indices = [
        Index("ravelryPatternId"),
        Index("canonicalUrl"),
        Index("originalUrl"),
        Index("localPdfUri"),
    ],
)
data class SavedPatternEntity(
    // CPD-OFF: Room-entity peilaa domain-mallia ilman, etta DAO-raja vuotaa UI:hin.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,
    val ravelryPatternId: Int? = null,
    val name: String,
    val designerName: String,
    val thumbnailUrl: String? = null,
    val difficulty: Float? = null,
    val gaugeStitches: Float? = null,
    val gaugeRows: Float? = null,
    val needleSize: String? = null,
    val yarnWeight: String? = null,
    val yardage: Int? = null,
    val availability: String = "unknown",
    val originalUrl: String = "",
    val canonicalUrl: String = "",
    val localPdfUri: String? = null,
    val isAvailableOffline: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = savedAt,
    val lastSyncedAt: Long? = null,
    // CPD-ON
) {
    val ravelryId: Int
        get() = ravelryPatternId ?: 0

    val patternUrl: String
        get() = localPdfUri ?: canonicalUrl.ifBlank { originalUrl }
}
