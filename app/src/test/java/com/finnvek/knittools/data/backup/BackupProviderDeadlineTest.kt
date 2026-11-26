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
    @Test fun providerResultAfterDeadlineIsRejectedEvenWhenWatchdogHasNotRun() {
        var now = 0L
        val scheduler = mockk<ScheduledExecutorService>()
        every { scheduler.schedule(any<Runnable>(), any<Long>(), TimeUnit.NANOSECONDS) } returns
            mockk<ScheduledFuture<*>>(relaxed = true)
        val closer = mockk<ExecutorService>(relaxed = true)
        val deadline = ProviderDeadline(mockk(), 30, scheduler, closer, onFinished = {}, nanoTime = { now }) {}

        assertThrows(IOException::class.java) {
            deadline.call {
                now = TimeUnit.MILLISECONDS.toNanos(31)
                42
            }
        }
        verify(exactly = 2) { closer.execute(any()) }
        deadline.finish()
    }

    @Test fun lateWatchdogFromCompletedCallCannotExpireTheNextCall() {
        val callbacks = mutableListOf<Runnable>()
        val scheduler = mockk<ScheduledExecutorService>()
        every { scheduler.schedule(any<Runnable>(), any<Long>(), TimeUnit.NANOSECONDS) } answers {
            callbacks += firstArg<Runnable>()
            mockk<ScheduledFuture<*>>(relaxed = true)
        }
        val closer = mockk<ExecutorService>(relaxed = true)
        val deadline = ProviderDeadline(mockk(), 30_000, scheduler, closer, onFinished = {}, onStopped = {})
        deadline.call { Unit }

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

    @Test fun zeroProgressExpiresEvenWhenWatchdogHasNotRun() {
        var now = 0L
        val scheduler = mockk<ScheduledExecutorService>()
        every { scheduler.schedule(any<Runnable>(), any<Long>(), TimeUnit.NANOSECONDS) } returns
            mockk<ScheduledFuture<*>>(relaxed = true)
        val closer = mockk<ExecutorService>(relaxed = true)
        val deadline = ProviderDeadline(mockk(), 30, scheduler, closer, onFinished = {}, nanoTime = { now }) {}
        deadline.call { Unit }

        assertEquals(0, deadline.call { 0 })
        now = TimeUnit.MILLISECONDS.toNanos(29)
        assertEquals(0, deadline.call(resetDeadline = false) { 0 })
        now = TimeUnit.MILLISECONDS.toNanos(30)
        assertThrows(IOException::class.java) {
            deadline.call(resetDeadline = false) { error("Expired operation must not read again") }
        }
        verify(exactly = 2) { closer.execute(any()) }
        deadline.finish()
    }
}
