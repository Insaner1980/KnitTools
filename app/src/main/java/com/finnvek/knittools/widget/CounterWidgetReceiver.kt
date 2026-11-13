package com.finnvek.knittools.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.finnvek.knittools.analytics.UsageEvent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val pending = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                entryPoint.analytics().trackFromWidget(UsageEvent.WIDGET_INSTALLED)
            } finally {
                pending.finish()
            }
        }
    }
}
