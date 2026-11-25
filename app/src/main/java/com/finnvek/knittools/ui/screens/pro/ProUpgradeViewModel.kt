package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.BillingUserMessage
import com.finnvek.knittools.billing.RestorePurchasesResult
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.TrialStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProUpgradeViewModel
    @Inject
    constructor(
        private val proManager: ProManager,
        private val billingManager: BillingManager,
        private val trialJourneyCoordinator: TrialJourneyCoordinator,
    ) : ViewModel() {
        val proState: StateFlow<ProState> = proManager.proState
        val proStateReady: StateFlow<Boolean> = proManager.initialStateReady
        val controlsReady: StateFlow<Boolean> = proManager.initialControlsReady
        val purchaseQueryFailed: StateFlow<Boolean> = billingManager.purchaseQueryFailed
        val selectedOffer: StateFlow<SelectedOneTimeOffer?> = billingManager.selectedOffer
        val productStatus: StateFlow<BillingProductStatus> = billingManager.productStatus
        val purchaseFlowInFlight: StateFlow<Boolean> = billingManager.purchaseFlowInFlight

        private val _statusMessageRes = MutableStateFlow<Int?>(null)
        val statusMessageRes: StateFlow<Int?> = _statusMessageRes.asStateFlow()

        private val _isRestoring = MutableStateFlow(false)
        val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()
        private val _isStartingTrial = MutableStateFlow(false)
        val isStartingTrial: StateFlow<Boolean> = _isStartingTrial.asStateFlow()

        init {
            viewModelScope.launch {
                billingManager.purchaseMessages.collect { message ->
                    _statusMessageRes.value = message.toMessageRes()
                }
            }
        }

        fun retryPurchaseState() = billingManager.refreshPurchasesOnResume()

        fun restorePurchases() {
            if (_isRestoring.value) return
            _isRestoring.value = true
            viewModelScope.launch {
                try {
                    _statusMessageRes.value =
                        when (billingManager.restorePurchasesWithResult()) {
                            RestorePurchasesResult.RESTORED -> com.finnvek.knittools.R.string.pro_restored
                            RestorePurchasesResult.NOT_FOUND -> com.finnvek.knittools.R.string.no_purchases_found
                            RestorePurchasesResult.FAILED -> com.finnvek.knittools.R.string.generic_error_unknown
                        }
                } finally {
                    _isRestoring.value = false
                }
            }
        }

        fun startTrial() {
            if (_isStartingTrial.value) return
            _isStartingTrial.value = true
            viewModelScope.launch {
                try {
                    val result = proManager.startTrial()
                    if (result == TrialStartResult.Started) trialJourneyCoordinator.requestStartedJourney()
                    _statusMessageRes.value =
                        if (result == TrialStartResult.Failed) {
                            com.finnvek.knittools.R.string.pro_trial_start_failed
                        } else {
                            null
                        }
                } finally {
                    _isStartingTrial.value = false
                }
            }
        }

        fun retryProductDetails() {
            billingManager.retryProductDetails()
        }

        fun onPriceViewResumed() {
            billingManager.onPriceViewResumed()
        }
    }

internal fun BillingUserMessage.toMessageRes(): Int =
    when (this) {
        BillingUserMessage.PRICE_REFRESHING -> {
            com.finnvek.knittools.R.string.billing_price_refreshing
        }

        BillingUserMessage.PRICE_CHANGED_REVIEW -> {
            com.finnvek.knittools.R.string.pro_price_changed_review
        }

        BillingUserMessage.PURCHASE_CANCELLED -> {
            com.finnvek.knittools.R.string.billing_purchase_cancelled
        }

        BillingUserMessage.PURCHASE_PENDING -> {
            com.finnvek.knittools.R.string.billing_purchase_pending
        }

        BillingUserMessage.PURCHASE_UNAVAILABLE -> {
            com.finnvek.knittools.R.string.billing_purchase_unavailable
        }

        BillingUserMessage.PURCHASE_NETWORK_ERROR -> {
            com.finnvek.knittools.R.string.billing_purchase_network_error
        }

        BillingUserMessage.PURCHASE_FAILED -> {
            com.finnvek.knittools.R.string.billing_purchase_failed
        }

        BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED -> {
            com.finnvek.knittools.R.string.billing_already_owned_restore_failed
        }
    }
