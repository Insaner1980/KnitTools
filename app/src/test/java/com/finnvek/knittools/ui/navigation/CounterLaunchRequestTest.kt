package com.finnvek.knittools.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterLaunchRequestTest {
    @Test
    fun `valid launch token trust creates counter request`() {
        var consumedLaunchId: String? = null
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = "launch-1",
            ).withValidatedCounterLaunchTrust { launchId ->
                consumedLaunchId = launchId
                true
            }

        val request = CounterLaunchRequest.fromIntentData(intentData, consumedRequestId = null)

        assertEquals("launch-1", consumedLaunchId)
        assertEquals("launch-1", request?.requestId)
        assertEquals(42L, request?.projectId)
    }

    @Test
    fun `rejected launch token trust does not create counter request`() {
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = "expired-launch",
            ).withValidatedCounterLaunchTrust { false }

        assertNull(CounterLaunchRequest.fromIntentData(intentData, consumedRequestId = null))
    }

    @Test
    fun `oauth callback does not consume counter launch token`() {
        var tokenConsumptionAttempted = false
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = "launch-1",
                isOAuthCallback = true,
            ).withValidatedCounterLaunchTrust {
                tokenConsumptionAttempted = true
                true
            }

        assertTrue(intentData.isOAuthCallback)
        assertNull(CounterLaunchRequest.fromIntentData(intentData, consumedRequestId = null))
        assertEquals(false, tokenConsumptionAttempted)
    }

    @Test
    fun `consumed widget launch id is ignored during activity recreation`() {
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = "launch-1",
                isTrustedCounterLaunch = true,
            )

        val request =
            CounterLaunchRequest.fromIntentData(
                intentData = intentData,
                consumedRequestId = "launch-1",
            )

        assertNull(request)
    }

    @Test
    fun `new widget launch id with same project is accepted after previous launch was consumed`() {
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = "launch-2",
                isTrustedCounterLaunch = true,
            )

        val request =
            CounterLaunchRequest.fromIntentData(
                intentData = intentData,
                consumedRequestId = "launch-1",
            )

        assertNotNull(request)
        assertEquals("launch-2", request?.requestId)
        assertEquals(42L, request?.projectId)
    }

    @Test
    fun `legacy widget launch without explicit id can be consumed during activity recreation`() {
        val intentData =
            CounterLaunchIntentData(
                shouldOpenCounter = true,
                projectId = 42L,
                launchId = null,
                isTrustedCounterLaunch = true,
            )
        val firstRequest =
            CounterLaunchRequest.fromIntentData(
                intentData = intentData,
                consumedRequestId = null,
            )

        val restoredRequest =
            CounterLaunchRequest.fromIntentData(
                intentData = intentData,
                consumedRequestId = firstRequest?.requestId,
            )

        assertNotNull(firstRequest)
        assertNull(restoredRequest)
    }

    @Test
    fun `plain app launch does not create counter launch request`() {
        val request =
            CounterLaunchRequest.fromIntentData(
                intentData =
                    CounterLaunchIntentData(
                        shouldOpenCounter = false,
                        projectId = 42L,
                        launchId = "launch-1",
                    ),
                consumedRequestId = null,
            )

        assertNull(request)
    }

    @Test
    fun `untrusted counter launch does not create counter launch request`() {
        val request =
            CounterLaunchRequest.fromIntentData(
                intentData =
                    CounterLaunchIntentData(
                        shouldOpenCounter = true,
                        projectId = 42L,
                        launchId = "launch-1",
                        isTrustedCounterLaunch = false,
                    ),
                consumedRequestId = null,
            )

        assertNull(request)
    }

    @Test
    fun `oauth callback launch does not create counter launch request even with counter extras`() {
        val request =
            CounterLaunchRequest.fromIntentData(
                intentData =
                    CounterLaunchIntentData(
                        shouldOpenCounter = true,
                        projectId = 42L,
                        launchId = "launch-1",
                        isTrustedCounterLaunch = false,
                        isOAuthCallback = true,
                    ),
                consumedRequestId = null,
            )

        assertNull(request)
    }
}
