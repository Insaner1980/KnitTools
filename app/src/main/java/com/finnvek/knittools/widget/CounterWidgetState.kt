package com.finnvek.knittools.widget

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.finnvek.knittools.data.datastore.updatePreferencesSafely
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.MainCounterLabelType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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
    val labelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    val customLabel: String? = null,
)

// Widgetit eivät ole projektisidonnaisia per instanssi, vaan kaikki instanssit
// peilaavat samaa aktiivista projektia shared widget-storesta.
object CounterWidgetState {
    val KEY_PROJECT_NAME = stringPreferencesKey("project_name")
    val KEY_COUNT = intPreferencesKey("count")
    val KEY_PROJECT_ID = longPreferencesKey("project_id")
    val KEY_TARGET_ROWS = intPreferencesKey("target_rows")
    val KEY_SECTION_NAME = stringPreferencesKey("section_name")
    val KEY_CURRENT_STITCH = intPreferencesKey("current_stitch")
    val KEY_TOTAL_STITCHES = intPreferencesKey("total_stitches")
    val KEY_STITCH_TRACKING = booleanPreferencesKey("stitch_tracking")
    val KEY_LABEL_TYPE = stringPreferencesKey("label_type")
    val KEY_CUSTOM_LABEL = stringPreferencesKey("custom_label")
    private val KEY_REVISION = intPreferencesKey("rev")

    fun defaultData(context: Context): WidgetData =
        WidgetData(projectName = context.getString(R.string.default_project_name))

    suspend fun load(context: Context): WidgetData {
        val prefs = context.widgetDataStore.safePreferencesData.first()
        return fromPreferences(context, prefs)
    }

    suspend fun save(
        context: Context,
        data: WidgetData,
    ) {
        context.widgetDataStore.updatePreferencesSafely {
            applyWidgetData(data)
        }
    }

    suspend fun loadGlance(
        context: Context,
        glanceId: GlanceId,
    ): WidgetData {
        val prefs =
            getAppWidgetState(
                context,
                PreferencesGlanceStateDefinition,
                glanceId,
            )
        return fromPreferences(context, prefs)
    }

    suspend fun saveGlance(
        context: Context,
        glanceId: GlanceId,
        data: WidgetData,
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                applyWidgetData(data)
            }
        }
    }

    suspend fun syncAll(
        context: Context,
        data: WidgetData,
    ) {
        // Päivitä ensin jaettu store ja peilaa sama data kaikkiin Glance-instansseihin.
        save(context, data)

        updatePlacedWidgets(context) { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    applyWidgetData(data)
                    this[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
                }
            }
        }
    }

    suspend fun refreshAll(context: Context) {
        updatePlacedWidgets(context) {}
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun updatePlacedWidgets(
        context: Context,
        beforeUpdate: suspend (GlanceId) -> Unit,
    ) {
        val widget = CounterWidget()
        val glanceIds =
            withContext(AppDispatchers.IO) {
                // Glance 1.1.1 queries AppWidgetManager synchronously, even in its suspend API.
                GlanceAppWidgetManager(context).getGlanceIds(CounterWidget::class.java)
            }
        glanceIds.forEach { glanceId ->
            try {
                beforeUpdate(glanceId)
                widget.update(context, glanceId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A removed or unavailable instance must not prevent the remaining widgets from refreshing.
            }
        }
    }

    suspend fun save(
        context: Context,
        project: CounterProject,
    ) = save(context, project.toWidgetData())

    fun fromPreferences(
        context: Context,
        prefs: Preferences,
    ): WidgetData =
        WidgetData(
            projectName = prefs[KEY_PROJECT_NAME] ?: context.getString(R.string.default_project_name),
            count = prefs[KEY_COUNT] ?: 0,
            projectId = prefs[KEY_PROJECT_ID] ?: 0L,
            targetRows = prefs[KEY_TARGET_ROWS]?.takeIf { it > 0 },
            sectionName = prefs[KEY_SECTION_NAME]?.takeIf { it.isNotBlank() },
            currentStitch = prefs[KEY_CURRENT_STITCH] ?: 0,
            totalStitches = prefs[KEY_TOTAL_STITCHES]?.takeIf { it > 0 },
            stitchTrackingEnabled = prefs[KEY_STITCH_TRACKING] ?: false,
            labelType =
                MainCounterLabelType.entries.firstOrNull { it.name == prefs[KEY_LABEL_TYPE] }
                    ?: MainCounterLabelType.ROWS,
            customLabel = prefs[KEY_CUSTOM_LABEL]?.takeIf { it.isNotBlank() },
        )

    private fun MutablePreferences.applyWidgetData(data: WidgetData) {
        this[KEY_PROJECT_NAME] = data.projectName
        this[KEY_COUNT] = data.count
        this[KEY_PROJECT_ID] = data.projectId
        data.targetRows?.let { this[KEY_TARGET_ROWS] = it } ?: remove(KEY_TARGET_ROWS)
        data.sectionName?.let { this[KEY_SECTION_NAME] = it } ?: remove(KEY_SECTION_NAME)
        this[KEY_CURRENT_STITCH] = data.currentStitch
        data.totalStitches?.let { this[KEY_TOTAL_STITCHES] = it } ?: remove(KEY_TOTAL_STITCHES)
        this[KEY_STITCH_TRACKING] = data.stitchTrackingEnabled
        this[KEY_LABEL_TYPE] = data.labelType.name
        data.customLabel?.let { this[KEY_CUSTOM_LABEL] = it } ?: remove(KEY_CUSTOM_LABEL)
    }
}

fun CounterProject.toWidgetData(): WidgetData {
    val label = CounterValueFormatter.mainCounterLabel(craftType, mainCounterLabelType, mainCounterCustomLabel)
    return WidgetData(
        projectName = name,
        count = count,
        projectId = id,
        targetRows = this.targetRows,
        sectionName = sectionName,
        currentStitch = currentStitch,
        totalStitches = stitchCount,
        stitchTrackingEnabled = stitchTrackingEnabled,
        labelType = label.labelType,
        customLabel = label.customLabel,
    )
}
