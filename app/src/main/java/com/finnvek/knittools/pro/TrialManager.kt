package com.finnvek.knittools.pro

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TrialState(
    val isActive: Boolean = false,
    val daysRemaining: Int = 0,
    val startTimestamp: Long = 0L,
    val isFirstLaunch: Boolean = false,
    val clockTampered: Boolean = false,
)

@Singleton
class TrialManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val _trialState = MutableStateFlow(TrialState())
        val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

        suspend fun initialize() {
            val prefs = context.trialDataStore.data.first()
            val startTimestamp = prefs[KEY_TRIAL_START] ?: 0L
            val lastKnown = prefs[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
            val now = System.currentTimeMillis()

            context.trialDataStore.edit { it[KEY_LAST_KNOWN_TIMESTAMP] = now }

            val isFirstLaunch = startTimestamp == 0L
            val actualStart =
                if (isFirstLaunch) {
                    context.trialDataStore.edit { it[KEY_TRIAL_START] = now }
                    now
                } else {
                    startTimestamp
                }

            _trialState.value = calculateTrialState(now, actualStart, lastKnown, isFirstLaunch)
        }

        suspend fun updateTimestamp() {
            context.trialDataStore.edit {
                it[KEY_LAST_KNOWN_TIMESTAMP] = System.currentTimeMillis()
            }
        }

        companion object {
            const val TRIAL_DURATION_DAYS = 7
            private val Context.trialDataStore by preferencesDataStore(name = "trial_state")
            private val KEY_TRIAL_START = longPreferencesKey("trial_start_timestamp")
            private val KEY_LAST_KNOWN_TIMESTAMP = longPreferencesKey("last_known_timestamp")

            // Puhdas laskentalogiikka erotettuna DataStore-I/O:sta testattavuuden vuoksi
            @VisibleForTesting
            internal fun calculateTrialState(
                now: Long,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                isFirstLaunch: Boolean,
            ): TrialState {
                val clockTampered =
                    lastKnownTimestamp > 0L &&
                        now < lastKnownTimestamp - TimeUnit.HOURS.toMillis(1)
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(now - startTimestamp).toInt()
                val daysRemaining = (TRIAL_DURATION_DAYS - daysElapsed).coerceAtLeast(0)
                val isActive = daysRemaining > 0 && !clockTampered
                return TrialState(
                    isActive = isActive,
                    daysRemaining = daysRemaining,
                    startTimestamp = startTimestamp,
                    isFirstLaunch = isFirstLaunch,
                    clockTampered = clockTampered,
                )
            }
        }
    }
