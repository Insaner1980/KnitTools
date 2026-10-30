package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterProReadinessTest : CounterViewModelFixture() {
    @Test
    fun `presentation waits for actual pro readiness after controls become ready`() =
        runTest {
            val controlsReady = MutableStateFlow(false)
            val accessReady = MutableStateFlow(false)
            val proState = MutableStateFlow(ProState())
            val manager = mockk<ProManager>(relaxed = true)
            every { manager.proState } returns proState
            val vm =
                viewModel(
                    proManagerOverride = manager,
                    startupControlsReady = controlsReady,
                    startupProAccessReady = accessReady,
                )
            runCurrent()
            controlsReady.value = true
            runCurrent()
            assertTrue(vm.uiState.value.proStateLoaded)
            assertFalse(vm.uiState.value.proAccessReady)

            proState.value = ProState(status = ProStatus.TRIAL_ACTIVE)
            accessReady.value = true
            runCurrent()
            assertEquals(proState.value, vm.uiState.value.proState)
            assertTrue(vm.uiState.value.proAccessReady)

            accessReady.value = false
            runCurrent()
            assertTrue(vm.uiState.value.proStateLoaded)
            assertFalse(vm.uiState.value.proAccessReady)
        }
}
