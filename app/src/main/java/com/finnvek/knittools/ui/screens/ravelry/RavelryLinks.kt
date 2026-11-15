package com.finnvek.knittools.ui.screens.ravelry

import java.net.URI
import java.util.Locale

private const val RAVELRY_SCHEME = "https"
internal const val RAVELRY_PATTERN_SEARCH_URL = "https://www.ravelry.com/patterns/search"

/** Restoration and app-driven Back are page loads, never download redirects. */
internal fun restorableRavelryPageOrNull(url: String?): String? =
    url?.takeIf { it.length <= MAX_RESTORABLE_URL_LENGTH }?.let(::eligibleRavelryPageOrNull)

internal fun eligibleRavelryPageOrNull(url: String?): String? =
    url
        ?.let(::parseRavelryHttpsUri)
        ?.takeIf { it.rawUserInfo == null && !isRavelryPurchaseUrl(it.host, it.path) }
        ?.toString()

private const val MAX_RESTORABLE_URL_LENGTH = 2048

/** The initial page uses the two exact Ravelry hosts. */
internal fun ravelryPageUrlOrNull(url: String): String? =
    parseRavelryHttpsUri(url)
        ?.takeIf {
            it.host.equals(RAVELRY_DOMAIN, ignoreCase = true) ||
                it.host.equals("www.$RAVELRY_DOMAIN", ignoreCase = true)
        }?.toString()

private fun parseRavelryHttpsUri(url: String): URI? {
    val parsedUri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!parsedUri.scheme.equals(RAVELRY_SCHEME, ignoreCase = true)) return null
    return parsedUri.takeIf { isRavelryHost(it.host) }
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
    val normalizedHost = host?.removeSuffix(".")?.lowercase(Locale.US) ?: return false
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
 * Checkout and ordinary main-frame links to other sites open in the user's browser.
 * Other HTTPS redirects and subframes can stay in the WebView for downloads.
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
