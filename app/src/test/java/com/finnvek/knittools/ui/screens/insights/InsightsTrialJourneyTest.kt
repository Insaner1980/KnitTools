package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsTrialJourneyTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()
    private val onboarding = mockk<OnboardingStateRepository>(relaxed = true)
    private val ready = MutableStateFlow(true)
    private val pro = MutableStateFlow(ProState(ProStatus.TRIAL_ACTIVE, trialStartTimestamp = 100L))
    private val zone = ZoneId.of("UTC")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(::insightsCalendarChanges)
        every { insightsCalendarChanges(any(), any()) } returns
            flowOf(InsightsCalendar(LocalDate.of(2026, 10, 30), zone))
        every { repository.getAllProjects() } returns flowOf(listOf(CounterProject(1L, "Project")))
        every { repository.observeCompletions() } returns flowOf(emptyList())
        stubSessions(repository, flowOf(listOf(session(27), session(28))))
        every { proManager.hasFeature(any()) } returns true
        every { proManager.hasFeatureFlow(any()) } returns flowOf(true)
        every { proManager.initialStateReady } returns ready
        every { proManager.proState } returns pro
    }

    @After
    fun tearDown() {
        unmockkStatic(::insightsCalendarChanges)
        Dispatchers.resetMain()
    }

    @Test
    fun `visible weekly chart records the loaded active trial timestamp`() =
        runTest {
            val vm = weeklyModel()

            vm.onWeeklyChartVisible()
            runCurrent()

            coVerify(exactly = 1) { onboarding.markTrialChartSeen(100L) }
        }

    @Test
    fun `unready expired and purchased states cannot record a trial chart`() =
        runTest {
            val vm = weeklyModel()
            ready.value = false
            vm.onWeeklyChartVisible()
            runCurrent()
            coVerify(exactly = 0) { onboarding.markTrialChartSeen(any()) }
            ready.value = true
            listOf(ProStatus.TRIAL_EXPIRED, ProStatus.PRO_PURCHASED, ProStatus.TRIAL_NOT_STARTED).forEach { status ->
                pro.value = ProState(status, trialStartTimestamp = 100L)
                vm.onWeeklyChartVisible()
                runCurrent()
                coVerify(exactly = 0) { onboarding.markTrialChartSeen(any()) }
            }
            runCurrent()

            coVerify(exactly = 0) { onboarding.markTrialChartSeen(any()) }
        }

    @Test
    fun `trial marker persistence failure preserves the visible chart`() =
        runTest {
            val vm = weeklyModel()
            coEvery { onboarding.markTrialChartSeen(100L) } throws IOException("preferences")

            vm.onWeeklyChartVisible()
            runCurrent()

            assertTrue(vm.uiState.value.hasMeaningfulChartData)
            coVerify(exactly = 1) { onboarding.markTrialChartSeen(100L) }
        }

    private suspend fun TestScope.weeklyModel(): InsightsViewModel {
        val vm = InsightsViewModel(repository, proManager, dispatcher, SavedStateHandle(), onboarding, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.selectTimeRange(TimeRange.THIS_WEEK)
        val state = vm.uiState.first { !it.isLoading && it.timeRange == TimeRange.THIS_WEEK }
        assertTrue(state.hasMeaningfulChartData)
        return vm
    }

    private fun session(day: Int): KnitSession {
        val start =
            LocalDate
                .of(2026, 10, day)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        return KnitSession(
            projectId = 1L,
            startedAt = start,
            endedAt = start + 60_000L,
            startRow = 0,
            endRow = 1,
            durationMinutes = 1,
            durationSeconds = 60L,
            rowsWorked = 1,
            zoneId = zone.id,
        )
    }
}
