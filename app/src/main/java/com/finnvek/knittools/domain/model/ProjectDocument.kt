package com.finnvek.knittools.domain.model

const val PROJECT_DOCUMENT_LABEL_MAX_LENGTH = 50

sealed interface ProjectDocumentLabelValidation {
    data class Valid(
        val label: String,
    ) : ProjectDocumentLabelValidation

    data object Empty : ProjectDocumentLabelValidation

    data object TooLong : ProjectDocumentLabelValidation
}

fun validateProjectDocumentLabel(value: String): ProjectDocumentLabelValidation {
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> ProjectDocumentLabelValidation.Empty
        trimmed.length > PROJECT_DOCUMENT_LABEL_MAX_LENGTH -> ProjectDocumentLabelValidation.TooLong
        else -> ProjectDocumentLabelValidation.Valid(trimmed)
    }
}

data class ProjectDocument(
    val id: Long = 0,
    val projectId: Long,
    val savedPatternId: Long?,
    val documentKey: String,
    val label: String,
    val localPdfUri: String,
    val sortOrder: Int,
    val isPrimary: Boolean,
    val currentPage: Int,
    val rowMapping: String?,
    val readingLineEnabled: Boolean,
    val readingLineYFraction: Float,
    val readingLineFollowCurrentRow: Boolean,
    val verticalReadingGuideEnabled: Boolean,
    val verticalReadingGuideXFraction: Float,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun sanitized(): ProjectDocument =
        copy(
            documentKey = documentKey.trim(),
            label = sanitizedProjectDocumentLabel(label),
            localPdfUri = localPdfUri.trim(),
            sortOrder = sortOrder.coerceAtLeast(0),
            currentPage = currentPage.coerceAtLeast(0),
            readingLineYFraction = sanitizePersistedFraction(readingLineYFraction),
            verticalReadingGuideXFraction = sanitizePersistedFraction(verticalReadingGuideXFraction),
        )

    fun canOpen(isFileAvailable: Boolean): Boolean =
        documentKey.isNotBlank() && localPdfUri.isNotBlank() && isFileAvailable
}

fun List<ProjectDocument>.inDocumentOrder(): List<ProjectDocument> =
    sortedWith(compareBy(ProjectDocument::sortOrder, ProjectDocument::id))

fun List<ProjectDocument>.primaryFallback(): ProjectDocument? = inDocumentOrder().firstOrNull()

private fun sanitizedProjectDocumentLabel(value: String): String {
    val trimmed = value.trim()
    return trimmed.take(PROJECT_DOCUMENT_LABEL_MAX_LENGTH).ifEmpty { "Pattern" }
}

private fun sanitizePersistedFraction(value: Float): Float =
    if (value.isFinite()) {
        sanitizeReadingGuideFraction(value)
    } else {
        DEFAULT_READING_GUIDE_FRACTION
    }
