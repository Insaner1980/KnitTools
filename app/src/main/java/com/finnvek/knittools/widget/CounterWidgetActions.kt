package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAction(appContext, action)
            } catch (_: Exception) {
                // Widget-toiminto on best effort; finish() ajetaan silti aina, eikä käyttäjädataa lokiteta.
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
        if (!entryPoint.proManager().hasFeatureAfterInitialLoad(ProFeature.WIDGET)) {
            return
        }

        val repository = entryPoint.counterRepository()
        val widgetData = repository.loadActionWidgetData(context) ?: return
        val widgetAction = action.toWidgetAction() ?: return
        applyCountChangeAndSync(context, repository, widgetData, widgetAction)
    }

    private fun widgetEntryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java,
        )

    private suspend fun CounterRepository.loadActionWidgetData(context: Context): WidgetData? {
        val widgetData = CounterWidgetState.load(context)
        if (widgetData.projectId == 0L) {
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
                null
            }
        }

    private suspend fun applyCountChangeAndSync(
        context: Context,
        repository: CounterRepository,
        widgetData: WidgetData,
        widgetAction: WidgetCounterAction,
    ) {
        repository.applyWidgetCountChange(widgetData.projectId, widgetAction.increments)
        val updatedProject =
            repository.activeWidgetProjectOrSyncFallback(context, widgetData) ?: return
        CounterWidgetState.syncAll(context, updatedProject.toWidgetData())
    }

    private suspend fun CounterRepository.activeWidgetProjectOrSyncFallback(
        context: Context,
        widgetData: WidgetData,
    ): CounterProject? =
        getActiveWidgetProject(widgetData) ?: run {
            syncFallbackWidgetData(context)
            null
        }

    private suspend fun CounterRepository.syncFallbackWidgetData(context: Context) {
        CounterWidgetState.syncAll(context, resolveWidgetDisplayData(context, emptyList()))
    }

    private val WidgetCounterAction.increments: Boolean
        get() = this == WidgetCounterAction.Increment

    companion object {
        private const val ACTION_INCREMENT = "com.finnvek.knittools.widget.INCREMENT"
        private const val ACTION_DECREMENT = "com.finnvek.knittools.widget.DECREMENT"

        fun incrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_INCREMENT)

        fun decrementIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_DECREMENT)
    }
}
