package com.finnvek.knittools.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "knittools_preferences")

enum class ThemeMode(
    val value: Int,
) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val useImperial: Boolean = false,
)

@Singleton
class PreferencesManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val preferences: Flow<AppPreferences> =
            context.dataStore.data.map { prefs ->
                AppPreferences(
                    themeMode =
                        ThemeMode.entries.firstOrNull {
                            it.value == (prefs[KEY_THEME_MODE] ?: 0)
                        } ?: ThemeMode.SYSTEM,
                    hapticFeedback = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
                    keepScreenAwake = prefs[KEY_KEEP_SCREEN_AWAKE] ?: false,
                    useImperial = prefs[KEY_USE_IMPERIAL] ?: false,
                )
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            context.dataStore.edit { it[KEY_THEME_MODE] = mode.value }
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

        private companion object {
            val KEY_THEME_MODE = intPreferencesKey("theme_mode")
            val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
            val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
            val KEY_USE_IMPERIAL = booleanPreferencesKey("use_imperial")
        }
    }
