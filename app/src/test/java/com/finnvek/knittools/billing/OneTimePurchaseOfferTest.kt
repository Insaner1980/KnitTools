package com.finnvek.knittools.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneTimePurchaseOfferTest {
    @Test
    fun `one time offer selection prefers permanent base offer deterministically`() {
        val product = mockk<ProductDetails>()
        val discounted = oneTimeOffer("discount", "discount-token", "promo", 1_000_000L)
        val permanent = oneTimeOffer("regular", "base-token", null, 2_000_000L)
        every { product.oneTimePurchaseOfferDetailsList } returns listOf(discounted, permanent)
        every { product.oneTimePurchaseOfferDetails } returns permanent

        assertEquals(
            SelectedOneTimeOffer("regular", "base-token", 2_000_000L, "USD", "permanent"),
            selectOneTimePurchaseOffer(product),
        )
    }

    @Test
    fun `one time offer selection rejects rental only product`() {
        val product = mockk<ProductDetails>()
        val rental = oneTimeOffer("rental", "rental-token", null, 1_000_000L)
        every { rental.rentalDetails } returns mockk()
        every { product.oneTimePurchaseOfferDetailsList } returns listOf(rental)
        every { product.oneTimePurchaseOfferDetails } returns rental

        assertNull(selectOneTimePurchaseOffer(product))
    }

    @Test
    fun `active launch offer wins over its permanent base offer`() {
        val product = mockk<ProductDetails>()
        val permanent = oneTimeOffer("$14.99", "base-token", null, 14_990_000L)
        val launch = oneTimeOffer("$9.99", "launch-token", "launch", 9_990_000L)
        val window = mockk<ProductDetails.OneTimePurchaseOfferDetails.ValidTimeWindow>()
        every { window.startTimeMillis } returns System.currentTimeMillis() - 60_000L
        every { window.endTimeMillis } returns System.currentTimeMillis() + 60_000L
        every { launch.validTimeWindow } returns window
        every { product.oneTimePurchaseOfferDetailsList } returns listOf(permanent, launch)
        every { product.oneTimePurchaseOfferDetails } returns permanent

        assertEquals("launch-token", selectOneTimePurchaseOffer(product)?.offerToken)
    }

    @Test
    fun `launch boundaries are start inclusive and end exclusive`() {
        val product = launchProduct(1_000L, 2_000L)
        listOf(999L to "base-token", 1_000L to "launch-token", 1_999L to "launch-token", 2_000L to "base-token")
            .forEach { (now, token) -> assertEquals(token, selectOneTimePurchaseOffer(product, now)?.offerToken) }
    }

    @Test
    fun `regional amounts and currencies come directly from the selected offer`() {
        listOf(
            Triple("USD", 9_990_000L, 14_990_000L),
            Triple("EUR", 9_990_000L, 14_990_000L),
            Triple("BRL", 24_900_000L, 32_900_000L),
        ).forEach { (currency, amount, full) ->
            val selected = selectOneTimePurchaseOffer(launchProduct(1_000, 2_000, currency, amount, full), 1_500)
            assertEquals(currency, selected?.priceCurrencyCode)
            assertEquals(amount, selected?.priceAmountMicros)
            assertEquals(full, selected?.launchDetails?.fullPriceMicros)
            assertEquals("Play price", selected?.formattedPrice)
        }
    }

    @Test
    fun `missing and reversed windows and blank token never fall back to launch`() {
        listOf(null to 2_000L, 1_000L to null, 2_000L to 1_000L, 1_000L to 1_000L).forEach { (start, end) ->
            assertEquals("base-token", selectOneTimePurchaseOffer(launchProduct(start, end), 1_500)?.offerToken)
        }
        assertEquals(
            "base-token",
            selectOneTimePurchaseOffer(launchProduct(1_000, 2_000, token = ""), 1_500)?.offerToken,
        )
    }

    @Test
    fun `malformed or unbounded launch timestamps fall back to regular purchase`() {
        listOf(0L to 2_000L, Long.MIN_VALUE to 2_000L, 1_000L to Long.MAX_VALUE)
            .forEach { (start, end) ->
                assertEquals("base-token", selectOneTimePurchaseOffer(launchProduct(start, end), 1_500)?.offerToken)
            }
    }

    @Test
    fun `unknown or non-increasing full price omits only future price`() {
        listOf(null, 9_990_000L, 5_000_000L).forEach { full ->
            val selected = selectOneTimePurchaseOffer(launchProduct(1_000, 2_000, full = full), 1_500)
            assertEquals("launch-token", selected?.offerToken)
            assertEquals(2_000L, selected?.launchDetails?.endsAtMillis)
            assertNull(selected?.launchDetails?.fullPriceMicros)
        }
    }

    @Test
    fun `legacy fallback permits permanent purchases and rejects rentals and preorders`() {
        val product = mockk<ProductDetails>()
        val base = oneTimeOffer("Legacy", "", null, 14_990_000L)
        every { product.oneTimePurchaseOfferDetailsList } returns emptyList()
        every { product.oneTimePurchaseOfferDetails } returns base
        assertEquals("Legacy", selectOneTimePurchaseOffer(product, 1_500)?.formattedPrice)
        every { base.rentalDetails } returns mockk()
        assertNull(selectOneTimePurchaseOffer(product, 1_500))
        every { base.rentalDetails } returns null
        every { base.preorderDetails } returns mockk()
        assertNull(selectOneTimePurchaseOffer(product, 1_500))
        every { product.oneTimePurchaseOfferDetailsList } returns listOf(base)
        assertNull(selectOneTimePurchaseOffer(product, 1_500))
    }
}

internal fun launchProduct(
    start: Long?,
    end: Long?,
    currency: String = "USD",
    amount: Long = 9_990_000L,
    full: Long? = 14_990_000L,
    token: String = "launch-token",
): ProductDetails {
    val base = oneTimeOffer("Regular", "base-token", null, 14_990_000L)
    val launch = oneTimeOffer("Play price", token, "launch", amount)
    val window = mockk<ProductDetails.OneTimePurchaseOfferDetails.ValidTimeWindow>()
    every { window.startTimeMillis } returns start
    every { window.endTimeMillis } returns end
    every { launch.validTimeWindow } returns window
    every { launch.priceCurrencyCode } returns currency
    every { launch.fullPriceMicros } returns full
    return mockk<ProductDetails>(relaxed = true).also {
        every { it.productId } returns BillingManager.PRODUCT_ID
        every { it.productType } returns BillingClient.ProductType.INAPP
        every { it.oneTimePurchaseOfferDetailsList } returns listOf(base, launch)
        every { it.oneTimePurchaseOfferDetails } returns base
    }
}

internal fun oneTimeOffer(
    formattedPrice: String,
    offerToken: String,
    offerId: String?,
    priceMicros: Long,
): ProductDetails.OneTimePurchaseOfferDetails =
    mockk<ProductDetails.OneTimePurchaseOfferDetails>().also { offer ->
        every { offer.formattedPrice } returns formattedPrice
        every { offer.offerToken } returns offerToken
        every { offer.offerId } returns offerId
        every { offer.purchaseOptionId } returns "permanent"
        every { offer.priceAmountMicros } returns priceMicros
        every { offer.priceCurrencyCode } returns "USD"
        every { offer.fullPriceMicros } returns null
        every { offer.rentalDetails } returns null
        every { offer.preorderDetails } returns null
        every { offer.validTimeWindow } returns null
        every { offer.limitedQuantityInfo } returns null
    }
