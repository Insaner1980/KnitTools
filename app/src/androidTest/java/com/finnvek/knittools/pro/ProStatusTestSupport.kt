package com.finnvek.knittools.pro

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.finnvek.knittools.domain.model.SessionTimeSnapshot

internal suspend fun DataStore<Preferences>.seedTrialClock(
    now: SessionTimeSnapshot,
    elapsed: Long,
) {
    edit {
        it.clear()
        it[longPreferencesKey("trial_start_timestamp")] = now.wallClockMillis - elapsed
        it[longPreferencesKey("last_known_timestamp")] = now.wallClockMillis
        it[longPreferencesKey("trial_elapsed_duration_millis")] = elapsed
        it[longPreferencesKey("trial_anchor_wall_clock_millis")] = now.wallClockMillis
        it[longPreferencesKey("trial_anchor_elapsed_realtime_millis")] = now.elapsedRealtimeMillis
        it[longPreferencesKey("trial_anchor_boot_count")] = checkNotNull(now.bootCount)
    }
}

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
