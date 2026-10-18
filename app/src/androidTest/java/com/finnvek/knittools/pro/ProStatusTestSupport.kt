package com.finnvek.knittools.pro

internal fun setTestProStatus(state: ProStatus) {
    ProPolicyTestControl.setPurchased(state == ProStatus.PRO_PURCHASED)
    ProPolicyTestControl.setTrialState(
        TrialState(
            hasStarted = state != ProStatus.TRIAL_NOT_STARTED,
            isActive =
                state == ProStatus.TRIAL_ACTIVE,
            daysRemaining = if (state == ProStatus.TRIAL_ACTIVE) 7 else 0,
        ),
    )
}
