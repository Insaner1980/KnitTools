package com.finnvek.knittools.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PurchasesUpdatedListener {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _isProPurchased = MutableStateFlow(false)
        val isProPurchased: StateFlow<Boolean> = _isProPurchased.asStateFlow()

        private val _productDetails = MutableStateFlow<ProductDetails?>(null)
        val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

        private var billingClient: BillingClient? = null

        fun initialize() {
            billingClient =
                BillingClient
                    .newBuilder(context)
                    .setListener(this)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                    ).build()

            billingClient?.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            scope.launch {
                                queryPurchases()
                                queryProductDetails()
                            }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Retry on next user action
                    }
                },
            )
        }

        fun launchPurchaseFlow(activity: Activity) {
            val details = _productDetails.value ?: return

            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(details)
                    .build()

            val flowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

            billingClient?.launchBillingFlow(activity, flowParams)
        }

        fun restorePurchases() {
            scope.launch { queryPurchases() }
        }

        suspend fun restorePurchasesWithResult(): RestorePurchasesResult =
            when (val result = queryPurchasesInternal()) {
                is PurchaseQueryResult.Success -> {
                    _isProPurchased.value = result.proPurchases.isNotEmpty()
                    result.proPurchases
                        .filter { !it.isAcknowledged }
                        .forEach { acknowledgePurchase(it) }
                    if (result.proPurchases.isNotEmpty()) {
                        RestorePurchasesResult.RESTORED
                    } else {
                        RestorePurchasesResult.NOT_FOUND
                    }
                }

                PurchaseQueryResult.Failure -> {
                    RestorePurchasesResult.NOT_FOUND
                }
            }

        override fun onPurchasesUpdated(
            result: BillingResult,
            purchases: List<Purchase>?,
        ) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _isProPurchased.value = true
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }

        fun destroy() {
            billingClient?.endConnection()
            billingClient = null
        }

        private suspend fun queryPurchases() {
            when (val result = queryPurchasesInternal()) {
                is PurchaseQueryResult.Success -> {
                    _isProPurchased.value = result.proPurchases.isNotEmpty()
                    // Google Play palauttaa vahvistamattomat ostot 3 päivän jälkeen —
                    // varmista vahvistus joka käynnistyskerralla
                    result.proPurchases
                        .filter { !it.isAcknowledged }
                        .forEach { acknowledgePurchase(it) }
                }

                PurchaseQueryResult.Failure -> {
                    Unit
                }
            }
        }

        private suspend fun queryPurchasesInternal(): PurchaseQueryResult {
            val client = billingClient ?: return PurchaseQueryResult.Failure
            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            val result = client.queryPurchasesAsync(params)
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return PurchaseQueryResult.Failure
            }

            val proPurchases =
                result.purchasesList.filter {
                    it.products.contains(PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            return PurchaseQueryResult.Success(proPurchases)
        }

        private suspend fun queryProductDetails() {
            val client = billingClient ?: return
            val product =
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(listOf(product))
                    .build()

            val result = client.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = result.productDetailsList?.firstOrNull()
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private fun acknowledgePurchase(purchase: Purchase) {
            if (purchase.isAcknowledged) return
            try {
                val params =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                billingClient?.acknowledgePurchase(params) { /* acknowledged */ }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Will retry on next app start
            }
        }

        companion object {
            const val PRODUCT_ID = "knittools_pro"
        }
    }

enum class RestorePurchasesResult {
    RESTORED,
    NOT_FOUND,
}

private sealed interface PurchaseQueryResult {
    data class Success(
        val proPurchases: List<Purchase>,
    ) : PurchaseQueryResult

    data object Failure : PurchaseQueryResult
}
