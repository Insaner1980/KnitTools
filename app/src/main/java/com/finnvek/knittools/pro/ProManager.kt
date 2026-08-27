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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
        private val _initialStateReady = MutableStateFlow(false)
        val initialStateReady: StateFlow<Boolean> = _initialStateReady.asStateFlow()
        val isProUser: Flow<Boolean> =
            proState
                .map { it.isPro }
                .distinctUntilChanged()

        private var initialized = false

        @Suppress("TooGenericExceptionCaught")
        fun initialize() {
            if (initialized) return
            initialized = true
            _initialStateReady.value = false
            scope.launch {
                try {
                    trialManager.initialize()

                    combine(
                        trialManager.trialState,
                        billingManager.isProPurchased,
                        billingManager.purchaseStateReady,
                    ) { trial, isPurchased, purchaseStateReady ->
                        val state =
                            when {
                                isPurchased -> {
                                    ProState(
                                        status = ProStatus.PRO_PURCHASED,
                                        trialDaysRemaining = 0,
                                        trialStartTimestamp = trial.startTimestamp,
                                        purchaseTimestamp = System.currentTimeMillis(),
                                    )
                                }

                                trial.isActive -> {
                                    ProState(
                                        status = ProStatus.TRIAL_ACTIVE,
                                        trialDaysRemaining = trial.daysRemaining,
                                        trialStartTimestamp = trial.startTimestamp,
                                    )
                                }

                                !trial.hasStarted -> {
                                    ProState(status = ProStatus.TRIAL_NOT_STARTED)
                                }

                                else -> {
                                    ProState(
                                        status = ProStatus.TRIAL_EXPIRED,
                                        trialDaysRemaining = 0,
                                        trialStartTimestamp = trial.startTimestamp,
                                    )
                                }
                            }
                        state to purchaseStateReady
                    }.collect { (state, purchaseStateReady) ->
                        _proState.value = state
                        _initialStateReady.value = purchaseStateReady
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _initialStateReady.value = false
                    initialized = false
                }
            }
        }

        fun hasFeature(feature: ProFeature): Boolean = _proState.value.hasFeature(feature)

        suspend fun startTrial(): TrialStartResult = trialManager.startTrial()

        suspend fun hasFeatureAfterInitialLoad(feature: ProFeature): Boolean {
            waitForInitialProState()
            waitForInitialPurchaseState()
            return billingManager.isProPurchased.value || hasFeature(feature)
        }

        fun hasFeatureFlow(feature: ProFeature): Flow<Boolean> =
            proState
                .map { it.hasFeature(feature) }
                .distinctUntilChanged()

        fun isPro(): Boolean = _proState.value.isPro

        private suspend fun waitForInitialPurchaseState() {
            if (billingManager.purchaseStateReady.value) return
            withTimeoutOrNull(INITIAL_STATE_WAIT_TIMEOUT_MS) {
                billingManager.purchaseStateReady.first { it }
            }
        }

        private suspend fun waitForInitialProState() {
            if (_initialStateReady.value) return
            withTimeoutOrNull(INITIAL_STATE_WAIT_TIMEOUT_MS) {
                initialStateReady.first { it }
            }
        }

        private companion object {
            private const val INITIAL_STATE_WAIT_TIMEOUT_MS = 2_000L
        }
    }
