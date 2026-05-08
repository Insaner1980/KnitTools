package com.finnvek.knittools.domain.model

data class SessionInsightsSummary(
    val totalMinutes: Int,
    val totalRows: Int,
    val sessionCount: Int,
)

data class SessionProjectTimeSummary(
    val projectId: Long,
    val projectName: String,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
)
