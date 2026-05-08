package com.finnvek.knittools.domain.model

data class PatternAnnotation(
    val id: Long = 0,
    val projectId: Long,
    val page: Int,
    val pathData: String,
    val color: String,
    val strokeWidth: Float,
    val createdAt: Long = System.currentTimeMillis(),
)
