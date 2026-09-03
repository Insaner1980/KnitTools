package com.finnvek.knittools.data.datastore

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.domain.model.ProjectSortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "knittools_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode(
    val value: Int,
) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val useImperial: Boolean = false,
    val showCompletedProjects: Boolean = false,
    val projectSortOrder: ProjectSortOrder = ProjectSortOrder.DEFAULT,
)

@Singleton
class PreferencesManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val _storedAppLanguageApplied = MutableStateFlow(false)
        val storedAppLanguageApplied: StateFlow<Boolean> = _storedAppLanguageApplied.asStateFlow()

        val preferences: Flow<AppPreferences> =
            context.dataStore.safePreferencesData.map { prefs ->
                AppPreferences(
                    themeMode =
                        ThemeMode.entries.firstOrNull {
                            it.value == (prefs[KEY_THEME_MODE] ?: 1)
                        } ?: ThemeMode.LIGHT,
                    appLanguage = resolveAppLanguage(prefs),
                    hapticFeedback = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
                    keepScreenAwake = prefs[KEY_KEEP_SCREEN_AWAKE] ?: false,
                    useImperial = prefs[KEY_USE_IMPERIAL] ?: false,
                    showCompletedProjects = prefs[KEY_SHOW_COMPLETED] ?: false,
                    projectSortOrder = ProjectSortOrder.fromPersistedValue(prefs[KEY_SORT_ORDER]),
                )
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            context.dataStore.editPreferencesSafely {
                it[KEY_THEME_MODE] = mode.value
            }
        }

        suspend fun setAppLanguage(language: AppLanguage) {
            val saved =
                context.dataStore.editPreferencesSafely {
                    it[KEY_APP_LANGUAGE] = language.value
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] = true
                    }
                }
            if (saved) applyAppLanguage(language)
        }

        suspend fun setHapticFeedback(enabled: Boolean) {
            context.dataStore.editPreferencesSafely {
                it[KEY_HAPTIC_FEEDBACK] = enabled
            }
        }

        suspend fun setKeepScreenAwake(enabled: Boolean) {
            context.dataStore.editPreferencesSafely {
                it[KEY_KEEP_SCREEN_AWAKE] = enabled
            }
        }

        suspend fun setUseImperial(imperial: Boolean) {
            context.dataStore.editPreferencesSafely {
                it[KEY_USE_IMPERIAL] = imperial
            }
        }

        suspend fun toggleShowCompletedProjects() {
            context.dataStore.editPreferencesSafely {
                it[KEY_SHOW_COMPLETED] = !(it[KEY_SHOW_COMPLETED] ?: false)
            }
        }

        suspend fun setProjectSortOrder(order: ProjectSortOrder) {
            context.dataStore.editPreferencesSafely {
                it[KEY_SORT_ORDER] = order.persistedValue
            }
        }

        // Tooltipit

        val dismissedTooltips: Flow<Set<String>> =
            context.dataStore.safePreferencesData.map { prefs ->
                prefs[KEY_DISMISSED_TOOLTIPS] ?: emptySet()
            }

        suspend fun dismissTooltip(id: String) {
            context.dataStore.editPreferencesSafely { prefs ->
                val current = prefs[KEY_DISMISSED_TOOLTIPS] ?: emptySet()
                prefs[KEY_DISMISSED_TOOLTIPS] = current + id
            }
        }

        suspend fun applyStoredAppLanguage() {
            try {
                val prefs = context.dataStore.readPreferencesOrNull() ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    migrateStoredLanguageToSystemIfNeeded(prefs)
                } else {
                    applyAppLanguage(AppLanguage.fromValue(prefs[KEY_APP_LANGUAGE]))
                }
            } finally {
                _storedAppLanguageApplied.value = true
            }
        }

        suspend fun syncAppLanguageFromSystem() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

            val currentLanguage = currentAppLanguage()
            context.dataStore.editPreferencesSafely { prefs ->
                if (prefs[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] != true) {
                    return@editPreferencesSafely
                }
                if (AppLanguage.fromValue(prefs[KEY_APP_LANGUAGE]) != currentLanguage) {
                    prefs[KEY_APP_LANGUAGE] = currentLanguage.value
                }
                prefs[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] = true
            }
        }

        private fun applyAppLanguage(language: AppLanguage) {
            val languageTags = language.languageTag.orEmpty()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                    LocaleList.forLanguageTags(languageTags)
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTags))
            }
        }

        private suspend fun migrateStoredLanguageToSystemIfNeeded(prefs: Preferences) {
            if (prefs[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] == true) return

            val storedLanguage = AppLanguage.fromValue(prefs[KEY_APP_LANGUAGE])
            val currentLanguage = currentAppLanguage()
            if (storedLanguage != AppLanguage.SYSTEM && currentLanguage == AppLanguage.SYSTEM) {
                applyAppLanguage(storedLanguage)
            }

            context.dataStore.editPreferencesSafely {
                val languageToStore =
                    currentAppLanguage().takeIf { language -> language != AppLanguage.SYSTEM }
                        ?: storedLanguage
                it[KEY_APP_LANGUAGE] = languageToStore.value
                it[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] = true
            }
        }

        private fun resolveAppLanguage(prefs: Preferences): AppLanguage {
            val currentLanguage = currentAppLanguage()
            if (currentLanguage != AppLanguage.SYSTEM) return currentLanguage

            val migratedToSystem = prefs[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] ?: false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && migratedToSystem) {
                AppLanguage.SYSTEM
            } else {
                AppLanguage.fromValue(prefs[KEY_APP_LANGUAGE])
            }
        }

        private fun currentAppLanguage(): AppLanguage {
            val languageTags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context
                        .getSystemService(LocaleManager::class.java)
                        ?.applicationLocales
                        ?.toLanguageTags()
                        .orEmpty()
                } else {
                    AppCompatDelegate.getApplicationLocales().toLanguageTags()
                }
            return AppLanguage.fromLanguageTag(languageTags)
        }

        private companion object {
            val KEY_THEME_MODE = intPreferencesKey("theme_mode")
            val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
            val KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM =
                booleanPreferencesKey("app_language_migrated_to_system")
            val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
            val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
            val KEY_USE_IMPERIAL = booleanPreferencesKey("use_imperial")
            val KEY_SHOW_COMPLETED = booleanPreferencesKey("show_completed_projects")
            val KEY_SORT_ORDER = stringPreferencesKey("project_sort_order")
            val KEY_DISMISSED_TOOLTIPS = stringSetPreferencesKey("dismissed_tooltips")
        }
    }
