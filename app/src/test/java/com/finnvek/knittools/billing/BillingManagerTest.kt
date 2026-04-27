package com.finnvek.knittools.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
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
        Dispatchers.resetMain()
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
    fun `already owned response leaves pro unpurchased until restore query succeeds`() {
        val manager = BillingManager(context)

        manager.onPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
            null,
        )

        assertFalse(manager.isProPurchased.value)
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
