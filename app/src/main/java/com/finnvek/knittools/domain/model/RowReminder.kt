package com.finnvek.knittools.domain.model

data class RowReminder(
    val id: Long = 0,
    val projectId: Long,
    val targetRow: Int,
    val repeatInterval: Int? = null,
    val message: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
