package com.finnvek.knittools.billing

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.finnvek.knittools.ProjectSourceFiles
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        unmockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        unmockkStatic(TextUtils::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `billing client enables automatic service reconnection`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt",
            )

        assertTrue(source.contains(".enableAutoServiceReconnection()"))
    }

    @Test
    fun `billing disconnect invalidates cached product details with a retryable state`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt",
            )
        val disconnectHandler =
            source
                .substringAfter("override fun onBillingServiceDisconnected()")
                .substringBefore("}", missingDelimiterValue = "")

        assertTrue(
            disconnectHandler.contains(
                "applyProductUnavailable(BillingUserMessage.PURCHASE_NETWORK_ERROR)",
            ),
        )
    }

    @Test
    fun `purchase flow passes selected one time offer token`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt",
            )

        assertTrue(source.contains("val offer ="))
        assertTrue(source.contains("if (offer.offerToken.isNotBlank())"))
        assertTrue(source.contains("setOfferToken(offer.offerToken)"))
    }

    @Test
    fun `purchase flow ignores repeated launch until billing callback`() {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        val billingClient = mockk<BillingClient>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val product = mockk<ProductDetails>(relaxed = true)
        val offer = oneTimeOffer("regular", "", null, 2_000_000L)
        every { product.oneTimePurchaseOfferDetailsList } returns listOf(offer)
        every { product.oneTimePurchaseOfferDetails } returns offer
        every { billingClient.launchBillingFlow(activity, any()) } returns
            billingResult(BillingClient.BillingResponseCode.OK)
        val manager = createManager(billingClient)
        manager.applyProductDetailsResult(
            ProductDetailsResult(
                billingResult(BillingClient.BillingResponseCode.OK),
                listOf(product),
            ),
        )

        manager.launchPurchaseFlow(activity)
        manager.launchPurchaseFlow(activity)

        assertTrue(manager.purchaseFlowInFlight.value)
        verify(exactly = 1) { billingClient.launchBillingFlow(activity, any()) }

        manager.onPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.USER_CANCELED),
            null,
        )

        assertFalse(manager.purchaseFlowInFlight.value)
        manager.launchPurchaseFlow(activity)
        verify(exactly = 2) { billingClient.launchBillingFlow(activity, any()) }
    }

    @Test
    fun `billing setup failure retries connection before marking product unavailable`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt",
            )

        assertTrue(source.contains("private fun startBillingConnection()"))
        assertTrue(source.contains("private fun scheduleConnectionRetry(): Boolean"))
        assertTrue(source.contains("if (scheduleConnectionRetry())"))
        assertTrue(source.contains("private const val CONNECTION_MAX_ATTEMPTS = 3"))
    }

    @Test
    fun `failed acknowledgement retries are bounded`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            val purchase = proPurchase()
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            coEvery { billingClient.queryPurchasesAsync(any()) } returns
                PurchasesResult(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    listOf(purchase),
                )
            every { billingClient.acknowledgePurchase(any(), any()) } answers {
                secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(
                    billingResult(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),
                )
            }
            val manager = createManager(billingClient)

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.OK),
                listOf(purchase),
            )
            advanceUntilIdle()

            verify(exactly = 4) { billingClient.acknowledgePurchase(any(), any()) }
        }

    @Test
    fun `terminal acknowledgement failure is not retried`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            every { billingClient.acknowledgePurchase(any(), any()) } answers {
                secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(
                    billingResult(BillingClient.BillingResponseCode.DEVELOPER_ERROR),
                )
            }
            val manager = createManager(billingClient)

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.OK),
                listOf(proPurchase()),
            )
            advanceUntilIdle()

            verify(exactly = 1) { billingClient.acknowledgePurchase(any(), any()) }
        }

    @Test
    fun `successful pro purchase marks pro purchased`() {
        val billingClient = mockk<BillingClient>(relaxed = true)
        val manager = createManager(billingClient)

        manager.onPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.OK),
            listOf(proPurchase()),
        )

        assertTrue(manager.isProPurchased.value)
        verify { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `duplicate purchase callbacks acknowledge the same token only once`() {
        val billingClient = mockk<BillingClient>(relaxed = true)
        every { billingClient.acknowledgePurchase(any(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(
                billingResult(BillingClient.BillingResponseCode.OK),
            )
        }
        val manager = createManager(billingClient)
        val purchase = proPurchase()

        manager.onPurchasesUpdated(billingResult(BillingClient.BillingResponseCode.OK), listOf(purchase))
        manager.onPurchasesUpdated(billingResult(BillingClient.BillingResponseCode.OK), listOf(purchase))

        verify(exactly = 1) { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `pending pro purchase does not grant entitlement and reports pending state`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            val manager = createManager(billingClient)
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.OK),
                listOf(proPurchase(purchaseState = 4)),
            )

            assertFalse(manager.isProPurchased.value)
            assertEquals(BillingUserMessage.PURCHASE_PENDING, message.await())
            verify(exactly = 0) { billingClient.acknowledgePurchase(any(), any()) }
        }

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

    @Test
    fun `expired cached quote refreshes without launching the higher price on the same tap`() =
        runTest {
            mockkStatic(TextUtils::class)
            every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            val client = mockk<BillingClient>(relaxed = true)
            val activity = mockk<Activity>(relaxed = true)
            every { client.launchBillingFlow(activity, any()) } returns
                billingResult(BillingClient.BillingResponseCode.OK)
            val manager = createManager(client)
            val product = launchProduct(1_000, 2_000)
            coEvery { client.queryProductDetails(any()) } returns productResult(product)
            manager.applyProductDetailsResult(productResult(product))
            // Simulate a cached timed snapshot whose wall-clock deadline has passed.
            val selected = BillingManager::class.java.getDeclaredField("_selectedOffer").apply { isAccessible = true }

            @Suppress("UNCHECKED_CAST")
            val quote = selected.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<SelectedOneTimeOffer?>
            quote.value =
                SelectedOneTimeOffer(
                    "$9.99",
                    "launch-token",
                    9_990_000,
                    "USD",
                    launchDetails = LaunchOfferDetails(1, 2, 14_990_000),
                )
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }
            manager.launchPurchaseFlow(activity)
            assertEquals(BillingUserMessage.PRICE_CHANGED_REVIEW, message.await())
            verify(exactly = 0) { client.launchBillingFlow(any(), any()) }
            runCurrent()
            assertEquals("base-token", manager.selectedOffer.value?.offerToken)
            manager.launchPurchaseFlow(activity)
            verify(exactly = 1) { client.launchBillingFlow(activity, any()) }
            manager.destroy()
        }

    @Test
    fun `overlapping price refreshes coalesce and destroy rejects a late response`() =
        runTest {
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            val client = mockk<BillingClient>(relaxed = true)
            val response = CompletableDeferred<ProductDetailsResult>()
            coEvery { client.queryProductDetails(any()) } coAnswers { response.await() }
            val manager = createManager(client)
            manager.retryProductDetails()
            manager.retryProductDetails()
            assertEquals(BillingProductStatus.Loading, manager.productStatus.value)
            coVerify(exactly = 1) { client.queryProductDetails(any()) }
            manager.destroy()
            response.complete(productResult(launchProduct(1_000, 2_000)))
            runCurrent()
            assertNull(manager.selectedOffer.value)
        }

    @Test
    fun `deadline invalidation clears the quote before a failed refresh`() =
        runTest {
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            val client = mockk<BillingClient>(relaxed = true)
            coEvery { client.queryProductDetails(any()) } returns
                ProductDetailsResult(billingResult(BillingClient.BillingResponseCode.NETWORK_ERROR), emptyList())
            val manager = createManager(client)
            val now = System.currentTimeMillis()
            manager.applyProductDetailsResult(productResult(launchProduct(now - 10_000, now + 10_000)))
            assertEquals("launch-token", manager.selectedOffer.value?.offerToken)
            advanceTimeBy(10_001)
            runCurrent()
            assertNull(manager.selectedOffer.value)
            assertTrue(manager.productStatus.value is BillingProductStatus.Unavailable)
            manager.destroy()
        }

    private fun productResult(product: ProductDetails) =
        ProductDetailsResult(billingResult(BillingClient.BillingResponseCode.OK), listOf(product))

    private fun launchProduct(
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
            every { it.oneTimePurchaseOfferDetailsList } returns listOf(base, launch)
            every { it.oneTimePurchaseOfferDetails } returns base
        }
    }

    private fun oneTimeOffer(
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

    private fun createManager(billingClient: BillingClient): BillingManager =
        BillingManager(context).also { manager ->
            every { billingClient.isReady } returns true
            val field = BillingManager::class.java.getDeclaredField("billingClient")
            field.isAccessible = true
            field.set(manager, billingClient)
        }

    @Test
    fun `restore waits for initial billing connection before querying purchases`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            every { billingClient.isReady } returns false
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            coEvery { billingClient.queryPurchasesAsync(any()) } returns
                PurchasesResult(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    listOf(proPurchase()),
                )
            val manager = BillingManager(context)
            val clientField = BillingManager::class.java.getDeclaredField("billingClient")
            clientField.isAccessible = true
            clientField.set(manager, billingClient)

            val restore = async { manager.restorePurchasesWithResult() }
            runCurrent()
            assertFalse(restore.isCompleted)

            val readyField = BillingManager::class.java.getDeclaredField("_purchaseStateReady")
            readyField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (readyField.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>).value = true

            assertEquals(RestorePurchasesResult.RESTORED, restore.await())
            coVerify(exactly = 1) { billingClient.queryPurchasesAsync(any()) }
        }

    @Test
    fun `cancelled purchase leaves pro unpurchased`() {
        val manager = BillingManager(context)

        manager.onPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.USER_CANCELED),
            null,
        )

        assertFalse(manager.isProPurchased.value)
    }

    @Test
    fun `cancelled purchase emits cancellation message`() =
        runTest {
            val manager = BillingManager(context)
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.USER_CANCELED),
                null,
            )

            assertEquals(BillingUserMessage.PURCHASE_CANCELLED, message.await())
        }

    @Test
    fun `network purchase failure emits network message`() =
        runTest {
            val manager = BillingManager(context)
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.NETWORK_ERROR),
                null,
            )

            assertEquals(BillingUserMessage.PURCHASE_NETWORK_ERROR, message.await())
        }

    @Test
    fun `already owned response restores pro from purchase query`() =
        runTest {
            val billingClient = clientWithProPurchase()
            val manager = createManager(billingClient)

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
                null,
            )
            advanceUntilIdle()

            assertTrue(manager.isProPurchased.value)
            coVerify { billingClient.queryPurchasesAsync(any()) }
        }

    @Test
    fun `already owned response emits restore failed message when purchase query finds nothing`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            coEvery { billingClient.queryPurchasesAsync(any()) } returns
                PurchasesResult(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    emptyList(),
                )
            val manager = createManager(billingClient)
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

            manager.onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
                null,
            )
            advanceUntilIdle()

            assertFalse(manager.isProPurchased.value)
            assertEquals(BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED, message.await())
        }

    @Test
    fun `purchase flow result emits returned network failure`() =
        runTest {
            val manager = BillingManager(context)
            val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

            manager.applyPurchaseFlowResult(
                billingResult(BillingClient.BillingResponseCode.NETWORK_ERROR),
            )

            assertEquals(BillingUserMessage.PURCHASE_NETWORK_ERROR, message.await())
        }

    @Test
    fun `already owned launch result restores purchased entitlement`() =
        runTest {
            val billingClient = clientWithProPurchase()
            val manager = createManager(billingClient)

            manager.applyPurchaseFlowResult(billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED))
            advanceUntilIdle()

            assertTrue(manager.isProPurchased.value)
            assertTrue(manager.purchaseStateReady.value)
            assertFalse(manager.purchaseFlowInFlight.value)
            coVerify(exactly = 1) { billingClient.queryPurchasesAsync(any()) }
            manager.destroy()
        }

    @Test
    fun `already owned launch result reports empty or failed restore`() =
        runTest {
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            listOf(
                BillingClient.BillingResponseCode.OK,
                BillingClient.BillingResponseCode.NETWORK_ERROR,
            ).forEach { code ->
                val billingClient = mockk<BillingClient>(relaxed = true)
                coEvery { billingClient.queryPurchasesAsync(any()) } returns
                    PurchasesResult(billingResult(code), emptyList())
                val manager = createManager(billingClient)
                val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }

                manager.applyPurchaseFlowResult(billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED))
                advanceUntilIdle()

                assertFalse(manager.isProPurchased.value)
                assertEquals(BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED, message.await())
                coVerify(exactly = 1) { billingClient.queryPurchasesAsync(any()) }
                manager.destroy()
            }
        }

    @Test
    fun `empty product details marks product unavailable`() {
        val manager = BillingManager(context)

        manager.applyProductDetailsResult(
            ProductDetailsResult(
                billingResult(BillingClient.BillingResponseCode.OK),
                emptyList(),
            ),
        )

        assertEquals(
            BillingProductStatus.Unavailable(BillingUserMessage.PURCHASE_UNAVAILABLE),
            manager.productStatus.value,
        )
    }

    @Test
    fun `network product details failure marks product network error`() {
        val manager = BillingManager(context)

        manager.applyProductDetailsResult(
            ProductDetailsResult(
                billingResult(BillingClient.BillingResponseCode.NETWORK_ERROR),
                emptyList(),
            ),
        )

        assertEquals(
            BillingProductStatus.Unavailable(BillingUserMessage.PURCHASE_NETWORK_ERROR),
            manager.productStatus.value,
        )
    }

    @Test
    fun `restore result distinguishes query failure from missing purchases`() =
        runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            coEvery { billingClient.queryPurchasesAsync(any()) } returns
                PurchasesResult(
                    billingResult(BillingClient.BillingResponseCode.ERROR),
                    emptyList(),
                )
            val manager = createManager(billingClient)

            val result = manager.restorePurchasesWithResult()

            assertEquals(RestorePurchasesResult.FAILED, result)
        }

    @Test
    fun `developer error leaves pro unpurchased`() {
        val manager = BillingManager(context)

        manager.onPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.DEVELOPER_ERROR),
            null,
        )

        assertFalse(manager.isProPurchased.value)
    }

    private fun billingResult(responseCode: Int): BillingResult =
        BillingResult
            .newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage("test")
            .build()

    private fun proPurchase(purchaseState: Int = 0): Purchase =
        Purchase(
            """
            {
              "orderId": "GPA.1234-5678-9012-34567",
              "packageName": "com.finnvek.knittools",
              "productId": "${BillingManager.PRODUCT_ID}",
              "purchaseTime": 1700000000000,
              "purchaseState": $purchaseState,
              "purchaseToken": "token",
              "quantity": 1,
              "acknowledged": false
            }
            """.trimIndent(),
            "signature",
        )

    private fun clientWithProPurchase(): BillingClient {
        val billingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        coEvery { billingClient.queryPurchasesAsync(any()) } returns
            PurchasesResult(
                billingResult(BillingClient.BillingResponseCode.OK),
                listOf(proPurchase()),
            )
        return billingClient
    }
}
