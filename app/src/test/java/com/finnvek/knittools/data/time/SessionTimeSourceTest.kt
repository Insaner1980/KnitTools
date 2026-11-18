package com.finnvek.knittools.data.time

import android.content.Context
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTimeSourceTest {
    @Test
    fun `unavailable boot fallback returns usable wall and elapsed timestamps`() {
        val snapshot = UnavailableBootSessionTimeSource.snapshot()

        assertTrue(snapshot.wallClockMillis > 0)
        assertTrue(snapshot.elapsedRealtimeMillis > 0)
        assertNull(snapshot.bootCount)
        assertNotNull(snapshot.zoneId)
    }

    @Test
    fun `android time source keeps boot identity optional when settings are unavailable`() {
        mockkStatic(SystemClock::class)
        val snapshot =
            try {
                every { SystemClock.elapsedRealtime() } returns 123L
                AndroidSessionTimeSource(mockk<Context>(relaxed = true)).snapshot()
            } finally {
                unmockkStatic(SystemClock::class)
            }

        assertTrue(snapshot.wallClockMillis > 0)
        assertTrue(snapshot.elapsedRealtimeMillis == 123L)
        assertNull(snapshot.bootCount)
        assertNotNull(snapshot.zoneId)
    }
}
