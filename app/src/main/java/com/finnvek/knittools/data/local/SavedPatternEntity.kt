package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_patterns")
data class SavedPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ravelryId: Int,
    val name: String,
    val designerName: String,
    val thumbnailUrl: String? = null,
    val difficulty: Float? = null,
    val gaugeStitches: Float? = null,
    val gaugeRows: Float? = null,
    val needleSize: String? = null,
    val yarnWeight: String? = null,
    val yardage: Int? = null,
    val isFree: Boolean = true,
    val patternUrl: String = "",
    val savedAt: Long = System.currentTimeMillis(),
)
