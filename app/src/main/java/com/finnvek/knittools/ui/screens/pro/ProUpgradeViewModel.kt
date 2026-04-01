package com.finnvek.knittools.ui.screens.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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

        fun purchase(activity: Activity) {
            billingManager.launchPurchaseFlow(activity)
        }

        fun restorePurchases() {
            billingManager.restorePurchases()
        }
    }
