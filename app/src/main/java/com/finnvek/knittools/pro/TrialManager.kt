package com.finnvek.knittools.pro

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.data.time.UnavailableBootSessionTimeSource
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TrialState(
    val isActive: Boolean = false,
    val daysRemaining: Int = 0,
    val startTimestamp: Long = 0L,
    val hasStarted: Boolean = false,
    val clockTampered: Boolean = false,
    val elapsedDurationMillis: Long = 0L,
    val isAvailable: Boolean = true,
)

enum class TrialStartResult {
    Started,
    AlreadyActive,
    AlreadyExpired,
    AlreadyTampered,
    Failed,
}

internal sealed interface StoredTrialTiming {
    data object Legacy : StoredTrialTiming

    data object Malformed : StoredTrialTiming

    data class Anchored(
        val elapsedDurationMillis: Long,
        val anchorWallClockMillis: Long,
        val anchorElapsedRealtimeMillis: Long,
        val anchorBootCount: Long?,
    ) : StoredTrialTiming
}

internal data class TrialTimingEvaluation(
    val state: TrialState,
    val anchors: StoredTrialTiming.Anchored?,
    val lastKnownTimestamp: Long,
)

@Singleton
class TrialManager
    internal constructor(
        private val dataStore: DataStore<Preferences>,
        private val ioDispatcher: CoroutineDispatcher,
        private val sessionTimeSource: SessionTimeSource,
    ) {
        @Inject
        constructor(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
            sessionTimeSource: SessionTimeSource = UnavailableBootSessionTimeSource,
        ) : this(context.trialDataStore, ioDispatcher, sessionTimeSource)

        private val _trialState = MutableStateFlow(TrialState(isAvailable = false))
        val trialState: StateFlow<TrialState> = _trialState.asStateFlow()
        private var lastKnownEvaluation: TrialTimingEvaluation? = null
        private val refreshScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var refreshJob: Job? = null
        private val refreshLoopMutex = Mutex()
        private val stateMutex = Mutex()

        suspend fun initialize() {
            startRefreshLoop(refreshTrialState())
        }

        suspend fun startTrial(): TrialStartResult =
            stateMutex.withLock {
                var startResult: TrialStartResult? = null
                var evaluation: TrialTimingEvaluation? = null
                val didWrite =
                    dataStore.editPreferencesSafely { preferences ->
                        val now = withContext(ioDispatcher) { sessionTimeSource.snapshot() }
                        val startTimestamp = preferences[KEY_TRIAL_START] ?: 0L
                        val lastKnownTimestamp = preferences[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
                        val clockTamperedAlready = preferences[KEY_CLOCK_TAMPERED] ?: false
                        val storedTiming = preferences.readStoredTrialTiming()
                        val cleanUnstarted =
                            isCleanUnstartedState(
                                startTimestamp = startTimestamp,
                                lastKnownTimestamp = lastKnownTimestamp,
                                clockTamperedAlready = clockTamperedAlready,
                                storedTiming = storedTiming,
                            )
                        if (cleanUnstarted && isValidTimeSnapshot(now)) {
                            preferences[KEY_TRIAL_START] = now.wallClockMillis
                            val state = createTrialState(now.wallClockMillis, 0L, clockTampered = false)
                            val started =
                                TrialTimingEvaluation(
                                    state = state,
                                    anchors =
                                        StoredTrialTiming.Anchored(
                                            elapsedDurationMillis = 0L,
                                            anchorWallClockMillis = now.wallClockMillis,
                                            anchorElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                                            anchorBootCount = now.bootCount,
                                        ),
                                    lastKnownTimestamp = now.wallClockMillis,
                                )
                            preferences.persistTrialEvaluation(started)
                            evaluation = started
                            startResult = TrialStartResult.Started
                        } else if (cleanUnstarted) {
                            startResult = TrialStartResult.Failed
                        } else {
                            val existing = preferences.evaluateAndPersistTrialState(now)
                            evaluation = existing
                            startResult = classifyExistingTrial(existing.state)
                        }
                    }
                if (!didWrite) return@withLock TrialStartResult.Failed

                evaluation?.let(::publishTrialEvaluation)
                startResult ?: TrialStartResult.Failed
            }

        suspend fun claimTrialEndNotice(): Boolean {
            var claimed = false
            val didWrite =
                dataStore.editPreferencesSafely { preferences ->
                    val hasStarted = (preferences[KEY_TRIAL_START] ?: 0L) > 0L
                    val wasShown = preferences[KEY_TRIAL_END_NOTICE_SHOWN] ?: false
                    if (hasStarted && !wasShown) {
                        preferences[KEY_TRIAL_END_NOTICE_SHOWN] = true
                        claimed = true
                    }
                }
            return didWrite && claimed
        }

        suspend fun claimTrialThreeDayNotice(): Boolean =
            stateMutex.withLock {
                var claimed = false
                var evaluation: TrialTimingEvaluation? = null
                val didWrite =
                    dataStore.editPreferencesSafely { preferences ->
                        val now = withContext(ioDispatcher) { sessionTimeSource.snapshot() }
                        val current = preferences.evaluateAndPersistTrialState(now)
                        evaluation = current
                        val state = current.state
                        if (state.isActive &&
                            !state.clockTampered &&
                            state.daysRemaining == 3 &&
                            preferences[KEY_TRIAL_THREE_DAY_NOTICE_SHOWN] != true
                        ) {
                            preferences[KEY_TRIAL_THREE_DAY_NOTICE_SHOWN] = true
                            claimed = true
                        }
                    }
                if (didWrite) evaluation?.let(::publishTrialEvaluation)
                didWrite && claimed
            }

        suspend fun markTrialEndNoticeShown() {
            dataStore.editPreferencesSafely { preferences ->
                if ((preferences[KEY_TRIAL_START] ?: 0L) > 0L) {
                    preferences[KEY_TRIAL_END_NOTICE_SHOWN] = true
                }
            }
        }

        private suspend fun refreshTrialState() = stateMutex.withLock { refreshTrialStateLocked() }

        private suspend fun refreshTrialStateLocked(): Boolean {
            var evaluation: TrialTimingEvaluation? = null
            val didWrite =
                dataStore.editPreferencesSafely { preferences ->
                    val now = withContext(ioDispatcher) { sessionTimeSource.snapshot() }
                    evaluation = preferences.evaluateAndPersistTrialState(now)
                }
            if (didWrite) {
                evaluation?.let(::publishTrialEvaluation)
            } else {
                _trialState.value = evaluation?.state ?: evaluateLastKnownTrial()
            }
            return didWrite
        }

        private fun MutablePreferences.evaluateAndPersistTrialState(now: SessionTimeSnapshot): TrialTimingEvaluation {
            val evaluation =
                evaluateTrialTiming(
                    now = now,
                    startTimestamp = this[KEY_TRIAL_START] ?: 0L,
                    lastKnownTimestamp = this[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L,
                    clockTamperedAlready = this[KEY_CLOCK_TAMPERED] == true || _trialState.value.clockTampered,
                    storedTiming = readStoredTrialTiming(),
                )
            persistTrialEvaluation(evaluation)
            return evaluation
        }

        private fun publishTrialEvaluation(evaluation: TrialTimingEvaluation) {
            lastKnownEvaluation = evaluation
            _trialState.value = evaluation.state
        }

        private suspend fun evaluateLastKnownTrial(): TrialState {
            val lastKnown = lastKnownEvaluation ?: return TrialState(isAvailable = false)
            if (!lastKnown.state.hasStarted) return lastKnown.state
            if (_trialState.value.clockTampered) return _trialState.value
            val now = withContext(ioDispatcher) { sessionTimeSource.snapshot() }
            return evaluateTrialTiming(
                now = now,
                startTimestamp = lastKnown.state.startTimestamp,
                lastKnownTimestamp = lastKnown.lastKnownTimestamp,
                clockTamperedAlready = lastKnown.state.clockTampered,
                storedTiming = lastKnown.anchors ?: StoredTrialTiming.Malformed,
            ).state
        }

        private fun MutablePreferences.readStoredTrialTiming(): StoredTrialTiming {
            val hasElapsedDuration = contains(KEY_TRIAL_ELAPSED_DURATION)
            val hasAnchorWallClock = contains(KEY_TRIAL_ANCHOR_WALL_CLOCK)
            val hasAnchorElapsedRealtime = contains(KEY_TRIAL_ANCHOR_ELAPSED_REALTIME)
            val hasAnchorBootCount = contains(KEY_TRIAL_ANCHOR_BOOT_COUNT)
            if (!hasElapsedDuration && !hasAnchorWallClock && !hasAnchorElapsedRealtime && !hasAnchorBootCount) {
                return StoredTrialTiming.Legacy
            }
            if (!hasElapsedDuration || !hasAnchorWallClock || !hasAnchorElapsedRealtime) {
                return StoredTrialTiming.Malformed
            }
            return StoredTrialTiming.Anchored(
                elapsedDurationMillis = checkNotNull(this[KEY_TRIAL_ELAPSED_DURATION]),
                anchorWallClockMillis = checkNotNull(this[KEY_TRIAL_ANCHOR_WALL_CLOCK]),
                anchorElapsedRealtimeMillis = checkNotNull(this[KEY_TRIAL_ANCHOR_ELAPSED_REALTIME]),
                anchorBootCount = this[KEY_TRIAL_ANCHOR_BOOT_COUNT],
            )
        }

        private fun MutablePreferences.persistTrialEvaluation(evaluation: TrialTimingEvaluation) {
            this[KEY_LAST_KNOWN_TIMESTAMP] = evaluation.lastKnownTimestamp
            this[KEY_CLOCK_TAMPERED] = evaluation.state.clockTampered
            evaluation.anchors?.let { anchors ->
                this[KEY_TRIAL_ELAPSED_DURATION] = anchors.elapsedDurationMillis
                this[KEY_TRIAL_ANCHOR_WALL_CLOCK] = anchors.anchorWallClockMillis
                this[KEY_TRIAL_ANCHOR_ELAPSED_REALTIME] = anchors.anchorElapsedRealtimeMillis
                anchors.anchorBootCount?.let {
                    this[KEY_TRIAL_ANCHOR_BOOT_COUNT] = it
                } ?: remove(KEY_TRIAL_ANCHOR_BOOT_COUNT)
            }
        }

        private suspend fun startRefreshLoop(initialRefreshSucceeded: Boolean) =
            refreshLoopMutex.withLock {
                if (refreshJob?.isActive == true) return@withLock

                refreshJob =
                    refreshScope.launch {
                        var refreshSucceeded = initialRefreshSucceeded
                        while (isActive) {
                            val currentState = _trialState.value
                            val normalDelay = calculateTrialRefreshDelayMillis(currentState.elapsedDurationMillis)
                            delay(if (refreshSucceeded) normalDelay else minOf(normalDelay, TRIAL_REFRESH_RETRY_MS))
                            refreshSucceeded = refreshTrialState()
                        }
                    }
            }

        @Suppress("TooManyFunctions") // Trialin puhtaat aikasiirtymät pidetään yhden auditoitavan rajan sisällä.
        companion object {
            const val TRIAL_DURATION_DAYS = 14
            private val KEY_TRIAL_START = longPreferencesKey("trial_start_timestamp")
            private val KEY_LAST_KNOWN_TIMESTAMP = longPreferencesKey("last_known_timestamp")
            private val KEY_CLOCK_TAMPERED = booleanPreferencesKey("clock_tampered")
            private val KEY_TRIAL_END_NOTICE_SHOWN = booleanPreferencesKey("trial_end_notice_shown")
            private val KEY_TRIAL_THREE_DAY_NOTICE_SHOWN = booleanPreferencesKey("trial_three_day_notice_shown")
            private val KEY_TRIAL_ELAPSED_DURATION = longPreferencesKey("trial_elapsed_duration_millis")
            private val KEY_TRIAL_ANCHOR_WALL_CLOCK = longPreferencesKey("trial_anchor_wall_clock_millis")
            private val KEY_TRIAL_ANCHOR_ELAPSED_REALTIME =
                longPreferencesKey("trial_anchor_elapsed_realtime_millis")
            private val KEY_TRIAL_ANCHOR_BOOT_COUNT = longPreferencesKey("trial_anchor_boot_count")
            private val Context.trialDataStore by preferencesDataStore(
                name = "trial_state",
                corruptionHandler =
                    ReplaceFileCorruptionHandler {
                        preferencesOf(KEY_CLOCK_TAMPERED to true)
                    },
            )
            private val TRIAL_REFRESH_POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)
            private val TRIAL_REFRESH_RETRY_MS = TimeUnit.SECONDS.toMillis(30)
            private val ROLLBACK_TOLERANCE_MILLIS = TimeUnit.HOURS.toMillis(1)
            private val TRIAL_DURATION_MILLIS = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS.toLong())
            private const val MIN_REFRESH_DELAY_MS = 1L

            @VisibleForTesting
            internal fun calculateTrialRefreshDelayMillis(elapsedDurationMillis: Long): Long {
                if (elapsedDurationMillis < 0L) return MIN_REFRESH_DELAY_MS
                if (elapsedDurationMillis >= TRIAL_DURATION_MILLIS) return TRIAL_REFRESH_POLL_INTERVAL_MS

                val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedDurationMillis)
                val nextDayBoundaryMillis = TimeUnit.DAYS.toMillis(elapsedDays + 1L)
                val millisUntilNextDayBoundary =
                    (nextDayBoundaryMillis - elapsedDurationMillis).coerceAtLeast(MIN_REFRESH_DELAY_MS)
                return minOf(TRIAL_REFRESH_POLL_INTERVAL_MS, millisUntilNextDayBoundary)
            }

            // Puhdas laskentalogiikka erotettuna DataStore-I/O:sta testattavuuden vuoksi.
            @VisibleForTesting
            internal fun classifyExistingTrial(state: TrialState): TrialStartResult =
                when {
                    state.clockTampered -> TrialStartResult.AlreadyTampered
                    state.isActive -> TrialStartResult.AlreadyActive
                    else -> TrialStartResult.AlreadyExpired
                }

            @VisibleForTesting
            internal fun evaluateTrialTiming(
                now: SessionTimeSnapshot,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                clockTamperedAlready: Boolean = false,
                storedTiming: StoredTrialTiming = StoredTrialTiming.Legacy,
            ): TrialTimingEvaluation {
                val nextLastKnownTimestamp =
                    if (now.wallClockMillis >= 0L && lastKnownTimestamp >= 0L) {
                        maxOf(now.wallClockMillis, lastKnownTimestamp)
                    } else {
                        maxOf(now.wallClockMillis, 0L)
                    }
                if (
                    isCleanUnstartedState(
                        startTimestamp = startTimestamp,
                        lastKnownTimestamp = lastKnownTimestamp,
                        clockTamperedAlready = clockTamperedAlready,
                        storedTiming = storedTiming,
                    )
                ) {
                    return TrialTimingEvaluation(
                        state = TrialState(),
                        anchors = null,
                        lastKnownTimestamp = nextLastKnownTimestamp,
                    )
                }
                if (
                    hasInvalidEvaluationInput(
                        now = now,
                        startTimestamp = startTimestamp,
                        lastKnownTimestamp = lastKnownTimestamp,
                        storedTiming = storedTiming,
                    )
                ) {
                    return failClosedEvaluation(
                        startTimestamp = startTimestamp,
                        elapsedDurationMillis = storedTiming.validElapsedDurationOrZero(),
                        lastKnownTimestamp = nextLastKnownTimestamp,
                    )
                }
                val elapsedDurationMillis =
                    calculateElapsedDuration(
                        now = now,
                        startTimestamp = startTimestamp,
                        lastKnownTimestamp = lastKnownTimestamp,
                        storedTiming = storedTiming,
                    ) ?: return failClosedEvaluation(
                        startTimestamp = startTimestamp,
                        elapsedDurationMillis = storedTiming.validElapsedDurationOrZero(),
                        lastKnownTimestamp = nextLastKnownTimestamp,
                    )
                // Ankkuroitu kesto kasvaa monotonisesta kellosta, joten saman käynnistyksen aikainen
                // seinäkellon oikaisu taaksepäin ei pidennä kokeilua eikä ole peukalointia. Käynnistysten
                // välinen taaksepäin siirto hylätään jo kestolaskennassa.
                val clockTampered =
                    clockTamperedAlready ||
                        storedTiming == StoredTrialTiming.Legacy
                return createTimingEvaluation(
                    now = now,
                    startTimestamp = startTimestamp,
                    elapsedDurationMillis = elapsedDurationMillis,
                    clockTampered = clockTampered,
                    lastKnownTimestamp = nextLastKnownTimestamp,
                )
            }

            private fun calculateElapsedDuration(
                now: SessionTimeSnapshot,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                storedTiming: StoredTrialTiming,
            ): Long? =
                when (storedTiming) {
                    StoredTrialTiming.Legacy -> {
                        maxOf(
                            nonNegativeDifference(now.wallClockMillis, startTimestamp) ?: 0L,
                            nonNegativeDifference(lastKnownTimestamp, startTimestamp) ?: 0L,
                        )
                    }

                    StoredTrialTiming.Malformed -> {
                        null
                    }

                    is StoredTrialTiming.Anchored -> {
                        advanceElapsedDuration(
                            storedTiming = storedTiming,
                            now = now,
                            startTimestamp = startTimestamp,
                            lastKnownTimestamp = lastKnownTimestamp,
                        )
                    }
                }

            private fun advanceElapsedDuration(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
            ): Long? {
                if (
                    hasInvalidAnchorValues(storedTiming) ||
                    hasInconsistentWallAnchors(storedTiming, startTimestamp, lastKnownTimestamp)
                ) {
                    return null
                }
                val deltaMillis =
                    calculateAnchorDelta(storedTiming, now) ?: return null
                return checkedAdd(storedTiming.elapsedDurationMillis, deltaMillis)
            }

            private fun calculateAnchorDelta(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
            ): Long? {
                val storedBootCount = storedTiming.anchorBootCount
                val currentBootCount = now.bootCount
                if (storedBootCount == null || currentBootCount == null) {
                    return calculateUnknownBootDelta(storedTiming, now)
                }
                return calculateKnownBootDelta(storedTiming, now, storedBootCount, currentBootCount)
            }

            // Kaikki laitteet eivät ylläpidä BOOT_COUNT-arvoa, ja sen luku voi epäonnistua hetkellisesti.
            // Uudelleenkäynnistys tunnistetaan silloin elapsedRealtimen nollautumisesta. Saman käynnistyksen
            // aikana kuluu suurempi kahdesta kellosta, jotta kumpikaan ei voi lyhentää kulunutta aikaa.
            private fun calculateUnknownBootDelta(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
            ): Long? {
                val elapsedDelta =
                    nonNegativeDifference(now.elapsedRealtimeMillis, storedTiming.anchorElapsedRealtimeMillis)
                if (elapsedDelta != null) {
                    val wallDelta = nonNegativeDifference(now.wallClockMillis, storedTiming.anchorWallClockMillis) ?: 0L
                    return maxOf(elapsedDelta, wallDelta)
                }
                val wallClockDelta =
                    nonNegativeDifference(now.wallClockMillis, storedTiming.anchorWallClockMillis) ?: return null
                if (wallClockDelta > Long.MAX_VALUE - ROLLBACK_TOLERANCE_MILLIS) return null
                val compensatedDelta = wallClockDelta + ROLLBACK_TOLERANCE_MILLIS
                return compensatedDelta.takeIf { it >= now.elapsedRealtimeMillis }
            }

            private fun calculateKnownBootDelta(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
                storedBootCount: Long,
                currentBootCount: Long,
            ): Long? {
                return when {
                    currentBootCount < storedBootCount -> {
                        null
                    }

                    currentBootCount == storedBootCount -> {
                        nonNegativeDifference(
                            now.elapsedRealtimeMillis,
                            storedTiming.anchorElapsedRealtimeMillis,
                        )
                    }

                    else -> {
                        val wallClockDelta =
                            nonNegativeDifference(
                                now.wallClockMillis,
                                storedTiming.anchorWallClockMillis,
                            ) ?: return null
                        val bootCountDelta = currentBootCount - storedBootCount
                        if (bootCountDelta > (Long.MAX_VALUE - wallClockDelta) / ROLLBACK_TOLERANCE_MILLIS) {
                            return null
                        }
                        val compensatedDelta = wallClockDelta + bootCountDelta * ROLLBACK_TOLERANCE_MILLIS
                        compensatedDelta.takeIf { it >= now.elapsedRealtimeMillis }
                    }
                }
            }

            private fun createTimingEvaluation(
                now: SessionTimeSnapshot,
                startTimestamp: Long,
                elapsedDurationMillis: Long,
                clockTampered: Boolean,
                lastKnownTimestamp: Long,
            ): TrialTimingEvaluation =
                TrialTimingEvaluation(
                    state = createTrialState(startTimestamp, elapsedDurationMillis, clockTampered),
                    anchors =
                        StoredTrialTiming.Anchored(
                            elapsedDurationMillis = elapsedDurationMillis,
                            anchorWallClockMillis = now.wallClockMillis,
                            anchorElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                            anchorBootCount = now.bootCount,
                        ),
                    lastKnownTimestamp = lastKnownTimestamp,
                )

            private fun createTrialState(
                startTimestamp: Long,
                elapsedDurationMillis: Long,
                clockTampered: Boolean,
            ): TrialState {
                val remainingMillis =
                    (TRIAL_DURATION_MILLIS - minOf(elapsedDurationMillis, TRIAL_DURATION_MILLIS))
                        .coerceAtLeast(0L)
                val dayMillis = TimeUnit.DAYS.toMillis(1L)
                val daysRemaining =
                    if (remainingMillis == 0L) {
                        0
                    } else {
                        ((remainingMillis + dayMillis - 1L) / dayMillis).toInt()
                    }
                return TrialState(
                    isActive = daysRemaining > 0 && !clockTampered,
                    daysRemaining = daysRemaining,
                    startTimestamp = startTimestamp,
                    hasStarted = true,
                    clockTampered = clockTampered,
                    elapsedDurationMillis = elapsedDurationMillis,
                )
            }

            private fun failClosedEvaluation(
                startTimestamp: Long,
                elapsedDurationMillis: Long,
                lastKnownTimestamp: Long,
            ): TrialTimingEvaluation =
                TrialTimingEvaluation(
                    state =
                        createTrialState(
                            startTimestamp = startTimestamp,
                            elapsedDurationMillis = elapsedDurationMillis,
                            clockTampered = true,
                        ),
                    anchors = null,
                    lastKnownTimestamp = lastKnownTimestamp,
                )

            private fun StoredTrialTiming.validElapsedDurationOrZero(): Long =
                (this as? StoredTrialTiming.Anchored)?.elapsedDurationMillis?.takeIf { it >= 0L } ?: 0L

            private fun isCleanUnstartedState(
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                clockTamperedAlready: Boolean,
                storedTiming: StoredTrialTiming,
            ): Boolean =
                startTimestamp == 0L &&
                    lastKnownTimestamp >= 0L &&
                    !clockTamperedAlready &&
                    storedTiming == StoredTrialTiming.Legacy

            private fun hasInvalidEvaluationInput(
                now: SessionTimeSnapshot,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                storedTiming: StoredTrialTiming,
            ): Boolean =
                startTimestamp <= 0L ||
                    lastKnownTimestamp < 0L ||
                    !isValidTimeSnapshot(now) ||
                    storedTiming == StoredTrialTiming.Malformed

            private fun hasInvalidAnchorValues(storedTiming: StoredTrialTiming.Anchored): Boolean =
                storedTiming.elapsedDurationMillis < 0L ||
                    storedTiming.anchorWallClockMillis <= 0L ||
                    storedTiming.anchorElapsedRealtimeMillis < 0L ||
                    storedTiming.anchorBootCount?.let { it < 0L } == true

            private fun hasInconsistentWallAnchors(
                storedTiming: StoredTrialTiming.Anchored,
                startTimestamp: Long,
                lastKnownTimestamp: Long,
            ): Boolean =
                lastKnownTimestamp < startTimestamp ||
                    storedTiming.anchorWallClockMillis > lastKnownTimestamp

            private fun isValidTimeSnapshot(snapshot: SessionTimeSnapshot): Boolean =
                snapshot.wallClockMillis > 0L &&
                    snapshot.elapsedRealtimeMillis >= 0L &&
                    snapshot.bootCount?.let { it >= 0L } != false

            private fun nonNegativeDifference(
                later: Long,
                earlier: Long,
            ): Long? = if (later >= earlier) later - earlier else null

            private fun checkedAdd(
                first: Long,
                second: Long,
            ): Long? =
                if (first < 0L || second < 0L || first > Long.MAX_VALUE - second) {
                    null
                } else {
                    first + second
                }
        }
    }
