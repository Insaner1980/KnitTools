package com.finnvek.knittools.data.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.widget.CounterWidgetReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface WidgetPinHost {
    suspend fun isSupported(): Boolean

    fun requestPin(): Boolean
}

internal class CounterWidgetPinHost(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.IO,
) : WidgetPinHost {
    private val manager = AppWidgetManager.getInstance(context)
    private val receiver = ComponentName(context, CounterWidgetReceiver::class.java)

    override suspend fun isSupported(): Boolean =
        withContext(ioDispatcher) {
            try {
                manager.isRequestPinAppWidgetSupported
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }

    suspend fun hasInstalledWidgets(): Boolean =
        withContext(ioDispatcher) {
            manager.getAppWidgetIds(receiver).isNotEmpty()
        }

    override fun requestPin(): Boolean = manager.requestPinAppWidget(receiver, null, null)
}

internal suspend fun requestCounterWidgetPin(host: WidgetPinHost): Boolean =
    try {
        host.isSupported() && host.requestPin()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
