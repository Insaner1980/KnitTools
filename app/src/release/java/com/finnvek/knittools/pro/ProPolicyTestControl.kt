package com.finnvek.knittools.pro

import kotlinx.coroutines.flow.Flow

/** Releasessa ei ole policy-testipakettia: oikeat osto- ja kokeilutilat kulkevat sellaisenaan. */
internal object ProPolicyTestControl {
    val isActive: Boolean = false

    fun trialState(source: Flow<TrialState>): Flow<TrialState> = source

    fun purchased(source: Flow<Boolean>): Flow<Boolean> = source

    fun purchaseStateReady(source: Flow<Boolean>): Flow<Boolean> = source

    fun currentPurchased(real: Boolean): Boolean = real

    fun currentPurchaseStateReady(real: Boolean): Boolean = real

    suspend fun startTrial(real: suspend () -> TrialStartResult): TrialStartResult = real()
}
