package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrialJourneyStartViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val manager = mockk<ProManager>()
    private val trialManager = mockk<TrialManager>(relaxed = true)
    private val billing = mockk<BillingManager>()
    private val coordinator = TrialJourneyCoordinator()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { manager.proState } returns MutableStateFlow(ProState())
        every { manager.initialStateReady } returns MutableStateFlow(true)
        every { billing.purchaseQueryFailed } returns MutableStateFlow(false)
        every { billing.selectedOffer } returns MutableStateFlow(null)
        every { billing.productStatus } returns MutableStateFlow(BillingProductStatus.Loading)
        every { billing.purchaseFlowInFlight } returns MutableStateFlow(false)
        every { billing.purchaseMessages } returns MutableSharedFlow()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `prompt queues only Started before result delivery and ignores duplicate clicks`() =
        runTest {
            val result = CompletableDeferred<TrialStartResult>()
            coEvery { manager.startTrial() } coAnswers { result.await() }
            val viewModel = ProPromptViewModel(manager, trialManager, coordinator)
            store.put("prompt", viewModel)
            val received = mutableListOf<Pair<TrialStartResult, Boolean>>()
            val observer =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.trialStartResults.collect { received += it to coordinator.pending.value }
                }
            viewModel.startTrial()
            viewModel.startTrial()
            runCurrent()
            assertTrue(viewModel.isStartingTrial.value)
            assertFalse(coordinator.pending.value)
            result.complete(TrialStartResult.Started)
            runCurrent()
            assertEquals(listOf(TrialStartResult.Started to true), received)
            assertFalse(viewModel.isStartingTrial.value)
            coVerify(exactly = 1) { manager.startTrial() }
            observer.cancel()
            val replayed = mutableListOf<TrialStartResult>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.trialStartResults.collect { replayed += it }
            }
            runCurrent()
            assertTrue(replayed.isEmpty())
        }

    @Test
    fun `prompt non-start results never request a journey`() =
        runTest {
            val viewModel = ProPromptViewModel(manager, trialManager, coordinator)
            store.put("prompt", viewModel)
            TrialStartResult.entries.filter { it != TrialStartResult.Started }.forEach { result ->
                coEvery { manager.startTrial() } returns result
                viewModel.startTrial()
                runCurrent()
                assertFalse("Unexpected journey for $result", coordinator.pending.value)
                assertFalse(viewModel.isStartingTrial.value)
            }
        }

    @Test
    fun `upgrade requests journey only for Started and keeps failed status message`() =
        runTest {
            val viewModel = ProUpgradeViewModel(manager, billing, coordinator)
            store.put("upgrade", viewModel)
            TrialStartResult.entries.forEach { result ->
                coordinator.consume()
                coEvery { manager.startTrial() } returns result
                viewModel.startTrial()
                runCurrent()
                assertEquals(result == TrialStartResult.Started, coordinator.pending.value)
                assertEquals(
                    if (result ==
                        TrialStartResult.Failed
                    ) {
                        R.string.pro_trial_start_failed
                    } else {
                        null
                    },
                    viewModel.statusMessageRes.value,
                )
                assertFalse(viewModel.isStartingTrial.value)
            }
        }
}
