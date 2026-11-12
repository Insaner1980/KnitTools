package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.model.ActiveWorkSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterStartupReadinessTest : CounterViewModelFixture() {
    @Test
    fun `guidance waits for saved session facts again after switching projects`() =
        runTest {
            val firstSessions = MutableSharedFlow<Boolean>(replay = 1)
            val otherSessions = MutableSharedFlow<Boolean>(replay = 1)
            every { repository.observeHasSessionsForProject(7L) } returns firstSessions
            every { repository.observeHasSessionsForProject(8L) } returns otherSessions
            val other = observeOtherProject(count = 0)
            val vm = viewModel(startupPreferences = flowOf(AppPreferences()))
            runCurrent()

            assertFalse(vm.uiState.value.startupReady)
            firstSessions.emit(true)
            runCurrent()
            assertTrue(vm.uiState.value.startupReady)
            assertTrue(vm.uiState.value.hasSessions)

            vm.selectProject(other)
            runCurrent()
            assertFalse(vm.uiState.value.startupReady)
            otherSessions.emit(false)
            runCurrent()
            assertTrue(vm.uiState.value.startupReady)
            assertFalse(vm.uiState.value.hasSessions)
        }

    @Test
    fun `selected counter waits for preferences session observation refresh and entitlement controls`() =
        runTest {
            val preferences = MutableSharedFlow<AppPreferences>(replay = 1)
            val sessions = MutableSharedFlow<ActiveWorkSession?>(replay = 1)
            val controls = MutableStateFlow(false)
            val refresh = CompletableDeferred<ActiveWorkSession?>()
            every { repository.observeActiveSession() } returns sessions
            every { repository.observeHasSessionsForProject(7L) } returns flowOf(false)
            coEvery { repository.refreshActiveSession() } coAnswers { refresh.await() }
            val vm = viewModel(startupPreferences = preferences, startupControlsReady = controls)
            runCurrent()
            assertEquals(7L, vm.uiState.value.projectId)
            assertFalse(vm.uiState.value.startupReady)
            preferences.emit(AppPreferences())
            runCurrent()
            assertFalse(vm.uiState.value.startupReady)
            sessions.emit(null)
            runCurrent()
            assertFalse(vm.uiState.value.startupReady)
            controls.value = true
            runCurrent()
            assertFalse(vm.uiState.value.startupReady)
            refresh.complete(null)
            runCurrent()
            assertTrue(vm.uiState.value.startupReady)
            coVerify(exactly = 0) { repository.startSession(any()) }
        }
}
