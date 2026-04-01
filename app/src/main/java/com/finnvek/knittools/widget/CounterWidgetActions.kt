package com.finnvek.knittools.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterWidgetActions : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val data = CounterWidgetState.load(context)
            val newCount = when (action) {
                ACTION_INCREMENT -> data.count + 1
                ACTION_DECREMENT -> maxOf(0, data.count - 1)
                else -> return@launch
            }
            CounterWidgetState.save(context, data.projectName, newCount)
            CounterWidget().updateAll(context)
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
