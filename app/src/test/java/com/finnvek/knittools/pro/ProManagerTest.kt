package com.finnvek.knittools.pro

import android.util.Log
import com.finnvek.knittools.billing.BillingManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize logs failure and allows retry when trial initialization fails`() {
        val failure = IllegalStateException("trial state unavailable")
        val trialManager =
            mockk<TrialManager> {
                coEvery { initialize() } throws failure
            }
        val billingManager = mockk<BillingManager>(relaxed = true)
        val manager = ProManager(trialManager, billingManager)

        manager.initialize()

        verify { Log.e("ProManager", "Pro-tilan alustus epäonnistui", failure) }

        manager.initialize()

        coVerify(exactly = 2) { trialManager.initialize() }
    }
}
