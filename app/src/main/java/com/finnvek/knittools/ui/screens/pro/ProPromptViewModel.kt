package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProPromptViewModel
    @Inject
    constructor(
        private val proManager: ProManager,
        private val trialManager: TrialManager,
        private val trialJourneyCoordinator: TrialJourneyCoordinator,
        private val billingManager: BillingManager,
    ) : ViewModel() {
        val proState = proManager.proState
        val selectedOffer = billingManager.selectedOffer
        val productStatus = billingManager.productStatus

        fun retryProductDetails() = billingManager.retryProductDetails()

        private val _trialStartResults = MutableSharedFlow<TrialStartResult>(extraBufferCapacity = 1)
        val trialStartResults = _trialStartResults.asSharedFlow()
        private val _isStartingTrial = MutableStateFlow(false)
        val isStartingTrial = _isStartingTrial.asStateFlow()

        private val _failedRequestId = MutableStateFlow<String?>(null)
        val failedRequestId = _failedRequestId.asStateFlow()

        fun startTrial(): String? {
            if (_isStartingTrial.value) return null
            val requestId = UUID.randomUUID().toString()
            _failedRequestId.value = null
            _isStartingTrial.value = true
            viewModelScope.launch {
                try {
                    val result = proManager.startTrial()
                    if (result == TrialStartResult.Started) trialJourneyCoordinator.requestStartedJourney()
                    if (result == TrialStartResult.Failed) _failedRequestId.value = requestId
                    _trialStartResults.emit(result)
                } finally {
                    _isStartingTrial.value = false
                }
            }
            return requestId
        }

        fun markContextualPromptShown() {
            viewModelScope.launch { trialManager.markTrialEndNoticeShown() }
        }
    }
