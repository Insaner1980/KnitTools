package com.finnvek.knittools.domain.model

data class ProjectCounter(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val count: Int = 0,
    val stepSize: Int = 1,
    val repeatAt: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val counterType: String = "COUNT_UP",
    val startingStitches: Int? = null,
    val stitchChange: Int? = null,
    val shapeEveryN: Int? = null,
    val repeatStartRow: Int? = null,
    val repeatEndRow: Int? = null,
    val totalRepeats: Int? = null,
    val currentRepeat: Int? = null,
)
