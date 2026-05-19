package com.finnvek.knittools.pro

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val _trialState = MutableStateFlow(TrialState())
        val trialState: StateFlow<TrialState> = _trialState.asStateFlow()
        private val refreshScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var refreshJob: Job? = null

        suspend fun initialize() {
            refreshTrialState()
            startRefreshLoop()
        }

        suspend fun updateTimestamp() {
            val prefs = context.trialDataStore.safePreferencesData.first()
            val lastKnown = prefs[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
            val now = System.currentTimeMillis()
            val nextLastKnown = calculateNextLastKnownTimestamp(now, lastKnown)

            context.trialDataStore.editPreferencesSafely("Kokeilujakson aikaleiman päivitys") {
                it[KEY_LAST_KNOWN_TIMESTAMP] = nextLastKnown
            }
        }

        private suspend fun refreshTrialState() {
            val prefs = context.trialDataStore.safePreferencesData.first()
            val startTimestamp = prefs[KEY_TRIAL_START] ?: 0L
            val lastKnown = prefs[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
            val clockTamperedAlready = prefs[KEY_CLOCK_TAMPERED] ?: false
            val now = System.currentTimeMillis()

            val isFirstLaunch = startTimestamp == 0L
            val actualStartTimestamp =
                if (isFirstLaunch) {
                    now
                } else {
                    startTimestamp
                }
            val state =
                calculateTrialState(
                    now = now,
                    startTimestamp = actualStartTimestamp,
                    lastKnownTimestamp = lastKnown,
                    isFirstLaunch = isFirstLaunch,
                    clockTamperedAlready = clockTamperedAlready,
                )
            val nextLastKnown = calculateNextLastKnownTimestamp(now, lastKnown)

            context.trialDataStore.editPreferencesSafely("Kokeilujakson tilan tallennus") {
                if (isFirstLaunch) {
                    it[KEY_TRIAL_START] = actualStartTimestamp
                }
                it[KEY_LAST_KNOWN_TIMESTAMP] = nextLastKnown
                if (state.clockTampered) {
                    it[KEY_CLOCK_TAMPERED] = true
                }
            }

            _trialState.value = state
        }

        private fun startRefreshLoop() {
            if (refreshJob?.isActive == true) return

            refreshJob =
                refreshScope.launch {
                    while (isActive) {
                        val currentState = _trialState.value
                        delay(
                            calculateTrialRefreshDelayMillis(
                                now = System.currentTimeMillis(),
                                startTimestamp = currentState.startTimestamp,
                            ),
                        )
                        refreshTrialState()
                    }
                }
        }

        companion object {
            const val TRIAL_DURATION_DAYS = 14
            private val Context.trialDataStore by preferencesDataStore(
                name = "trial_state",
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            )
            private val KEY_TRIAL_START = longPreferencesKey("trial_start_timestamp")
            private val KEY_LAST_KNOWN_TIMESTAMP = longPreferencesKey("last_known_timestamp")
            private val KEY_CLOCK_TAMPERED = booleanPreferencesKey("clock_tampered")
            private val TRIAL_REFRESH_POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)
            private const val MIN_REFRESH_DELAY_MS = 1L

            @VisibleForTesting
            internal fun calculateNextLastKnownTimestamp(
                now: Long,
                lastKnownTimestamp: Long,
            ): Long = maxOf(now, lastKnownTimestamp)

            @VisibleForTesting
            internal fun calculateTrialRefreshDelayMillis(
                now: Long,
                startTimestamp: Long,
            ): Long {
                if (startTimestamp <= 0L) return TRIAL_REFRESH_POLL_INTERVAL_MS

                val elapsedMillis = now - startTimestamp
                if (elapsedMillis < 0L) return TRIAL_REFRESH_POLL_INTERVAL_MS

                val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis)
                val nextDayBoundaryMillis = TimeUnit.DAYS.toMillis(elapsedDays + 1)
                val millisUntilNextDayBoundary =
                    (nextDayBoundaryMillis - elapsedMillis).coerceAtLeast(MIN_REFRESH_DELAY_MS)
                return minOf(TRIAL_REFRESH_POLL_INTERVAL_MS, millisUntilNextDayBoundary)
            }

            // Puhdas laskentalogiikka erotettuna DataStore-I/O:sta testattavuuden vuoksi
            @VisibleForTesting
            internal fun calculateTrialState(
                now: Long,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                isFirstLaunch: Boolean,
                clockTamperedAlready: Boolean = false,
            ): TrialState {
                val clockTampered =
                    clockTamperedAlready ||
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
