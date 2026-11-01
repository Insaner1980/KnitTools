package com.finnvek.knittools.ui.components

import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ProPriceFormattingTest {
    private val quote =
        SelectedOneTimeOffer(
            "Play price",
            "test",
            9_990_000,
            "USD",
            launchDetails = LaunchOfferDetails(1, 2, 14_990_000),
        )

    @Test
    fun `full price uses exact micros and Play currency even when language differs`() {
        assertEquals("$14.99", formatOfferFullPrice(quote, Locale.US))
        assertTrue(formatOfferFullPrice(quote, Locale.FRANCE).orEmpty().contains("14,99"))
        assertTrue(formatOfferFullPrice(quote, Locale.FRANCE).orEmpty().contains("US"))
        assertTrue(formatOfferFullPrice(quote, Locale.forLanguageTag("fi")).orEmpty().contains("$"))
        val brazil =
            quote.copy(
                priceCurrencyCode = "BRL",
                priceAmountMicros = 24_900_000,
                launchDetails = LaunchOfferDetails(1, 2, 32_900_000),
            )
        assertTrue(formatOfferFullPrice(brazil, Locale.forLanguageTag("pt")).orEmpty().contains("32,90"))
        assertTrue(
            formatOfferFullPrice(
                quote.copy(priceCurrencyCode = "EUR"),
                Locale.forLanguageTag("pt-BR"),
            ).orEmpty().contains("€"),
        )
    }

    @Test
    fun `future price respects the currency fraction digits rather than the locale default`() {
        val yen =
            quote.copy(
                priceCurrencyCode = "JPY",
                priceAmountMicros = 1_000_000_000,
                launchDetails = LaunchOfferDetails(1, 2, 1_500_000_000),
            )
        assertEquals("¥1,500", formatOfferFullPrice(yen, Locale.US))
        val dinar = quote.copy(priceCurrencyCode = "BHD", launchDetails = LaunchOfferDetails(1, 2, 14_999_000))
        assertTrue(formatOfferFullPrice(dinar, Locale.US).orEmpty().contains("14.999"))
    }

    @Test
    fun `unknown currency or unsupported full price produces no future price`() {
        assertNull(formatOfferFullPrice(quote.copy(priceCurrencyCode = ""), Locale.US))
        assertNull(formatOfferFullPrice(quote.copy(launchDetails = null), Locale.US))
        assertNull(formatOfferFullPrice(quote.copy(launchDetails = LaunchOfferDetails(1, 2, null)), Locale.US))
        assertNull(formatOfferFullPrice(quote.copy(launchDetails = LaunchOfferDetails(1, 2, 9_990_000)), Locale.US))
    }
}
