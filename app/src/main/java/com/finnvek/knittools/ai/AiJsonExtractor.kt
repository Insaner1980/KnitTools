package com.finnvek.knittools.ai

internal object AiJsonExtractor {
    private val codeBlockPattern = Regex("""```(?:json)?\s*\n?(.*?)\n?```""", RegexOption.DOT_MATCHES_ALL)

    fun extractObject(response: String): String? {
        val trimmed = response.trim()
        if (trimmed.startsWith("{")) return trimmed

        codeBlockPattern.find(trimmed)?.let { return it.groupValues[1].trim() }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)

        return null
    }
}
