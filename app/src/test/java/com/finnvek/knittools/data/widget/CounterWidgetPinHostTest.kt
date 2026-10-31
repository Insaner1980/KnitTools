package com.finnvek.knittools.data.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterWidgetPinHostTest {
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
    fun `system metadata runs on IO while pin request stays on the caller`() =
        runTest {
            val callerThread = Thread.currentThread()
            val manager = mockk<AppWidgetManager>()
            val context = mockk<Context>(relaxed = true)
            mockkStatic(AppWidgetManager::class)
            every { AppWidgetManager.getInstance(context) } returns manager
            every { manager.isRequestPinAppWidgetSupported } answers {
                assertNotSame(callerThread, Thread.currentThread())
                true
            }
            every { manager.getAppWidgetIds(any()) } answers {
                assertNotSame(callerThread, Thread.currentThread())
                intArrayOf(42)
            }
            every { manager.requestPinAppWidget(any(), null, null) } answers {
                assertSame(callerThread, Thread.currentThread())
                true
            }
            val host = CounterWidgetPinHost(context)

            assertTrue(host.isSupported())
            assertTrue(host.hasInstalledWidgets())
            assertTrue(requestCounterWidgetPin(host))
        }

    @Test
    fun `supported launcher receives one pin request`() =
        runTest {
            val host = FakeWidgetPinHost(isSupported = true)
            assertTrue(requestCounterWidgetPin(host))
            assertEquals(1, host.requests)
        }

    @Test
    fun `unsupported launcher requests manual instructions without calling pin`() =
        runTest {
            val host = FakeWidgetPinHost(isSupported = false)
            assertFalse(requestCounterWidgetPin(host))
            assertEquals(0, host.requests)
        }

    @Test
    fun `launcher declining a supported request also uses manual instructions`() =
        runTest {
            val host = FakeWidgetPinHost(isSupported = true, accepts = false)
            assertFalse(requestCounterWidgetPin(host))
            assertEquals(1, host.requests)
        }

    private class FakeWidgetPinHost(
        private val isSupported: Boolean,
        private val accepts: Boolean = true,
    ) : WidgetPinHost {
        var requests = 0

        override suspend fun isSupported(): Boolean = isSupported

        override fun requestPin(): Boolean {
            requests++
            return accepts
        }
    }

    @Test
    fun `cancelled support lookup never issues a pin request`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val support = CompletableDeferred<Boolean>()
            var requests = 0
            val host =
                object : WidgetPinHost {
                    override suspend fun isSupported(): Boolean {
                        started.complete(Unit)
                        return support.await()
                    }

                    override fun requestPin(): Boolean {
                        requests++
                        return true
                    }
                }
            val request = launch { requestCounterWidgetPin(host) }
            started.await()
            request.cancel()
            support.complete(true)
            request.join()

            assertTrue(request.isCancelled)
            assertEquals(0, requests)
        }

    @Test
    fun `pin request failure uses manual instructions`() =
        runTest {
            val host =
                object : WidgetPinHost {
                    override suspend fun isSupported(): Boolean = true

                    override fun requestPin(): Boolean = throw IllegalStateException("Not foreground")
                }

            assertFalse(requestCounterWidgetPin(host))
        }

    @Test
    fun `pin request cancellation propagates`() =
        runTest {
            val host =
                object : WidgetPinHost {
                    override suspend fun isSupported(): Boolean = throw CancellationException()

                    override fun requestPin(): Boolean = error("Must not request pin")
                }

            assertTrue(runCatching { requestCounterWidgetPin(host) }.exceptionOrNull() is CancellationException)
        }
}
