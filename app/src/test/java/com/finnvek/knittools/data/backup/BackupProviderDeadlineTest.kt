package com.finnvek.knittools.data.backup

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BackupProviderDeadlineTest {
    @Test fun lateWatchdogFromCompletedCallCannotExpireTheNextCall() {
        val callbacks = mutableListOf<Runnable>()
        val scheduler = mockk<ScheduledExecutorService>()
        every { scheduler.schedule(any<Runnable>(), any<Long>(), TimeUnit.NANOSECONDS) } answers {
            callbacks += firstArg<Runnable>()
            mockk<ScheduledFuture<*>>(relaxed = true)
        }
        val closer = mockk<ExecutorService>(relaxed = true)
        val deadline = ProviderDeadline(mockk(), 30_000, scheduler, closer, {}, {})
        deadline.call { Unit }
        deadline.markOpened()

        val result =
            deadline.call {
                callbacks.first().run()
                42
            }

        assertEquals(42, result)
        verify(exactly = 0) { closer.execute(any()) }
        assertThrows(IOException::class.java) {
            deadline.call { callbacks.last().run() }
        }
        verify(exactly = 2) { closer.execute(any()) }
        deadline.finish()
    }
}
