package com.finnvek.knittools.domain.model

data class ProjectCompletion(
    val id: Long,
    val projectId: Long,
    val completedAt: Long,
    val zoneId: String?,
)
