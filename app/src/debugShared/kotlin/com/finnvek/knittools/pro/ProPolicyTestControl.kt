package com.finnvek.knittools.pro

import com.finnvek.knittools.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Debug-buildin ohjaus erilliselle, verkkoluvattomalle policy-testipaketille (`.activepolicytest`,
 * ks. `knittools.isolatedTest`). Paketissa ei ole BILLING-oikeutta, joten testi asettaa osto- ja
 * kokeilutilan tästä. Release-buildin vastine välittää oikeat arvot sellaisenaan eikä sisällä asettajia.
 */
internal object ProPolicyTestControl {
    /** Policy-testipaketti aloittaa tyhjästä ja ilman debugin Pro-avausta. */
    val isActive: Boolean = BuildConfig.ACTIVE_POLICY_TEST

    private val purchasedOverride = MutableStateFlow<Boolean?>(null)
    private val trialStateOverride = MutableStateFlow<TrialState?>(null)

    @Volatile
    private var trialStartResult: TrialStartResult? = null

    fun trialState(source: Flow<TrialState>): Flow<TrialState> =
        combine(source, trialStateOverride) { real, override -> override ?: real }

    fun purchased(source: Flow<Boolean>): Flow<Boolean> =
        combine(source, purchasedOverride) { real, override -> override ?: real }

    fun purchaseStateReady(source: Flow<Boolean>): Flow<Boolean> =
        combine(source, purchasedOverride) { real, override -> real || override != null }

    fun currentPurchased(real: Boolean): Boolean = purchasedOverride.value ?: real

    fun currentPurchaseStateReady(real: Boolean): Boolean = real || purchasedOverride.value != null

    suspend fun startTrial(real: suspend () -> TrialStartResult): TrialStartResult {
        val result = trialStartResult ?: return real()
        if (result == TrialStartResult.Started || result == TrialStartResult.AlreadyActive) {
            trialStateOverride.value =
                TrialState(isActive = true, daysRemaining = TrialManager.TRIAL_DURATION_DAYS, hasStarted = true)
        }
        return result
    }

    fun reset() {
        requirePurchaseControl()
        purchasedOverride.value = null
        trialStateOverride.value = null
        trialStartResult = null
    }

    fun setPurchased(value: Boolean) {
        requirePurchaseControl()
        purchasedOverride.value = value
    }

    fun setTrialState(state: TrialState) {
        requireActive()
        trialStateOverride.value = state
    }

    fun setTrialStartResult(result: TrialStartResult) {
        requireActive()
        trialStartResult = result
    }

    private fun requirePurchaseControl() = check(isActive || BuildConfig.ONBOARDING_TEST)

    private fun requireActive() = check(isActive) { "Pro-tilan ohjaus on sallittu vain policy-testipaketissa" }
}
