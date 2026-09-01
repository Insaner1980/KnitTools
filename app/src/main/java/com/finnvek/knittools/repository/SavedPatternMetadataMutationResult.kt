package com.finnvek.knittools.repository

sealed interface SavedPatternMetadataMutationResult {
    data class Attached(
        val savedPatternId: Long,
    ) : SavedPatternMetadataMutationResult

    data class AlreadyAttached(
        val savedPatternId: Long,
    ) : SavedPatternMetadataMutationResult

    data class ReplacementRequired(
        val existingSavedPatternId: Long,
    ) : SavedPatternMetadataMutationResult

    data object Unlinked : SavedPatternMetadataMutationResult

    data object AlreadyUnlinked : SavedPatternMetadataMutationResult

    data object ProjectMissing : SavedPatternMetadataMutationResult

    data object PatternMissing : SavedPatternMetadataMutationResult

    data object NotWebPattern : SavedPatternMetadataMutationResult

    data object StaleAction : SavedPatternMetadataMutationResult

    data object PersistenceFailure : SavedPatternMetadataMutationResult
}
