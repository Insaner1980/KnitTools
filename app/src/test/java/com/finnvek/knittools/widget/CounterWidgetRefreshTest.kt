package com.finnvek.knittools.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.finnvek.knittools.data.datastore.updatePreferencesSafely
import com.finnvek.knittools.di.AppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class CounterWidgetRefreshTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `metadata lookup runs on IO and widget updates resume on the caller`() =
        runTest {
            val callerThread = Thread.currentThread()
            val context = context()
            val (first, second) = widgetInstances()
            coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java) } coAnswers {
                assertNotSame(callerThread, Thread.currentThread())
                assertEquals(AppDispatchers.IO, currentCoroutineContext()[ContinuationInterceptor])
                listOf(first, second)
            }
            coEvery { anyConstructed<CounterWidget>().update(context, any()) } coAnswers {
                assertSame(callerThread, Thread.currentThread())
            }

            CounterWidgetState.refreshAll(context)

            coVerify(exactly = 1) { anyConstructed<CounterWidget>().update(context, first) }
            coVerify(exactly = 1) { anyConstructed<CounterWidget>().update(context, second) }
        }

    @Test
    fun `cancellation during blocking metadata lookup prevents later widget updates`() =
        runTest {
            val context = context()
            val (first, second) = widgetInstances()
            val started = CompletableDeferred<Unit>()
            val releaseLookup = CountDownLatch(1)
            coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java) } coAnswers {
                started.complete(Unit)
                check(releaseLookup.await(5, TimeUnit.SECONDS))
                listOf(first, second)
            }

            val refresh = launch { CounterWidgetState.refreshAll(context) }
            try {
                started.await()
                refresh.cancel()
            } finally {
                releaseLookup.countDown()
            }
            refresh.join()

            assertTrue(refresh.isCancelled)
            coVerify(exactly = 0) { anyConstructed<CounterWidget>().update(context, any()) }
        }

    @Test
    fun `metadata lookup failure propagates and a later refresh still works`() =
        runTest {
            val context = context()
            val (first, second) = widgetInstances()
            coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java) } throws
                IllegalStateException("Unavailable")

            val failure = runCatching { CounterWidgetState.refreshAll(context) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            coVerify(exactly = 0) { anyConstructed<CounterWidget>().update(context, any()) }
            coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java) } returns
                listOf(first, second)
            coEvery { anyConstructed<CounterWidget>().update(context, any()) } returns Unit
            CounterWidgetState.refreshAll(context)
            coVerify(exactly = 1) { anyConstructed<CounterWidget>().update(context, second) }
        }

    @Test
    fun `failed shared state persistence prevents publishing to widget instances`() =
        runTest {
            val context = context()
            widgetInstances()
            mockkStatic("com.finnvek.knittools.data.datastore.PreferencesDataStoreSafetyKt")
            coEvery { any<DataStore<Preferences>>().updatePreferencesSafely(any()) } returns false

            val failure =
                runCatching { CounterWidgetState.syncAll(context, WidgetData(projectId = 2L)) }.exceptionOrNull()

            assertTrue(failure is IOException)
            coVerify(exactly = 0) {
                anyConstructed<GlanceAppWidgetManager>().getGlanceIds(CounterWidget::class.java)
                anyConstructed<CounterWidget>().update(any(), any())
            }
        }

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
