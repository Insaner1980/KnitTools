package com.finnvek.knittools.billing

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.queryProductDetails
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingPriceRefreshTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        unmockkStatic(TextUtils::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `unchanged refresh keeps the price visible and blocks checkout until it completes`() = verifyRefresh(false)

    @Test
    fun `changed refresh blocks stale checkout and requires another tap after price review`() = verifyRefresh(true)

    private fun verifyRefresh(changed: Boolean) =
        runTest {
            val client = mockk<BillingClient>(relaxed = true)
            val activity = mockk<Activity>(relaxed = true)
            val ok = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            every { client.isReady } returns true
            every { client.launchBillingFlow(activity, any()) } returns ok
            val response = CompletableDeferred<ProductDetailsResult>()
            coEvery { client.queryProductDetails(any()) } coAnswers { response.await() }
            val manager = BillingManager(mockk<Context>(relaxed = true), CoroutineScope(Dispatchers.Main.immediate))
            BillingManager::class.java.getDeclaredField("billingClient").apply {
                isAccessible = true
                set(manager, client)
            }
            try {
                manager.applyProductDetailsResult(ProductDetailsResult(ok, listOf(product(9_990_000))))
                val quote = manager.selectedOffer.value
                manager.retryProductDetails()
                assertEquals(BillingProductStatus.Available, manager.productStatus.value)
                assertEquals(quote, manager.selectedOffer.value)
                assertPurchaseBlocked(manager, client, activity, BillingUserMessage.PRICE_REFRESHING)

                val amount = if (changed) 14_990_000L else 9_990_000L
                response.complete(ProductDetailsResult(ok, listOf(product(amount))))
                runCurrent()
                assertEquals(BillingProductStatus.Available, manager.productStatus.value)
                assertEquals(amount, manager.selectedOffer.value?.priceAmountMicros)
                if (changed) {
                    repeat(2) {
                        assertPurchaseBlocked(manager, client, activity, BillingUserMessage.PRICE_CHANGED_REVIEW, quote)
                    }
                } else {
                    assertEquals(quote, manager.selectedOffer.value)
                }
                manager.launchPurchaseFlow(activity, manager.selectedOffer.value)
                verify(exactly = 1) { client.launchBillingFlow(activity, any()) }
            } finally {
                manager.destroy()
            }
        }

    private fun product(amount: Long): ProductDetails {
        val offer = mockk<ProductDetails.OneTimePurchaseOfferDetails>(relaxed = true)
        every { offer.formattedPrice } returns "Play price"
        every { offer.priceAmountMicros } returns amount
        every { offer.priceCurrencyCode } returns "USD"
        every { offer.offerToken } returns "base-token"
        every { offer.offerId } returns null
        every { offer.purchaseOptionId } returns "permanent"
        every { offer.validTimeWindow } returns null
        every { offer.limitedQuantityInfo } returns null
        every { offer.fullPriceMicros } returns null
        every { offer.rentalDetails } returns null
        every { offer.preorderDetails } returns null
        return mockk<ProductDetails>(relaxed = true).also {
            every { it.productId } returns BillingManager.PRODUCT_ID
            every { it.productType } returns BillingClient.ProductType.INAPP
            every { it.oneTimePurchaseOfferDetailsList } returns listOf(offer)
        }
    }
}
