package com.finnvek.knittools.analytics

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.posthog.PostHogInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/** Runs the production consent/payload boundary on ART with a recording, offline SDK sink. */
class AuditPart15AnalyticsTest {
    @Test fun everyAllowedEventRespectsConsentAndContainsOnlyAllowlistedProperties() {
        val captured = mutableListOf<Pair<String, Map<*, *>>>()
        var initializations = 0
        val client =
            Proxy.newProxyInstance(
                PostHogInterface::class.java.classLoader,
                arrayOf(PostHogInterface::class.java),
            ) { _, method, args ->
                if (method.name == "capture") {
                    captured +=
                        (args[0] as String) to
                        (args.filterIsInstance<Map<*, *>>().firstOrNull() ?: emptyMap<Any, Any>())
                }
                when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    else -> null
                }
            } as PostHogInterface
        val analytics =
            PostHogAnalytics(
                PostHogClientFactory {
                    initializations++
                    PostHogConsentClient(client, AnalyticsDeliveryGate()) {}
                },
                CoroutineScope(Dispatchers.IO),
                Dispatchers.IO,
            )
        val owner =
            object : LifecycleOwner {
                override val lifecycle: Lifecycle = LifecycleRegistry.createUnsafe(this)
            }
        analytics.onStart(owner)
        analytics.screenViewed("pattern_viewer/987?name=private-text")
        UsageEvent.entries.forEach(analytics::track)
        assertEquals(0, initializations)
        assertTrue(captured.isEmpty())
        analytics.setEnabled(true)
        analytics.onStart(owner)
        analytics.screenViewed("pattern_viewer/987?name=private-text")
        UsageEvent.entries.forEach(analytics::track)
        analytics.onStop(owner)
        analytics.onStop(owner)
        for (event in UsageEvent.entries) {
            val payload = captured.single { it.first == event.eventName }.second
            assertEquals(mapOf("\$geoip_disable" to true), payload)
        }
        assertEquals(
            listOf("app opened", "\$screen") + UsageEvent.entries.map { it.eventName } +
                listOf("screen exited", "app backgrounded"),
            captured.map { it.first },
        )
        val eventPropertyKeys =
            mapOf(
                "app opened" to emptySet(),
                "\$screen" to setOf("\$screen_name"),
                "screen exited" to setOf("screen", "duration_seconds"),
                "app backgrounded" to setOf("duration_seconds"),
            )
        eventPropertyKeys.forEach { (event, keys) ->
            assertEquals(keys + "\$geoip_disable", captured.single { it.first == event }.second.keys)
        }
        assertEquals("pattern_viewer", captured.single { it.first == "\$screen" }.second["\$screen_name"])
        assertFalse(captured.toString().contains("987"))
        assertFalse(captured.toString().contains("private-text"))
        assertTrue(captured.all { it.second["\$geoip_disable"] == true })
        analytics.setEnabled(false)
        val count = captured.size
        UsageEvent.entries.forEach(analytics::track)
        analytics.onStart(owner)
        analytics.screenViewed("counter")
        analytics.onStop(owner)
        assertEquals(count, captured.size)
    }
}
