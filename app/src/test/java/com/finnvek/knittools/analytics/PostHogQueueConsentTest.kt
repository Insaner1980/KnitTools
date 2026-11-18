package com.finnvek.knittools.analytics

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.finnvek.knittools.data.datastore.USAGE_ANALYTICS_ENABLED
import com.finnvek.knittools.data.datastore.USAGE_ANALYTICS_GENERATION
import com.finnvek.knittools.data.datastore.writeUsageAnalyticsConsent
import com.posthog.PostHog
import com.posthog.internal.PostHogDateProvider
import com.posthog.internal.PostHogMemoryPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PostHogQueueConsentTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val harnesses = mutableListOf<Harness>()

    private fun harness(): Harness = Harness(temporaryFolder.newFolder("sdk")).also(harnesses::add)

    private fun Harness.queueEventWithFailedCleanup(): Pair<PostHogAnalytics, PostHogTestInstance> {
        val analytics = controller(deleteDirectory = { false })
        analytics.setEnabled(true)
        val instance = latest()
        analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
        instance.drain()
        return analytics to instance
    }

    @After
    fun cleanup() {
        harnesses.forEach(Harness::close)
    }

    @Test
    fun `queued event is discarded on revoke and only new consent can deliver`() {
        val harness = harness()
        val analytics = harness.controller()
        analytics.setEnabled(true)
        val old = harness.latest()
        analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
        old.drain()
        assertEquals(listOf(BEFORE), old.queuedNames())
        assertEquals(1, old.queue.size)
        assertTrue(harness.attempts.isEmpty())
        println("RW-07B acceptance: A on disk, batch attempts=0, no request in flight")

        analytics.setEnabled(false)
        analytics.track(UsageEvent.PDF_IMPORT_FAILED)
        old.drain()
        old.sdk.flush()
        old.queue.flush()
        old.drain()
        harness.clock.advanceSeconds(120)
        old.queue.flush()
        old.sdk.close()
        old.drain()
        assertTrue(harness.attempts.isEmpty())
        assertTrue("No retired event records remain", old.queuedNames().isEmpty())
        assertEquals(0, old.queue.size)

        analytics.setEnabled(true)
        val fresh = harness.latest()
        assertNotEquals(old.directory, fresh.directory)
        analytics.track(UsageEvent.PROJECT_CREATED)
        fresh.drain()
        fresh.sdk.flush()
        fresh.drain()
        assertEquals(listOf(listOf(AFTER)), harness.attempts)
        assertTrue(fresh.queuedNames().isEmpty())
        assertFalse(harness.attempts.flatten().contains(DISABLED))
        println("RW-07B acceptance: disabled capture/flush/retry/close silent; re-opt-in delivered only C")
    }

    @Test
    fun `late writer and failed cleanup cannot replay through recreation with the same storage`() {
        val harness = harness()
        val unrelated = File(harness.root, "unrelated.txt").apply { writeText("keep") }
        val analytics = harness.controller(deleteDirectory = { false })
        analytics.setEnabled(true)
        val old = harness.latest()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        old.executor.submit {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
        }
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            // Capture is admitted, but its asynchronous disk write is still pending.
            analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
            analytics.setEnabled(false)
        } finally {
            release.countDown()
        }
        old.drain()
        assertTrue(harness.attempts.isEmpty())
        // Even explicitly reinitializing the old SDK/config cannot reopen its gate.
        old.sdk.setup(old.config)
        old.sdk.optIn()
        old.sdk.capture(BEFORE, properties = mapOf("\$geoip_disable" to true))
        old.drain()
        assertEquals(listOf(BEFORE), old.queuedNames())

        val recreated = harness.controller(deleteDirectory = { false })
        recreated.setEnabled(false)
        recreated.track(UsageEvent.PDF_IMPORT_FAILED)
        assertEquals(1, harness.instances.size)
        recreated.setEnabled(true)
        val fresh = harness.latest()
        assertNotEquals(old.directory, fresh.directory)
        assertTrue(old.directory.exists())
        assertTrue(fresh.queuedNames().isEmpty())
        recreated.track(UsageEvent.PROJECT_CREATED)
        fresh.drain()
        fresh.sdk.flush()
        fresh.drain()
        old.sdk.flush()
        old.drain()
        assertEquals(listOf(listOf(AFTER)), harness.attempts)
        assertTrue(old.queuedNames().isEmpty())
        assertEquals("keep", unrelated.readText())

        recreated.setEnabled(false)
        old.drain()
        fresh.drain()
        val next = harness.controller()
        next.setEnabled(true)
        assertFalse(old.directory.exists())
        assertFalse(fresh.directory.exists())
        assertTrue(unrelated.exists())
        assertEquals(listOf(listOf(AFTER)), harness.attempts)
        println("RW-07B acceptance: late writer + failed deletion + same-root recreation cannot revive A")
    }

    @Test
    fun `a request waiting before the delivery gate is rejected even after re-opt-in`() {
        val harness = harness()
        val (analytics, old) = harness.queueEventWithFailedCleanup()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = AtomicBoolean(true)
        harness.beforeGate = {
            if (first.getAndSet(false)) {
                entered.countDown()
                check(release.await(10, TimeUnit.SECONDS))
            }
        }
        try {
            old.sdk.flush()
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            analytics.setEnabled(false)
            analytics.setEnabled(true)
            val fresh = harness.latest()
            analytics.track(UsageEvent.PROJECT_CREATED)
            fresh.drain()
            fresh.sdk.flush()
            fresh.drain()
        } finally {
            release.countDown()
        }
        old.drain()
        assertEquals(listOf(listOf(AFTER)), harness.attempts)
        assertTrue(old.queuedNames().isEmpty())
        println("RW-07B acceptance: request paused before dispatch stayed blocked after re-opt-in")
    }

    @Test
    fun `already in-flight delivery is distinct from forbidden retries after revocation`() {
        val harness = harness()
        val (analytics, old) = harness.queueEventWithFailedCleanup()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        harness.responseCode.set(503)
        harness.atRecorder = {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
        }
        try {
            old.sdk.flush()
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            assertEquals(listOf(listOf(BEFORE)), harness.attempts)
            analytics.setEnabled(false)
            assertTrue(requireNotNull(harness.recordingCall).isCanceled())
        } finally {
            release.countDown()
        }
        old.drain()
        harness.atRecorder = null
        harness.clock.advanceSeconds(120)
        old.queue.flush()
        old.drain()
        old.sdk.close()
        old.drain()
        assertEquals(listOf(listOf(BEFORE)), harness.attempts)
        analytics.setEnabled(true)
        harness.responseCode.set(200)
        analytics.track(UsageEvent.PROJECT_CREATED)
        harness.latest().drain()
        harness.latest().sdk.flush()
        harness.latest().drain()
        assertEquals(listOf(listOf(BEFORE), listOf(AFTER)), harness.attempts)
        println("RW-07B acceptance: one pre-revocation in-flight attempt; cancellation requested; no old retry")
    }

    @Test
    fun `queue cleanup preserves a regular file with a consent directory name`() {
        val root = temporaryFolder.newFolder()
        val old = File(root, "consent-00000000-0000-0000-0000-000000000001").apply { mkdir() }
        val unrelated = File(root, "consent-00000000-0000-0000-0000-000000000002").apply { writeText("keep") }

        val fresh = AnalyticsQueueStorage(root).createDirectory()

        assertFalse(old.exists())
        assertEquals("keep", unrelated.readText())
        assertTrue(fresh.isDirectory)
    }

    @Test
    fun `new instance cannot import prior default or consent queues when cleanup throws`() {
        val harness = harness()
        val analytics = harness.controller(deleteDirectory = { throw SecurityException("Synthetic denied deletion") })
        analytics.setEnabled(true)
        val old = harness.latest()
        analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
        old.drain()
        val pending =
            requireNotNull(old.queue.queueDirectory)
                .listFiles()
                .orEmpty()
                .single()
                .readBytes()
        analytics.setEnabled(false)
        old.drain()
        // Interrupted cleanup/process exit leaves an actual SDK-serialized record behind.
        val leftover = requireNotNull(old.queue.queueDirectory).apply { mkdirs() }
        File(leftover, "retained.event").writeBytes(pending)
        val legacy = File(harness.root, "posthog-disk-queue/$TOKEN").apply { mkdirs() }
        File(legacy, "legacy.event").writeBytes(pending)

        val restarted = harness.controller(deleteDirectory = { throw SecurityException("Synthetic denied deletion") })
        restarted.setEnabled(false)
        assertEquals(1, harness.instances.size)
        restarted.setEnabled(true)
        val fresh = harness.latest()
        assertTrue(requireNotNull(fresh.config.legacyStoragePrefix).startsWith(fresh.directory.path))
        assertNotEquals(old.config.storagePrefix, fresh.config.storagePrefix)
        fresh.sdk.flush()
        fresh.drain()
        restarted.track(UsageEvent.PROJECT_CREATED)
        fresh.drain()
        fresh.sdk.flush()
        fresh.drain()
        assertEquals(listOf(listOf(AFTER)), harness.attempts)
        assertTrue(File(leftover, "retained.event").exists())
        assertTrue(File(legacy, "legacy.event").exists())
        println("RW-07B acceptance: interrupted/throwing cleanup leaves inert files, never a reusable queue")
    }

    @Test
    fun `DataStore keeps revocation across conflated off-on updates and recreation`() =
        runBlocking {
            val harness = harness()
            val analytics = harness.controller(deleteDirectory = { false })
            val preferencesFile = File(temporaryFolder.root, "consent.preferences_pb")
            // Android's API-0 JVM stub selects renameTo, which cannot replace files on Windows.
            // Keep the real DataStore/protobuf path using its existing cross-platform storage.
            val storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { preferencesFile.path.toPath() },
                )
            val job = SupervisorJob()
            val store =
                PreferenceDataStoreFactory.create(
                    storage = storage,
                    scope = CoroutineScope(Dispatchers.IO + job),
                )
            var generation: String? = null
            try {
                assertTrue(store.writeUsageAnalyticsConsent(true))
                val initial = store.data.first()
                analytics.setEnabled(initial[USAGE_ANALYTICS_ENABLED] == true, initial[USAGE_ANALYTICS_GENERATION])
                val old = harness.latest()
                analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
                old.drain()
                assertEquals(listOf(BEFORE), old.queuedNames())
                assertTrue(harness.attempts.isEmpty())

                assertTrue(store.writeUsageAnalyticsConsent(false))
                assertTrue(store.writeUsageAnalyticsConsent(true))
                // Model a slow collector that only sees the final state after both atomic writes.
                val latest = store.data.first()
                generation = requireNotNull(latest[USAGE_ANALYTICS_GENERATION])
                assertNotEquals(initial[USAGE_ANALYTICS_GENERATION], generation)
                analytics.setEnabled(latest[USAGE_ANALYTICS_ENABLED] == true, generation)
                val fresh = harness.latest()
                assertNotEquals(old.directory, fresh.directory)
                old.queue.flush()
                old.drain()
                analytics.track(UsageEvent.PROJECT_CREATED)
                fresh.drain()
                fresh.sdk.flush()
                fresh.drain()
                assertEquals(listOf(listOf(AFTER)), harness.attempts)
            } finally {
                job.cancelAndJoin()
            }

            val restartedJob = SupervisorJob()
            try {
                val restarted =
                    PreferenceDataStoreFactory
                        .create(
                            storage = storage,
                            scope = CoroutineScope(Dispatchers.IO + restartedJob),
                        ).data
                        .first()
                assertTrue(restarted[USAGE_ANALYTICS_ENABLED] == true)
                assertEquals(generation, restarted[USAGE_ANALYTICS_GENERATION])
            } finally {
                restartedJob.cancelAndJoin()
            }
            println("RW-07B acceptance: atomic DataStore revocation survives conflation and disk recreation")
        }

    private class Harness(
        val root: File,
    ) {
        val attempts = CopyOnWriteArrayList<List<String>>()
        val instances = CopyOnWriteArrayList<PostHogTestInstance>()
        val responseCode = AtomicInteger(200)
        val clock = QueueClock()
        private val dnsAttempts = AtomicInteger()
        private val preferences = PostHogMemoryPreferences()
        private val controllers = mutableListOf<PostHogAnalytics>()

        @Volatile var beforeGate: (() -> Unit)? = null

        @Volatile var atRecorder: (() -> Unit)? = null

        @Volatile var recordingCall: Call? = null

        // All URLs terminate here; this is installed before either SDK initializes.
        private val transport =
            OkHttpClient
                .Builder()
                .dns {
                    dnsAttempts.incrementAndGet()
                    error("RW-07B must not resolve network destinations")
                }.addInterceptor { chain ->
                    val request = chain.request()
                    val status =
                        if (request.url.encodedPath == "/batch") {
                            val buffer = Buffer()
                            requireNotNull(request.body).writeTo(buffer)
                            val events = JSONObject(buffer.readUtf8()).getJSONArray("batch")
                            attempts.add(
                                (0 until events.length()).map { index ->
                                    val event = events.getJSONObject(index)
                                    assertTrue(event.getJSONObject("properties").getBoolean("\$geoip_disable"))
                                    event.getString("event")
                                },
                            )
                            recordingCall = chain.call()
                            atRecorder?.invoke()
                            responseCode.get()
                        } else {
                            200
                        }
                    Response
                        .Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(status)
                        .message("RW-07B terminal recorder")
                        .body("{}".toResponseBody())
                        .build()
                }.build()

        fun controller(deleteDirectory: (File) -> Boolean = { it.deleteRecursively() }): PostHogAnalytics {
            val factory =
                postHogClientFactory(TOKEN, AnalyticsQueueStorage(root, deleteDirectory), transport) { config ->
                    assertTrue(config.optOut)
                    assertFalse(config.persistOptOut)
                    config.cachePreferences = preferences
                    config.dateProvider = clock
                    config.flushAt = 100
                    config.flushIntervalSeconds = 86_400
                    config.httpClient =
                        requireNotNull(config.httpClient)
                            .newBuilder()
                            .apply {
                                interceptors().add(
                                    0,
                                    Interceptor { chain ->
                                        if (chain.request().url.encodedPath == "/batch") beforeGate?.invoke()
                                        chain.proceed(chain.request())
                                    },
                                )
                            }.build()
                    val sdk = PostHog.with(config) as PostHog
                    PostHogTestInstance(sdk, config, timeoutSeconds = 10).also(instances::add).sdk
                }
            return PostHogAnalytics(factory).also(controllers::add)
        }

        fun latest(): PostHogTestInstance = instances.last()

        fun close() {
            controllers.forEach { it.setEnabled(false) }
            instances.forEach { it.sdk.close() }
            val executors = instances.flatMap { it.workers() }.distinct()
            executors.forEach { it.shutdown() }
            executors.forEach { assertTrue("SDK executor terminated", it.awaitTermination(10, TimeUnit.SECONDS)) }
            transport.dispatcher.executorService.shutdown()
            assertTrue(transport.dispatcher.executorService.awaitTermination(10, TimeUnit.SECONDS))
            transport.connectionPool.evictAll()
            assertEquals(0, dnsAttempts.get())
            check(root.name == "sdk" && requireNotNull(root.canonicalFile.parentFile).name.startsWith("junit"))
            assertTrue(root.deleteRecursively())
            assertFalse(root.exists())
            println("RW-07B cleanup: instances=${instances.size}, executors stopped, storage removed, DNS=0")
        }
    }

    private class QueueClock : PostHogDateProvider {
        @Volatile private var milliseconds = 1_800_000_000_000L

        override fun currentDate(): Date = Date(milliseconds)

        override fun addSecondsToCurrentDate(seconds: Int): Date = Date(milliseconds + seconds * 1_000L)

        override fun currentTimeMillis(): Long = milliseconds

        override fun nanoTime(): Long = milliseconds * 1_000_000L

        fun advanceSeconds(seconds: Int) {
            milliseconds += seconds * 1_000L
        }
    }

    private companion object {
        const val TOKEN = "phc_rw07bSyntheticOnly"
        const val BEFORE = "pdf import succeeded"
        const val DISABLED = "pdf import failed"
        const val AFTER = "project created"
    }
}
