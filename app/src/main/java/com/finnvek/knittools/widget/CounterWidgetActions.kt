package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterWidgetActions : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action ?: run {
            Log.w(TAG, "onReceive with null action")
            return
        }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java,
                    )
                if (!entryPoint.proManager().isPro()) {
                    Log.w(TAG, "Widget action ignored — not Pro")
                    return@launch
                }

                val repository = entryPoint.counterRepository()
                val widgetData = CounterWidgetState.load(context)

                if (widgetData.projectId == 0L) {
                    Log.w(TAG, "Widget action ignored — no projectId in widget state")
                    return@launch
                }

                val project = repository.getProject(widgetData.projectId) ?: run {
                    Log.w(TAG, "Widget action ignored — project ${widgetData.projectId} not found")
                    return@launch
                }

                val delta =
                    when (action) {
                        ACTION_INCREMENT -> 1
                        ACTION_DECREMENT -> -1
                        else -> {
                            Log.w(TAG, "Unknown action: $action")
                            return@launch
                        }
                    }

                Log.d(TAG, "action=$action projectId=${project.id} before=${project.count}")
                repository.adjustProjectCount(project.id, delta)
                val updatedProject = repository.getProject(project.id) ?: run {
                    Log.e(TAG, "Project ${project.id} disappeared after adjustProjectCount")
                    return@launch
                }
                Log.d(TAG, "after=${updatedProject.count}")
                CounterWidgetState.save(context, updatedProject)

                // Glance ei havaitse oman DataStoremme muutoksia → bump rev-laskuri
                // Glancen omaan state-storeen, jolloin Glance merkitsee widgetin
                // dirty:ksi ja provideGlance ajetaan uudelleen widget.update():ssa.
                val widget = CounterWidget()
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(CounterWidget::class.java)
                Log.d(TAG, "refreshing ${glanceIds.size} widget instance(s)")
                glanceIds.forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        prefs.toMutablePreferences().apply {
                            val key = intPreferencesKey("rev")
                            this[key] = (prefs[key] ?: 0) + 1
                        }
                    }
                    widget.update(context, id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Widget action failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }


    companion object {
        private const val TAG = "CounterWidgetActions"
        private const val ACTION_INCREMENT = "com.finnvek.knittools.widget.INCREMENT"
        private const val ACTION_DECREMENT = "com.finnvek.knittools.widget.DECREMENT"

        fun incrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_INCREMENT)

        fun decrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_DECREMENT)
    }
}
