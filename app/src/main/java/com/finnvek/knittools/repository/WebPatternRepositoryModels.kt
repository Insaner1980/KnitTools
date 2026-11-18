package com.finnvek.knittools.repository

data class WebPatternInput(
    val title: String,
    val designer: String = "",
    val url: String,
)

sealed interface WebPatternMutationResult {
    data class Created(
        val patternId: Long,
    ) : WebPatternMutationResult

    data class Updated(
        val patternId: Long,
    ) : WebPatternMutationResult

    data class Duplicate(
        val existingPatternId: Long,
    ) : WebPatternMutationResult

    data object RavelryOwnedUrl : WebPatternMutationResult

    data object InvalidTitle : WebPatternMutationResult

    data object InvalidDesigner : WebPatternMutationResult

    data object InvalidUrl : WebPatternMutationResult

    data object PatternMissing : WebPatternMutationResult

    data object NotEditableAsWebPattern : WebPatternMutationResult

    data object StaleAction : WebPatternMutationResult

    data object PersistenceFailure : WebPatternMutationResult
}
