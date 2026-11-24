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
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
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
    val projectSetupHidden: Boolean = false,
    val projectSetupCelebrated: Boolean = false,
    val firstUseEpochDay: Long = 0L,
    val backupCreated: Boolean = false,
    val backupNoticeDismissed: Boolean = false,
    val readingLineNoticeDismissed: Boolean = false,
    val trialChartSeenForStart: Long = 0L,
    val trialPhotoPdfCreatedForStart: Long = 0L,
)

private const val BACKUP_NOTICE_MINIMUM_DAYS = 7L

internal fun OnboardingState.shouldShowBackupNotice(
    hasAnyProject: Boolean,
    today: Long,
): Boolean {
    val noticeHandled = backupCreated || backupNoticeDismissed
    return hasAnyProject &&
        initializedVersion > 0 &&
        !noticeHandled &&
        today - firstUseEpochDay >= BACKUP_NOTICE_MINIMUM_DAYS
}

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
            .onEach(::removeLegacyPreferences)
            .map { prefs ->
                prefs[KEY_JOURNEY_DRAFT]?.let {
                    try {
                        val stored = draftJson.parseToJsonElement(it)
                        val migrated =
                            if (stored is JsonObject && stored["step"] == JsonPrimitive("Benefits")) {
                                JsonObject(stored + ("step" to JsonPrimitive(OnboardingStep.Project.name)))
                            } else {
                                stored
                            }
                        draftJson.decodeFromJsonElement<OnboardingDraft>(migrated)
                    } catch (_: SerializationException) {
                        null
                    }
                }
            }.distinctUntilChanged()

    suspend fun saveDraft(draft: OnboardingDraft): Boolean =
        dataStore.editPreferencesSafely { prefs ->
            prefs[KEY_JOURNEY_DRAFT] = draftJson.encodeToString(draft)
            if (draft.projectId != null) prefs[KEY_STARTER_PROJECT_ID] = draft.projectId
            if (draft.completed) prefs[KEY_WELCOME_DISMISSED] = true
        }

    val state: Flow<OnboardingState> =
        dataStore.safePreferencesData
            .onEach(::removeLegacyPreferences)
            .map { prefs ->
                OnboardingState(
                    initializedVersion = prefs[KEY_INITIALIZED_VERSION] ?: 0,
                    welcomeDismissed = prefs[KEY_WELCOME_DISMISSED] ?: false,
                    starterProjectId = prefs[KEY_STARTER_PROJECT_ID],
                    projectSetupHidden = prefs[KEY_PROJECT_SETUP_HIDDEN] ?: false,
                    projectSetupCelebrated = prefs[KEY_PROJECT_SETUP_CELEBRATED] ?: false,
                    firstUseEpochDay = prefs[KEY_FIRST_USE_EPOCH_DAY] ?: 0L,
                    backupCreated = prefs[KEY_BACKUP_CREATED] ?: false,
                    backupNoticeDismissed = prefs[KEY_BACKUP_NOTICE_DISMISSED] ?: false,
                    readingLineNoticeDismissed = prefs[KEY_READING_LINE_NOTICE_DISMISSED] ?: false,
                    trialChartSeenForStart = prefs[KEY_TRIAL_CHART_SEEN_FOR_START] ?: 0L,
                    trialPhotoPdfCreatedForStart = prefs[KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START] ?: 0L,
                )
            }.distinctUntilChanged()

    suspend fun initializeIfNeeded(today: Long) {
        dataStore.editPreferencesSafely { prefs ->
            if ((prefs[KEY_INITIALIZED_VERSION] ?: 0) >= ONBOARDING_STATE_VERSION) return@editPreferencesSafely
            prefs[KEY_FIRST_USE_EPOCH_DAY] = today
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

    suspend fun hideProjectSetup(projectId: Long): Boolean =
        dataStore.editPreferencesSafely {
            if (it[KEY_STARTER_PROJECT_ID] == projectId) it[KEY_PROJECT_SETUP_HIDDEN] = true
        }

    suspend fun markProjectSetupCelebrated() {
        dataStore.editPreferencesSafely { it[KEY_PROJECT_SETUP_CELEBRATED] = true }
    }

    private suspend fun removeLegacyPreferences(prefs: Preferences) {
        if (prefs.asMap().keys.none { it.name in LEGACY_KEYS }) return
        dataStore.editPreferencesSafely { current ->
            current
                .asMap()
                .keys
                .filter { it.name in LEGACY_KEYS }
                .forEach { current.remove(it) }
        }
    }

    suspend fun dismissBackupNotice() {
        dataStore.editPreferencesSafely { it[KEY_BACKUP_NOTICE_DISMISSED] = true }
    }

    suspend fun dismissReadingLineNotice() {
        dataStore.editPreferencesSafely { it[KEY_READING_LINE_NOTICE_DISMISSED] = true }
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
        val KEY_INITIALIZED_VERSION = intPreferencesKey("initialized_version")
        val KEY_WELCOME_DISMISSED = booleanPreferencesKey("welcome_dismissed")
        val KEY_STARTER_PROJECT_ID = longPreferencesKey("starter_project_id")
        val KEY_PROJECT_SETUP_HIDDEN = booleanPreferencesKey("project_setup_hidden")
        val KEY_PROJECT_SETUP_CELEBRATED = booleanPreferencesKey("project_setup_celebrated")
        val KEY_FIRST_USE_EPOCH_DAY = longPreferencesKey("first_use_epoch_day")
        val KEY_BACKUP_CREATED = booleanPreferencesKey("backup_created")
        val KEY_BACKUP_NOTICE_DISMISSED = booleanPreferencesKey("backup_notice_dismissed")
        val KEY_READING_LINE_NOTICE_DISMISSED = booleanPreferencesKey("reading_line_notice_dismissed")
        val KEY_TRIAL_CHART_SEEN_FOR_START = longPreferencesKey("trial_chart_seen_for_start")
        val KEY_TRIAL_PHOTO_PDF_CREATED_FOR_START = longPreferencesKey("trial_photo_pdf_created_for_start")

        val LEGACY_KEYS =
            setOf(
                "tips_seen",
                "counter_open_count",
                "counter_open_days",
                "counter_open_last_day",
                "first_session_tip_pending",
                "first_save_observed",
                "target_celebrated_projects",
                "target_celebrated_project_order",
                "project_increment_counts",
                "guided_pattern_project_id",
            )
    }
}
