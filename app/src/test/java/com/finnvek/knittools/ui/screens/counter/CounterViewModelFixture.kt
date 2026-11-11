package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.CommittedMainCounterChange
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentFileAvailability
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.ui.tips.TipSession
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CounterViewModelFixture {
    protected val dispatcher = StandardTestDispatcher()
    protected val repository = mockk<CounterRepository>(relaxed = true)
    protected val reviewManager = mockk<InAppReviewManager>(relaxed = true)
    protected val photoRepository = mockk<ProgressPhotoRepository>()
    protected val layers = MutableStateFlow(listOf(layer(41L, active = true), layer(42L, active = false)))
    protected val observedProject = MutableStateFlow(CounterProject(id = 7L, name = "Project"))
    protected val tipSession = mockk<TipSession>()
    protected val onboardingStateRepository = mockk<OnboardingStateRepository>()
    protected val onboardingState = MutableStateFlow(OnboardingState(initializedVersion = 1, starterProjectId = 7L))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockCounterProcessAndWidget()
        val project = CounterProject(id = 7L, name = "Project")
        observedProject.value = project
        every { repository.getActiveProjects() } returns flowOf(listOf(project))
        every { repository.observeProject(7L) } returns observedProject
        every { repository.observeActiveSession() } returns flowOf(null)
        every { repository.observeCanUndo(any()) } returns flowOf(false)
        coEvery { repository.refreshActiveSession() } returns null
        every { tipSession.onSignal(any()) } returns Unit
        every { onboardingStateRepository.state } returns onboardingState
    }

    @After
    fun tearDown() {
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

    protected fun captureWidgetUpdates(): List<WidgetData> {
        val published = mutableListOf<WidgetData>()
        coEvery { CounterWidgetState.syncAll(any(), capture(published)) } returns Unit
        return published
    }

    protected fun pendingIncrement(count: Int): CompletableDeferred<CommittedMainCounterChange?> {
        val result = CompletableDeferred<CommittedMainCounterChange?>()
        coEvery { repository.applyMainCounterChangeWithResult(7L, MainCounterChange.Increment) } coAnswers {
            result.await()
        }
        coEvery { repository.getProject(7L) } returns observedProject.value.copy(count = count)
        return result
    }

    protected fun observeOtherProject(count: Int): CounterProject {
        val project = CounterProject(id = 8L, name = "Other", count = count)
        every { repository.observeProject(project.id) } returns flowOf(project)
        return project
    }

    protected fun TestScope.viewModel(
        savedPatternRows: Flow<List<SavedPattern>> = flowOf(emptyList()),
        countersOverride: ProjectCounterRepository? = null,
        proManagerOverride: ProManager? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        documentRepositoryOverride: ProjectDocumentRepository? = null,
        yarnRepositoryOverride: YarnCardRepository? = null,
        reminderRepositoryOverride: ReminderRepository? = null,
        yarnNoteRepositoryOverride: ProjectYarnNoteRepository? = null,
        savedPatternsOverride: SavedPatternRepository? = null,
        patternStorageOverride: PatternDocumentStorage? = null,
        contextOverride: Context? = null,
        startupPreferences: Flow<com.finnvek.knittools.data.datastore.AppPreferences> = emptyFlow(),
        startupControlsReady: kotlinx.coroutines.flow.StateFlow<Boolean> = MutableStateFlow(true),
        startupProAccessReady: kotlinx.coroutines.flow.StateFlow<Boolean> = MutableStateFlow(true),
    ): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns startupPreferences
        val proManager = proManagerOverride ?: mockk<ProManager>()
        every { proManager.initialControlsReady } returns startupControlsReady
        every { proManager.initialStateReady } returns startupProAccessReady
        if (proManagerOverride == null) {
            every { proManager.proState } returns MutableStateFlow(ProState())
            every { proManager.hasFeature(any()) } returns false
        }
        val yarnRepository = yarnRepositoryOverride ?: mockk<YarnCardRepository>()
        every { yarnRepository.getAllCards() } returns emptyFlow()
        val savedPatterns = savedPatternsOverride ?: mockk<SavedPatternRepository>()
        every { savedPatterns.getAll() } returns savedPatternRows
        val reminders = reminderRepositoryOverride ?: mockk<ReminderRepository>()
        if (reminderRepositoryOverride == null) {
            every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
        }
        val counters = countersOverride ?: mockk<ProjectCounterRepository>()
        if (countersOverride == null) {
            every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
        }
        every { photoRepository.getLatestPhotos(any()) } returns flowOf(emptyList())
        every { photoRepository.getPhotosForProject(any()) } returns flowOf(emptyList())
        val yarnNotes = yarnNoteRepositoryOverride ?: mockk<ProjectYarnNoteRepository>()
        every { yarnNotes.observeForProject(any()) } returns flowOf(emptyList())
        val documentDao = mockk<ProjectDocumentDao>()
        every { documentDao.observeForProject(any()) } returns flowOf(listOf(document(41L), document(42L)))
        val layerRepository = mockk<PatternAnnotationLayerRepository>()
        every { layerRepository.observeLayers(any()) } returns layers
        val availability = mockk<ProjectDocumentFileAvailability>()
        coEvery { availability.isAvailable(any()) } returns true
        val documents =
            ProjectDocumentRepository(documentDao, mockk(), savedPatterns, layerRepository, mockk(), availability)
        return CounterViewModel(
            repository = repository,
            reminderRepository = reminders,
            projectCounterRepository = counters,
            photoRepository = photoRepository,
            projectYarnNoteRepository = yarnNotes,
            preferencesManager = preferences,
            proManager = proManager,
            yarnCardRepository = yarnRepository,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documentRepositoryOverride ?: documents,
            patternDocumentStorage = patternStorageOverride ?: mockk(),
            inAppReviewManager = reviewManager,
            savedStateHandle = savedStateHandle,
            context = contextOverride ?: mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
            tipSession = tipSession,
            onboardingStateRepository = onboardingStateRepository,
        )
    }

    protected fun TestScope.collectErrors(flow: Flow<Unit>): List<Unit> {
        val errors = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { errors.add(it) }
        }
        return errors
    }

    protected fun document(id: Long) =
        ProjectDocumentEntity(
            id = id,
            projectId = 7L,
            savedPatternId = null,
            documentKey = "local:$id",
            label = "Document $id",
            localPdfUri = "content://pattern/$id",
            sortOrder = id.toInt(),
            isPrimary = id == 41L,
            currentPage = if (id == 41L) 1 else 4,
            rowMapping =
                serializeMapping(
                    listOf(if (id == 41L) RowMarker(10, 1, 0.2f) else RowMarker(20, 4, 0.8f)),
                ),
            readingLineEnabled = true,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = false,
            verticalReadingGuideEnabled = id == 42L,
            verticalReadingGuideXFraction = if (id == 41L) 0.2f else 0.8f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    protected fun layer(
        id: Long,
        active: Boolean,
    ) = PatternAnnotationLayer(id, PatternAnnotationOwner.Project(7L, "local:$id"), active, 1L, 1L)
}

internal fun mockCounterProcessAndWidget() {
    mockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
    every { ProcessLifecycleOwner.get() } returns mockk<LifecycleOwner>(relaxed = true)
    coEvery { CounterWidgetState.syncAll(any(), any()) } returns Unit
}

internal fun CounterRepository.assertRestorationOnlyReads() {
    coVerify(atLeast = 0) {
        getActiveProjects()
        observeProject(any())
        observeActiveSession()
        observeHasSessionsForProject(any())
        observeCanUndo(any())
        refreshActiveSession()
        getTotalMinutesForProject(any())
    }
    confirmVerified(this)
}

internal fun CounterRepository.stubSessionReads() {
    every { observeActiveSession() } returns flowOf(null)
    every { observeHasSessionsForProject(any()) } returns flowOf(true)
    coEvery { refreshActiveSession() } returns null
    coEvery { getTotalMinutesForProject(any()) } answers { firstArg<Long>().toInt() * 10 }
}
