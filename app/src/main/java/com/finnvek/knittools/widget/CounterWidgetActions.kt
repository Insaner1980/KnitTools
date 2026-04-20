package com.finnvek.knittools.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
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

                // Pakota launcherin widget-päivitys ACTION_APPWIDGET_UPDATE -broadcastilla.
                // Glancen widget.update() ei aina laukaise RemoteViews-päivitystä
                // BroadcastReceiver-kontekstissa → käytetään OS:n virallista mekanismia.
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds =
                    appWidgetManager.getAppWidgetIds(
                        ComponentName(context, CounterWidgetReceiver::class.java),
                    )
                Log.d(TAG, "broadcasting update to ${widgetIds.size} widget instance(s)")
                val updateIntent =
                    Intent(context, CounterWidgetReceiver::class.java).apply {
                        this.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                context.sendBroadcast(updateIntent)
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
