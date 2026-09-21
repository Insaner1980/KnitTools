package com.finnvek.knittools.domain.model

const val WEB_PATTERN_SHARED_TEXT_MAX_LENGTH = 16 * 1_024

private const val WEB_PATTERN_SHARED_SUBJECT_MAX_LENGTH = 4 * 1_024
private val SHARED_URL_PATTERN = Regex("""(?i)\b[a-z][a-z0-9+.-]*://[^\s<>\"']+""")
private val POTENTIAL_UNSUPPORTED_URL_PATTERN = Regex("""(?i)\b[a-z][a-z0-9+.-]*:[^\s]+""")

sealed interface WebPatternShareParseResult {
    data class WebLink(
        val url: WebPatternUrl,
        val titleSuggestion: String,
    ) : WebPatternShareParseResult

    data class Ravelry(
        val url: WebPatternUrl,
        val titleSuggestion: String,
    ) : WebPatternShareParseResult

    data object Empty : WebPatternShareParseResult

    data object Invalid : WebPatternShareParseResult

    data object Ambiguous : WebPatternShareParseResult

    data object TooLong : WebPatternShareParseResult
}

// Syöterajat ja tyypitetyt virheet palautetaan ennen muuta käsittelyä.
@Suppress("ReturnCount")
fun parseWebPatternSharedText(
    text: String?,
    subject: String?,
): WebPatternShareParseResult {
    if (text == null || text.isBlank()) return WebPatternShareParseResult.Empty
    if (text.length > WEB_PATTERN_SHARED_TEXT_MAX_LENGTH) return WebPatternShareParseResult.TooLong
    if (
        POTENTIAL_UNSUPPORTED_URL_PATTERN.findAll(text).any { match ->
            match.value.length > WEB_PATTERN_URL_MAX_LENGTH
        }
    ) {
        return WebPatternShareParseResult.TooLong
    }

    val matches = SHARED_URL_PATTERN.findAll(text).toList()
    if (matches.isEmpty()) {
        return if (POTENTIAL_UNSUPPORTED_URL_PATTERN.containsMatchIn(text)) {
            WebPatternShareParseResult.Invalid
        } else {
            WebPatternShareParseResult.Empty
        }
    }

    val distinctUrls = linkedMapOf<String, WebPatternUrl>()
    for (match in matches) {
        val hasSurroundingText =
            text.substring(0, match.range.first).any(Char::isLetterOrDigit) ||
                text.substring(match.range.last + 1).any(Char::isLetterOrDigit)
        val candidate = match.value.trimSharedUrlPunctuation(hasSurroundingText)
        val validation =
            validateWebPatternUrl(candidate) as? WebPatternUrlValidation.Valid
                ?: return WebPatternShareParseResult.Invalid
        distinctUrls.putIfAbsent(validation.value.canonicalUrl, validation.value)
    }
    if (distinctUrls.size > 1) return WebPatternShareParseResult.Ambiguous

    val url = distinctUrls.values.singleOrNull() ?: return WebPatternShareParseResult.Invalid
    val titleSuggestion = sanitizeSharedWebPatternTitle(subject)
    return if (url.isRavelryPattern) {
        WebPatternShareParseResult.Ravelry(url, titleSuggestion)
    } else {
        WebPatternShareParseResult.WebLink(url, titleSuggestion)
    }
}

private fun sanitizeSharedWebPatternTitle(subject: String?): String {
    if (subject == null || subject.length > WEB_PATTERN_SHARED_SUBJECT_MAX_LENGTH) return ""
    return (validateWebPatternTitle(subject) as? WebPatternTitleValidation.Valid)?.value.orEmpty()
}

private fun String.trimSharedUrlPunctuation(hasSurroundingText: Boolean): String {
    val candidate = dropUnmatchedClosingSuffix()
    if (!hasSurroundingText || '?' in candidate || '#' in candidate) return candidate
    return candidate.trimEnd { character -> character in SHARED_SENTENCE_PUNCTUATION }
}

private fun String.dropUnmatchedClosingSuffix(): String {
    val delimiterBalances = delimiterBalances()
    var trimmedEnd = length
    var index = lastIndex
    var trimming = true
    while (index >= 0 && trimming) {
        val delimiterIndex = SHARED_CLOSING_DELIMITERS.indexOf(this[index])
        when {
            this[index] in SHARED_SENTENCE_PUNCTUATION -> index -= 1
            delimiterIndex >= 0 && delimiterBalances[delimiterIndex] > 0 -> {
                delimiterBalances[delimiterIndex] -= 1
                trimmedEnd = index
                index -= 1
            }
            else -> trimming = false
        }
    }
    return if (trimmedEnd == length) this else substring(0, trimmedEnd)
}

private fun String.delimiterBalances(): IntArray =
    IntArray(SHARED_CLOSING_DELIMITERS.length).also { balances ->
        for (character in this) {
            val openingIndex = SHARED_OPENING_DELIMITERS.indexOf(character)
            if (openingIndex >= 0) balances[openingIndex] -= 1

            val closingIndex = SHARED_CLOSING_DELIMITERS.indexOf(character)
            if (closingIndex >= 0) balances[closingIndex] += 1
        }
    }

private const val SHARED_OPENING_DELIMITERS = "([{"
private const val SHARED_CLOSING_DELIMITERS = ")]}"
private const val SHARED_SENTENCE_PUNCTUATION = ".,!;:"
