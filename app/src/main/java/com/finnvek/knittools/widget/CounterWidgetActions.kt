package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterWidgetActions : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action =
            intent?.action ?: run {
                Log.w(TAG, "onReceive with null action")
                return
            }
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAction(appContext, action)
            } catch (e: Exception) {
                Log.e(TAG, "Widget handling failed: ${e.javaClass.simpleName}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAction(
        context: Context,
        action: String,
    ) {
        val entryPoint = widgetEntryPoint(context)
        if (!entryPoint.proManager().hasFeature(ProFeature.WIDGET)) {
            Log.w(TAG, "Widget action ignored — not Pro")
            return
        }

        val repository = entryPoint.counterRepository()
        val widgetData = repository.loadActionWidgetData(context) ?: return
        val widgetAction = action.toWidgetAction() ?: return
        applyCountChangeAndSync(context, repository, widgetData, action, widgetAction)
    }

    private fun widgetEntryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java,
        )

    private suspend fun CounterRepository.loadActionWidgetData(context: Context): WidgetData? {
        val widgetData = CounterWidgetState.load(context)
        if (widgetData.projectId == 0L) {
            Log.w(TAG, "Widget action ignored — widget state incomplete")
            return null
        }
        return widgetData
    }

    private fun String.toWidgetAction(): WidgetCounterAction? =
        when (this) {
            ACTION_INCREMENT -> {
                WidgetCounterAction.Increment
            }

            ACTION_DECREMENT -> {
                WidgetCounterAction.Decrement
            }

            else -> {
                Log.w(TAG, "Unknown action: $this")
                null
            }
        }

    private suspend fun applyCountChangeAndSync(
        context: Context,
        repository: CounterRepository,
        widgetData: WidgetData,
        action: String,
        widgetAction: WidgetCounterAction,
    ) {
        Log.d(TAG, "Applying widget action: $action")
        val changed = repository.applyWidgetCountChange(widgetData.projectId, widgetAction.increments)
        if (!changed) {
            Log.d(TAG, "Widget action ignored — target unchanged or inactive")
        }
        val updatedProject =
            repository.activeWidgetProjectOrSyncFallback(context, widgetData) {
                Log.e(TAG, "Widget action target disappeared or became inactive after update")
            } ?: return
        Log.d(TAG, "Widget action applied successfully")
        CounterWidgetState.syncAll(context, updatedProject.toWidgetData())
    }

    private suspend fun CounterRepository.activeWidgetProjectOrSyncFallback(
        context: Context,
        widgetData: WidgetData,
        onMissingTarget: () -> Unit,
    ): CounterProject? =
        getActiveWidgetProject(widgetData) ?: run {
            onMissingTarget()
            syncFallbackWidgetData(context)
            null
        }

    private suspend fun CounterRepository.syncFallbackWidgetData(context: Context) {
        CounterWidgetState.syncAll(context, resolveWidgetDisplayData(context, emptyList()))
    }

    private val WidgetCounterAction.increments: Boolean
        get() = this == WidgetCounterAction.Increment

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
