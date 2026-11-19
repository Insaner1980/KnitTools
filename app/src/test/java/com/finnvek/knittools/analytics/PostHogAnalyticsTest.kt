package com.finnvek.knittools.analytics

import androidx.lifecycle.LifecycleOwner
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.di.postHogConfig
import com.posthog.PersonProfiles
import com.posthog.PostHogInterface
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PostHogAnalyticsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val client = mockk<PostHogConsentClient>(relaxed = true)
    private val owner = mockk<LifecycleOwner>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var initializations = 0
    private val analytics =
        PostHogAnalytics(
            PostHogClientFactory {
                initializations++
                client
            },
            scope,
            Dispatchers.IO,
        )

    @Test
    fun `SDK creation failure retires delivery and discards its queue`() {
        val root = temporaryFolder.newFolder()
        val failure = IllegalStateException("SDK creation failed")
        var gate: AnalyticsDeliveryGate? = null
        val factory =
            postHogClientFactory("test-token", AnalyticsQueueStorage(root)) { config ->
                gate = requireNotNull(config.httpClient).interceptors.filterIsInstance<AnalyticsDeliveryGate>().single()
                throw failure
            }

        assertSame(failure, assertThrows(IllegalStateException::class.java) { factory.create() })
        assertTrue(requireNotNull(gate).isRetired)
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `failed opt-in retires delivery before close and discards the queue even when close fails`() {
        val root = temporaryFolder.newFolder()
        val sdk = mockk<PostHogInterface>()
        val closeFailure = IllegalStateException("SDK close failed")
        var gate: AnalyticsDeliveryGate? = null
        every { sdk.optIn() } throws IllegalArgumentException("SDK opt-in failed")
        every { sdk.close() } answers {
            assertTrue(requireNotNull(gate).isRetired)
            throw closeFailure
        }
        val factory =
            postHogClientFactory("test-token", AnalyticsQueueStorage(root)) { config ->
                gate = requireNotNull(config.httpClient).interceptors.filterIsInstance<AnalyticsDeliveryGate>().single()
                sdk
            }

        assertSame(closeFailure, assertThrows(IllegalStateException::class.java) { factory.create() })
        verify(exactly = 1) { sdk.close() }
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `no SDK initialization or capture before consent`() {
        assertFalse(AppPreferences().usageAnalyticsEnabled)
        analytics.setEnabled(false)
        analytics.onStart(owner)
        analytics.screenViewed("notes_editor/123?text=private")
        analytics.track(UsageEvent.PROJECT_CREATED)
        analytics.onStop(owner)
        assertEquals(0, initializations)
        verify { client wasNot Called }
    }

    @Test
    fun `consent starts collection and revocation blocks later events`() {
        analytics.onStart(owner)
        analytics.screenViewed("pattern_viewer/123?selectedProjectDocumentId=456")
        analytics.setEnabled(true)
        analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED)
        analytics.setEnabled(false)
        analytics.track(UsageEvent.PDF_IMPORT_FAILED)
        analytics.onStop(owner)
        assertEquals(1, initializations)
        verify(exactly = 1) { client.retire() }
        verify(exactly = 1) {
            client.capture(
                "\$screen",
                properties = mapOf("\$screen_name" to "pattern_viewer", "\$geoip_disable" to true),
            )
        }
        verify(exactly = 1) { client.capture("pdf import succeeded", properties = mapOf("\$geoip_disable" to true)) }
        verify(exactly = 0) { client.capture("pdf import failed", properties = any()) }
        verify(exactly = 0) { client.capture("app backgrounded", properties = any()) }
    }

    @Test
    fun `screen changes and foreground transitions are counted once`() {
        analytics.setEnabled(true)
        analytics.onStart(owner)
        analytics.onStart(owner)
        analytics.screenViewed("counter")
        analytics.screenViewed("counter")
        analytics.onStop(owner)
        analytics.onStop(owner)
        verify(exactly = 1) { client.capture("app opened", properties = any()) }
        verify(exactly = 1) { client.capture("\$screen", properties = any()) }
        verify(
            exactly = 1,
        ) { client.capture("screen exited", properties = match { (it["duration_seconds"] as Long) >= 0 }) }
        verify(exactly = 1) { client.capture("app backgrounded", properties = any()) }
    }

    @Test
    fun `unknown routes and all route arguments stay out of analytics`() {
        assertNull(screenNameForRoute(null))
        assertNull(screenNameForRoute("https://private.example/pattern"))
        assertNull(screenNameForRoute("private project title"))
        assertNull(screenNameForRoute("projects_tab"))
        assertEquals(
            "ravelry_browser",
            screenNameForRoute("ravelry_browser?url=https%3A%2F%2Fwww.ravelry.com%2Fprivate"),
        )
        assertEquals("gauge", screenNameForRoute("gauge?projectId=123"))
    }

    @Test
    fun `onboarding routes use allowlisted names without project or topic arguments`() {
        assertEquals("project_overview", screenNameForRoute("project_overview/123"))
        assertEquals("guide", screenNameForRoute("guide"))
        assertEquals("guide", screenNameForRoute("guide?projectId=123"))
        assertEquals("guide_topic", screenNameForRoute("guide/{topic}"))
        assertEquals("guide_topic", screenNameForRoute("guide/continue?projectId=123"))
        assertEquals("guide_topic", screenNameForRoute("guide_topic/private-value"))
    }

    @Test
    fun `onboarding events are predefined and contain no content properties`() {
        val events =
            setOf(
                "welcome demo used",
                "welcome tools chosen",
                "welcome dismissed",
                "guide opened",
                "tip acted",
                "tip dismissed",
            )
        val onboardingEvents = UsageEvent.entries.filter { it.eventName in events }
        assertEquals(events, onboardingEvents.map { it.eventName }.toSet())
        onboardingEvents.forEach(analytics::track)
        verify { client wasNot Called }

        analytics.setEnabled(true)
        onboardingEvents.forEach(analytics::track)
        events.forEach { event ->
            verify(exactly = 1) { client.capture(event, properties = mapOf("\$geoip_disable" to true)) }
        }
    }

    @Test
    fun `SDK failure does not interrupt user action`() {
        analytics.setEnabled(true)
        every { client.capture(any(), properties = any()) } throws IllegalStateException("SDK failure")
        analytics.track(UsageEvent.PROJECT_CREATED)
        analytics.setEnabled(false)
        verify { client.retire() }
    }

    @Test
    fun `re-opt-in uses a new client and never reopens the retired client`() {
        val next = mockk<PostHogConsentClient>(relaxed = true)
        var creations = 0
        val controller =
            PostHogAnalytics(PostHogClientFactory { if (creations++ == 0) client else next }, scope, Dispatchers.IO)
        controller.setEnabled(true)
        controller.setEnabled(false)
        controller.setEnabled(false)
        controller.track(UsageEvent.PDF_IMPORT_FAILED)
        controller.setEnabled(true)
        controller.track(UsageEvent.PROJECT_CREATED)
        assertEquals(2, creations)
        verify(exactly = 1) { client.retire() }
        verify(exactly = 0) { client.capture(any(), any()) }
        verify(exactly = 1) { next.capture("project created", mapOf("\$geoip_disable" to true)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `failed initialization retries while consent stays current and revocation cancels the retry`() =
        runTest {
            var creations = 0
            val controller =
                PostHogAnalytics(
                    PostHogClientFactory {
                        if (++creations != 2) throw IllegalStateException("Initialization failed")
                        client
                    },
                    backgroundScope,
                    StandardTestDispatcher(testScheduler),
                )
            controller.setEnabled(true, "initial")
            controller.track(UsageEvent.PDF_IMPORT_FAILED)
            verify { client wasNot Called }
            advanceTimeBy(1_000)
            runCurrent()
            controller.track(UsageEvent.PROJECT_CREATED)
            verify(exactly = 1) { client.capture("project created", mapOf("\$geoip_disable" to true)) }
            advanceTimeBy(60_000)
            assertEquals(2, creations)

            controller.setEnabled(true, "replacement")
            controller.setEnabled(false, "revoked")
            advanceTimeBy(60_000)
            assertEquals(3, creations)
        }

    @Test
    fun `SDK configuration disables automatic capture and optional features`() {
        val config = postHogConfig("phc_test")
        assertEquals("https://eu.i.posthog.com", config.host)
        assertTrue(config.optOut)
        assertFalse(config.captureApplicationLifecycleEvents)
        assertFalse(config.captureScreenViews)
        assertFalse(config.captureDeepLinks)
        assertFalse(config.capturePushNotificationSubscriptions)
        assertFalse(config.capturePushNotificationOpened)
        assertFalse(config.preloadFeatureFlags)
        assertFalse(config.sendFeatureFlagEvent)
        assertFalse(config.sessionReplay)
        assertFalse(config.surveys)
        assertFalse(config.errorTrackingConfig.autoCapture)
        assertFalse(config.errorTrackingConfig.captureNativeCrashes)
        assertFalse(config.debug)
        assertEquals(PersonProfiles.NEVER, config.personProfiles)
    }

    @Test
    fun `expanded events send only fixed vocabulary while consent is active`() {
        analytics.track(UsageEvent.PDF_IMPORT_STARTED, UsageDetail.SOURCE_CAMERA)
        assertEquals(0, initializations)
        analytics.setEnabled(true)
        analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_CAMERA, UsageDetail.ERROR_TOO_LARGE)
        analytics.setEnabled(false)
        analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, UsageDetail.SOURCE_CAMERA)
        verify(exactly = 1) {
            client.capture(
                "pdf import failed",
                mapOf(
                    "source" to "camera",
                    "reason" to "too_large",
                    "\$geoip_disable" to true,
                ),
            )
        }
        verify(exactly = 0) { client.capture("pdf import started", any()) }
        verify(exactly = 0) { client.capture("pdf import succeeded", any()) }
    }

    @Test
    fun `onboarding starts at the currently visible step after consent without replaying earlier steps`() {
        analytics.onStart(owner)
        analytics.screenViewed("onboarding/Welcome")
        analytics.screenViewed("onboarding/Craft")
        analytics.setEnabled(true)
        verify(exactly = 1) {
            client.capture(
                "\$screen",
                mapOf(
                    "\$screen_name" to "onboarding_craft",
                    "\$geoip_disable" to true,
                ),
            )
        }
        verify(exactly = 0) { client.capture("\$screen", match { it["\$screen_name"] == "onboarding_welcome" }) }
        assertNull(screenNameForRoute("onboarding/private project title"))
        assertNull(screenNameForRoute("onboarding/Welcome/private"))
    }
}
