package com.finnvek.knittools.data.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWidgetPinHostTest {
    @Test
    fun `supported launcher receives one pin request`() {
        val host = FakeWidgetPinHost(isSupported = true)
        assertTrue(requestCounterWidgetPin(host))
        assertEquals(1, host.requests)
    }

    @Test
    fun `unsupported launcher requests manual instructions without calling pin`() {
        val host = FakeWidgetPinHost(isSupported = false)
        assertFalse(requestCounterWidgetPin(host))
        assertEquals(0, host.requests)
    }

    @Test
    fun `launcher declining a supported request also uses manual instructions`() {
        val host = FakeWidgetPinHost(isSupported = true, accepts = false)
        assertFalse(requestCounterWidgetPin(host))
        assertEquals(1, host.requests)
    }

    private class FakeWidgetPinHost(
        override val isSupported: Boolean,
        private val accepts: Boolean = true,
    ) : WidgetPinHost {
        var requests = 0

        override fun requestPin(): Boolean {
            requests++
            return accepts
        }
    }
}
