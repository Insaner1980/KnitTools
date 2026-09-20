package com.finnvek.knittools.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.posthog.PostHogInterface
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostHogAnalytics
    @Inject
    constructor(
        private val client: Lazy<PostHogInterface>,
    ) : UsageAnalytics,
        DefaultLifecycleObserver {
        private var enabled = false
        private var foreground = false
        private var screen: String? = null
        private var foregroundStarted: Long? = null
        private var screenStarted: Long? = null

        @Synchronized
        fun setEnabled(allowed: Boolean) {
            if (allowed == enabled) return
            if (!allowed) {
                enabled = false
                foregroundStarted = null
                screenStarted = null
                safely { client.get().optOut() }
                return
            }
            safely {
                client.get().optIn()
                enabled = true
                if (foreground) startForeground()
            }
        }

        @Synchronized
        override fun track(event: UsageEvent) {
            send(event.eventName)
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
            safely { client.get().capture(event, properties = properties + ("\$geoip_disable" to true)) }
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
    }

internal fun screenNameForRoute(route: String?): String? =
    route?.substringBefore('?')?.substringBefore('/')?.takeIf { it in ANALYTICS_SCREENS }

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
        "ravelry",
        "ravelry_import",
        "ravelry_detail",
        "project_list",
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
