package com.finnvek.knittools.ravelry

import java.net.URI
import java.util.Locale

object RavelryShareImportUrls {
    private const val PATTERN_LIBRARY_PATH = "/patterns/library/"
    private const val TRAILING_SHARED_URL_PUNCTUATION = ".,!?;:)]}"
    private val urlPattern = Regex("""https://[^\s<>"']+""")

    fun extractPatternUrl(text: String?): String? =
        text
            ?.let(urlPattern::findAll)
            ?.map { match -> match.value.trimSharedUrlPunctuation() }
            ?.firstOrNull(::isRavelryPatternUrl)

    private fun isRavelryPatternUrl(candidate: String): Boolean {
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false

        val host = uri.host?.lowercase(Locale.US) ?: return false
        if (host != "ravelry.com" && host != "www.ravelry.com") return false

        val path = uri.path ?: return false
        if (!path.startsWith(PATTERN_LIBRARY_PATH)) return false
        return path.removePrefix(PATTERN_LIBRARY_PATH).isNotBlank()
    }

    private fun String.trimSharedUrlPunctuation(): String =
        trimEnd { character -> character in TRAILING_SHARED_URL_PUNCTUATION }
}
