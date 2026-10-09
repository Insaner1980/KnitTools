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
    return isRavelryHost(normalizedHost) &&
        (
            normalizedHost.startsWith("checkout.") ||
                path.orEmpty().split('/').any { it.equals("checkout", ignoreCase = true) }
        )
}

/** Jaettu tai tallennettu kassalinkki: aloitusosoite ei kulje navigointisuodattimen kautta. */
internal fun isRavelryPurchasePage(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    return isRavelryPurchaseUrl(uri.host, uri.path)
}

private const val RAVELRY_DOMAIN = "ravelry.com"

internal fun isRavelryHost(host: String?): Boolean {
    val normalizedHost = host?.lowercase(Locale.US) ?: return false
    return normalizedHost == RAVELRY_DOMAIN || normalizedHost.endsWith(".$RAVELRY_DOMAIN")
}

internal enum class RavelryNavigation {
    InApp,
    Purchase,
    External,
    Blocked,
}

/**
 * Sovelluksen Ravelry-selain näyttää vain Ravelryn sivuja. Muiden sivustojen linkit ja kassa avataan
 * käyttäjän selaimessa. Palvelimen uudelleenohjaukset (esim. Ravelryn latauspalvelin) ja upotetut
 * kehykset jatkuvat täällä, jottei Download PDF katkea.
 */
internal fun ravelryNavigation(
    scheme: String?,
    host: String?,
    path: String?,
    isMainFrame: Boolean,
    isRedirect: Boolean,
): RavelryNavigation =
    when {
        !scheme.equals(RAVELRY_SCHEME, ignoreCase = true) -> RavelryNavigation.Blocked
        isRavelryPurchaseUrl(host, path) -> RavelryNavigation.Purchase
        isMainFrame && !isRedirect && !isRavelryHost(host) -> RavelryNavigation.External
        else -> RavelryNavigation.InApp
    }
