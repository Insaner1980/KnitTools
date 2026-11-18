package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsCounterUnitsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `changing current labels cannot assign a unit to historical sessions`() =
        runTest {
            val repository = mockInsightsRepository()
            val pro = mockk<ProManager>()
            every { pro.hasFeature(any()) } returns true
            every { pro.hasFeatureFlow(any()) } returns flowOf(true)
            val project =
                CounterProject(
                    id = 1,
                    craftType = CraftType.CROCHET,
                    mainCounterLabelType = MainCounterLabelType.ROUNDS,
                )
            val projects = MutableStateFlow(listOf(project))
            every { repository.getAllProjects() } returns projects
            val start = System.currentTimeMillis() - 120_000L
            val session =
                KnitSession(
                    projectId = 1,
                    startedAt = start,
                    endedAt = start + 60_000L,
                    startRow = 10,
                    endRow = 20,
                    durationMinutes = 1,
                )
            stubSessions(repository, flowOf(listOf(session)))
            val model =
                InsightsViewModel(
                    repository,
                    pro,
                    dispatcher,
                    SavedStateHandle(),
                    mockk(relaxed = true),
                    backgroundScope,
                )

            for (label in listOf(
                MainCounterLabelType.ROUNDS,
                MainCounterLabelType.ROWS,
                MainCounterLabelType.REPEATS,
                MainCounterLabelType.CUSTOM,
            )) {
                projects.value = listOf(project.copy(mainCounterLabelType = label, mainCounterCustomLabel = "Motifs"))
                for (filter in listOf(null, 1L)) {
                    model.selectProject(filter)
                    val state =
                        model.uiState.first {
                            !it.isLoading &&
                                it.selectedProjectId == filter &&
                                it.projects.single().mainCounterLabelType == label
                        }
                    assertFalse(state.rowMetricsAvailable)
                    assertEquals(MinutesPerRowDisplay.Unavailable, state.minutesPerRow)
                    assertEquals(1, state.totalMinutes)
                    assertEquals(10L, state.totalRows)
                    assertEquals(10L, state.timePerProject.single().totalRows)
                    assertEquals(10L, state.chartBuckets.sumOf { it.totalRows })
                }
            }
        }
}
