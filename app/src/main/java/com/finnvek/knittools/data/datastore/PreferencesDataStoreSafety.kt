package com.finnvek.knittools.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

internal val DataStore<Preferences>.safePreferencesData: Flow<Preferences>
    get() =
        data.retryWhen { exception, attempt ->
            if (exception !is IOException) return@retryWhen false
            if (attempt == 0L) emit(emptyPreferences())
            delay(100L shl attempt.coerceAtMost(5L).toInt())
            true
        }

internal suspend fun DataStore<Preferences>.readPreferencesOrNull(): Preferences? =
    try {
        data.first()
    } catch (_: IOException) {
        null
    }

internal suspend fun DataStore<Preferences>.editPreferencesSafely(
    transform: suspend (MutablePreferences) -> Unit,
): Boolean =
    try {
        edit(transform)
        true
    } catch (_: IOException) {
        false
    }

internal suspend fun DataStore<Preferences>.updatePreferencesSafely(
    transform: MutablePreferences.() -> Unit,
): Boolean =
    try {
        updateData { preferences ->
            preferences.toMutablePreferences().apply(transform)
        }
        true
    } catch (_: IOException) {
        false
    }
