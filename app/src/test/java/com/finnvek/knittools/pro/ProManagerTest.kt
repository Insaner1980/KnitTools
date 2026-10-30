package com.finnvek.knittools.pro

import com.finnvek.knittools.billing.BillingManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `initial controls wait for trial and purchase resolution including terminal query failure`() =
        kotlinx.coroutines.test.runTest {
            val initialized = kotlinx.coroutines.CompletableDeferred<Unit>()
            val trial =
                mockk<TrialManager> {
                    coEvery { initialize() } coAnswers { initialized.await() }
                    every { trialState } returns MutableStateFlow(TrialState())
                }
            val ready = MutableStateFlow(false)
            val failed = MutableStateFlow(false)
            val billing =
                mockk<BillingManager> {
                    every { isProPurchased } returns MutableStateFlow(false)
                    every { purchaseStateReady } returns ready
                    every { purchaseQueryFailed } returns failed
                }
            val manager = ProManager(trial, billing)
            manager.initialize()
            assertFalse(manager.initialControlsReady.value)
            initialized.complete(Unit)
            assertFalse(manager.initialControlsReady.value)
            failed.value = true
            assertTrue(manager.initialControlsReady.value)
            assertFalse(manager.initialStateReady.value)
            assertEquals(ProStatus.TRIAL_NOT_STARTED, manager.proState.value.status)
            ready.value = true
            assertTrue(manager.initialControlsReady.value)
            assertTrue(manager.initialStateReady.value)
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

    @Test
    fun `startTrial delegates the explicit user action`() =
        kotlinx.coroutines.test.runTest {
            val trialManager = mockk<TrialManager>()
            coEvery { trialManager.startTrial() } returns TrialStartResult.Started
            val manager = ProManager(trialManager, mockk(relaxed = true))

            val result = manager.startTrial()

            assertEquals(TrialStartResult.Started, result)
            coVerify(exactly = 1) { trialManager.startTrial() }
        }

    @Test
    fun `verified purchase stays purchased when trial is expired or tampered`() {
        val trialStates = MutableStateFlow(TrialState(hasStarted = true))
        val trialManager =
            mockk<TrialManager> {
                coEvery { initialize() } returns Unit
                every { trialState } returns trialStates
            }
        val billingManager =
            mockk<BillingManager> {
                every { isProPurchased } returns MutableStateFlow(true)
                every { purchaseStateReady } returns MutableStateFlow(true)
                every { purchaseQueryFailed } returns MutableStateFlow(false)
            }
        val manager = ProManager(trialManager, billingManager)

        manager.initialize()
        assertEquals(ProStatus.PRO_PURCHASED, manager.proState.value.status)

        trialStates.value = TrialState(hasStarted = true, clockTampered = true)
        assertEquals(ProStatus.PRO_PURCHASED, manager.proState.value.status)
    }
}
