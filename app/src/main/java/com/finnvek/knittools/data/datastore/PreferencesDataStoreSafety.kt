package com.finnvek.knittools.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

internal val DataStore<Preferences>.safePreferencesData: Flow<Preferences>
    get() =
        data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
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
