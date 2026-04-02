package com.finnvek.knittools.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore by preferencesDataStore(name = "counter_widget")

data class WidgetData(
    val projectName: String = "My Project",
    val count: Int = 0,
    val projectId: Long = 0,
)

object CounterWidgetState {
    private val KEY_PROJECT_NAME = stringPreferencesKey("project_name")
    private val KEY_COUNT = intPreferencesKey("count")
    private val KEY_PROJECT_ID = longPreferencesKey("project_id")

    suspend fun load(context: Context): WidgetData {
        val prefs = context.widgetDataStore.data.first()
        return WidgetData(
            projectName = prefs[KEY_PROJECT_NAME] ?: "My Project",
            count = prefs[KEY_COUNT] ?: 0,
            projectId = prefs[KEY_PROJECT_ID] ?: 0L,
        )
    }

    suspend fun save(
        context: Context,
        name: String,
        count: Int,
        projectId: Long,
    ) {
        context.widgetDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[KEY_PROJECT_NAME] = name
                this[KEY_COUNT] = count
                this[KEY_PROJECT_ID] = projectId
            }
        }
    }
}
