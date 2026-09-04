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
import com.android.billingclient.api.queryPurchasesAsync
import com.finnvek.knittools.ProjectSourceFiles
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `purchase readiness is assigned only from a successful purchase query`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt",
            )

        assertTrue(source.contains("_purchaseStateReady.value = queryPurchases()"))
        assertFalse(
            source.contains(
                "queryPurchases()\n                                _purchaseStateReady.value = true",
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
            SelectedOneTimeOffer(formattedPrice = "regular", offerToken = "base-token"),
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
            val billingClient = mockk<BillingClient>(relaxed = true)
            mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
            coEvery { billingClient.queryPurchasesAsync(any()) } returns
                PurchasesResult(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    listOf(proPurchase()),
                )
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
}
