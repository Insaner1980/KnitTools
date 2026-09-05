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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

        private val _purchaseStateReady = MutableStateFlow(false)
        val purchaseStateReady: StateFlow<Boolean> = _purchaseStateReady.asStateFlow()

        private val _productDetails = MutableStateFlow<ProductDetails?>(null)
        val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

        private val _selectedOffer = MutableStateFlow<SelectedOneTimeOffer?>(null)
        val selectedOffer: StateFlow<SelectedOneTimeOffer?> = _selectedOffer.asStateFlow()

        private val _productStatus = MutableStateFlow<BillingProductStatus>(BillingProductStatus.Loading)
        val productStatus: StateFlow<BillingProductStatus> = _productStatus.asStateFlow()

        private val _purchaseMessages = MutableSharedFlow<BillingUserMessage>(extraBufferCapacity = 1)
        val purchaseMessages: SharedFlow<BillingUserMessage> = _purchaseMessages.asSharedFlow()

        private val _purchaseFlowInFlight = MutableStateFlow(false)
        val purchaseFlowInFlight: StateFlow<Boolean> = _purchaseFlowInFlight.asStateFlow()

        private var billingClient: BillingClient? = null
        private val acknowledgementsInFlight = mutableSetOf<String>()
        private val acknowledgedPurchaseTokens = mutableSetOf<String>()
        private val pendingAcknowledgementRetries = mutableSetOf<String>()
        private val acknowledgementRetryCounts = mutableMapOf<String, Int>()
        private var connectionAttempt = 0
        private var connectionRetryJob: Job? = null

        fun initialize() {
            _purchaseStateReady.value = false
            _purchaseFlowInFlight.value = false
            resetProductDetails()
            connectionAttempt = 0
            connectionRetryJob?.cancel()
            connectionRetryJob = null
            billingClient =
                BillingClient
                    .newBuilder(context)
                    .setListener(this)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                    ).enableAutoServiceReconnection()
                    .build()

            startBillingConnection()
        }

        private fun startBillingConnection() {
            val client = billingClient ?: return
            connectionAttempt++
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            connectionAttempt = 0
                            connectionRetryJob?.cancel()
                            connectionRetryJob = null
                            scope.launch {
                                _purchaseStateReady.value = queryPurchases()
                                queryProductDetails()
                            }
                        } else {
                            if (scheduleConnectionRetry()) {
                                return
                            }
                            _purchaseStateReady.value = false
                            applyProductUnavailable(result.toUserMessage())
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        applyProductUnavailable(BillingUserMessage.PURCHASE_NETWORK_ERROR)
                    }
                },
            )
        }

        private fun scheduleConnectionRetry(): Boolean {
            if (connectionAttempt >= CONNECTION_MAX_ATTEMPTS) return false
            connectionRetryJob?.cancel()
            connectionRetryJob =
                scope.launch {
                    delay(CONNECTION_RETRY_DELAY_MS)
                    connectionRetryJob = null
                    startBillingConnection()
                }
            return true
        }

        fun launchPurchaseFlow(activity: Activity) {
            if (_purchaseFlowInFlight.value) return
            val details =
                _productDetails.value
                    ?: run {
                        emitPurchaseMessage(productUnavailableMessage())
                        return
                    }
            val offer =
                _selectedOffer.value
                    ?: run {
                        emitPurchaseMessage(productUnavailableMessage())
                        return
                    }

            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(details)
                    .apply {
                        if (offer.offerToken.isNotBlank()) {
                            setOfferToken(offer.offerToken)
                        }
                    }.build()

            val flowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

            _purchaseFlowInFlight.value = true
            val result =
                billingClient?.launchBillingFlow(activity, flowParams)
                    ?: run {
                        _purchaseFlowInFlight.value = false
                        emitPurchaseMessage(BillingUserMessage.PURCHASE_FAILED)
                        return
                    }
            applyPurchaseFlowResult(result)
        }

        fun restorePurchases() {
            scope.launch { queryPurchases() }
        }

        fun retryProductDetails() {
            _productStatus.value = BillingProductStatus.Loading
            scope.launch { queryProductDetails() }
        }

        suspend fun restorePurchasesWithResult(): RestorePurchasesResult {
            if (!awaitInitialBillingConnection()) return RestorePurchasesResult.FAILED

            // CPD-OFF: Ostohaun sama tila kasitellaan kahdessa elinkaarivaiheessa.
            return when (val result = queryPurchasesInternal()) {
                is PurchaseQueryResult.Success -> {
                    _isProPurchased.value = result.proPurchases.isNotEmpty()
                    result.proPurchases
                        .filter { !it.isAcknowledged }
                        .forEach { acknowledgePurchase(it) }
                    if (result.proPurchases.isNotEmpty()) {
                        // CPD-ON
                        RestorePurchasesResult.RESTORED
                    } else {
                        RestorePurchasesResult.NOT_FOUND
                    }
                }

                PurchaseQueryResult.Failure -> {
                    RestorePurchasesResult.FAILED
                }
            }
        }

        override fun onPurchasesUpdated(
            result: BillingResult,
            purchases: List<Purchase>?,
        ) {
            _purchaseFlowInFlight.value = false
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { purchase ->
                        if (purchase.products.contains(PRODUCT_ID)) {
                            when (purchase.purchaseState) {
                                Purchase.PurchaseState.PURCHASED -> {
                                    _isProPurchased.value = true
                                    acknowledgePurchase(purchase)
                                }

                                Purchase.PurchaseState.PENDING -> {
                                    emitPurchaseMessage(BillingUserMessage.PURCHASE_PENDING)
                                }

                                else -> Unit
                            }
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
            connectionRetryJob?.cancel()
            connectionRetryJob = null
            billingClient?.endConnection()
            billingClient = null
            acknowledgementsInFlight.clear()
            pendingAcknowledgementRetries.clear()
            _purchaseStateReady.value = false
            _purchaseFlowInFlight.value = false
        }

        internal suspend fun queryPurchases(): Boolean =
            when (val result = queryPurchasesInternal()) {
                is PurchaseQueryResult.Success -> {
                    _isProPurchased.value = result.proPurchases.isNotEmpty()
                    // Google Play palauttaa vahvistamattomat ostot 3 päivän jälkeen —
                    // varmista vahvistus joka käynnistyskerralla
                    result.proPurchases
                        .filter { !it.isAcknowledged }
                        .forEach { acknowledgePurchase(it) }
                    true
                }

                PurchaseQueryResult.Failure -> {
                    // Säilytä viimeksi tunnettu ostotila, jos Play-kysely epäonnistuu.
                    false
                }
            }

        private suspend fun awaitInitialBillingConnection(): Boolean {
            val client = billingClient ?: return false
            if (client.isReady || _purchaseStateReady.value) return true
            return withTimeoutOrNull(RESTORE_CONNECTION_WAIT_TIMEOUT_MS) {
                purchaseStateReady.first { it }
                true
            } ?: false
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
            val selectedOffer = details?.let(::selectOneTimePurchaseOffer)
            if (details == null || selectedOffer == null) {
                applyProductUnavailable(BillingUserMessage.PURCHASE_UNAVAILABLE)
            } else {
                _productDetails.value = details
                _selectedOffer.value = selectedOffer
                _productStatus.value = BillingProductStatus.Available
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private fun acknowledgePurchase(purchase: Purchase) {
            val purchaseToken = purchase.purchaseToken
            val isEligibleForAcknowledgement =
                !purchase.isAcknowledged &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(PRODUCT_ID)
            if (!isEligibleForAcknowledgement) return
            if (!canBeginAcknowledgement(purchaseToken)) return
            try {
                val params =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchaseToken)
                        .build()
                val client = billingClient
                if (client == null) {
                    acknowledgementsInFlight.remove(purchaseToken)
                    return
                }
                client.acknowledgePurchase(params) { result ->
                    acknowledgementsInFlight.remove(purchaseToken)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        acknowledgedPurchaseTokens.add(purchaseToken)
                        pendingAcknowledgementRetries.remove(purchaseToken)
                        acknowledgementRetryCounts.remove(purchaseToken)
                    } else if (shouldRetryAcknowledgement(result.responseCode)) {
                        scheduleAcknowledgementRetry(purchaseToken)
                    } else {
                        pendingAcknowledgementRetries.remove(purchaseToken)
                        acknowledgementRetryCounts.remove(purchaseToken)
                    }
                }
            } catch (e: CancellationException) {
                acknowledgementsInFlight.remove(purchaseToken)
                throw e
            } catch (_: Exception) {
                acknowledgementsInFlight.remove(purchaseToken)
                scheduleAcknowledgementRetry(purchaseToken)
            }
        }

        private fun canBeginAcknowledgement(purchaseToken: String): Boolean =
            purchaseToken.isNotBlank() &&
                purchaseToken !in acknowledgedPurchaseTokens &&
                purchaseToken !in pendingAcknowledgementRetries &&
                acknowledgementsInFlight.add(purchaseToken)

        private fun scheduleAcknowledgementRetry(purchaseToken: String) {
            if (!pendingAcknowledgementRetries.add(purchaseToken)) return
            val retryCount = (acknowledgementRetryCounts[purchaseToken] ?: 0) + 1
            if (retryCount > ACKNOWLEDGEMENT_MAX_RETRIES) {
                pendingAcknowledgementRetries.remove(purchaseToken)
                return
            }
            acknowledgementRetryCounts[purchaseToken] = retryCount
            scope.launch {
                delay(ACKNOWLEDGEMENT_RETRY_DELAY_MS)
                pendingAcknowledgementRetries.remove(purchaseToken)
                queryPurchases()
            }
        }

        private fun shouldRetryAcknowledgement(responseCode: Int): Boolean =
            responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR ||
                responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ||
                responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                responseCode == BillingClient.BillingResponseCode.ERROR ||
                responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED

        internal fun applyPurchaseFlowResult(result: BillingResult) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK) return
            _purchaseFlowInFlight.value = false
            emitPurchaseMessage(result.toUserMessage())
        }

        private fun applyProductUnavailable(message: BillingUserMessage) {
            _productDetails.value = null
            _selectedOffer.value = null
            _productStatus.value = BillingProductStatus.Unavailable(message)
        }

        private fun resetProductDetails() {
            _productDetails.value = null
            _selectedOffer.value = null
            _productStatus.value = BillingProductStatus.Loading
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
            private const val CONNECTION_MAX_ATTEMPTS = 3
            private const val CONNECTION_RETRY_DELAY_MS = 2_000L
            private const val ACKNOWLEDGEMENT_MAX_RETRIES = 3
            private const val ACKNOWLEDGEMENT_RETRY_DELAY_MS = 5_000L
            private const val RESTORE_CONNECTION_WAIT_TIMEOUT_MS = 2_000L
        }
    }

enum class RestorePurchasesResult {
    RESTORED,
    NOT_FOUND,
    FAILED,
}

enum class BillingUserMessage {
    PURCHASE_CANCELLED,
    PURCHASE_PENDING,
    PURCHASE_UNAVAILABLE,
    PURCHASE_NETWORK_ERROR,
    PURCHASE_FAILED,
    ALREADY_OWNED_RESTORE_FAILED,
}

data class SelectedOneTimeOffer(
    val formattedPrice: String,
    val offerToken: String,
)

internal fun selectOneTimePurchaseOffer(productDetails: ProductDetails): SelectedOneTimeOffer? {
    val currentOffers = productDetails.oneTimePurchaseOfferDetailsList.orEmpty()
    val selected =
        if (currentOffers.isEmpty()) {
            productDetails.oneTimePurchaseOfferDetails
        } else {
            currentOffers
                .filter { it.rentalDetails == null && it.preorderDetails == null }
                .sortedWith(
                    compareByDescending<ProductDetails.OneTimePurchaseOfferDetails> { it.offerId == null }
                        .thenByDescending { it.validTimeWindow == null }
                        .thenByDescending { it.limitedQuantityInfo == null }
                        .thenBy { it.purchaseOptionId.orEmpty() }
                        .thenBy { it.offerId.orEmpty() }
                        .thenBy { it.priceAmountMicros },
                ).firstOrNull()
        }
    return selected?.let {
        SelectedOneTimeOffer(
            formattedPrice = it.formattedPrice,
            offerToken = it.offerToken.orEmpty(),
        )
    }
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
