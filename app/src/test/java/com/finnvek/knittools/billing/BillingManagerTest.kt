package com.finnvek.knittools.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.queryPurchasesAsync
import com.finnvek.knittools.ProjectSourceFiles
import io.mockk.coEvery
import io.mockk.coVerify
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun createManager(billingClient: BillingClient): BillingManager =
        BillingManager(context).also { manager ->
            val field = BillingManager::class.java.getDeclaredField("billingClient")
            field.isAccessible = true
            field.set(manager, billingClient)
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

    private fun proPurchase(): Purchase =
        Purchase(
            """
            {
              "orderId": "GPA.1234-5678-9012-34567",
              "packageName": "com.finnvek.knittools",
              "productId": "${BillingManager.PRODUCT_ID}",
              "purchaseTime": 1700000000000,
              "purchaseState": 0,
              "purchaseToken": "token",
              "quantity": 1,
              "acknowledged": false
            }
            """.trimIndent(),
            "signature",
        )
}
