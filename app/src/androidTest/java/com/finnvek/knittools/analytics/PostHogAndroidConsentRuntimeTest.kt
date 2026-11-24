package com.finnvek.knittools.analytics

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.datastore.USAGE_ANALYTICS_ENABLED
import com.finnvek.knittools.data.datastore.USAGE_ANALYTICS_GENERATION
import com.finnvek.knittools.data.datastore.writeUsageAnalyticsConsent
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.internal.PostHogNetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** One controlled Android-wrapper journey; no UI or process-death simulation. */
class PostHogAndroidConsentRuntimeTest {
    @Test
    fun revokedQueueCannotDeliverAfterReenableAndOwnerRecreation() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val base = instrumentation.targetContext
            assertEquals("com.finnvek.knittools.activepolicytest", base.packageName)
            assertEquals("${base.packageName}.test", instrumentation.context.packageName)
            listOf(base.packageName, instrumentation.context.packageName).forEach { packageName ->
                val permissions =
                    base.packageManager
                        .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                        .requestedPermissions
                        .orEmpty()
                assertFalse("Isolated package must not have INTERNET", "android.permission.INTERNET" in permissions)
            }
            val root = File(base.cacheDir, "rw-07c-${UUID.randomUUID()}")
            check(root.mkdirs())
            val harness = Harness(base, root)
            val owners = mutableListOf<ConsentOwner>()
            val restoreLanguage =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val locales = requireNotNull(base.getSystemService(LocaleManager::class.java))
                    val previous = locales.applicationLocales
                    locales.applicationLocales = LocaleList.forLanguageTags("fi")
                    AutoCloseable { locales.applicationLocales = previous }
                } else {
                    null
                }
            val cleanup =
                AutoCloseable {
                    cleanupPostHogTest(
                        *owners.map { owner -> { runBlocking { owner.close() } } }.toTypedArray(),
                        harness::close,
                        { restoreLanguage?.close() },
                    )
                }
            cleanup.use {
                val firstContext = harness.context()
                val first = ConsentOwner(root, harness.controller(firstContext)).also(owners::add)
                first.await(false, null)
                assertTrue(harness.instances.isEmpty())
                assertTrue(first.store.writeUsageAnalyticsConsent(true))
                first.await(true, null)
                val old = harness.latest()
                first.analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
                old.drain()
                assertEquals(listOf(A), old.queuedNames())
                assertEquals(1, old.queue.size)
                assertTrue(harness.deliveries.isEmpty())
                assertEquals(0, harness.inFlight.get())
                val oldRecord = old.eventFiles().single().readBytes()
                harness.persistPreferenceSentinel()
                evidence("A queued", "diskRecords=1; queueSize=1; deliveries=0; inFlight=0")

                assertTrue(first.store.writeUsageAnalyticsConsent(false))
                val revoked = first.store.data.first()
                val generation = requireNotNull(revoked[USAGE_ANALYTICS_GENERATION])
                assertFalse(revoked[USAGE_ANALYTICS_ENABLED] == true)
                UUID.fromString(generation)
                first.await(false, generation)
                first.analytics.track(UsageEvent.PDF_IMPORT_FAILED)
                old.sdk.flush()
                old.queue.flush()
                old.sdk.close()
                old.drain()
                assertTrue(harness.deliveries.isEmpty())
                assertTrue(old.queuedNames().isEmpty())
                assertEquals(0, old.queue.size)
                evidence("Revoked", "collector=(false,rotatedGeneration); B blocked; flush/close deliveries=0")

                assertTrue(first.store.writeUsageAnalyticsConsent(true))
                first.await(true, generation)
                assertEquals(generation, first.store.data.first()[USAGE_ANALYTICS_GENERATION])
                val fresh = harness.latest()
                assertNotEquals(old.directory, fresh.directory)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // The SDK reads the app locale on its own worker after initialization.
                    val context = requireNotNull(fresh.config.context)
                    withTimeout(15_000) {
                        while (context.getDynamicContext()["\$app_language"] != "fi") delay(10)
                    }
                }
                first.analytics.track(UsageEvent.PROJECT_CREATED)
                fresh.drain()
                fresh.sdk.flush()
                fresh.drain()
                assertEquals(listOf(listOf(C)), harness.deliveries.toList())
                assertTrue(fresh.queuedNames().isEmpty())
                evidence("Re-enabled", "same persisted generation; fresh queue; recorder=[C]")

                first.close()
                harness.stopSdkWorkers()
                harness.persistPreferenceSentinel()
                // Restore only our own serialized A to its retired directory to make replay proof non-vacuous.
                val retiredQueue = requireNotNull(old.queue.queueDirectory)
                check(retiredQueue.mkdirs() || retiredQueue.isDirectory)
                File(retiredQueue, "rw07c-retired.event").writeBytes(oldRecord)
                assertEquals(listOf(A), old.queuedNames())
                val recreatedContext = harness.context()
                assertNotSame(firstContext, recreatedContext)
                val recreated = ConsentOwner(root, harness.controller(recreatedContext)).also(owners::add)
                recreated.await(true, generation)
                val restarted = harness.latest()
                assertNotSame(fresh.sdk, restarted.sdk)
                assertNotSame(fresh.config.cachePreferences, restarted.config.cachePreferences)
                assertNotEquals(fresh.directory, restarted.directory)
                assertNotEquals(old.directory, restarted.directory)
                assertTrue(recreated.store.data.first()[USAGE_ANALYTICS_ENABLED] == true)
                assertEquals(generation, recreated.store.data.first()[USAGE_ANALYTICS_GENERATION])
                harness.assertPreferenceSentinel()
                restarted.drain()
                restarted.sdk.flush()
                restarted.queue.flush()
                restarted.drain()
                assertTrue(restarted.queuedNames().isEmpty())
                assertEquals(0, restarted.queue.size)
                assertEquals(listOf(listOf(C)), harness.deliveries.toList())
                assertFalse(old.directory.exists())
                assertEquals(
                    listOf(false to null, true to null, false to generation, true to generation),
                    first.observed.toList(),
                )
                assertEquals(listOf(true to generation), recreated.observed.toList())
                assertEquals(0, harness.inFlight.get())
                assertEquals(0, harness.dnsAttempts.get())
                evidence(
                    "Owners recreated",
                    "same DataStore file/generation and Android preference sentinel; retired A present before init; " +
                        "recorder still=[C]; old directory removed",
                )
                evidence(
                    "SDK",
                    "instances=${harness.instances.size}; sdk=${restarted.sdk.javaClass.name}; " +
                        "preferences=${restarted.config.cachePreferences?.javaClass?.name}; " +
                        "requests=${harness.paths}; DNS=0",
                )
            }
        }

    private class ConsentOwner(
        root: File,
        val analytics: PostHogAnalytics,
    ) {
        private val scopeJob = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + scopeJob)
        val store = PreferenceDataStoreFactory.create(scope = scope) { File(root, "consent.preferences_pb") }
        val observed = CopyOnWriteArrayList<Pair<Boolean, String?>>()
        private val applied = Channel<Pair<Boolean, String?>>(Channel.UNLIMITED)
        private val collector =
            scope.launch {
                store.data
                    .map { (it[USAGE_ANALYTICS_ENABLED] == true) to it[USAGE_ANALYTICS_GENERATION] }
                    .distinctUntilChanged()
                    .collect { snapshot ->
                        analytics.setEnabled(snapshot.first, snapshot.second)
                        observed.add(snapshot)
                        applied.send(snapshot)
                    }
            }

        suspend fun await(
            enabled: Boolean,
            generation: String?,
        ) {
            assertEquals(enabled to generation, withTimeout(15_000) { applied.receive() })
        }

        suspend fun close() {
            try {
                collector.cancelAndJoin()
                analytics.setEnabled(false)
            } finally {
                scopeJob.cancelAndJoin()
                applied.close()
            }
        }
    }

    private class Harness(
        private val base: Context,
        private val root: File,
    ) {
        private val analyticsScope = CoroutineScope(Dispatchers.IO)
        val instances = mutableListOf<PostHogTestInstance>()
        val deliveries = CopyOnWriteArrayList<List<String>>()
        val paths = CopyOnWriteArrayList<String>()
        val inFlight = AtomicInteger()
        val dnsAttempts = AtomicInteger()
        private val preferenceNames = mutableSetOf<String>()
        private val stopped = mutableSetOf<PostHogTestInstance>()
        private val transport =
            OkHttpClient
                .Builder()
                .dns {
                    dnsAttempts.incrementAndGet()
                    error("RW-07C forbids DNS")
                }.addInterceptor { chain ->
                    inFlight.incrementAndGet()
                    try {
                        val request = chain.request()
                        paths.add(request.url.encodedPath)
                        if (request.url.encodedPath == "/i/v1/analytics/events") {
                            assertEquals("Bearer $TOKEN", request.header("Authorization"))
                            val buffer = Buffer()
                            requireNotNull(request.body).writeTo(buffer)
                            val batch = JSONObject(buffer.readUtf8()).getJSONArray("batch")
                            deliveries.add(
                                (0 until batch.length()).map { index ->
                                    val event = batch.getJSONObject(index)
                                    val properties = event.getJSONObject("properties")
                                    assertTrue(properties.getBoolean("\$geoip_disable"))
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        assertEquals("fi", properties.getString("\$app_language"))
                                    } else {
                                        assertFalse(properties.has("\$app_language"))
                                    }
                                    assertFalse(properties.has("usage_analytics_generation"))
                                    event.getString("event")
                                },
                            )
                        }
                        // Every URL terminates locally, including any initialization request.
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("RW-07C terminal recorder")
                            .body("""{"results":{}}""".toResponseBody())
                            .build()
                    } finally {
                        inFlight.decrementAndGet()
                    }
                }.build()

        fun context(): Context =
            object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this

                override fun getCacheDir(): File = File(root, "android-cache").apply { mkdirs() }

                override fun getFilesDir(): File = File(root, "android-files").apply { mkdirs() }

                override fun getSharedPreferences(
                    name: String,
                    mode: Int,
                ): SharedPreferences {
                    val scoped = "${root.name}-$name"
                    preferenceNames.add(scoped)
                    return base.getSharedPreferences(scoped, mode)
                }
            }

        fun controller(context: Context): PostHogAnalytics =
            PostHogAnalytics(
                postHogClientFactory(
                    token = TOKEN,
                    storage = AnalyticsQueueStorage(File(root, "queues")),
                    transport = transport,
                ) { config ->
                    assertTrue(config.optOut)
                    assertFalse(config.persistOptOut)
                    assertEquals(null, config.cachePreferences)
                    config.flushAt = 100
                    config.flushIntervalSeconds = 86_400
                    // Keep the flush sequence controlled even if Android reports network availability.
                    config.networkStatus =
                        object : PostHogNetworkStatus {
                            override fun isConnected() = true
                        }
                    val sdk = PostHogAndroid.with(context, config) as PostHog
                    assertTrue(
                        requireNotNull(config.cachePreferences).javaClass.name.startsWith("com.posthog.android."),
                    )
                    PostHogTestInstance(sdk, config, timeoutSeconds = 15)
                        .also {
                            instances.add(it)
                            assertTrue(it.directory.canonicalPath.startsWith(root.canonicalPath + File.separator))
                            assertTrue(
                                requireNotNull(it.queue.queueDirectory).canonicalPath.startsWith(
                                    root.canonicalPath + File.separator,
                                ),
                            )
                        }.sdk
                },
                analyticsScope,
                Dispatchers.IO,
            )

        fun latest(): PostHogTestInstance = instances.last()

        suspend fun persistPreferenceSentinel() {
            assertTrue("Android SDK must open real SharedPreferences", preferenceNames.isNotEmpty())
            preferenceNames.forEach { name ->
                assertTrue(
                    base
                        .getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE,
                        ).edit()
                        .putString("rw07c_sentinel", "keep")
                        .commit(),
                )
                // A concurrent SDK preferences write can temporarily rename the XML file to its backup.
                val file = File(base.applicationInfo.dataDir, "shared_prefs/$name.xml")
                withTimeout(5_000) {
                    while (!file.isFile) delay(10)
                }
            }
        }

        fun assertPreferenceSentinel() {
            preferenceNames.forEach { name ->
                assertEquals(
                    "keep",
                    base.getSharedPreferences(name, Context.MODE_PRIVATE).getString("rw07c_sentinel", null),
                )
            }
        }

        fun stopSdkWorkers() {
            cleanupPostHogTest(
                *instances.filterNot { it in stopped }.map { instance -> { stopInstance(instance) } }.toTypedArray(),
            )
        }

        private fun stopInstance(instance: PostHogTestInstance) {
            val executors = instance.workers()
            cleanupPostHogTest(
                instance.sdk::close,
                instance::drain,
                { executors.forEach { it.shutdown() } },
                *executors
                    .map { worker ->
                        { assertTrue("Task SDK worker terminated", worker.awaitTermination(15, TimeUnit.SECONDS)) }
                    }.toTypedArray(),
            )
            stopped.add(instance)
        }

        fun close() {
            cleanupPostHogTest(
                { analyticsScope.cancel() },
                ::stopSdkWorkers,
                {
                    transport.dispatcher.executorService.shutdown()
                    assertTrue(transport.dispatcher.executorService.awaitTermination(15, TimeUnit.SECONDS))
                },
                { transport.connectionPool.evictAll() },
                { assertEquals(0, dnsAttempts.get()) },
                { assertEquals(0, inFlight.get()) },
                *preferenceNames.map { name -> { deletePreference(name) } }.toTypedArray(),
                {
                    check(
                        root.canonicalFile.parentFile == base.cacheDir.canonicalFile && root.name.startsWith("rw-07c-"),
                    )
                    assertTrue(root.deleteRecursively())
                    assertFalse(root.exists())
                },
            )
            evidence(
                "Cleanup",
                "root=${root.name}; SDK instances=${stopped.size}; private workers terminated by test only; " +
                    "DataStore owners joined; preferences=${preferenceNames.size} deleted; root removed; DNS=0",
            )
        }

        private fun deletePreference(name: String) {
            check(name.startsWith(root.name + "-"))
            base
                .getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            assertTrue(base.deleteSharedPreferences(name))
            assertFalse(File(base.applicationInfo.dataDir, "shared_prefs/$name.xml").exists())
        }
    }

    private companion object {
        const val TOKEN = "phc_rw07cSyntheticOnly"
        const val A = "pdf import succeeded"
        const val C = "project created"

        fun evidence(
            stage: String,
            detail: String,
        ) {
            InstrumentationRegistry.getInstrumentation().sendStatus(
                0,
                Bundle().apply {
                    putString("rw07c_stage", stage)
                    putString("rw07c_evidence", detail)
                },
            )
        }
    }
}
