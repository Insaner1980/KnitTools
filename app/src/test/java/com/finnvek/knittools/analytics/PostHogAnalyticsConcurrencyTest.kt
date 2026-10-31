package com.finnvek.knittools.analytics

import androidx.lifecycle.LifecycleOwner
import com.posthog.PostHogInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PostHogAnalyticsConcurrencyTest {
    private val owner = mockk<LifecycleOwner>()

    @Test
    fun `slow initialization does not block lifecycle and navigation callbacks`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val creations = AtomicInteger()
        val client = mockk<PostHogConsentClient>(relaxed = true)
        val analytics =
            analyticsWithBlockedInitialization(entered, release) {
                creations.incrementAndGet()
                client
            }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val initializing = workers.submit { analytics.setEnabled(true) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            workers.submit { updateForegroundScreen(analytics) }.get(5, TimeUnit.SECONDS)
            workers.submit { analytics.setEnabled(true) }.get(5, TimeUnit.SECONDS)
            verify(exactly = 0) { client.capture(any(), any()) }
            release.countDown()
            initializing.get(5, TimeUnit.SECONDS)
            assertEquals(1, creations.get())
            verify(exactly = 1) { client.capture("app opened", any()) }
            verify(exactly = 1) {
                client.capture("\$screen", mapOf("\$screen_name" to "gauge", "\$geoip_disable" to true))
            }
            verify(exactly = 0) { client.capture("screen exited", any()) }
        } finally {
            release.countDown()
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `slow SDK shutdown does not block callbacks and delivery is already retired`() {
        checkSlowRetirement(blockShutdown = true)
    }

    @Test
    fun `slow storage cleanup does not block callbacks and delivery is already retired`() {
        checkSlowRetirement(blockShutdown = false)
    }

    @Test
    fun `revocation during initialization returns promptly and rejects the unfinished client`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val sdk = mockk<PostHogInterface>(relaxed = true)
        val gate = AnalyticsDeliveryGate()
        val client = PostHogConsentClient(sdk, gate) {}
        val analytics = analyticsWithBlockedInitialization(entered, release) { client }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val initializing = workers.submit { analytics.setEnabled(true) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            workers.submit { analytics.setEnabled(false) }.get(5, TimeUnit.SECONDS)
            updateForegroundScreen(analytics)
            release.countDown()
            initializing.get(5, TimeUnit.SECONDS)
            assertTrue(gate.isRetired)
            verify(exactly = 1) { sdk.close() }
            verify(exactly = 0) { sdk.capture(any(), properties = any()) }
        } finally {
            release.countDown()
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `new consent generation supersedes initialization and closes it before creating a fresh client`() {
        checkSupersededInitialization(revokeFirst = false, failFirst = false)
    }

    @Test
    fun `off-on with the same generation cannot publish the old initialization`() {
        checkSupersededInitialization(revokeFirst = true, failFirst = false)
    }

    @Test
    fun `superseded initialization failure cannot disable newer consent`() {
        checkSupersededInitialization(revokeFirst = true, failFirst = true)
    }

    private fun analyticsWithBlockedInitialization(
        entered: CountDownLatch,
        release: CountDownLatch,
        createClient: () -> PostHogConsentClient,
    ): PostHogAnalytics =
        PostHogAnalytics(
            PostHogClientFactory {
                val client = createClient()
                entered.countDown()
                check(release.await(10, TimeUnit.SECONDS))
                client
            },
        )

    private fun checkSupersededInitialization(
        revokeFirst: Boolean,
        failFirst: Boolean,
    ) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val creations = AtomicInteger()
        val oldSdk = mockk<PostHogInterface>(relaxed = true)
        val oldGate = AnalyticsDeliveryGate()
        val old = PostHogConsentClient(oldSdk, oldGate) {}
        val fresh = mockk<PostHogConsentClient>(relaxed = true)
        val analytics =
            PostHogAnalytics(
                PostHogClientFactory {
                    if (creations.incrementAndGet() == 1) {
                        entered.countDown()
                        check(release.await(10, TimeUnit.SECONDS))
                        if (failFirst) throw IllegalStateException("Synthetic initialization failure")
                        old
                    } else {
                        if (!failFirst) {
                            assertTrue(oldGate.isRetired)
                            verify(exactly = 1) { oldSdk.close() }
                        }
                        fresh
                    }
                },
            )
        val workers = Executors.newFixedThreadPool(2)
        try {
            val initializing = workers.submit { analytics.setEnabled(true, "first") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            if (revokeFirst) workers.submit { analytics.setEnabled(false, "first") }.get(5, TimeUnit.SECONDS)
            val nextThread = AtomicReference<Thread>()
            val enabling =
                workers.submit {
                    nextThread.set(Thread.currentThread())
                    analytics.setEnabled(true, if (revokeFirst) "first" else "second")
                }
            awaitBlockedWorker(nextThread)
            // The newer request is waiting for serialized SDK work, without owning the state monitor.
            updateForegroundScreen(analytics)
            release.countDown()
            initializing.get(5, TimeUnit.SECONDS)
            enabling.get(5, TimeUnit.SECONDS)
            analytics.track(UsageEvent.PROJECT_CREATED)
            assertEquals(2, creations.get())
            verify(exactly = 0) { oldSdk.capture(any(), properties = any()) }
            verify(exactly = 1) { fresh.capture("app opened", any()) }
            verify(exactly = 1) { fresh.capture("project created", any()) }
        } finally {
            release.countDown()
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private fun awaitBlockedWorker(worker: AtomicReference<Thread>) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (worker.get()?.state != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.sleep(1)
        }
        assertEquals(Thread.State.BLOCKED, worker.get()?.state)
    }

    private fun checkSlowRetirement(blockShutdown: Boolean) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val sdk = mockk<PostHogInterface>(relaxed = true)
        val gate = AnalyticsDeliveryGate()
        val block = {
            assertTrue(gate.isRetired)
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
        }
        if (blockShutdown) every { sdk.close() } answers { block() }
        val client = PostHogConsentClient(sdk, gate) { if (!blockShutdown) block() }
        val analytics = PostHogAnalytics(PostHogClientFactory { client })
        val workers = Executors.newFixedThreadPool(2)
        try {
            analytics.setEnabled(true)
            assertFalse(gate.isRetired)
            val retiring = workers.submit { analytics.setEnabled(false) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            workers.submit { updateForegroundScreen(analytics) }.get(5, TimeUnit.SECONDS)
            verify(exactly = 0) { sdk.capture(any(), properties = any()) }
            release.countDown()
            retiring.get(5, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private fun updateForegroundScreen(analytics: PostHogAnalytics) {
        analytics.onStart(owner)
        analytics.screenViewed("counter")
        analytics.track(UsageEvent.PROJECT_CREATED)
        analytics.onStop(owner)
        analytics.screenViewed("gauge")
        analytics.onStart(owner)
    }
}
