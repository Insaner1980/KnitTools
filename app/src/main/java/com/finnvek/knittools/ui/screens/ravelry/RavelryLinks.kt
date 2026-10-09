package com.finnvek.knittools.ui.screens.ravelry

import java.net.URI
import java.util.Locale

private const val RAVELRY_SCHEME = "https"
private val RAVELRY_HOSTS = setOf("ravelry.com", "www.ravelry.com")

/** Hyväksyy vain Ravelryn HTTPS-sivun; sovelluksen Ravelry-selain avaa muut osoitteet kaavahakuun. */
internal fun ravelryPageUrlOrNull(url: String): String? {
    val parsedUri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!parsedUri.scheme.equals(RAVELRY_SCHEME, ignoreCase = true)) return null

    val host = parsedUri.host?.lowercase(Locale.US) ?: return null
    if (host !in RAVELRY_HOSTS) return null

    return parsedUri.toString()
}
