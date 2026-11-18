package com.finnvek.knittools.repository

sealed interface SavedPatternDeleteResult {
    data object Deleted : SavedPatternDeleteResult

    data object PatternMissing : SavedPatternDeleteResult

    data object NotWebPattern : SavedPatternDeleteResult

    data object PersistenceFailure : SavedPatternDeleteResult
}
