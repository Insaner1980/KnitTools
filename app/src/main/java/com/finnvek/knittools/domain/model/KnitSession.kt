package com.finnvek.knittools.domain.model

data class KnitSession(
    val id: Long = 0,
    val projectId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val startRow: Int,
    val endRow: Int,
    val durationMinutes: Int,
)
