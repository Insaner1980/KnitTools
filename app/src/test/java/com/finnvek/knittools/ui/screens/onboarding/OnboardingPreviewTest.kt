package com.finnvek.knittools.ui.screens.onboarding

import com.finnvek.knittools.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class OnboardingPreviewTest {
    @Test
    fun `fixture is only available in the explicitly selected preview build`() {
        assertEquals(BuildConfig.ONBOARDING_TEST, OnboardingPreview.offer(Locale.US) != null)
    }

    @Test
    fun `preview shows the agreed prices in the same currency across app languages`() {
        assumeTrue(BuildConfig.ONBOARDING_TEST)
        listOf(Locale.US, Locale.forLanguageTag("fi")).forEach { locale ->
            val offer = requireNotNull(OnboardingPreview.offer(locale))
            assertEquals(9_990_000L, offer.priceAmountMicros)
            assertEquals("EUR", offer.priceCurrencyCode)
            assertEquals(14_990_000L, offer.launchDetails?.fullPriceMicros)
            assertTrue(offer.formattedPrice.contains("9"))
            assertFalse(offer.formattedPrice.isBlank())
            assertEquals("preview-only-no-checkout", offer.offerToken)
        }
    }

    @Test
    fun `example launch window is one calendar month`() {
        assumeTrue(BuildConfig.ONBOARDING_TEST)
        val launch = OnboardingPreview.offer(Locale.US)?.launchDetails
        assertNotNull(launch)
        val zone = ZoneId.of("Europe/Helsinki")
        val start = Instant.ofEpochMilli(requireNotNull(launch).startsAtMillis).atZone(zone)
        val end = Instant.ofEpochMilli(launch.endsAtMillis).atZone(zone)
        assertEquals(start.plusMonths(1), end)
    }
}
