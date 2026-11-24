package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.pro.ProFeature
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

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
                handleAction(appContext, action, intent.data?.lastPathSegment?.toLongOrNull())
            } catch (cancelled: CancellationException) {
                throw cancelled
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
        renderedProjectId: Long?,
    ) {
        val entryPoint = widgetEntryPoint(context)
        if (action == ACTION_RETRY) {
            withTimeoutOrNull(RETRY_TIMEOUT_MS) {
                entryPoint.billingManager().restorePurchasesWithResult()
                CounterWidgetState.refreshAll(context)
            }
            return
        }
        if (!entryPoint.proManager().hasFeatureAfterInitialLoad(ProFeature.WIDGET)) {
            return
        }

        val widgetAction = action.toWidgetAction() ?: return
        val repository = entryPoint.counterRepository()
        val shared = CounterWidgetState.load(context)
        val changed =
            renderedProjectId != null &&
                renderedProjectId > 0L &&
                renderedProjectId == shared.projectId &&
                repository.applyWidgetCountChange(renderedProjectId, widgetAction.increments)
        val latestShared = CounterWidgetState.load(context)
        val resolved = repository.resolveWidgetDisplayData(context, listOf(latestShared))
        try {
            CounterWidgetState.replaceIfCurrent(context, latestShared, resolved)
        } catch (_: IOException) {
            // Refresh can render the committed count directly from the repository.
        }
        CounterWidgetState.refreshAll(context)
        if (changed) entryPoint.analytics().trackFromWidget(UsageEvent.WIDGET_COUNTER_CHANGED)
    }

    private fun widgetEntryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java,
        )

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

    private val WidgetCounterAction.increments: Boolean
        get() = this == WidgetCounterAction.Increment

    companion object {
        private const val RETRY_TIMEOUT_MS = 8_000L
        private const val ACTION_INCREMENT = "com.finnvek.knittools.widget.INCREMENT"
        private const val ACTION_DECREMENT = "com.finnvek.knittools.widget.DECREMENT"
        private const val ACTION_RETRY = "com.finnvek.knittools.widget.RETRY"

        fun retryIntent(context: Context): Intent =
            Intent(context, CounterWidgetActions::class.java).setAction(ACTION_RETRY)

        fun incrementIntent(
            context: Context,
            projectId: Long,
        ): Intent = countIntent(context, ACTION_INCREMENT, projectId)

        fun decrementIntent(
            context: Context,
            projectId: Long,
        ): Intent = countIntent(context, ACTION_DECREMENT, projectId)

        private fun countIntent(
            context: Context,
            action: String,
            projectId: Long,
        ): Intent =
            Intent(context, CounterWidgetActions::class.java)
                .setAction(action)
                .setData("knittools://widget/$projectId".toUri())
    }
}
