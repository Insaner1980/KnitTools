package com.finnvek.knittools.repository

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyFacts
import com.finnvek.knittools.pro.TrialManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TrialJourneyStateRepositoryTest {
    @Test
    fun `facts stay unknown until every source and entitlement state are ready`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()
            assertNull(fixture.repository.state.value)
            fixture.projects.emit(emptyList())
            fixture.counters.emit(emptyList())
            fixture.yarn.emit(emptyList())
            fixture.marks.emit(OnboardingState())
            runCurrent()
            assertNull(fixture.repository.state.value)
            fixture.photos.emit(emptyList())
            runCurrent()
            assertNull(fixture.repository.state.value)
            fixture.ready.value = true
            runCurrent()
            assertEquals(0, fixture.facts.projectsAdded)
            fixture.ready.value = false
            runCurrent()
            assertNull(fixture.repository.state.value)
        }

    @Test
    fun `all four counts include trial start and exclude trial end even after expiration`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            val end = START + TimeUnit.DAYS.toMillis(TrialManager.TRIAL_DURATION_DAYS.toLong())
            val times = listOf(START - 1L, START, end - 1L, end, end + 1L)
            fixture.pro.value = ProState(status = ProStatus.TRIAL_EXPIRED, trialStartTimestamp = START)
            fixture.emitRows(
                projectRows =
                    times.mapIndexed { index, time ->
                        CounterProject(id = index + 1L, name = "Project", createdAt = time, isCompleted = index == 1)
                    },
                counterRows = times.map { ProjectCounter(projectId = 1L, name = "Counter", createdAt = it) },
                yarnRows = times.map { YarnCard(createdAt = it) },
                photoRows =
                    times.map {
                        ProgressPhoto(
                            projectId = 1L,
                            photoUri = "local",
                            rowNumber = 0,
                            createdAt = it,
                        )
                    },
            )
            runCurrent()

            assertEquals(2, fixture.facts.projectsAdded)
            assertEquals(2, fixture.facts.countersAdded)
            assertEquals(2, fixture.facts.yarnCardsAdded)
            assertEquals(2, fixture.facts.photosAdded)
            assertEquals(4, fixture.facts.activeProjectCount)
        }

    @Test
    fun `latest active project is unfiltered and breaks updated time ties by id`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            val first = CounterProject(id = 1L, name = "First", updatedAt = 100L)
            val latest = first.copy(id = 7L, name = "Latest")
            fixture.emitRows(
                projectRows = listOf(latest.copy(id = 8L, updatedAt = 200L, isCompleted = true), first, latest),
            )
            runCurrent()

            assertEquals(2, fixture.facts.activeProjectCount)
            assertEquals(latest, fixture.facts.latestActiveProject)
            assertEquals(TrialJourneyAction.entries, fixture.facts.steps(widgetInstalled = false).map { it.action })
            fixture.projects.emit(listOf(first.copy(isCompleted = true)))
            runCurrent()
            assertNull(fixture.facts.latestActiveProject)
            assertEquals(0, fixture.facts.activeProjectCount)
            assertEquals(
                listOf(TrialJourneyAction.AnotherProject, TrialJourneyAction.WeeklyChart),
                fixture.facts.steps(widgetInstalled = true).map { it.action },
            )
        }

    @Test
    fun `only a repeating counter created during the trial completes the repeat step`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            val repeating = ProjectCounter(projectId = 7L, name = "Repeat", counterType = ProjectCounterType.REPEATING)
            fixture.emitRows(
                counterRows =
                    listOf(
                        repeating.copy(createdAt = START - 1L),
                        repeating.copy(createdAt = START, counterType = ProjectCounterType.REPEAT_SECTION),
                    ),
            )
            runCurrent()
            assertFalse(fixture.facts.repeatCounterAdded)
            fixture.counters.emit(listOf(repeating.copy(createdAt = START)))
            runCurrent()
            assertTrue(fixture.facts.repeatCounterAdded)
        }

    @Test
    fun `markers belong to the actual trial and checklist widget reflects installed state`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            fixture.emitRows(projectRows = listOf(CounterProject(id = 7L, name = "New", createdAt = START)))
            fixture.marks.emit(
                OnboardingState(trialChartSeenForStart = START, trialPhotoPdfCreatedForStart = START - 1L),
            )
            runCurrent()
            assertTrue(fixture.facts.chartSeen)
            assertFalse(fixture.facts.photoPdfCreated)
            assertEquals(TrialJourneyAction.entries, fixture.facts.steps(widgetInstalled = false).map { it.action })
            assertFalse(
                fixture.facts
                    .steps(widgetInstalled = false)
                    .first()
                    .done,
            )
            assertTrue(
                fixture.facts
                    .steps(widgetInstalled = true)
                    .first()
                    .done,
            )
            val projectStep =
                fixture.facts.steps(widgetInstalled = false).single {
                    it.action ==
                        TrialJourneyAction.AnotherProject
                }
            assertTrue(projectStep.done)
            fixture.pro.value = ProState(trialStartTimestamp = START + 1L)
            runCurrent()
            assertFalse(fixture.facts.chartSeen)
            assertFalse(fixture.facts.photoPdfCreated)
            fixture.pro.value = ProState()
            fixture.marks.emit(OnboardingState())
            runCurrent()
            assertFalse(fixture.facts.chartSeen)
            assertFalse(fixture.facts.photoPdfCreated)
            assertEquals(0, fixture.facts.projectsAdded)
        }

    @Test
    fun `trial window arithmetic does not wrap near the largest timestamp`() =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
            fixture.pro.value = ProState(trialStartTimestamp = Long.MAX_VALUE - 1L)
            fixture.emitRows(
                projectRows =
                    listOf(
                        CounterProject(name = "Within", createdAt = Long.MAX_VALUE),
                        CounterProject(name = "Outside", createdAt = -1L),
                    ),
            )
            runCurrent()
            assertEquals(1, fixture.facts.projectsAdded)
        }

    private class Fixture(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
    ) {
        val projects = MutableSharedFlow<List<CounterProject>>(replay = 1)
        val counters = MutableSharedFlow<List<ProjectCounter>>(replay = 1)
        val yarn = MutableSharedFlow<List<YarnCard>>(replay = 1)
        val photos = MutableSharedFlow<List<ProgressPhoto>>(replay = 1)
        val marks = MutableSharedFlow<OnboardingState>(replay = 1)
        val ready = MutableStateFlow(false)
        val pro = MutableStateFlow(ProState(status = ProStatus.TRIAL_ACTIVE, trialStartTimestamp = START))
        val repository =
            TrialJourneyStateRepository(
                projects = mockk<CounterRepository> { every { getAllProjects() } returns projects },
                counters = mockk<ProjectCounterRepository> { every { getAllCounters() } returns counters },
                yarnCards = mockk<YarnCardRepository> { every { getAllCards() } returns yarn },
                photos = mockk<ProgressPhotoRepository> { every { getAllPhotos() } returns photos },
                onboarding = mockk<OnboardingStateRepository> { every { state } returns marks },
                proManager =
                    mockk<ProManager> {
                        every { proState } returns pro
                        every { initialStateReady } returns ready
                    },
                scope = scope,
                ioDispatcher = dispatcher,
            )
        val facts: TrialJourneyFacts get() = requireNotNull(repository.state.value)

        init {
            scope.launch { repository.state.collect {} }
        }

        suspend fun emitRows(
            projectRows: List<CounterProject> = emptyList(),
            counterRows: List<ProjectCounter> = emptyList(),
            yarnRows: List<YarnCard> = emptyList(),
            photoRows: List<ProgressPhoto> = emptyList(),
        ) {
            projects.emit(projectRows)
            counters.emit(counterRows)
            yarn.emit(yarnRows)
            photos.emit(photoRows)
            marks.emit(OnboardingState())
            ready.value = true
        }
    }

    private companion object {
        const val START = 1_700_000_000_000L
    }
}
