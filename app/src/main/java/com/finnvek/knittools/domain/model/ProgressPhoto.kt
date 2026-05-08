package com.finnvek.knittools.domain.model

data class ProgressPhoto(
    val id: Long = 0,
    val projectId: Long,
    val photoUri: String,
    val rowNumber: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
