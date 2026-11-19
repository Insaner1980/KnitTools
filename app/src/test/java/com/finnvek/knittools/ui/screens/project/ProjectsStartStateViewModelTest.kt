package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.repository.OnboardingState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsStartStateViewModelTest : ProjectListViewModelFixture() {
    @Test
    fun `welcome waits for onboarding state and preserves last craft`() =
        runTest {
            val onboarding = MutableSharedFlow<OnboardingState>(replay = 1)
            every { onboardingStateRepository.state } returns onboarding
            every { preferencesManager.preferences } returns
                flowOf(AppPreferences(lastProjectCraftType = CraftType.CROCHET))
            val vm = createViewModel()
            val store = ViewModelStore().apply { put("projects", vm) }
            try {
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                onboarding.emit(OnboardingState(welcomeDismissed = true))
                val loaded = vm.content.first { !it.isLoading }
                assertFalse(loaded.hasAnyProject)
                assertTrue(loaded.welcomeDismissed)
                assertEquals(CraftType.CROCHET, loaded.preferences.lastProjectCraftType)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `dismissal persists compact state and tools dismiss before navigation`() =
        runTest {
            val onboarding = MutableStateFlow(OnboardingState())
            every { onboardingStateRepository.state } returns onboarding
            coEvery { onboardingStateRepository.dismissWelcome() } answers {
                onboarding.value = onboarding.value.copy(welcomeDismissed = true)
            }
            val events = mutableListOf<UsageEvent>()
            val vm = createViewModel(analytics = UsageAnalytics { events += it })
            val store = ViewModelStore().apply { put("projects", vm) }
            try {
                vm.dismissWelcome()
                assertTrue(vm.content.first { !it.isLoading && it.welcomeDismissed }.welcomeDismissed)
                assertEquals(listOf(UsageEvent.WELCOME_DISMISSED), events)
                onboarding.value = OnboardingState()
                var navigated = false
                vm.chooseWelcomeTools {
                    assertTrue(onboarding.value.welcomeDismissed)
                    navigated = true
                }
                runCurrent()
                assertTrue(navigated)
                assertEquals(listOf(UsageEvent.WELCOME_DISMISSED, UsageEvent.WELCOME_TOOLS_CHOSEN), events)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `demo event is once per process across view models without persistence`() =
        runTest {
            val events = mutableListOf<UsageEvent>()
            val analytics = UsageAnalytics { events += it }
            val first = createViewModel(analytics = analytics)
            val second = createViewModel(analytics = analytics)
            val store =
                ViewModelStore().apply {
                    put("first", first)
                    put("second", second)
                }
            try {
                first.recordWelcomeDemoUsed()
                first.recordWelcomeDemoUsed()
                second.recordWelcomeDemoUsed()
                assertEquals(listOf(UsageEvent.WELCOME_DEMO_USED), events)
                coVerify(
                    exactly = 0,
                ) { repository.createProject(any(), any(), any(), any(), any(), any(), any(), any()) }
                coVerify(exactly = 0) { onboardingStateRepository.dismissWelcome() }
                coVerify(exactly = 0) { preferencesManager.setLastProjectCraftType(any()) }
            } finally {
                store.clear()
            }
        }
}
