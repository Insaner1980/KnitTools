package com.finnvek.knittools.analytics

import com.finnvek.knittools.di.postHogConfig
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroidConfig
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.util.UUID

fun interface PostHogClientFactory {
    fun create(): PostHogConsentClient
}

class PostHogConsentClient internal constructor(
    private val sdk: PostHogInterface,
    private val gate: AnalyticsDeliveryGate,
    private val discardStorage: () -> Unit,
) {
    fun capture(
        event: String,
        properties: Map<String, Any>,
    ) {
        if (!gate.isRetired) sdk.capture(event, properties = properties)
    }

    fun retire() {
        // close() flushes in core 6.47.1, so delivery must be retired first.
        gate.retire()
        try {
            sdk.optOut()
        } finally {
            try {
                sdk.close()
            } finally {
                discardStorage()
            }
        }
    }
}

internal class AnalyticsDeliveryGate : Interceptor {
    private val lock = Any()
    private val inFlight = mutableSetOf<Call>()

    @Volatile
    var isRetired: Boolean = false
        private set

    override fun intercept(chain: Interceptor.Chain): Response {
        val call = chain.call()
        synchronized(lock) {
            if (isRetired) {
                // Local discard, not an ingestion acknowledgement. Never enter the transport.
                return Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(204)
                    .message("Analytics consent revoked")
                    .body("".toResponseBody())
                    .build()
            }
            // This HTTP call is now in flight. Revocation also cancels it best-effort.
            inFlight.add(call)
        }
        try {
            return chain.proceed(chain.request())
        } finally {
            synchronized(lock) { inFlight.remove(call) }
        }
    }

    fun retire() {
        val calls =
            synchronized(lock) {
                isRetired = true
                inFlight.toList()
            }
        calls.forEach(Call::cancel)
    }
}

internal class AnalyticsQueueStorage(
    private val root: File,
    private val deleteDirectory: (File) -> Boolean = { it.deleteRecursively() },
) {
    fun createDirectory(): File {
        // Never reuse a queue, including after process death or a failed/late deletion.
        root.listFiles()?.forEach(::discard)
        val directory = File(root, "consent-${UUID.randomUUID()}")
        check(directory.mkdirs()) { "Cannot create analytics storage" }
        return directory
    }

    fun discard(directory: File) {
        try {
            if (
                directory.name.matches(CONSENT_DIRECTORY) &&
                directory.canonicalFile.parentFile == root.canonicalFile
            ) {
                deleteDirectory(directory)
            }
        } catch (_: IOException) {
            // Ineligible leftovers are retried at the next initialization.
        } catch (_: SecurityException) {
            // Delivery eligibility never depends on deletion succeeding.
        }
    }

    private companion object {
        val CONSENT_DIRECTORY = Regex("consent-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}

// These configuration seams are PostHogInternal in Android 3.72.1 / core 6.47.1.
// Keep the real-SDK consent/queue tests when changing either version.
internal fun postHogClientFactory(
    token: String,
    storage: AnalyticsQueueStorage,
    transport: OkHttpClient = OkHttpClient(),
    createSdk: (PostHogAndroidConfig) -> PostHogInterface,
): PostHogClientFactory =
    PostHogClientFactory {
        val directory = storage.createDirectory()
        val gate = AnalyticsDeliveryGate()
        val client =
            transport
                .newBuilder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .apply { interceptors().add(0, gate) }
                .build()
        var sdk: PostHogInterface? = null
        var completed = false
        try {
            val config =
                postHogConfig(token).apply {
                    persistOptOut = false
                    httpClient = client
                    storagePrefix = File(directory, "events").path
                    replayStoragePrefix = File(directory, "replay").path
                    logsStoragePrefix = File(directory, "logs").path
                    // Do not import the old default SDK queue into a new consent period.
                    legacyStoragePrefix = File(directory, "legacy").path
                }
            val initialized = createSdk(config)
            sdk = initialized
            initialized.optIn()
            PostHogConsentClient(initialized, gate) { storage.discard(directory) }.also { completed = true }
        } finally {
            if (!completed) {
                gate.retire()
                try {
                    sdk?.close()
                } finally {
                    storage.discard(directory)
                }
            }
        }
    }
