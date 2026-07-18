package com.finnvek.knittools.domain.model

data class PatternAnnotation(
    val id: Long = 0,
    val layerId: Long,
    val page: Int,
    val kind: PatternAnnotationKind,
    val payload: PatternAnnotationPayload,
    val zIndex: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    init {
        require(page >= 0) { "Pattern annotation page must be non-negative" }
    }
}

data class PatternAnnotationLayer(
    val id: Long = 0,
    val owner: PatternAnnotationOwner,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
