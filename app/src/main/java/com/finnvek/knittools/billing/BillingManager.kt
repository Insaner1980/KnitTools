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
import com.android.billingclient.api.ProductDetailsResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

        private val _productStatus = MutableStateFlow<BillingProductStatus>(BillingProductStatus.Loading)
        val productStatus: StateFlow<BillingProductStatus> = _productStatus.asStateFlow()

        private val _purchaseMessages = MutableSharedFlow<BillingUserMessage>(extraBufferCapacity = 1)
        val purchaseMessages: SharedFlow<BillingUserMessage> = _purchaseMessages.asSharedFlow()

        private var billingClient: BillingClient? = null
        private val pendingAcknowledgementRetries = mutableSetOf<String>()

        fun initialize() {
            billingClient =
                BillingClient
                    .newBuilder(context)
                    .setListener(this)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                    ).enableAutoServiceReconnection()
                    .build()

            billingClient?.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            scope.launch {
                                queryPurchases()
                                queryProductDetails()
                            }
                        } else {
                            applyProductUnavailable(result.toUserMessage())
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Billing 8:n automaattinen reconnect hoitaa seuraavan Billing API -kutsun.
                    }
                },
            )
        }

        fun launchPurchaseFlow(activity: Activity) {
            val details =
                _productDetails.value
                    ?: run {
                        emitPurchaseMessage(productUnavailableMessage())
                        return
                    }

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

            val result =
                billingClient?.launchBillingFlow(activity, flowParams)
                    ?: run {
                        emitPurchaseMessage(BillingUserMessage.PURCHASE_FAILED)
                        return
                    }
            applyPurchaseFlowResult(result)
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
                    RestorePurchasesResult.FAILED
                }
            }

        override fun onPurchasesUpdated(
            result: BillingResult,
            purchases: List<Purchase>?,
        ) {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            _isProPurchased.value = true
                            acknowledgePurchase(purchase)
                        }
                    }
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    scope.launch { restoreAlreadyOwnedPurchase() }
                }

                else -> {
                    emitPurchaseMessage(result.toUserMessage())
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

        private suspend fun restoreAlreadyOwnedPurchase() {
            when (val result = queryPurchasesInternal()) {
                is PurchaseQueryResult.Success -> {
                    _isProPurchased.value = result.proPurchases.isNotEmpty()
                    result.proPurchases
                        .filter { !it.isAcknowledged }
                        .forEach { acknowledgePurchase(it) }
                    if (result.proPurchases.isEmpty()) {
                        emitPurchaseMessage(BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED)
                    }
                }

                PurchaseQueryResult.Failure -> {
                    emitPurchaseMessage(BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED)
                }
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private suspend fun queryPurchasesInternal(): PurchaseQueryResult {
            val client = billingClient ?: return PurchaseQueryResult.Failure
            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            val result =
                try {
                    client.queryPurchasesAsync(params)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    return PurchaseQueryResult.Failure
                }
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

        @Suppress("TooGenericExceptionCaught")
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

            val result =
                try {
                    client.queryProductDetails(params)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    applyProductUnavailable(BillingUserMessage.PURCHASE_FAILED)
                    return
                }
            applyProductDetailsResult(result)
        }

        internal fun applyProductDetailsResult(result: ProductDetailsResult) {
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                applyProductUnavailable(result.billingResult.toUserMessage())
                return
            }

            val details = result.productDetailsList?.firstOrNull()
            if (details == null) {
                applyProductUnavailable(BillingUserMessage.PURCHASE_UNAVAILABLE)
            } else {
                _productDetails.value = details
                _productStatus.value = BillingProductStatus.Available
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
                billingClient?.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        pendingAcknowledgementRetries.remove(purchase.purchaseToken)
                    } else {
                        scheduleAcknowledgementRetry(purchase.purchaseToken)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                scheduleAcknowledgementRetry(purchase.purchaseToken)
            }
        }

        private fun scheduleAcknowledgementRetry(purchaseToken: String) {
            if (!pendingAcknowledgementRetries.add(purchaseToken)) return
            scope.launch {
                delay(ACKNOWLEDGEMENT_RETRY_DELAY_MS)
                pendingAcknowledgementRetries.remove(purchaseToken)
                queryPurchases()
            }
        }

        internal fun applyPurchaseFlowResult(result: BillingResult) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK) return
            emitPurchaseMessage(result.toUserMessage())
        }

        private fun applyProductUnavailable(message: BillingUserMessage) {
            _productDetails.value = null
            _productStatus.value = BillingProductStatus.Unavailable(message)
        }

        private fun productUnavailableMessage(): BillingUserMessage =
            when (val status = _productStatus.value) {
                BillingProductStatus.Available,
                BillingProductStatus.Loading,
                -> BillingUserMessage.PURCHASE_UNAVAILABLE

                is BillingProductStatus.Unavailable -> status.message
            }

        private fun emitPurchaseMessage(message: BillingUserMessage) {
            _purchaseMessages.tryEmit(message)
        }

        private fun BillingResult.toUserMessage(): BillingUserMessage =
            when (responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    BillingUserMessage.PURCHASE_CANCELLED
                }

                BillingClient.BillingResponseCode.NETWORK_ERROR,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                -> {
                    BillingUserMessage.PURCHASE_NETWORK_ERROR
                }

                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                -> {
                    BillingUserMessage.PURCHASE_UNAVAILABLE
                }

                else -> {
                    BillingUserMessage.PURCHASE_FAILED
                }
            }

        companion object {
            const val PRODUCT_ID = "knittools_pro"
            private const val ACKNOWLEDGEMENT_RETRY_DELAY_MS = 5_000L
        }
    }

enum class RestorePurchasesResult {
    RESTORED,
    NOT_FOUND,
    FAILED,
}

enum class BillingUserMessage {
    PURCHASE_CANCELLED,
    PURCHASE_UNAVAILABLE,
    PURCHASE_NETWORK_ERROR,
    PURCHASE_FAILED,
    ALREADY_OWNED_RESTORE_FAILED,
}

sealed interface BillingProductStatus {
    data object Loading : BillingProductStatus

    data object Available : BillingProductStatus

    data class Unavailable(
        val message: BillingUserMessage,
    ) : BillingProductStatus
}

private sealed interface PurchaseQueryResult {
    data class Success(
        val proPurchases: List<Purchase>,
    ) : PurchaseQueryResult

    data object Failure : PurchaseQueryResult
}
