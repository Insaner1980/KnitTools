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

fun parseWebPatternSharedText(
    text: String?,
    subject: String?,
): WebPatternShareParseResult {
    if (text == null || text.isBlank()) return WebPatternShareParseResult.Empty
    if (text.length > WEB_PATTERN_SHARED_TEXT_MAX_LENGTH) return WebPatternShareParseResult.TooLong

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
    var candidate = this
    while (true) {
        val trimmed = candidate.dropUnmatchedClosingSuffix()
        if (trimmed == candidate) break
        candidate = trimmed
    }
    if (!hasSurroundingText || '?' in candidate || '#' in candidate) return candidate
    return candidate.trimEnd { character -> character in SHARED_SENTENCE_PUNCTUATION }
}

private fun String.dropUnmatchedClosingSuffix(): String {
    val punctuationStart = indexOfTrailingSentencePunctuation()
    if (punctuationStart == 0) return this
    val closingIndex = punctuationStart - 1
    return when (this[closingIndex]) {
        ')' -> dropUnmatchedClosingPunctuation(closingIndex, '(', ')')
        ']' -> dropUnmatchedClosingPunctuation(closingIndex, '[', ']')
        '}' -> dropUnmatchedClosingPunctuation(closingIndex, '{', '}')
        else -> this
    }
}

private fun String.indexOfTrailingSentencePunctuation(): Int {
    var index = length
    while (index > 0 && this[index - 1] in SHARED_SENTENCE_PUNCTUATION) index -= 1
    return index
}

private fun String.dropUnmatchedClosingPunctuation(
    closingIndex: Int,
    opening: Char,
    closing: Char,
): String =
    if (substring(0, closingIndex + 1).count { it == closing } > count { it == opening }) {
        substring(0, closingIndex)
    } else {
        this
    }

private const val SHARED_SENTENCE_PUNCTUATION = ".,!;:"
