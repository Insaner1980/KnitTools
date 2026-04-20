package com.finnvek.knittools.widget

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore by preferencesDataStore(
    name = "counter_widget",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class WidgetData(
    val projectName: String = "",
    val count: Int = 0,
    val projectId: Long = 0,
    val targetRows: Int? = null,
    val sectionName: String? = null,
    val currentStitch: Int = 0,
    val totalStitches: Int? = null,
    val stitchTrackingEnabled: Boolean = false,
)

object CounterWidgetState {
    private val KEY_PROJECT_NAME = stringPreferencesKey("project_name")
    private val KEY_COUNT = intPreferencesKey("count")
    private val KEY_PROJECT_ID = longPreferencesKey("project_id")
    private val KEY_TARGET_ROWS = intPreferencesKey("target_rows")
    private val KEY_SECTION_NAME = stringPreferencesKey("section_name")
    private val KEY_CURRENT_STITCH = intPreferencesKey("current_stitch")
    private val KEY_TOTAL_STITCHES = intPreferencesKey("total_stitches")
    private val KEY_STITCH_TRACKING = booleanPreferencesKey("stitch_tracking")

    suspend fun load(context: Context): WidgetData {
        val prefs = context.widgetDataStore.data.first()
        return WidgetData(
            projectName = prefs[KEY_PROJECT_NAME] ?: context.getString(R.string.default_project_name),
            count = prefs[KEY_COUNT] ?: 0,
            projectId = prefs[KEY_PROJECT_ID] ?: 0L,
            targetRows = prefs[KEY_TARGET_ROWS]?.takeIf { it > 0 },
            sectionName = prefs[KEY_SECTION_NAME]?.takeIf { it.isNotBlank() },
            currentStitch = prefs[KEY_CURRENT_STITCH] ?: 0,
            totalStitches = prefs[KEY_TOTAL_STITCHES]?.takeIf { it > 0 },
            stitchTrackingEnabled = prefs[KEY_STITCH_TRACKING] ?: false,
        )
    }

    suspend fun save(
        context: Context,
        data: WidgetData,
    ) {
        context.widgetDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[KEY_PROJECT_NAME] = data.projectName
                this[KEY_COUNT] = data.count
                this[KEY_PROJECT_ID] = data.projectId
                data.targetRows?.let { this[KEY_TARGET_ROWS] = it } ?: remove(KEY_TARGET_ROWS)
                data.sectionName?.let { this[KEY_SECTION_NAME] = it } ?: remove(KEY_SECTION_NAME)
                this[KEY_CURRENT_STITCH] = data.currentStitch
                data.totalStitches?.let { this[KEY_TOTAL_STITCHES] = it } ?: remove(KEY_TOTAL_STITCHES)
                this[KEY_STITCH_TRACKING] = data.stitchTrackingEnabled
            }
        }
    }

    suspend fun save(
        context: Context,
        project: CounterProjectEntity,
    ) = save(context, project.toWidgetData())
}

fun CounterProjectEntity.toWidgetData(): WidgetData =
    WidgetData(
        projectName = name,
        count = count,
        projectId = id,
        targetRows = totalRows,
        sectionName = sectionName,
        currentStitch = currentStitch,
        totalStitches = stitchCount,
        stitchTrackingEnabled = stitchTrackingEnabled,
    )
