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

private val PAYMENT_HOSTS = listOf("paypal.com", "stripe.com", "braintreegateway.com", "braintree-api.com")

/**
 * Ravelryn kassa ja maksupalvelut avataan käyttäjän omassa selaimessa, jotta ohjetta ei osteta
 * sovelluksen sisällä Google Playn maksujärjestelmän ohi. Ostettu ohje ladataan sen jälkeen täällä.
 */
internal fun isRavelryPurchaseUrl(
    host: String?,
    path: String?,
): Boolean {
    val normalizedHost = host?.lowercase(Locale.US) ?: return false
    if (PAYMENT_HOSTS.any { normalizedHost == it || normalizedHost.endsWith(".$it") }) return true
    val isRavelry = normalizedHost == "ravelry.com" || normalizedHost.endsWith(".ravelry.com")
    return isRavelry &&
        (
            normalizedHost.startsWith("checkout.") ||
                path.orEmpty().split('/').any { it.equals("checkout", ignoreCase = true) }
        )
}
