package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
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
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val layers = MutableStateFlow(listOf(layer(41L, active = true), layer(42L, active = false)))
    private val observedProject = MutableStateFlow(CounterProject(id = 7L, name = "Project"))

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

    @Test
    fun `marker edit after document switch preserves secondary markers instead of copying primary markers`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            layers.value = listOf(layer(41L, active = false), layer(42L, active = true))
            advanceUntilIdle()
            val mapping = slot<String>()
            coEvery { repository.updatePatternRowMapping(7L, capture(mapping)) } returns Unit

            viewModel.upsertPatternRowMarker(row = 30, page = 4, yPosition = 0.7f)
            advanceUntilIdle()

            assertEquals(
                listOf(RowMarker(20, 4, 0.8f), RowMarker(30, 4, 0.7f)),
                parseMapping(mapping.captured),
            )
        }

    @Test
    fun `vertical guide toggle after document switch preserves secondary guide position`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            layers.value = listOf(layer(41L, active = false), layer(42L, active = true))
            advanceUntilIdle()

            viewModel.setVerticalReadingGuideEnabled(false)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.updateVerticalReadingGuide(7L, false, 0.8f) }
        }

    @Test
    fun `failed session replacement preserves conflict and offers retry`() =
        runTest {
            val active = activeSession()
            coEvery { repository.startSession(7L) } returns StartSessionResult.ProjectConflict(active, 7L)
            coEvery { repository.replaceActiveSession(7L, "existing", true) } returns
                StartSessionResult.PersistenceFailure
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.startWorkSession()
            advanceUntilIdle()

            viewModel.resolveSessionStartConflict(saveCurrent = true)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.sessionStartConflict)
            assertEquals(R.string.work_session_could_not_start, viewModel.uiState.value.workSessionErrorRes)
            assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)

            coEvery { repository.replaceActiveSession(7L, "existing", true) } returns
                StartSessionResult.Started(active.copy(projectId = 7L))
            viewModel.retryWorkSessionAction()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.sessionStartConflict)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `linked pattern attach edit and removal refresh the open project without reopening`() =
        runTest {
            val patterns = MutableStateFlow<List<SavedPattern>>(emptyList())
            val original = savedPattern(9L, "Original")
            val viewModel = viewModel(patterns)
            advanceUntilIdle()

            patterns.value = listOf(original)
            observedProject.value = observedProject.value.copy(linkedPatternId = 9L, patternName = "Original")
            advanceUntilIdle()

            assertEquals(original, viewModel.uiState.value.linkedPattern)

            val edited = original.copy(name = "Edited")
            patterns.value = listOf(edited)
            advanceUntilIdle()

            assertEquals(
                "Edited",
                viewModel.uiState.value.linkedPattern
                    ?.name,
            )

            patterns.value = emptyList()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.linkedPattern)
        }

    @Test
    fun `web metadata attach surfaces replacement and never uses legacy document attach`() =
        runTest {
            val pattern = savedPattern(9L, "Replacement")
            coEvery { repository.attachSavedPatternMetadata(7L, 9L, null) } returns
                SavedPatternMetadataMutationResult.ReplacementRequired(8L)
            val results = mutableListOf<SavedPatternMetadataMutationResult>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.attachSavedPatternMetadata(pattern.id, onResult = results::add)
            advanceUntilIdle()

            assertEquals(listOf(SavedPatternMetadataMutationResult.ReplacementRequired(8L)), results)
            coVerify(exactly = 1) { repository.attachSavedPatternMetadata(7L, 9L, null) }
            coVerify(exactly = 0) { repository.attachSavedPattern(any(), any()) }
        }

    @Test
    fun `confirmed metadata replacement and unlink preserve expected ids`() =
        runTest {
            coEvery { repository.attachSavedPatternMetadata(7L, 9L, 8L) } returns
                SavedPatternMetadataMutationResult.Attached(9L)
            coEvery { repository.unlinkSavedPatternMetadata(7L, 9L) } returns
                SavedPatternMetadataMutationResult.Unlinked
            val results = mutableListOf<SavedPatternMetadataMutationResult>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.attachSavedPatternMetadata(9L, expectedExistingSavedPatternId = 8L, onResult = results::add)
            viewModel.unlinkSavedPatternMetadata(9L, onResult = results::add)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SavedPatternMetadataMutationResult.Attached(9L),
                    SavedPatternMetadataMutationResult.Unlinked,
                ),
                results,
            )
            coVerify(exactly = 1) { repository.attachSavedPatternMetadata(7L, 9L, 8L) }
            coVerify(exactly = 1) { repository.unlinkSavedPatternMetadata(7L, 9L) }
        }

    private fun TestScope.viewModel(
        savedPatternRows: Flow<List<SavedPattern>> = flowOf(emptyList()),
    ): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns emptyFlow()
        val proManager = mockk<ProManager>()
        every { proManager.proState } returns MutableStateFlow(ProState())
        val yarnRepository = mockk<YarnCardRepository>()
        every { yarnRepository.getAllCards() } returns emptyFlow()
        val savedPatterns = mockk<SavedPatternRepository>()
        every { savedPatterns.getAll() } returns savedPatternRows
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(7L) } returns flowOf(emptyList())
        val counters = mockk<ProjectCounterRepository>()
        every { counters.getCountersForProject(7L) } returns flowOf(emptyList())
        val photos = mockk<ProgressPhotoRepository>()
        every { photos.getLatestPhotos(7L) } returns flowOf(emptyList())
        every { photos.getPhotosForProject(7L) } returns flowOf(emptyList())
        val yarnNotes = mockk<ProjectYarnNoteRepository>()
        every { yarnNotes.observeForProject(7L) } returns flowOf(emptyList())
        val documentDao = mockk<ProjectDocumentDao>()
        every { documentDao.observeForProject(7L) } returns flowOf(listOf(document(41L), document(42L)))
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
            savedStateHandle = SavedStateHandle(),
            context = mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
        )
    }

    private fun document(id: Long) =
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

    private fun layer(
        id: Long,
        active: Boolean,
    ) = PatternAnnotationLayer(id, PatternAnnotationOwner.Project(7L, "local:$id"), active, 1L, 1L)

    private fun savedPattern(
        id: Long,
        name: String,
    ) = SavedPattern(
        id = id,
        source = SavedPatternSource.Other,
        name = name,
        designerName = "",
        originalUrl = "https://example.com/$id",
        canonicalUrl = "https://example.com/$id",
    )

    private fun activeSession() =
        ActiveWorkSession(
            sessionToken = "existing",
            projectId = 8L,
            startedAtWallMillis = 1_000L,
            startZoneId = "Europe/Helsinki",
            startRow = 0,
            lastObservedRow = 0,
            trustedLastObservedRow = 0,
            trustedRowsWorked = 0,
            pendingRowsWorked = 0,
            reviewedRowsWorked = 0,
            reviewedLastObservedRow = 0,
            unreviewedRowsWorked = 0,
            timingAnchors = ActiveSessionTimingAnchors(1_000L, 1_000L, 1L, 0L, 0L),
            recoveryReason = null,
            recoveryIntervalToken = null,
            recoverySuggestedDurationSeconds = null,
            recoveryPromptShown = false,
        )
}
