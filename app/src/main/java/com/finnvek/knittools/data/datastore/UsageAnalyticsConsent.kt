package com.finnvek.knittools.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID

internal val USAGE_ANALYTICS_ENABLED = booleanPreferencesKey("usage_analytics_enabled")
internal val USAGE_ANALYTICS_GENERATION = stringPreferencesKey("usage_analytics_generation")

internal suspend fun DataStore<Preferences>.writeUsageAnalyticsConsent(enabled: Boolean): Boolean =
    editPreferencesSafely { preferences ->
        if (!enabled && preferences[USAGE_ANALYTICS_ENABLED] == true) {
            // A conflated off/on Flow emission must still retire the previous SDK.
            // This local marker is never sent to analytics.
            preferences[USAGE_ANALYTICS_GENERATION] = UUID.randomUUID().toString()
        }
        preferences[USAGE_ANALYTICS_ENABLED] = enabled
    }
