package com.finnvek.knittools.domain.model

data class ProjectCounterDraft(
    val name: String,
    val repeatAt: Int?,
    val stepSize: Int,
    val counterType: ProjectCounterType = ProjectCounterType.COUNT_UP,
    val startingStitches: Int? = null,
    val stitchChange: Int? = null,
    val shapeEveryN: Int? = null,
    val repeatStartRow: Int? = null,
    val repeatEndRow: Int? = null,
    val totalRepeats: Int? = null,
    val currentRepeat: Int? = null,
)
