package com.finnvek.knittools.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.tips.BASIC_TIPS
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.domain.tips.TipRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

const val ONBOARDING_STATE_VERSION = 1

private val Context.onboardingDataStore by preferencesDataStore(
    name = "onboarding_state",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class OnboardingState(
    val initializedVersion: Int = 0,
    val welcomeDismissed: Boolean = false,
    val starterProjectId: Long? = null,
    val guidedPatternProjectId: Long? = null,
    val projectSetupHidden: Boolean = false,
    val projectSetupCelebrated: Boolean = false,
    val tipsSeen: Set<TipId> = emptySet(),
    val firstUseEpochDay: Long = 0L,
    val counterOpenCount: Int = 0,
    val counterOpenDays: Int = 0,
    val counterOpenLastDay: Long? = null,
    val firstSessionTipPending: Boolean = false,
    val targetCelebratedProjects: Set<String> = emptySet(),
    val backupCreated: Boolean = false,
    val projectIncrementCounts: Map<Long, Int> = emptyMap(),
    val firstSaveObserved: Boolean = false,
    val trialChartSeenForStart: Long = 0L,
    val trialPhotoPdfCreatedForStart: Long = 0L,
)

@Singleton
class OnboardingStateRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.onboardingDataStore)

    val draft: Flow<OnboardingDraft?> =
        dataStore.data
            .map { prefs ->
                prefs[KEY_JOURNEY_DRAFT]?.let {
                    try {
                        draftJson.decodeFromString<OnboardingDraft>(it)
                    } catch (_: SerializationException) {
                        null
                    }
                }
            }.distinctUntilChanged()

    suspend fun saveDraft(draft: OnboardingDraft): Boolean =
        dataStore.editPreferencesSafely { prefs ->
            prefs[KEY_JOURNEY_DRAFT] = draftJson.encodeToString(draft)
            if (draft.projectId != null) prefs[KEY_STARTER_PROJECT_ID] = draft.projectId
            if (draft.completed) {
                prefs[KEY_WELCOME_DISMISSED] = true
                if (draft.projectId != null) {
                    prefs[KEY_TIPS_SEEN] = prefs[KEY_TIPS_SEEN].orEmpty() + setOf(TipId.FIRST_ROW.key, TipId.UNDO.key)
                    if (draft.pdfUri != null) prefs[KEY_GUIDED_PATTERN_PROJECT_ID] = draft.projectId
                }
            }
        }

    val state: Flow<OnboardingState> =
        dataStore.safePreferencesData
            .map { prefs ->
                OnboardingState(
                    initializedVersion = prefs[KEY_INITIALIZED_VERSION] ?: 0,
                    welcomeDismissed = prefs[KEY_WELCOME_DISMISSED] ?: false,
                    starterProjectId = prefs[KEY_STARTER_PROJECT_ID],
                    guidedPatternProjectId = prefs[KEY_GUIDED_PATTERN_PROJECT_ID],
                    projectSetupHidden = prefs[KEY_PROJECT_SETUP_HIDDEN] ?: false,
                    projectSetupCelebrated = prefs[KEY_PROJECT_SETUP_CELEBRATED] ?: false,
                    tipsSeen = TipId.entries.filterTo(mutableSetOf()) { it.key in prefs[KEY_TIPS_SEEN].orEmpty() },
                    firstUseEpochDay = prefs[KEY_FIRST_USE_EPOCH_DAY] ?: 0L,
                    counterOpenCount = prefs[KEY_COUNTER_OPEN_COUNT] ?: 0,
                    counterOpenDays = prefs[KEY_COUNTER_OPEN_DAYS] ?: 0,
                    counterOpenLastDay = prefs[KEY_COUNTER_OPEN_LAST_DAY],
                    firstSessionTipPending = prefs[KEY_FIRST_SESSION_TIP_PENDING] ?: false,
                    firstSaveObserved = prefs[KEY_FIRST_SAVE_OBSERVED] ?: false,
                    targetCelebratedProjects = prefs[KEY_TARGET_CELEBRATED_PROJECTS].orEmpty(),
                    backupCreated = prefs[KEY_BACKUP_CREATED] ?: false,
                    projectIncrementCounts = incrementCounts(prefs[KEY_PROJECT_INCREMENT_COUNTS].orEmpty()),
                    trialChartSeenForStart = prefs[KEY_TRIAL_CHART_SEEN_FOR_START] ?: 0L,
                    trialPhotoPdfCreatedForStart = prefs[KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START] ?: 0L,
                )
            }.distinctUntilChanged()

    suspend fun initializeIfNeeded(
        hasAnyProject: Boolean,
        today: Long,
    ) {
        dataStore.editPreferencesSafely { prefs ->
            if ((prefs[KEY_INITIALIZED_VERSION] ?: 0) >= ONBOARDING_STATE_VERSION) return@editPreferencesSafely
            prefs[KEY_FIRST_USE_EPOCH_DAY] = today
            if (hasAnyProject) {
                prefs[KEY_TIPS_SEEN] = prefs[KEY_TIPS_SEEN].orEmpty() + BASIC_TIPS.map { it.key }
            }
            prefs[KEY_INITIALIZED_VERSION] = ONBOARDING_STATE_VERSION
        }
    }

    suspend fun dismissWelcome() {
        dataStore.editPreferencesSafely { it[KEY_WELCOME_DISMISSED] = true }
    }

    suspend fun setStarterProject(projectId: Long) {
        dataStore.editPreferencesSafely {
            if (it[KEY_STARTER_PROJECT_ID] == null) it[KEY_STARTER_PROJECT_ID] = projectId
        }
    }

    suspend fun hideProjectSetup() {
        dataStore.editPreferencesSafely { it[KEY_PROJECT_SETUP_HIDDEN] = true }
    }

    suspend fun markProjectSetupCelebrated() {
        dataStore.editPreferencesSafely { it[KEY_PROJECT_SETUP_CELEBRATED] = true }
    }

    suspend fun markTipSeen(id: TipId) {
        dataStore.editPreferencesSafely { it[KEY_TIPS_SEEN] = it[KEY_TIPS_SEEN].orEmpty() + id.key }
    }

    suspend fun resetTips(): Boolean = dataStore.editPreferencesSafely { it.remove(KEY_TIPS_SEEN) }

    suspend fun recordCounterOpen(today: Long) {
        dataStore.editPreferencesSafely {
            it[KEY_COUNTER_OPEN_COUNT] = (it[KEY_COUNTER_OPEN_COUNT] ?: 0).coerceIn(0, Int.MAX_VALUE - 1) + 1
            if (it[KEY_COUNTER_OPEN_LAST_DAY] != today) {
                it[KEY_COUNTER_OPEN_DAYS] = (it[KEY_COUNTER_OPEN_DAYS] ?: 0).coerceIn(0, Int.MAX_VALUE - 1) + 1
                it[KEY_COUNTER_OPEN_LAST_DAY] = today
            }
        }
    }

    suspend fun markBasicTipsSeen() {
        dataStore.editPreferencesSafely {
            it[KEY_TIPS_SEEN] =
                it[KEY_TIPS_SEEN].orEmpty() + BASIC_TIPS.map { tip -> tip.key }
        }
    }

    suspend fun recordProjectIncrement(projectId: Long) {
        if (projectId <= 0L) return
        dataStore.editPreferencesSafely { prefs ->
            val counts = incrementCounts(prefs[KEY_PROJECT_INCREMENT_COUNTS].orEmpty())
            val previous = counts[projectId] ?: 0
            if (previous >= TipRules.STEP_INCREMENT_THRESHOLD) return@editPreferencesSafely
            prefs[KEY_PROJECT_INCREMENT_COUNTS] =
                (counts + (projectId to previous + 1)).map { "${it.key}:${it.value}" }.toSet()
        }
    }

    suspend fun setFirstSessionTipPending(pending: Boolean) {
        dataStore.editPreferencesSafely { it[KEY_FIRST_SESSION_TIP_PENDING] = pending }
    }

    suspend fun recordFirstSessionSaved(wasFirstCompletedSession: Boolean) {
        dataStore.editPreferencesSafely { prefs ->
            val alreadyObserved =
                prefs[KEY_FIRST_SAVE_OBSERVED] == true ||
                    prefs[KEY_FIRST_SESSION_TIP_PENDING] == true ||
                    TipId.FIRST_SESSION_SAVED.key in prefs[KEY_TIPS_SEEN].orEmpty()
            // Istuntojen poistaminen tai vihjeiden nollaus ei muuta tallennusta ensimmäiseksi uudelleen.
            prefs[KEY_FIRST_SAVE_OBSERVED] = true
            if (!alreadyObserved && wasFirstCompletedSession) prefs[KEY_FIRST_SESSION_TIP_PENDING] = true
        }
    }

    suspend fun markTargetCelebrated(projectId: Long) {
        dataStore.editPreferencesSafely { prefs ->
            val projectKey = projectId.toString()
            if (projectKey in prefs[KEY_TARGET_CELEBRATED_PROJECTS].orEmpty()) return@editPreferencesSafely
            // Preferences-joukko ei takaa järjestystä tiedostosta luettaessa, joten ikä tallennetaan erikseen.
            val orderedIds = prefs[KEY_TARGET_CELEBRATED_PROJECT_ORDER].orEmpty().split(',').filter { it.isNotEmpty() }
            val retainedIds = (orderedIds + projectKey).takeLast(MAX_TARGET_CELEBRATIONS)
            prefs[KEY_TARGET_CELEBRATED_PROJECTS] = retainedIds.toSet()
            prefs[KEY_TARGET_CELEBRATED_PROJECT_ORDER] = retainedIds.joinToString(",")
        }
    }

    suspend fun markBackupCreated() {
        dataStore.editPreferencesSafely { it[KEY_BACKUP_CREATED] = true }
    }

    suspend fun markTrialChartSeen(trialStartTimestamp: Long) {
        if (trialStartTimestamp <= 0L) return
        dataStore.editPreferencesSafely {
            it[KEY_TRIAL_CHART_SEEN_FOR_START] = maxOf(it[KEY_TRIAL_CHART_SEEN_FOR_START] ?: 0L, trialStartTimestamp)
        }
    }

    suspend fun markTrialPhotoPdfCreated(trialStartTimestamp: Long) {
        if (trialStartTimestamp <= 0L) return
        dataStore.editPreferencesSafely {
            it[KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START] =
                maxOf(it[KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START] ?: 0L, trialStartTimestamp)
        }
    }

    private companion object {
        val draftJson = Json { ignoreUnknownKeys = true }
        val KEY_JOURNEY_DRAFT = stringPreferencesKey("journey_draft_v1")
        const val MAX_TARGET_CELEBRATIONS = 200
        val KEY_INITIALIZED_VERSION = intPreferencesKey("initialized_version")
        val KEY_WELCOME_DISMISSED = booleanPreferencesKey("welcome_dismissed")
        val KEY_STARTER_PROJECT_ID = longPreferencesKey("starter_project_id")
        val KEY_GUIDED_PATTERN_PROJECT_ID = longPreferencesKey("guided_pattern_project_id")
        val KEY_PROJECT_SETUP_HIDDEN = booleanPreferencesKey("project_setup_hidden")
        val KEY_PROJECT_SETUP_CELEBRATED = booleanPreferencesKey("project_setup_celebrated")
        val KEY_TIPS_SEEN = stringSetPreferencesKey("tips_seen")
        val KEY_FIRST_USE_EPOCH_DAY = longPreferencesKey("first_use_epoch_day")
        val KEY_COUNTER_OPEN_COUNT = intPreferencesKey("counter_open_count")
        val KEY_COUNTER_OPEN_DAYS = intPreferencesKey("counter_open_days")
        val KEY_COUNTER_OPEN_LAST_DAY = longPreferencesKey("counter_open_last_day")
        val KEY_FIRST_SESSION_TIP_PENDING = booleanPreferencesKey("first_session_tip_pending")
        val KEY_FIRST_SAVE_OBSERVED = booleanPreferencesKey("first_save_observed")
        val KEY_TARGET_CELEBRATED_PROJECTS = stringSetPreferencesKey("target_celebrated_projects")
        val KEY_TARGET_CELEBRATED_PROJECT_ORDER = stringPreferencesKey("target_celebrated_project_order")
        val KEY_BACKUP_CREATED = booleanPreferencesKey("backup_created")
        val KEY_PROJECT_INCREMENT_COUNTS = stringSetPreferencesKey("project_increment_counts")
        val KEY_TRIAL_CHART_SEEN_FOR_START = longPreferencesKey("trial_chart_seen_for_start")
        val KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START = longPreferencesKey("trial_photo_pdf_created_for_start")

        fun incrementCounts(entries: Set<String>): Map<Long, Int> =
            buildMap {
                entries.forEach { entry ->
                    val parts = entry.split(':', limit = 2)
                    val projectId = parts.firstOrNull()?.toLongOrNull()?.takeIf { it > 0L }
                    val count = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 }
                    if (projectId != null && count != null) {
                        put(
                            projectId,
                            maxOf(get(projectId) ?: 0, count.coerceAtMost(TipRules.STEP_INCREMENT_THRESHOLD)),
                        )
                    }
                }
            }
    }
}
