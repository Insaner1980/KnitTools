package com.finnvek.knittools.billing

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Real SDK Purchase parsing and Android dispatchers; no Play connection or real purchase. */
class AuditPart15BillingTest {
    private lateinit var manager: BillingManager
    private lateinit var client: AuditPart15BillingClient

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName.endsWith(".activepolicytest"))
        client = AuditPart15BillingClient()
        manager = BillingManager(app)
        BillingManager::class.java
            .getDeclaredField("billingClient")
            .apply { isAccessible = true }
            .set(manager, client)
    }

    @After fun close() = manager.destroy()

    @Test
    fun returningToTheAppReconcilesOwnershipWithoutAPurchaseCallback() =
        runBlocking<Unit> {
            val app = ApplicationProvider.getApplicationContext<App>()
            val billing = app.billingManager.get()
            val pro = app.proManager.get()
            try {
                withContext(Dispatchers.Main) {
                    billing.destroy()
                    BillingManager::class.java
                        .getDeclaredField("billingClient")
                        .apply { isAccessible = true }
                        .set(billing, client)
                    ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
                    assertTrue(billing.queryPurchases())
                }
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.moveToState(Lifecycle.State.CREATED)
                    withContext(Dispatchers.Main) { client.purchases = listOf(purchase(acknowledged = true)) }
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    withTimeout(5_000) { pro.proState.first { it.status == ProStatus.PRO_PURCHASED } }

                    scenario.moveToState(Lifecycle.State.CREATED)
                    withContext(Dispatchers.Main) { client.purchases = emptyList() }
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    withTimeout(5_000) { pro.proState.first { it.status == ProStatus.TRIAL_EXPIRED } }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    try {
                        client.purchases = emptyList()
                        billing.queryPurchases()
                    } finally {
                        ProPolicyTestControl.reset()
                        billing.initialize()
                    }
                }
            }
        }

    @Test fun realSdkOfferParsingExcludesRentalAndPreorderAndBreaksTiesDeterministically() {
        fun offer(
            option: String,
            price: Long,
            token: String,
        ) = JSONObject()
            .put("formattedPrice", "$price EUR")
            .put("priceAmountMicros", price)
            .put("priceCurrencyCode", "EUR")
            .put("offerIdToken", token)
            .put("purchaseOptionId", option)
        val rental = offer("a", 1, "rental").put("rentalDetails", JSONObject().put("rentalPeriod", "P1D"))
        val preorder =
            offer("a", 1, "preorder").put(
                "preorderDetails",
                JSONObject()
                    .put("preorderReleaseTimeMillis", 1)
                    .put("preorderPresaleEndTimeMillis", 2),
            )
        val regular = offer("b", 50, "regular")
        val cheaperTie = offer("b", 40, "cheaper")
        val json =
            JSONObject()
                .put("productId", BillingManager.PRODUCT_ID)
                .put("type", "inapp")
                .put("oneTimePurchaseOfferDetailsList", JSONArray(listOf(rental, preorder, regular, cheaperTie)))
        val details =
            ProductDetails::class.java
                .getDeclaredConstructor(String::class.java)
                .apply { isAccessible = true }
                .newInstance(json.toString())
        assertEquals("cheaper", selectOneTimePurchaseOffer(details)?.offerToken)
        manager.applyProductDetailsResult(ProductDetailsResult(result(0), listOf(details)))
        assertEquals(BillingProductStatus.Available, manager.productStatus.value)
        assertEquals("40 EUR", manager.selectedOffer.value?.formattedPrice)
    }

    @Test fun missingProductDetailsLeavesPurchaseUnavailable() {
        manager.applyProductDetailsResult(ProductDetailsResult(result(0), emptyList()))
        assertEquals(
            BillingProductStatus.Unavailable(BillingUserMessage.PURCHASE_UNAVAILABLE),
            manager.productStatus.value,
        )
    }

    @Test fun pendingAndUnrelatedRecordsDoNotUnlockOrAcknowledge() =
        runBlocking(Dispatchers.Main) {
            manager.onPurchasesUpdated(result(0), listOf(purchase(pending = true)))
            assertFalse(manager.isProPurchased.value)
            manager.onPurchasesUpdated(result(0), listOf(purchase(product = "unrelated")))
            assertFalse(manager.isProPurchased.value)
            assertEquals(0, client.acknowledgements)
        }

    @Test fun matchingPurchasedUnlocksBeforeAckAndBlankOrAcknowledgedTokensAreNotAcknowledged() =
        runBlocking(Dispatchers.Main) {
            manager.onPurchasesUpdated(result(0), listOf(purchase(token = "")))
            assertTrue(manager.isProPurchased.value)
            assertEquals(0, client.acknowledgements)
            manager.onPurchasesUpdated(result(0), listOf(purchase(acknowledged = true)))
            assertEquals(0, client.acknowledgements)
            manager.onPurchasesUpdated(result(0), listOf(purchase()))
            manager.onPurchasesUpdated(result(0), listOf(purchase()))
            assertEquals(1, client.acknowledgements)
        }

    @Test fun alreadyOwnedLaunchRestoresAndEmptyOrFailedQueriesReportSpecificFailure() =
        runBlocking {
            withContext(Dispatchers.Main) {
                client.purchases = listOf(purchase())
                manager.applyPurchaseFlowResult(result(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED))
            }
            withTimeout(5_000) { manager.isProPurchased.first { it } }
            assertEquals(1, client.queries)
            for (code in listOf(0, BillingClient.BillingResponseCode.NETWORK_ERROR)) {
                val message = async(Dispatchers.Main.immediate) { manager.purchaseMessages.first() }
                withContext(Dispatchers.Main) {
                    client.purchases = emptyList()
                    client.queryCode = code
                    manager.applyPurchaseFlowResult(result(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED))
                }
                assertEquals(BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED, withTimeout(5_000) { message.await() })
            }
            assertEquals(3, client.queries)
            assertFalse(manager.isProPurchased.value)
            assertFalse(manager.purchaseFlowInFlight.value)
        }

    @Test fun failedQueryPreservesOwnershipWhileSuccessfulEmptyQueryRevokesIt() =
        runBlocking(Dispatchers.Main) {
            manager.onPurchasesUpdated(result(0), listOf(purchase(acknowledged = true)))
            client.queryCode = BillingClient.BillingResponseCode.NETWORK_ERROR
            assertFalse(manager.queryPurchases())
            assertTrue(manager.isProPurchased.value)
            client.queryCode = 0
            assertTrue(manager.queryPurchases())
            assertFalse(manager.isProPurchased.value)
            assertTrue(manager.purchaseStateReady.value)
        }

    @Test fun retryableAckQueriesAfterDelayAndNonretryableAckDoesNot() =
        runBlocking(Dispatchers.Main) {
            client.ackCode = BillingClient.BillingResponseCode.NETWORK_ERROR
            client.purchases = listOf(purchase())
            manager.onPurchasesUpdated(result(0), client.purchases)
            assertTrue(manager.isProPurchased.value)
            client.ackCode = BillingClient.BillingResponseCode.OK
            withTimeout(8_000) { while (client.queries == 0) delay(20) }
            assertEquals(2, client.acknowledgements)
            val queries = client.queries
            client.ackCode = BillingClient.BillingResponseCode.DEVELOPER_ERROR
            manager.onPurchasesUpdated(result(0), listOf(purchase(token = "nonretryable")))
            delay(5_300)
            assertEquals(queries, client.queries)
        }

    private fun purchase(
        product: String = BillingManager.PRODUCT_ID,
        token: String = "synthetic-token",
        pending: Boolean = false,
        acknowledged: Boolean = false,
    ): Purchase =
        Purchase(
            JSONObject()
                .put(
                    "productId",
                    product,
                ).put(
                    "purchaseToken",
                    token,
                ).put("purchaseState", if (pending) 4 else 0)
                .put("acknowledged", acknowledged)
                .toString(),
            "synthetic-signature",
        )

    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).build()
}
