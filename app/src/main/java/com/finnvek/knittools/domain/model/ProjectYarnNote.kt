package com.finnvek.knittools.domain.model

data class ProjectYarnNote(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val description: String = "",
    val quantity: Int = 1,
    val notes: String = "",
    val savedYarnCardId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
