package com.finnvek.knittools.analytics

import androidx.lifecycle.LifecycleOwner
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.di.postHogConfig
import com.posthog.PersonProfiles
import com.posthog.PostHogInterface
import dagger.Lazy
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostHogAnalyticsTest {
    private val client = mockk<PostHogInterface>(relaxed = true)
    private val owner = mockk<LifecycleOwner>()
    private var initializations = 0
    private val analytics =
        PostHogAnalytics(
            Lazy {
                initializations++
                client
            },
        )

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
        verify(exactly = 1) { client.optIn() }
        verify(exactly = 1) { client.optOut() }
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
        assertEquals("ravelry_import", screenNameForRoute("ravelry_import/private-pattern-url"))
        assertEquals("gauge", screenNameForRoute("gauge?projectId=123"))
    }

    @Test
    fun `SDK failure does not interrupt user action`() {
        analytics.setEnabled(true)
        every { client.capture(any(), properties = any()) } throws IllegalStateException("SDK failure")
        analytics.track(UsageEvent.PROJECT_CREATED)
        analytics.setEnabled(false)
        verify { client.optOut() }
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
}
