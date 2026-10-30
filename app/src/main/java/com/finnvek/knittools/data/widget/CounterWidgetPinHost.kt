package com.finnvek.knittools.data.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.finnvek.knittools.widget.CounterWidgetReceiver

internal interface WidgetPinHost {
    val isSupported: Boolean

    fun requestPin(): Boolean
}

internal class CounterWidgetPinHost(
    context: Context,
) : WidgetPinHost {
    private val manager = AppWidgetManager.getInstance(context)
    private val receiver = ComponentName(context, CounterWidgetReceiver::class.java)

    override val isSupported: Boolean
        get() = manager.isRequestPinAppWidgetSupported

    val hasInstalledWidgets: Boolean
        get() = manager.getAppWidgetIds(receiver).isNotEmpty()

    override fun requestPin(): Boolean = manager.requestPinAppWidget(receiver, null, null)
}

internal fun requestCounterWidgetPin(host: WidgetPinHost): Boolean = host.isSupported && host.requestPin()
