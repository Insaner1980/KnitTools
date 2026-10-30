package com.finnvek.knittools.analytics

import com.posthog.PostHog
import com.posthog.PostHogStateless
import com.posthog.android.PostHogAndroidConfig
import com.posthog.internal.PostHogQueue
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

internal class PostHogTestInstance(
    val sdk: PostHog,
    val config: PostHogAndroidConfig,
    private val timeoutSeconds: Long,
) {
    val directory = requireNotNull(File(requireNotNull(config.storagePrefix)).parentFile)
    val queue =
        PostHogStateless::class.java.getDeclaredField("queue").let {
            it.isAccessible = true
            it.get(sdk) as PostHogQueue<*>
        }
    val executor =
        PostHogQueue::class.java.getDeclaredField("executor").let {
            it.isAccessible = true
            it.get(queue) as ExecutorService
        }

    fun drain() {
        executor.submit {}.get(timeoutSeconds, TimeUnit.SECONDS)
    }

    fun eventFiles(): List<File> =
        requireNotNull(queue.queueDirectory).listFiles().orEmpty().filter { it.extension == "event" }

    fun queuedNames(): List<String> = eventFiles().map { JSONObject(it.readText()).getString("event") }.sorted()

    fun workers(): List<ExecutorService> =
        listOf(PostHog::class.java, PostHogStateless::class.java)
            .flatMap { it.declaredFields.toList() }
            .filter { ExecutorService::class.java.isAssignableFrom(it.type) }
            .map {
                it.isAccessible = true
                it.get(sdk) as ExecutorService
            }.distinct()
}
