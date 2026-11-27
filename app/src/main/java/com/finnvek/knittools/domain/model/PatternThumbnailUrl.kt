package com.finnvek.knittools.domain.model

import java.net.URI
import java.net.URISyntaxException

private val ravelryImageHost = Regex("""images[0-9]+(?:-[a-z])?\.ravelrycache\.com""", RegexOption.IGNORE_CASE)
private const val MAX_THUMBNAIL_URL_LENGTH = 2_048

internal fun normalizeRemotePatternImageUrl(imageUrl: String?): String? {
    val value = imageUrl?.trim().orEmpty()
    if (value.isEmpty() || value.length > MAX_THUMBNAIL_URL_LENGTH) return null
    val uri =
        try {
            URI(value)
        } catch (_: URISyntaxException) {
            return null
        }
    val host = uri.host ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) || !ravelryImageHost.matches(host)) return null
    return value.takeIf { uri.rawAuthority == host || uri.rawAuthority == "$host:443" }
}
