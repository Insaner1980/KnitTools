package com.finnvek.knittools.data.datastore

import android.util.Log
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
                Log.w(DATASTORE_TAG, "Asetusten lukeminen epäonnistui, käytetään oletuksia", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

internal suspend fun DataStore<Preferences>.readPreferencesOrNull(operation: String): Preferences? =
    try {
        data.first()
    } catch (e: IOException) {
        Log.w(DATASTORE_TAG, "$operation epäonnistui", e)
        null
    }

internal suspend fun DataStore<Preferences>.editPreferencesSafely(
    operation: String,
    transform: suspend (MutablePreferences) -> Unit,
): Boolean =
    try {
        edit(transform)
        true
    } catch (e: IOException) {
        Log.w(DATASTORE_TAG, "$operation epäonnistui", e)
        false
    }

internal suspend fun DataStore<Preferences>.updatePreferencesSafely(
    operation: String,
    transform: MutablePreferences.() -> Unit,
): Boolean =
    try {
        updateData { preferences ->
            preferences.toMutablePreferences().apply(transform)
        }
        true
    } catch (e: IOException) {
        Log.w(DATASTORE_TAG, "$operation epäonnistui", e)
        false
    }

private const val DATASTORE_TAG = "DataStoreSafety"
