package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentFileAvailability
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CounterViewModelFixture {
    protected val dispatcher = StandardTestDispatcher()
    protected val repository = mockk<CounterRepository>(relaxed = true)
    protected val layers = MutableStateFlow(listOf(layer(41L, active = true), layer(42L, active = false)))
    protected val observedProject = MutableStateFlow(CounterProject(id = 7L, name = "Project"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        every { ProcessLifecycleOwner.get() } returns mockk<LifecycleOwner>(relaxed = true)
        coEvery { CounterWidgetState.syncAll(any(), any()) } returns Unit
        val project = CounterProject(id = 7L, name = "Project")
        observedProject.value = project
        every { repository.getActiveProjects() } returns flowOf(listOf(project))
        every { repository.observeProject(7L) } returns observedProject
        every { repository.observeActiveSession() } returns flowOf(null)
        coEvery { repository.refreshActiveSession() } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

    protected fun TestScope.viewModel(
        savedPatternRows: Flow<List<SavedPattern>> = flowOf(emptyList()),
        countersOverride: ProjectCounterRepository? = null,
        proManagerOverride: ProManager? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns emptyFlow()
        val proManager = proManagerOverride ?: mockk<ProManager>()
        if (proManagerOverride == null) {
            every { proManager.proState } returns MutableStateFlow(ProState())
            every { proManager.hasFeature(any()) } returns false
        }
        val yarnRepository = mockk<YarnCardRepository>()
        every { yarnRepository.getAllCards() } returns emptyFlow()
        val savedPatterns = mockk<SavedPatternRepository>()
        every { savedPatterns.getAll() } returns savedPatternRows
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
        val counters = countersOverride ?: mockk<ProjectCounterRepository>()
        if (countersOverride == null) {
            every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
        }
        val photos = mockk<ProgressPhotoRepository>()
        every { photos.getLatestPhotos(any()) } returns flowOf(emptyList())
        every { photos.getPhotosForProject(any()) } returns flowOf(emptyList())
        val yarnNotes = mockk<ProjectYarnNoteRepository>()
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
            photoRepository = photos,
            projectYarnNoteRepository = yarnNotes,
            preferencesManager = preferences,
            proManager = proManager,
            yarnCardRepository = yarnRepository,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documents,
            patternDocumentStorage = mockk(),
            inAppReviewManager = mockk(),
            savedStateHandle = savedStateHandle,
            context = mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
        )
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
