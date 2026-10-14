package com.finnvek.knittools.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test

class CounterWidgetRefreshTest {
    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `one failed instance does not prevent other instances or a later refresh`() =
        runTest {
            val context = context()
            val (first, second) = widgetInstances()
            coEvery { anyConstructed<CounterWidget>().update(context, first) } throws
                IllegalStateException("Unavailable")
            coEvery { anyConstructed<CounterWidget>().update(context, second) } returns Unit

            CounterWidgetState.refreshAll(context)

            coVerify(exactly = 1) { anyConstructed<CounterWidget>().update(context, second) }
            coEvery { anyConstructed<CounterWidget>().update(context, first) } returns Unit
            CounterWidgetState.refreshAll(context)
            coVerify(exactly = 2) { anyConstructed<CounterWidget>().update(context, second) }
        }

    @Test
    fun `refresh cancellation propagates and stops remaining instances`() {
        assertThrows(CancellationException::class.java) {
            runTest {
                val context = context()
                val (first, second) = widgetInstances()
                coEvery { anyConstructed<CounterWidget>().update(context, first) } throws CancellationException()

                try {
                    CounterWidgetState.refreshAll(context)
                } finally {
                    coVerify(exactly = 0) { anyConstructed<CounterWidget>().update(context, second) }
                }
            }
        }
    }

    private fun context(): Context =
        mockk<Context>(relaxed = true).also {
            mockkStatic(AppWidgetManager::class)
            every { AppWidgetManager.getInstance(it) } returns mockk<AppWidgetManager>(relaxed = true)
            every { it.applicationContext } returns it
            every { it.getSystemService(Context.APPWIDGET_SERVICE) } returns mockk<AppWidgetManager>(relaxed = true)
        }

    private fun widgetInstances(): Pair<GlanceId, GlanceId> {
        val first = mockk<GlanceId>()
        val second = mockk<GlanceId>()
        mockkConstructor(GlanceAppWidgetManager::class, CounterWidget::class)
        coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java) } returns
            listOf(first, second)
        return first to second
    }
}
