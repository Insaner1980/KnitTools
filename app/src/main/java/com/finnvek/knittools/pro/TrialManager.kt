package com.finnvek.knittools.pro

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
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
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val sessionTimeSource: SessionTimeSource = UnavailableBootSessionTimeSource,
    ) {
        private val _trialState = MutableStateFlow(TrialState())
        val trialState: StateFlow<TrialState> = _trialState.asStateFlow()
        private val refreshScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var refreshJob: Job? = null
        private val refreshLoopMutex = Mutex()

        suspend fun initialize() {
            refreshTrialState()
            startRefreshLoop()
        }

        suspend fun startTrial(): TrialStartResult {
            val now = sessionTimeSource.snapshot()
            var startResult: TrialStartResult? = null
            val didWrite =
                context.trialDataStore.editPreferencesSafely { preferences ->
                    val startTimestamp = preferences[KEY_TRIAL_START] ?: 0L
                    val lastKnownTimestamp = preferences[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
                    val clockTamperedAlready = preferences[KEY_CLOCK_TAMPERED] ?: false
                    val storedTiming = preferences.readStoredTrialTiming()
                    if (
                        isCleanUnstartedState(
                            startTimestamp = startTimestamp,
                            lastKnownTimestamp = lastKnownTimestamp,
                            clockTamperedAlready = clockTamperedAlready,
                            storedTiming = storedTiming,
                        ) &&
                        isValidTimeSnapshot(now)
                    ) {
                        preferences[KEY_TRIAL_START] = now.wallClockMillis
                        preferences.persistTrialEvaluation(
                            TrialTimingEvaluation(
                                state =
                                    createTrialState(
                                        startTimestamp = now.wallClockMillis,
                                        elapsedDurationMillis = 0L,
                                        clockTampered = false,
                                    ),
                                anchors =
                                    StoredTrialTiming.Anchored(
                                        elapsedDurationMillis = 0L,
                                        anchorWallClockMillis = now.wallClockMillis,
                                        anchorElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                                        anchorBootCount = now.bootCount,
                                    ),
                                lastKnownTimestamp = maxOf(now.wallClockMillis, lastKnownTimestamp),
                            ),
                        )
                        startResult = TrialStartResult.Started
                    } else {
                        startResult =
                            classifyExistingTrial(
                                preferences.evaluateAndPersistTrialState(now),
                            )
                    }
                }
            if (!didWrite) return TrialStartResult.Failed

            refreshTrialState()
            return startResult ?: TrialStartResult.Failed
        }

        suspend fun claimTrialEndNotice(): Boolean {
            var claimed = false
            val didWrite =
                context.trialDataStore.editPreferencesSafely { preferences ->
                    val hasStarted = (preferences[KEY_TRIAL_START] ?: 0L) > 0L
                    val wasShown = preferences[KEY_TRIAL_END_NOTICE_SHOWN] ?: false
                    if (hasStarted && !wasShown) {
                        preferences[KEY_TRIAL_END_NOTICE_SHOWN] = true
                        claimed = true
                    }
                }
            return didWrite && claimed
        }

        suspend fun markTrialEndNoticeShown() {
            context.trialDataStore.editPreferencesSafely { preferences ->
                if ((preferences[KEY_TRIAL_START] ?: 0L) > 0L) {
                    preferences[KEY_TRIAL_END_NOTICE_SHOWN] = true
                }
            }
        }

        suspend fun updateTimestamp() {
            val now = sessionTimeSource.snapshot()
            val didWrite =
                context.trialDataStore.editPreferencesSafely { preferences ->
                    preferences.evaluateAndPersistTrialState(now)
                }
            if (!didWrite) {
                _trialState.value = TrialState()
            }
        }

        private suspend fun refreshTrialState() {
            val now = sessionTimeSource.snapshot()
            var refreshedState: TrialState? = null
            val didWrite =
                context.trialDataStore.editPreferencesSafely { preferences ->
                    refreshedState = preferences.evaluateAndPersistTrialState(now)
                }
            if (didWrite) {
                refreshedState?.let { _trialState.value = it }
            } else {
                _trialState.value = TrialState()
            }
        }

        private fun MutablePreferences.evaluateAndPersistTrialState(now: SessionTimeSnapshot): TrialState {
            val evaluation =
                evaluateTrialTiming(
                    now = now,
                    startTimestamp = this[KEY_TRIAL_START] ?: 0L,
                    lastKnownTimestamp = this[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L,
                    clockTamperedAlready = this[KEY_CLOCK_TAMPERED] ?: false,
                    storedTiming = readStoredTrialTiming(),
                )
            persistTrialEvaluation(evaluation)
            return evaluation.state
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

        private suspend fun startRefreshLoop() =
            refreshLoopMutex.withLock {
                if (refreshJob?.isActive == true) return@withLock

                refreshJob =
                    refreshScope.launch {
                        while (isActive) {
                            val currentState = _trialState.value
                            delay(calculateTrialRefreshDelayMillis(currentState.elapsedDurationMillis))
                            refreshTrialState()
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
                    ) &&
                    isValidTimeSnapshot(now)
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
                val clockTampered =
                    clockTamperedAlready ||
                        rollbackExceedsTolerance(now.wallClockMillis, lastKnownTimestamp) ||
                        rollbackExceedsTolerance(now.wallClockMillis, startTimestamp)
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
                    StoredTrialTiming.Legacy ->
                        maxOf(
                            nonNegativeDifference(now.wallClockMillis, startTimestamp) ?: 0L,
                            nonNegativeDifference(lastKnownTimestamp, startTimestamp) ?: 0L,
                        )

                    StoredTrialTiming.Malformed -> null
                    is StoredTrialTiming.Anchored ->
                        advanceElapsedDuration(
                            storedTiming = storedTiming,
                            now = now,
                            startTimestamp = startTimestamp,
                            lastKnownTimestamp = lastKnownTimestamp,
                        )
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
                return if (storedBootCount != null && currentBootCount != null) {
                    calculateKnownBootDelta(storedTiming, now, storedBootCount, currentBootCount)
                } else {
                    calculateUnavailableBootDelta(storedTiming, now)
                }
            }

            private fun calculateKnownBootDelta(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
                storedBootCount: Long,
                currentBootCount: Long,
            ): Long? =
                when {
                    currentBootCount < storedBootCount -> null
                    currentBootCount == storedBootCount ->
                        nonNegativeDifference(
                            now.elapsedRealtimeMillis,
                            storedTiming.anchorElapsedRealtimeMillis,
                        )

                    else ->
                        nonNegativeDifference(
                            now.wallClockMillis,
                            storedTiming.anchorWallClockMillis,
                        )
                }

            private fun calculateUnavailableBootDelta(
                storedTiming: StoredTrialTiming.Anchored,
                now: SessionTimeSnapshot,
            ): Long? {
                val elapsedDelta =
                    nonNegativeDifference(
                        now.elapsedRealtimeMillis,
                        storedTiming.anchorElapsedRealtimeMillis,
                    )
                val wallClockDelta =
                    nonNegativeDifference(
                        now.wallClockMillis,
                        storedTiming.anchorWallClockMillis,
                    )
                if (elapsedDelta == null && wallClockDelta == null) return null
                return maxOf(elapsedDelta ?: 0L, wallClockDelta ?: 0L)
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
                    storedTiming.anchorWallClockMillis > lastKnownTimestamp ||
                    rollbackExceedsTolerance(storedTiming.anchorWallClockMillis, startTimestamp)

            private fun isValidTimeSnapshot(snapshot: SessionTimeSnapshot): Boolean =
                snapshot.wallClockMillis > 0L &&
                    snapshot.elapsedRealtimeMillis >= 0L &&
                    snapshot.bootCount?.let { it >= 0L } != false

            private fun rollbackExceedsTolerance(
                currentTimestamp: Long,
                referenceTimestamp: Long,
            ): Boolean =
                referenceTimestamp > currentTimestamp &&
                    referenceTimestamp - currentTimestamp > ROLLBACK_TOLERANCE_MILLIS

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
