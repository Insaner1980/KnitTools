package com.finnvek.knittools.analytics

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostHogAnalytics
    @Inject
    constructor(
        private val clients: PostHogClientFactory,
        @ApplicationScope private val applicationScope: CoroutineScope,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : UsageAnalytics,
        DefaultLifecycleObserver {
        private var enabled = false
        private var requestedEnabled = false
        private var consentGeneration: String? = null

        @VisibleForTesting
        @Volatile
        internal var consentRevision = 0L
            private set
        private var retryJob: Job? = null
        private var client: PostHogConsentClient? = null
        private val retiredClients = mutableListOf<PostHogConsentClient>()
        private val clientWorkLock = Any()
        private var foreground = false
        private var screen: String? = null
        private var foregroundStarted: Long? = null
        private var screenStarted: Long? = null
        internal val initialConsentLoaded = CompletableDeferred<Unit>()

        suspend fun trackFromWidget(event: UsageEvent) {
            if (!BuildConfig.POSTHOG_ENABLED) return
            withTimeoutOrNull(1_000) {
                initialConsentLoaded.await()
                track(event)
            }
        }

        fun setEnabled(
            allowed: Boolean,
            generation: String? = null,
        ) {
            val revision =
                synchronized(this) {
                    if (allowed == requestedEnabled && generation == consentGeneration) return
                    requestedEnabled = allowed
                    consentGeneration = generation
                    consentRevision++
                    retryJob?.cancel()
                    retryJob = null
                    enabled = false
                    foregroundStarted = null
                    screenStarted = null
                    client?.let {
                        it.blockDelivery()
                        retiredClients.add(it)
                    }
                    client = null
                    // Revoking an unfinished initialization must not wait for it.
                    if (!allowed && retiredClients.isEmpty()) return
                    consentRevision
                }

            // Only consent workers take this lock; callbacks need only the short state monitor.
            // Serialize queue sweeps and SDK lifetimes, including superseded requests.
            synchronized(clientWorkLock) {
                val retired = synchronized(this) { retiredClients.toList().also { retiredClients.clear() } }
                retired.forEach { safely { it.retire() } }
                if (synchronized(this) { revision != consentRevision || !requestedEnabled }) return
                if (!initializeClient(revision)) scheduleInitializationRetry(revision)
            }
        }

        @Synchronized
        private fun scheduleInitializationRetry(revision: Long) {
            if (revision != consentRevision || !requestedEnabled) return
            retryJob =
                applicationScope.launch(ioDispatcher) {
                    var retryDelay = INITIAL_RETRY_DELAY_MS
                    while (true) {
                        delay(retryDelay)
                        val finished =
                            synchronized(clientWorkLock) {
                                synchronized(
                                    this@PostHogAnalytics,
                                ) { revision != consentRevision || !requestedEnabled } ||
                                    initializeClient(revision)
                            }
                        if (finished) return@launch
                        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                    }
                }
        }

        private fun initializeClient(revision: Long): Boolean {
            val initialized =
                try {
                    clients.create()
                } catch (_: RuntimeException) {
                    return false
                }
            val accepted =
                synchronized(this) {
                    if (revision != consentRevision) {
                        false
                    } else {
                        client = initialized
                        enabled = true
                        if (foreground) startForeground()
                        true
                    }
                }
            if (!accepted) safely { initialized.retire() }
            return true
        }

        @Synchronized
        override fun track(event: UsageEvent) {
            send(event.eventName)
        }

        @Synchronized
        override fun track(
            event: UsageEvent,
            vararg details: UsageDetail,
        ) {
            send(event.eventName, details.associate { it.property to it.value })
        }

        @Synchronized
        fun screenViewed(route: String?) {
            val next = screenNameForRoute(route)
            if (next == screen) return
            finishScreen()
            screen = next
            if (enabled && foreground) startScreen()
        }

        @Synchronized
        override fun onStart(owner: LifecycleOwner) {
            if (foreground) return
            foreground = true
            if (enabled) startForeground()
        }

        @Synchronized
        override fun onStop(owner: LifecycleOwner) {
            if (!foreground) return
            finishScreen()
            foregroundStarted?.let { send("app backgrounded", mapOf("duration_seconds" to elapsedSeconds(it))) }
            foregroundStarted = null
            foreground = false
        }

        private fun startForeground() {
            foregroundStarted = System.nanoTime()
            send("app opened")
            startScreen()
        }

        private fun startScreen() {
            screen?.let {
                screenStarted = System.nanoTime()
                send("\$screen", mapOf("\$screen_name" to it))
            }
        }

        private fun finishScreen() {
            val started = screenStarted ?: return
            screenStarted = null
            screen?.let {
                send("screen exited", mapOf("screen" to it, "duration_seconds" to elapsedSeconds(started)))
            }
        }

        private fun send(
            event: String,
            properties: Map<String, Any> = emptyMap(),
        ) {
            if (!enabled) return
            safely { client?.capture(event, properties = properties + ("\$geoip_disable" to true)) }
        }

        private fun elapsedSeconds(started: Long): Long =
            ((System.nanoTime() - started) / 1_000_000_000L).coerceAtLeast(0L)

        private inline fun safely(block: () -> Unit) {
            try {
                block()
            } catch (_: RuntimeException) {
                // Analytics must not interrupt app actions or expose exception details.
            }
        }

        private companion object {
            private const val INITIAL_RETRY_DELAY_MS = 1_000L
            private const val MAX_RETRY_DELAY_MS = 60_000L
        }
    }

internal fun screenNameForRoute(route: String?): String? {
    val path = route?.substringBefore('?') ?: return null
    if (path.startsWith("onboarding/")) {
        return when (path.substringAfter('/')) {
            "Welcome" -> "onboarding_welcome"
            "Craft" -> "onboarding_craft"
            "Needs" -> "onboarding_needs"
            "Counter" -> "onboarding_counter"
            "Toolbox" -> "onboarding_toolbox"
            "Project" -> "onboarding_project"
            "Ready" -> "onboarding_ready"
            "Price" -> "onboarding_price"
            else -> null
        }
    }
    val screen = if (path.startsWith("guide/")) "guide_topic" else path.substringBefore('/')
    return screen.takeIf { it in ANALYTICS_SCREENS }
}

private val ANALYTICS_SCREENS =
    setOf(
        "tools",
        "counter",
        "increase_decrease",
        "gauge",
        "cast_on",
        "yarn",
        "needles",
        "size_charts",
        "abbreviations",
        "chart_symbols",
        "settings",
        "backup",
        "pro_upgrade",
        "yarn_card_detail",
        "ravelry_browser",
        "project_list",
        "project_overview",
        "guide",
        "guide_topic",
        "photo_gallery",
        "pattern_viewer",
        "notes_editor",
        "session_history",
        "counter_history",
        "library",
        "saved_patterns",
        "saved_pattern_detail",
        "web_pattern_editor",
        "library_pattern_viewer",
        "my_yarn",
        "all_photos",
        "insights",
    )
