package com.finnvek.knittools.domain.model

data class SavedPattern(
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
