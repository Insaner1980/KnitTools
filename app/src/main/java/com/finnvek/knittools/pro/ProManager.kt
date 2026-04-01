package com.finnvek.knittools.pro

import com.finnvek.knittools.billing.BillingManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProManager
    @Inject
    constructor(
        private val trialManager: TrialManager,
        private val billingManager: BillingManager,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _proState = MutableStateFlow(ProState())
        val proState: StateFlow<ProState> = _proState.asStateFlow()
        val isProUser: Flow<Boolean> =
            proState
                .map { it.isPro }
                .distinctUntilChanged()

        private var initialized = false

        @Suppress("TooGenericExceptionCaught")
        fun initialize() {
            if (initialized) return
            initialized = true
            scope.launch {
                try {
                    trialManager.initialize()

                    combine(
                        trialManager.trialState,
                        billingManager.isProPurchased,
                    ) { trial, isPurchased ->
                        when {
                            isPurchased ->
                                ProState(
                                    status = ProStatus.PRO_PURCHASED,
                                    trialDaysRemaining = 0,
                                    trialStartTimestamp = trial.startTimestamp,
                                    purchaseTimestamp = System.currentTimeMillis(),
                                )

                            trial.isActive ->
                                ProState(
                                    status = ProStatus.TRIAL_ACTIVE,
                                    trialDaysRemaining = trial.daysRemaining,
                                    trialStartTimestamp = trial.startTimestamp,
                                )

                            else ->
                                ProState(
                                    status = ProStatus.TRIAL_EXPIRED,
                                    trialDaysRemaining = 0,
                                    trialStartTimestamp = trial.startTimestamp,
                                )
                        }
                    }.collect { state ->
                        _proState.value = state
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Pro state initialization failed — default to expired
                }
            }
        }

        fun hasFeature(feature: ProFeature): Boolean = _proState.value.hasFeature(feature)

        fun isPro(): Boolean = _proState.value.isPro
    }
