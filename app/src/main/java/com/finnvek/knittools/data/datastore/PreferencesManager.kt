package com.finnvek.knittools.data.datastore

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val showKnittingTips: Boolean = true,
    val showCompletedProjects: Boolean = false,
    val projectSortOrder: String = "updated",
    val voiceLiveEnabled: Boolean = true,
)

@Singleton
class PreferencesManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val preferences: Flow<AppPreferences> =
            context.dataStore.data.map { prefs ->
                AppPreferences(
                    themeMode =
                        ThemeMode.entries.firstOrNull {
                            it.value == (prefs[KEY_THEME_MODE] ?: 1)
                        } ?: ThemeMode.LIGHT,
                    appLanguage = AppLanguage.fromValue(prefs[KEY_APP_LANGUAGE]),
                    hapticFeedback = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
                    keepScreenAwake = prefs[KEY_KEEP_SCREEN_AWAKE] ?: false,
                    useImperial = prefs[KEY_USE_IMPERIAL] ?: false,
                    showKnittingTips = prefs[KEY_SHOW_KNITTING_TIPS] ?: true,
                    showCompletedProjects = prefs[KEY_SHOW_COMPLETED] ?: false,
                    projectSortOrder = prefs[KEY_SORT_ORDER] ?: "updated",
                    voiceLiveEnabled = prefs[KEY_VOICE_LIVE] ?: true,
                )
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            context.dataStore.edit { it[KEY_THEME_MODE] = mode.value }
        }

        suspend fun setAppLanguage(language: AppLanguage) {
            context.dataStore.edit { it[KEY_APP_LANGUAGE] = language.value }
            applyAppLanguage(language)
        }

        suspend fun setHapticFeedback(enabled: Boolean) {
            context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK] = enabled }
        }

        suspend fun setKeepScreenAwake(enabled: Boolean) {
            context.dataStore.edit { it[KEY_KEEP_SCREEN_AWAKE] = enabled }
        }

        suspend fun setUseImperial(imperial: Boolean) {
            context.dataStore.edit { it[KEY_USE_IMPERIAL] = imperial }
        }

        suspend fun setShowKnittingTips(enabled: Boolean) {
            context.dataStore.edit { it[KEY_SHOW_KNITTING_TIPS] = enabled }
        }

        suspend fun setShowCompletedProjects(show: Boolean) {
            context.dataStore.edit { it[KEY_SHOW_COMPLETED] = show }
        }

        suspend fun setProjectSortOrder(order: String) {
            context.dataStore.edit { it[KEY_SORT_ORDER] = order }
        }

        suspend fun setVoiceLiveEnabled(enabled: Boolean) {
            context.dataStore.edit { it[KEY_VOICE_LIVE] = enabled }
        }

        // Tooltipit

        val dismissedTooltips: Flow<Set<String>> =
            context.dataStore.data.map { prefs ->
                prefs[KEY_DISMISSED_TOOLTIPS] ?: emptySet()
            }

        suspend fun dismissTooltip(id: String) {
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_DISMISSED_TOOLTIPS] ?: emptySet()
                prefs[KEY_DISMISSED_TOOLTIPS] = current + id
            }
        }

        suspend fun applyStoredAppLanguage() {
            applyAppLanguage(preferences.first().appLanguage)
        }

        private fun applyAppLanguage(language: AppLanguage) {
            val locales =
                language.languageTag?.let(LocaleListCompat::forLanguageTags)
                    ?: LocaleListCompat.getEmptyLocaleList()
            AppCompatDelegate.setApplicationLocales(locales)
        }

        private companion object {
            val KEY_THEME_MODE = intPreferencesKey("theme_mode")
            val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
            val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
            val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
            val KEY_USE_IMPERIAL = booleanPreferencesKey("use_imperial")
            val KEY_SHOW_KNITTING_TIPS = booleanPreferencesKey("show_knitting_tips")
            val KEY_SHOW_COMPLETED = booleanPreferencesKey("show_completed_projects")
            val KEY_SORT_ORDER = stringPreferencesKey("project_sort_order")
            val KEY_VOICE_LIVE = booleanPreferencesKey("voice_live_enabled")
            val KEY_DISMISSED_TOOLTIPS = stringSetPreferencesKey("dismissed_tooltips")
        }
    }
