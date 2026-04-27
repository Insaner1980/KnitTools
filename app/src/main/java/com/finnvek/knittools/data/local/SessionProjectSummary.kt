package com.finnvek.knittools.data.local

data class SessionProjectSummary(
    val projectId: Long,
    val projectName: String,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
)
