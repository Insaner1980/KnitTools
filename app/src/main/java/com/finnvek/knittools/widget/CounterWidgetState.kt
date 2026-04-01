package com.finnvek.knittools.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "counter_widget")

data class WidgetData(
    val projectName: String = "My Project",
    val count: Int = 0,
    val projectId: Long = 0,
)

object CounterWidgetState {

    private val KEY_PROJECT_NAME = stringPreferencesKey("project_name")
    private val KEY_COUNT = intPreferencesKey("count")

    suspend fun load(context: Context): WidgetData {
        val prefs = context.widgetDataStore.data.first()
        return WidgetData(
            projectName = prefs[KEY_PROJECT_NAME] ?: "My Project",
            count = prefs[KEY_COUNT] ?: 0,
        )
    }

    suspend fun save(context: Context, name: String, count: Int) {
        context.widgetDataStore.edit {
            it[KEY_PROJECT_NAME] = name
            it[KEY_COUNT] = count
        }
    }

    private suspend fun androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.edit(
        transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        this.updateData { prefs ->
            prefs.toMutablePreferences().also { transform(it) }
        }
    }
}
