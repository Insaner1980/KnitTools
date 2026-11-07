package com.finnvek.knittools.ui.components

import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchPriceTagTest {
    private fun offer(
        priceMicros: Long,
        fullMicros: Long?,
    ) = SelectedOneTimeOffer(
        formattedPrice = "€9.99",
        offerToken = "token",
        priceAmountMicros = priceMicros,
        priceCurrencyCode = "EUR",
        offerId = "launch",
        launchDetails = fullMicros?.let { LaunchOfferDetails(0L, 1L, it) },
    )

    @Test fun `launch discount rounds to a whole percent`() {
        assertEquals(0.33, savingsPercent(offer(9_990_000L, 14_990_000L))!!, 0.0001)
    }

    @Test fun `no saving without a cheaper launch offer`() {
        assertNull(savingsPercent(offer(9_990_000L, null)))
        assertNull(savingsPercent(offer(14_990_000L, 14_990_000L)))
    }
}
