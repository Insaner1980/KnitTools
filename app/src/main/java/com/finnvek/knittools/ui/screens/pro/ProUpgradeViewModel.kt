package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.RestorePurchasesResult
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
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
    ) : ViewModel() {
        val proState: StateFlow<ProState> = proManager.proState
        val productDetails: StateFlow<ProductDetails?> = billingManager.productDetails

        private val _restoreMessageRes = MutableStateFlow<Int?>(null)
        val restoreMessageRes: StateFlow<Int?> = _restoreMessageRes.asStateFlow()

        private val _isRestoring = MutableStateFlow(false)
        val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

        fun restorePurchases() {
            viewModelScope.launch {
                _isRestoring.value = true
                try {
                    _restoreMessageRes.value =
                        when (billingManager.restorePurchasesWithResult()) {
                            RestorePurchasesResult.RESTORED -> com.finnvek.knittools.R.string.pro_restored
                            RestorePurchasesResult.NOT_FOUND -> com.finnvek.knittools.R.string.no_purchases_found
                        }
                } finally {
                    _isRestoring.value = false
                }
            }
        }
    }
