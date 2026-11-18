package com.finnvek.knittools.domain.model

const val PATTERN_BOOKMARK_NAME_MAX_LENGTH = 50

data class PatternBookmark(
    val id: Long = 0,
    val projectId: Long,
    val documentKey: String,
    val name: String,
    val pageIndex: Int,
    val yFraction: Float,
    val createdAt: Long,
)

sealed interface PatternBookmarkNameValidation {
    data class Valid(
        val name: String,
    ) : PatternBookmarkNameValidation

    data object Empty : PatternBookmarkNameValidation

    data object TooLong : PatternBookmarkNameValidation
}

fun validatePatternBookmarkName(name: String): PatternBookmarkNameValidation {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> PatternBookmarkNameValidation.Empty
        trimmed.length > PATTERN_BOOKMARK_NAME_MAX_LENGTH -> PatternBookmarkNameValidation.TooLong
        else -> PatternBookmarkNameValidation.Valid(trimmed)
    }
}
