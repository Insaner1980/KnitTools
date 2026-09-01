package com.finnvek.knittools.ravelry

import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.domain.model.validateWebPatternUrl

object RavelryShareImportUrls {
    private const val MAX_SHARED_PATTERN_URL_LENGTH = 2_048
    private const val TRAILING_SHARED_URL_PUNCTUATION = ".,!?;:)]}"
    private val urlPattern = Regex("""https://[^\s<>"']+""")

    fun extractPatternUrl(text: String?): String? =
        text
            ?.let(urlPattern::findAll)
            ?.map { match -> match.value.trimSharedUrlPunctuation() }
            ?.firstOrNull { candidate ->
                candidate.length <= MAX_SHARED_PATTERN_URL_LENGTH && isRavelryPatternUrl(candidate)
            }

    private fun isRavelryPatternUrl(candidate: String): Boolean {
        val validation = validateWebPatternUrl(candidate) as? WebPatternUrlValidation.Valid ?: return false
        return validation.value.isRavelryPattern
    }

    private fun String.trimSharedUrlPunctuation(): String =
        trimEnd { character -> character in TRAILING_SHARED_URL_PUNCTUATION }
}
