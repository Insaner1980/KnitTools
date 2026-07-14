package com.finnvek.knittools.pro

import com.finnvek.knittools.billing.BillingManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize allows retry when trial initialization fails`() {
        val failure = IllegalStateException("trial state unavailable")
        val trialManager =
            mockk<TrialManager> {
                coEvery { initialize() } throws failure
            }
        val billingManager = mockk<BillingManager>(relaxed = true)
        val manager = ProManager(trialManager, billingManager)

        manager.initialize()

        manager.initialize()

        coVerify(exactly = 2) { trialManager.initialize() }
    }
}
