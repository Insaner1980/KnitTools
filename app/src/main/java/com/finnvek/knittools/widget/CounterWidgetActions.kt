package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterWidgetActions : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java,
                    )
                val repository = entryPoint.counterRepository()
                val widgetData = CounterWidgetState.load(context)

                if (widgetData.projectId == 0L) return@launch

                val project = repository.getProject(widgetData.projectId) ?: return@launch

                val delta =
                    when (action) {
                        ACTION_INCREMENT -> 1
                        ACTION_DECREMENT -> -1
                        else -> return@launch
                    }

                repository.adjustProjectCount(project.id, delta)
                val updatedProject = repository.getProject(project.id) ?: return@launch
                CounterWidgetState.save(context, updatedProject.name, updatedProject.count, updatedProject.id)
                CounterWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_INCREMENT = "com.finnvek.knittools.widget.INCREMENT"
        private const val ACTION_DECREMENT = "com.finnvek.knittools.widget.DECREMENT"

        fun incrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_INCREMENT)

        fun decrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_DECREMENT)
    }
}
