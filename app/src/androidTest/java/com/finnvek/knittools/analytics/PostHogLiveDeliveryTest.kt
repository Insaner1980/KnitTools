package com.finnvek.knittools.analytics

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.BuildConfig
import com.posthog.PostHog
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicitly opted-in live smoke test using only synthetic actions and isolated SDK storage. */
class PostHogLiveDeliveryTest {
    @Test
    fun consentedAndroidEventsReachTheConfiguredEuProject() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("posthogLive") == "true")
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            "The live PostHog test needs INTERNET. " +
                "Assemble a network-enabled test APK and run it with adb on a disposable emulator.",
            PackageManager.PERMISSION_GRANTED,
            base.checkSelfPermission(Manifest.permission.INTERNET),
        )
        assertTrue(BuildConfig.POSTHOG_PROJECT_TOKEN.startsWith("phc_"))
        val prefix = "posthog-verification-${UUID.randomUUID()}"
        val root = File(base.cacheDir, prefix)
        val preferences = mutableSetOf<String>()
        val context =
            object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this

                override fun getSharedPreferences(
                    name: String,
                    mode: Int,
                ): SharedPreferences {
                    val isolated = "$prefix-$name"
                    synchronized(preferences) { preferences.add(isolated) }
                    return base.getSharedPreferences(isolated, mode)
                }
            }
        val delivered = CopyOnWriteArrayList<String>()
        val attempted = CopyOnWriteArrayList<String>()
        val acknowledged = CountDownLatch(3)
        val transport =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    assertEquals("eu.i.posthog.com", request.url.host)
                    val names = mutableMapOf<String, String>()
                    if (request.url.encodedPath == "/i/v1/analytics/events") {
                        assertTrue(request.header("Authorization") == "Bearer ${BuildConfig.POSTHOG_PROJECT_TOKEN}")
                        val buffer = Buffer()
                        requireNotNull(request.body).writeTo(buffer)
                        val batch = JSONObject(buffer.readUtf8()).getJSONArray("batch")
                        for (index in 0 until batch.length()) {
                            val event = batch.getJSONObject(index)
                            val properties = event.getJSONObject("properties")
                            assertEquals("verification", properties.getString("traffic"))
                            assertTrue(properties.getBoolean("\$geoip_disable"))
                            listOf(
                                "projectId",
                                "project_id",
                                "name",
                                "text",
                                "notes",
                                "url",
                                "email",
                                "billing_state",
                                "\$set",
                                "\$set_once",
                            ).forEach {
                                assertFalse("Unexpected content property: $it", properties.has(it))
                            }
                            names[event.getString("uuid")] = event.getString("event")
                        }
                    }
                    attempted.addAll(names.values)
                    chain.proceed(request).also { response ->
                        if (response.isSuccessful && names.isNotEmpty()) {
                            val results = JSONObject(response.peekBody(64 * 1024L).string()).getJSONObject("results")
                            names.forEach { (uuid, name) ->
                                val result = results.optJSONObject(uuid)?.optString("result")
                                if (result != "retry" && result != "drop") {
                                    delivered.add(name)
                                    acknowledged.countDown()
                                }
                            }
                        }
                    }
                }.build()
        var sdk: PostHogInterface? = null
        var instance: PostHogTestInstance? = null
        val analyticsScope = CoroutineScope(Dispatchers.IO)
        val analytics =
            PostHogAnalytics(
                postHogClientFactory(
                    token = BuildConfig.POSTHOG_PROJECT_TOKEN,
                    storage = AnalyticsQueueStorage(root),
                    transport = transport,
                    createSdk = { config ->
                        config.flushAt = 1
                        PostHogAndroid.with(context, config).also {
                            sdk = it
                            instance = PostHogTestInstance(it as PostHog, config, 10)
                        }
                    },
                ),
                analyticsScope,
                Dispatchers.IO,
            )
        val cleanup =
            AutoCloseable {
                val workers = instance?.workers().orEmpty()
                cleanupPostHogTest(
                    { analytics.setEnabled(false) },
                    { analyticsScope.cancel() },
                    { workers.forEach { it.shutdownNow() } },
                    *workers
                        .map { worker ->
                            { assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS)) }
                        }.toTypedArray(),
                    {
                        transport.dispatcher.executorService.shutdown()
                        assertTrue(transport.dispatcher.executorService.awaitTermination(10, TimeUnit.SECONDS))
                    },
                    { transport.connectionPool.evictAll() },
                    { synchronized(preferences) { preferences.forEach(base::deleteSharedPreferences) } },
                    { assertTrue(!root.exists() || root.deleteRecursively()) },
                )
            }
        cleanup.use {
            analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.VERIFICATION)
            assertEquals(null, sdk)
            analytics.setEnabled(true)
            analytics.track(UsageEvent.CALCULATOR_RESULT_SHOWN, UsageDetail.TOOL_CONVERT, UsageDetail.VERIFICATION)
            analytics.track(UsageEvent.BACKUP_EXPORT_SUCCEEDED, UsageDetail.VERIFICATION)
            analytics.track(UsageEvent.GUIDE_TOPIC_OPENED, UsageDetail.GUIDE_TOOLS, UsageDetail.VERIFICATION)
            requireNotNull(sdk).flush()
            assertTrue(
                "EU ingestion did not acknowledge all three synthetic events",
                acknowledged.await(45, TimeUnit.SECONDS),
            )
            analytics.setEnabled(false)
            analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.VERIFICATION)
            val retired = requireNotNull(instance)
            retired.sdk.flush()
            retired.queue.flush()
            retired.workers().forEach { worker ->
                if (worker.isShutdown) {
                    assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS))
                } else {
                    worker.submit {}.get(10, TimeUnit.SECONDS)
                }
            }
            retired.drain()
            assertFalse("A revoked event must not enter transport", "pdf import failed" in attempted)
            assertEquals(
                setOf("calculator result shown", "backup export succeeded", "guide topic opened"),
                delivered.toSet(),
            )
            assertEquals(3, delivered.size)
        }
    }
}
