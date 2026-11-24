package com.finnvek.knittools.ui.screens.pattern

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATIONS
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
import com.finnvek.knittools.data.storage.PatternPdfExportLimitException
import com.finnvek.knittools.data.storage.PatternPdfExportLimitReason
import com.finnvek.knittools.data.storage.PatternPdfExportProgress
import com.finnvek.knittools.data.storage.PatternPdfExporter
import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCorner
import com.finnvek.knittools.domain.model.ChartCounterType
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.ChartTrackingMode
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.repositoryReadRetryDelayMillis
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationPageLimitViewModelTest {
    @Test
    fun `annotation editor only matches the visible document and page`() {
        val owner = PatternAnnotationOwner.Project(7L, "current-document")
        val context = PatternPdfExportContext(mockk<Uri>(), owner, projectDocumentId = 15L)
        val state = PatternAnnotationUiState(owner = owner, currentPage = 2, projectDocumentId = 15L)

        assertTrue(state.matchesDisplayedPage(context, 2))
        assertFalse(state.matchesDisplayedPage(context, 1))
        assertFalse(state.matchesDisplayedPage(context.copy(projectDocumentId = 16L), 2))
        assertFalse(state.matchesDisplayedPage(context.copy(owner = owner.copy(documentKey = "other-document")), 2))
        assertFalse(state.matchesDisplayedPage(null, 2))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun hidingEditableLayerRejectsPendingGestureAndOrdinaryWrites() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            val model = route.viewModel()
            val store = androidx.lifecycle.ViewModelStore().apply { put("hidden-layer-writes", model) }
            try {
                runCurrent()
                model.setActiveTool(PatternAnnotationTool.PEN)
                model.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
                model.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
                model.setProjectLayerVisible(false)
                model.commitStroke(0f)
                model.addTextBox("Hidden")
                runCurrent()
                assertEquals(null, model.uiState.value.inProgressAnnotation)

                model.addTextBox("Still hidden")
                runCurrent()
                coVerify(exactly = 0) { route.annotationRepository.insertAnnotation(any()) }
            } finally {
                store.clear()
            }
        }

    @Test
    fun longStrokeKeepsItsEndpointAndStaysWithinThePayloadLimit() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            val model = route.viewModel()
            val store = androidx.lifecycle.ViewModelStore().apply { put("long-stroke", model) }
            try {
                runCurrent()
                model.setActiveTool(PatternAnnotationTool.PEN)
                model.beginStroke(NormalizedPatternPoint(0f, 0.5f))
                for (index in 1..3_000) {
                    model.appendStrokePoint(NormalizedPatternPoint(index / 3_000f, 0.5f))
                }
                runCurrent()

                val points = checkNotNull(model.uiState.value.draftStroke).points
                assertTrue(points.size <= 2_048)
                assertEquals(0f, points.first().x)
                assertEquals(1f, points.last().x)
            } finally {
                store.clear()
            }
        }

    @Test
    fun hiddenEditablePageCannotBeCleared() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            every { route.annotationRepository.observePage(41L, 0) } returns flowOf(listOf(annotation(41L, 0)))
            val model = route.viewModel()
            val store = androidx.lifecycle.ViewModelStore().apply { put("hidden-layer", model) }
            try {
                runCurrent()
                assertEquals(1, model.uiState.value.projectAnnotations.size)
                model.setProjectLayerVisible(false)
                runCurrent()
                model.clearEditablePage()
                runCurrent()
                coVerify(exactly = 0) { route.annotationRepository.clearPage(any(), any()) }
            } finally {
                store.clear()
            }
        }

    @Test fun `blocked page clears prior annotations and leaves valid pages available`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            val visible =
                PatternAnnotation(
                    1L,
                    31L,
                    0,
                    PatternAnnotationKind.LINE,
                    ShapePayload(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f), 0, 2f),
                    0L,
                )
            every { annotationRepository.observePage(31L, 0) } returns flowOf(listOf(visible))
            every { annotationRepository.observePage(31L, 1) } returns
                flow { throw PatternAnnotationPageLimitException() }
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()
            assertEquals(listOf(visible), viewModel.uiState.value.masterAnnotations)
            viewModel.setCurrentPage(1)
            advanceUntilIdle()
            assertEquals(PatternAnnotationLoadError.PAGE_LIMIT, viewModel.uiState.value.loadError)
            assertTrue(
                viewModel.uiState.value.masterAnnotations
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.projectAnnotations
                    .isEmpty(),
            )
            assertEquals(null, viewModel.uiState.value.editableLayerId)
            viewModel.eraseStrokeAt(NormalizedPatternPoint(0.5f, 0.5f))
            coVerify(exactly = 0) { annotationRepository.deleteAnnotation(any()) }
            viewModel.setCurrentPage(0)
            advanceUntilIdle()
            assertEquals(PatternAnnotationLoadError.NONE, viewModel.uiState.value.loadError)
            assertEquals(listOf(visible), viewModel.uiState.value.masterAnnotations)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `route requires exactly one positive owner id`() {
        val counterRepository = mockk<CounterRepository>(relaxed = true)
        val layerRepository = mockk<PatternAnnotationLayerRepository>(relaxed = true)
        val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)

        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(),
                counterRepository,
                layerRepository,
                annotationRepository,
                projectDocumentRepository = mockk(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("projectId" to 7L, "savedPatternId" to 12L)),
                counterRepository,
                layerRepository,
                annotationRepository,
                projectDocumentRepository = mockk(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("projectId" to 0L)),
                counterRepository,
                layerRepository,
                annotationRepository,
                projectDocumentRepository = mockk(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("savedPatternId" to -1L)),
                counterRepository,
                layerRepository,
                annotationRepository,
                projectDocumentRepository = mockk(),
            )
        }
    }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `library route edits only master layer and follows current page`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
            every { annotationRepository.observePage(31L, any()) } answers {
                // CPD-ON
                flowOf(listOf(annotation(layerId = 31L, page = secondArg())))
            }

            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            assertEquals(PatternAnnotationOwner.SavedPattern(12L, documentKey), viewModel.uiState.value.owner)
            assertEquals(31L, viewModel.uiState.value.editableLayerId)
            assertEquals(
                0,
                viewModel.uiState.value.masterAnnotations
                    .single()
                    .page,
            )
            assertTrue(
                viewModel.uiState.value.projectAnnotations
                    .isEmpty(),
            )

            viewModel.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.5f))
            runCurrent()
            assertEquals(31L, viewModel.uiState.value.selectedAnnotationId)

            viewModel.setCurrentPage(3)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.currentPage)
            assertEquals(null, viewModel.uiState.value.selectedAnnotationId)
            assertEquals(
                3,
                viewModel.uiState.value.masterAnnotations
                    .single()
                    .page,
            )
        }

    @Test
    fun `project route combines read only master and editable project annotations`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(31L, 0) } returns flowOf(listOf(annotation(31L, 0)))
            every { route.annotationRepository.observePage(41L, 0) } returns flowOf(listOf(annotation(41L, 0)))

            val viewModel = route.viewModel()
            advanceUntilIdle()

            assertEquals(route.projectOwner, viewModel.uiState.value.owner)
            assertEquals(41L, viewModel.uiState.value.editableLayerId)
            assertEquals(
                31L,
                viewModel.uiState.value.masterAnnotations
                    .single()
                    .layerId,
            )
            assertEquals(
                41L,
                viewModel.uiState.value.projectAnnotations
                    .single()
                    .layerId,
            )

            viewModel.setMasterLayerVisible(false)
            viewModel.setProjectLayerVisible(false)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.masterLayerVisible)
            assertFalse(viewModel.uiState.value.projectLayerVisible)
        }

    @Test
    fun `selection prioritizes project layer and ignores annotations in hidden layers`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(31L, 0) } returns
                flowOf(listOf(annotation(layerId = 31L, page = 0, zIndex = 100L)))
            every { route.annotationRepository.observePage(41L, 0) } returns flowOf(listOf(annotation(41L, 0)))
            val viewModel = route.viewModel()
            advanceUntilIdle()
            val hitPoint = NormalizedPatternPoint(0.5f, 0.5f)

            viewModel.selectAnnotationAt(hitPoint)
            runCurrent()
            assertEquals(41L, viewModel.uiState.value.selectedAnnotationId)
            assertTrue(viewModel.uiState.value.selectedAnnotationIsEditable)
            assertFalse(viewModel.uiState.value.selectedAnnotationSupportsChartTracker)

            viewModel.setProjectLayerVisible(false)
            advanceUntilIdle()
            viewModel.selectAnnotationAt(hitPoint)
            runCurrent()
            assertEquals(31L, viewModel.uiState.value.selectedAnnotationId)
            assertFalse(viewModel.uiState.value.selectedAnnotationIsEditable)
            assertFalse(viewModel.uiState.value.selectedAnnotationSupportsChartTracker)

            viewModel.setProjectLayerVisible(true)
            viewModel.setMasterLayerVisible(false)
            advanceUntilIdle()
            viewModel.selectAnnotationAt(hitPoint)
            runCurrent()
            assertEquals(41L, viewModel.uiState.value.selectedAnnotationId)

            viewModel.setProjectLayerVisible(false)
            advanceUntilIdle()
            viewModel.selectAnnotationAt(hitPoint)
            runCurrent()
            assertEquals(null, viewModel.uiState.value.selectedAnnotationId)
        }

    @Test
    fun `project keeps active annotation document when saved pattern link disappears`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            every { route.annotationRepository.observePage(41L, 0) } returns
                flowOf(listOf(annotation(layerId = 41L, page = 0)))
            val viewModel = route.viewModel()
            advanceUntilIdle()

            route.project.value = route.project.value.copy(linkedPatternId = null)
            route.documents.value = route.documents.value.map { it.copy(savedPatternId = null) }
            advanceUntilIdle()

            assertEquals(route.projectOwner, viewModel.uiState.value.owner)
            assertEquals(41L, viewModel.uiState.value.editableLayerId)
            assertEquals(null, viewModel.uiState.value.masterLayerId)
            assertEquals(
                41L,
                viewModel.uiState.value.projectAnnotations
                    .single()
                    .layerId,
            )
        }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `repository load failure is recoverable`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } throws
                IOException("temporary") andThen masterLayer
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )

            runCurrent()
            assertEquals(PatternAnnotationLoadError.READ_FAILED, viewModel.uiState.value.loadError)
            assertFalse(viewModel.uiState.value.selectedAnnotationIsEditable)
            advanceTimeBy(repositoryReadRetryDelayMillis(attempt = 0))
            advanceUntilIdle()

            assertEquals(PatternAnnotationLoadError.NONE, viewModel.uiState.value.loadError)
            assertEquals(31L, viewModel.uiState.value.editableLayerId)
        }
    // CPD-ON

    @Test
    fun `pointer moves stay in memory and gesture end persists one pressure stroke`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } returns 55L
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f, pressure = 0.35f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f, pressure = 0.8f))
            runCurrent()

            coVerify(exactly = 0) { annotationRepository.insertAnnotation(any()) }
            assertEquals(
                2,
                viewModel.uiState.value.draftStroke
                    ?.points
                    ?.size,
            )

            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                annotationRepository.insertAnnotation(
                    match { saved ->
                        val payload = saved.payload as FreehandPayload
                        saved.kind == PatternAnnotationKind.FREEHAND &&
                            payload.pressureEnabled &&
                            payload.points.last().pressure == 0.8f
                    },
                )
            }
            assertEquals(null, viewModel.uiState.value.draftStroke)
        }

    // CPD-OFF: Sivunvaihtotestin skenaariokohtainen asetelma pidetaan testin yhteydessa.
    @Test
    fun `page change discards an unfinished stroke without persisting it`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, any()) } returns flowOf(emptyList())
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.setCurrentPage(1)
            advanceUntilIdle()

            coVerify(exactly = 0) { annotationRepository.insertAnnotation(any()) }
            assertEquals(null, viewModel.uiState.value.draftStroke)
            assertEquals(1, viewModel.uiState.value.currentPage)
        }
    // CPD-ON

    @Test
    fun `failed stroke write keeps draft available for retry`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } throws IOException("write failed")
            // CPD-ON
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.HIGHLIGHTER)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.8f, 0.2f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()

            assertEquals(PatternAnnotationWriteError.WRITE_FAILED, viewModel.uiState.value.writeError)
            assertEquals(null, viewModel.uiState.value.inProgressAnnotation)
            assertFalse(viewModel.uiState.value.canUndo)
            assertEquals(
                2,
                viewModel.uiState.value.draftStroke
                    ?.points
                    ?.size,
            )
            coEvery { annotationRepository.insertAnnotation(any()) } returns 55L
            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()

            coVerify(exactly = 2) { annotationRepository.insertAnnotation(any()) }
            assertEquals(null, viewModel.uiState.value.draftStroke)
            assertEquals(PatternAnnotationWriteError.NONE, viewModel.uiState.value.writeError)
        }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `cancellation is not converted into an annotation write error`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } throws CancellationException("cancelled")
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()

            assertEquals(PatternAnnotationWriteError.NONE, viewModel.uiState.value.writeError)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `undo cancellation preserves history without reporting a write error`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } returns 55L
            coEvery { annotationRepository.deleteAnnotation(55L) } throws CancellationException("cancelled")
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.undo()
            advanceUntilIdle()

            assertEquals(PatternAnnotationWriteError.NONE, viewModel.uiState.value.writeError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.canUndo)
        }
    // CPD-ON

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `tool change cannot cancel an active stroke write`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val writeGate = CompletableDeferred<Unit>()
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } coAnswers {
                writeGate.await()
                55L
            }
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            runCurrent()

            viewModel.setActiveTool(PatternAnnotationTool.BROWSE)
            runCurrent()

            assertTrue(viewModel.uiState.value.isSaving)
            assertEquals(
                2,
                viewModel.uiState.value.draftStroke
                    ?.points
                    ?.size,
            )

            writeGate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSaving)
            assertEquals(null, viewModel.uiState.value.draftStroke)
        }
    // CPD-ON

    @Test
    fun `shape insertion supports forward undo and redo`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } returns 61L
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.RECTANGLE)
            viewModel.beginStroke(NormalizedPatternPoint(0.2f, 0.3f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.7f, 0.8f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                annotationRepository.insertAnnotation(
                    match { it.kind == PatternAnnotationKind.RECTANGLE },
                )
            }
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.undo()
            advanceUntilIdle()
            coVerify(exactly = 1) { annotationRepository.deleteAnnotation(61L) }
            assertTrue(viewModel.uiState.value.canRedo)

            viewModel.redo()
            advanceUntilIdle()
            coVerify(exactly = 1) { annotationRepository.restoreBatch(match { it.single().id == 61L }) }
        }

    @Test
    fun `eraser deletes the topmost editable shape`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val lowerShape =
                PatternAnnotation(
                    id = 60L,
                    layerId = 31L,
                    page = 0,
                    kind = PatternAnnotationKind.RECTANGLE,
                    payload =
                        ShapePayload(
                            start = NormalizedPatternPoint(0.2f, 0.3f),
                            end = NormalizedPatternPoint(0.7f, 0.8f),
                            strokeArgb = 0xFF000000.toInt(),
                            strokeWidth = 2f,
                        ),
                    zIndex = 3L,
                )
            val topShape = lowerShape.copy(id = 61L, zIndex = 4L)
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns
                layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { annotationRepository.observePage(31L, 0) } returns
                flowOf(listOf(lowerShape, topShape))
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("savedPatternId" to 12L)),
                    mockk(relaxed = true),
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository = mockk(),
                )
            advanceUntilIdle()

            viewModel.selectAnnotationAt(NormalizedPatternPoint(0.2f, 0.5f))
            runCurrent()
            assertEquals(61L, viewModel.uiState.value.selectedAnnotationId)
            viewModel.eraseStrokeAt(NormalizedPatternPoint(0.2f, 0.5f))
            advanceUntilIdle()

            coVerify(exactly = 1) { annotationRepository.deleteAnnotation(61L) }
            coVerify(exactly = 0) { annotationRepository.deleteAnnotation(60L) }
        }

    @Test
    @Suppress("LongMethod")
    fun `selected master chart region is copied to project tracker`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val counterRepository = mockk<CounterRepository>()
            val projectCounterRepository = mockk<ProjectCounterRepository>()
            val projectDocumentRepository = mockk<ProjectDocumentRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val projectOwner = PatternAnnotationOwner.Project(7L, documentKey)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            val projectLayer = layer(id = 41L, owner = projectOwner)
            val region =
                ChartRegionPayload(
                    bounds = NormalizedPatternBounds(0.2f, 0.2f, 0.8f, 0.8f),
                    name = "Master chart",
                    rows = 8,
                    columns = 9,
                    rowDirection = ChartRowDirection.BOTTOM_TO_TOP,
                    columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                )
            every { counterRepository.observeProject(7L) } returns
                flowOf(CounterProject(id = 7L, name = "Project", count = 14, linkedPatternId = 12L))
            every { projectCounterRepository.getCountersForProject(7L) } returns flowOf(emptyList())
            every { layerRepository.observeLayers(any()) } returns flowOf(listOf(projectLayer))
            every { projectDocumentRepository.observeDocuments(7L) } returns flowOf(listOf(projectDocument()))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
            every { annotationRepository.observePage(31L, 0) } returns
                flowOf(
                    listOf(
                        PatternAnnotation(
                            id = 101L,
                            layerId = 31L,
                            page = 0,
                            kind = PatternAnnotationKind.CHART_REGION,
                            payload = region,
                            zIndex = 0L,
                        ),
                    ),
                )
            every { annotationRepository.observePage(41L, 0) } returns flowOf(emptyList())
            coEvery { annotationRepository.insertAnnotation(any()) } returns 202L
            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("projectId" to 7L)),
                    counterRepository,
                    layerRepository,
                    annotationRepository,
                    projectDocumentRepository,
                    projectCounterRepository,
                )
            advanceUntilIdle()

            viewModel.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.5f))
            runCurrent()
            assertFalse(viewModel.uiState.value.selectedAnnotationIsEditable)
            assertTrue(viewModel.uiState.value.selectedAnnotationSupportsChartTracker)
            viewModel.addChartTrackerFromSelected(
                PatternChartTrackerDraft(
                    rows = 6,
                    columns = 7,
                    rowDirection = ChartRowDirection.TOP_TO_BOTTOM,
                    columnDirection = ChartColumnDirection.ALTERNATING,
                    trackingMode = ChartTrackingMode.CROSSHAIR,
                    counter =
                        viewModel.uiState.value.chartCounterOptions
                            .single(),
                    gridStartIndex = 2,
                    wrapAtEnd = true,
                    c2cOrigin = ChartCorner.TOP_LEFT,
                ),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                annotationRepository.insertAnnotation(
                    match { annotation ->
                        val tracker = annotation.payload as ChartTrackerPayload
                        annotation.layerId == 41L &&
                            tracker.counterType == ChartCounterType.MAIN &&
                            tracker.counterStartValue == 14 &&
                            tracker.region.rows == 6 &&
                            tracker.region.columns == 7 &&
                            tracker.wrapAtEnd
                    },
                )
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationOrderingViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bring forward at maximum rank preserves ordering through undo and redo`() =
        verifyOrdering(Long.MAX_VALUE, toFront = true)

    @Test
    fun `send backward at minimum rank preserves ordering through undo and redo`() =
        verifyOrdering(Long.MIN_VALUE, toFront = false)

    @Test
    fun `ordinary forward supports undo and redo`() = verifyOrdering(5L, toFront = true)

    @Test
    fun `ordinary backward supports undo and redo`() = verifyOrdering(-5L, toFront = false)

    private fun verifyOrdering(
        otherRank: Long,
        toFront: Boolean,
    ) = runTest {
        val fixture = OrderingFixture(otherRank)
        val before = fixture.annotations.value
        val viewModel = fixture.route.viewModel()
        advanceUntilIdle()
        viewModel.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.2f))
        runCurrent()
        assertEquals(1L, viewModel.uiState.value.selectedAnnotationId)
        if (toFront) viewModel.bringSelectedForward() else viewModel.sendSelectedBackward()
        advanceUntilIdle()
        val expected = if (toFront) listOf(2L, 1L) else listOf(1L, 2L)
        assertEquals(expected, fixture.orderedIds())
        viewModel.undo()
        advanceUntilIdle()
        assertEquals(before, fixture.annotations.value)
        viewModel.redo()
        advanceUntilIdle()
        assertEquals(expected, fixture.orderedIds())
    }

    @Test
    fun `duplicate stays above an existing maximum rank annotation`() =
        runTest {
            val fixture = OrderingFixture(Long.MAX_VALUE)
            val viewModel = fixture.route.viewModel()
            advanceUntilIdle()
            viewModel.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.2f))
            runCurrent()
            viewModel.duplicateSelected()
            advanceUntilIdle()
            assertEquals(listOf(1L, 2L, 3L), fixture.orderedIds())
            val duplicate = fixture.annotations.value.last()
            assertEquals(Long.MAX_VALUE, duplicate.zIndex)
        }

    private class OrderingFixture(
        otherRank: Long,
    ) {
        val route = projectRoute()
        val annotations =
            MutableStateFlow(
                listOf(
                    line(1L, 0L, 0.2f),
                    line(2L, otherRank, 0.8f),
                ),
            )

        init {
            every { route.annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            every { route.annotationRepository.observePage(41L, 0) } returns annotations
            coEvery { route.annotationRepository.updateAnnotation(any()) } coAnswers {
                val changed = firstArg<PatternAnnotation>()
                annotations.value = annotations.value.map { if (it.id == changed.id) changed else it }
            }
            coEvery { route.annotationRepository.restoreBatch(any()) } coAnswers {
                val changed = firstArg<List<PatternAnnotation>>().associateBy { it.id }
                annotations.value = annotations.value.map { changed[it.id] ?: it }
            }
            coEvery { route.annotationRepository.insertAnnotation(any()) } coAnswers {
                annotations.value += firstArg<PatternAnnotation>().copy(id = 3L)
                3L
            }
        }

        fun orderedIds(): List<Long> =
            annotations.value.sortedWith(compareBy<PatternAnnotation>({ it.zIndex }, { it.id })).map { it.id }

        private fun line(
            id: Long,
            rank: Long,
            y: Float,
        ) = annotation(41L, 0, rank).copy(
            id = id,
            payload =
                FreehandPayload(
                    listOf(NormalizedPatternPoint(0.1f, y), NormalizedPatternPoint(0.9f, y)),
                    0,
                    2f,
                ),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationPressureViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tool switches keep pen preference while highlighter input draft and payload ignore pressure`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            val saved = mutableListOf<PatternAnnotation>()
            coEvery { route.annotationRepository.insertAnnotation(capture(saved)) } answers { saved.size.toLong() }
            val viewModel = route.viewModel()
            advanceUntilIdle()
            for (penPressure in listOf(true, false, true)) {
                viewModel.setPressureEnabled(penPressure)
                for (tool in listOf(
                    PatternAnnotationTool.PEN,
                    PatternAnnotationTool.HIGHLIGHTER,
                    PatternAnnotationTool.PEN,
                )) {
                    viewModel.setActiveTool(tool)
                    runCurrent()
                    val expectedPressure = tool == PatternAnnotationTool.PEN && penPressure
                    assertEquals(expectedPressure, viewModel.uiState.value.pressureEnabled)
                    viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f, pressure = 0.2f))
                    viewModel.appendStrokePoint(NormalizedPatternPoint(0.5f, 0.4f, pressure = 0.9f))
                    runCurrent()
                    val draft = requireNotNull(viewModel.uiState.value.draftStroke)
                    assertEquals(expectedPressure, draft.pressureEnabled)
                    assertEquals(
                        if (expectedPressure) listOf(0.2f, 0.9f) else listOf(1f, 1f),
                        draft.points.map { it.pressure },
                    )
                    val preview = requireNotNull(viewModel.uiState.value.inProgressAnnotation)
                    assertEquals(expectedPressure, (preview.payload as FreehandPayload).pressureEnabled)
                    viewModel.commitStroke(0f)
                    advanceUntilIdle()
                    val annotation = saved.last()
                    assertEquals(preview.payload, annotation.payload)
                    val encoded =
                        requireNotNull(
                            com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec.encode(
                                annotation.kind,
                                annotation.payload,
                            ),
                        )
                    assertEquals(
                        annotation.payload,
                        com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec.decode(
                            annotation.kind,
                            encoded,
                        ),
                    )
                }
            }
            assertEquals(9, saved.size)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationDocumentSelectionTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completed strokes wait for a successful write without being overwritten`() =
        verifyCompletedStrokesDuringWrite(firstWriteFails = false, switchDocument = false)

    @Test
    fun `completed strokes are saved after the previous write fails`() =
        verifyCompletedStrokesDuringWrite(firstWriteFails = true, switchDocument = false)

    @Test
    fun `queued strokes retain their page and layer after document switch`() =
        verifyCompletedStrokesDuringWrite(firstWriteFails = false, switchDocument = true)

    private fun verifyCompletedStrokesDuringWrite(
        firstWriteFails: Boolean,
        switchDocument: Boolean,
    ) = runTest {
        val route = projectRoute()
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<PatternAnnotation>()
        every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
        coEvery { route.annotationRepository.insertAnnotation(any()) } coAnswers {
            writes += firstArg<PatternAnnotation>()
            if (writes.size == 1) {
                gate.await()
                if (firstWriteFails) throw IOException("synthetic write failure")
            }
            writes.size.toLong() + 100L
        }
        val viewModel = route.viewModel()
        advanceUntilIdle()
        viewModel.setActiveTool(PatternAnnotationTool.PEN)
        for (index in 1..3) {
            viewModel.beginStroke(NormalizedPatternPoint(index / 10f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(index / 10f, 0.5f))
            viewModel.commitStroke(0f)
            viewModel.commitStroke(0f)
            runCurrent()
        }
        assertEquals(1, writes.size)
        assertTrue(viewModel.uiState.value.isSaving)
        if (switchDocument) {
            val key = PatternAnnotationDocumentKey.savedPattern(13L)
            route.documents.value += projectDocument(id = 52L, savedPatternId = 13L, isPrimary = false)
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, key) } returns
                layer(id = 32L, owner = PatternAnnotationOwner.SavedPattern(13L, key))
            route.projectLayers.value =
                listOf(route.projectLayer.copy(isActive = false), layer(42L, PatternAnnotationOwner.Project(7L, key)))
        }
        viewModel.setCurrentPage(1)
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(3, writes.size)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), writes.map { (it.payload as FreehandPayload).points.first().x })
        assertTrue(writes.all { it.layerId == 41L && it.page == 0 })
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(null, viewModel.uiState.value.draftStroke)
        assertEquals(!switchDocument, viewModel.uiState.value.canUndo)
        assertEquals(
            if (firstWriteFails) PatternAnnotationWriteError.WRITE_FAILED else PatternAnnotationWriteError.NONE,
            viewModel.uiState.value.writeError,
        )
    }

    @Test
    fun `degenerate shape is discarded without blocking the next valid stroke`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            coEvery { route.annotationRepository.insertAnnotation(any()) } returns 101L
            val viewModel = route.viewModel()
            advanceUntilIdle()
            viewModel.setActiveTool(PatternAnnotationTool.RECTANGLE)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.commitStroke(0f)
            advanceUntilIdle()

            coVerify(exactly = 0) { route.annotationRepository.insertAnnotation(any()) }
            assertEquals(null, viewModel.uiState.value.draftStroke)
            assertFalse(viewModel.uiState.value.isSaving)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.2f, 0.3f))
            viewModel.commitStroke(0f)
            advanceUntilIdle()
            coVerify(exactly = 1) { route.annotationRepository.insertAnnotation(any()) }
        }

    @Test
    fun `stroke completed during undo is persisted after undo finishes`() =
        runTest {
            val route = projectRoute()
            val gate = CompletableDeferred<Unit>()
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            coEvery { route.annotationRepository.insertAnnotation(any()) } returnsMany listOf(101L, 102L)
            coEvery { route.annotationRepository.deleteAnnotation(101L) } coAnswers { gate.await() }
            val viewModel = route.viewModel()
            advanceUntilIdle()
            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.2f, 0.3f))
            viewModel.commitStroke(0f)
            advanceUntilIdle()
            viewModel.undo()
            runCurrent()
            viewModel.beginStroke(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.5f, 0.6f))
            viewModel.commitStroke(0f)
            runCurrent()
            coVerify(exactly = 1) { route.annotationRepository.insertAnnotation(any()) }
            gate.complete(Unit)
            advanceUntilIdle()
            coVerify(exactly = 2) { route.annotationRepository.insertAnnotation(any()) }
            assertTrue(viewModel.uiState.value.canUndo)
            assertFalse(viewModel.uiState.value.canRedo)
            assertEquals(null, viewModel.uiState.value.draftStroke)
        }

    @Test
    fun `selecting secondary project document switches its master and editable annotations`() =
        runTest {
            val route = projectRoute()
            val secondaryKey = PatternAnnotationDocumentKey.savedPattern(13L)
            val secondaryOwner = PatternAnnotationOwner.Project(7L, secondaryKey)
            val secondaryLayer = layer(id = 42L, owner = secondaryOwner)
            route.documents.value += projectDocument(id = 52L, savedPatternId = 13L, isPrimary = false)
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, secondaryKey) } returns
                layer(id = 32L, owner = PatternAnnotationOwner.SavedPattern(13L, secondaryKey))
            every { route.annotationRepository.observePage(any(), 0) } answers {
                flowOf(listOf(annotation(layerId = firstArg(), page = 0)))
            }
            val viewModel = route.viewModel()
            advanceUntilIdle()
            assertEquals(31L, viewModel.uiState.value.masterLayerId)

            route.projectLayers.value = listOf(route.projectLayer.copy(isActive = false), secondaryLayer)
            advanceUntilIdle()

            assertEquals(12L, route.project.value.linkedPatternId)
            assertEquals(secondaryOwner, viewModel.uiState.value.owner)
            assertEquals(42L, viewModel.uiState.value.editableLayerId)
            assertEquals(
                listOf(32L),
                viewModel.uiState.value.masterAnnotations
                    .map { it.layerId },
            )
            assertEquals(
                listOf(42L),
                viewModel.uiState.value.projectAnnotations
                    .map { it.layerId },
            )

            route.projectLayers.value = listOf(route.projectLayer, secondaryLayer.copy(isActive = false))
            advanceUntilIdle()

            assertEquals(route.projectOwner, viewModel.uiState.value.owner)
            assertEquals(
                listOf(31L),
                viewModel.uiState.value.masterAnnotations
                    .map { it.layerId },
            )
            assertEquals(
                listOf(41L),
                viewModel.uiState.value.projectAnnotations
                    .map { it.layerId },
            )
        }

    @Test
    fun `late write from previous document cannot enter the new documents undo history`() =
        runTest {
            // CPD-OFF: Dokumentinvaihtotestin skenaariokohtainen asetelma pidetaan testin yhteydessa.
            val route = projectRoute()
            val secondaryKey = PatternAnnotationDocumentKey.savedPattern(13L)
            val secondaryLayer = layer(id = 42L, owner = PatternAnnotationOwner.Project(7L, secondaryKey))
            val writeGate = CompletableDeferred<Unit>()
            route.documents.value += projectDocument(id = 52L, savedPatternId = 13L, isPrimary = false)
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, secondaryKey) } returns
                layer(id = 32L, owner = PatternAnnotationOwner.SavedPattern(13L, secondaryKey))
            every { route.annotationRepository.observePage(any(), 0) } returns flowOf(emptyList())
            coEvery { route.annotationRepository.insertAnnotation(any()) } coAnswers {
                writeGate.await()
                77L
            }
            val viewModel = route.viewModel()
            // CPD-ON
            advanceUntilIdle()

            viewModel.setActiveTool(PatternAnnotationTool.PEN)
            viewModel.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            viewModel.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.5f))
            viewModel.commitStroke(simplificationTolerance = 0f)
            runCurrent()

            route.projectLayers.value = listOf(route.projectLayer.copy(isActive = false), secondaryLayer)
            runCurrent()
            assertEquals(42L, viewModel.uiState.value.editableLayerId)

            writeGate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                route.annotationRepository.insertAnnotation(match { it.layerId == 41L })
            }
            assertFalse(viewModel.uiState.value.canUndo)
            assertFalse(viewModel.uiState.value.canRedo)
            assertEquals(null, viewModel.uiState.value.draftStroke)
        }

    @Test
    fun `secondary project document export contains its own master instead of primary master`() =
        runTest {
            val route = projectRoute()
            val secondaryKey = PatternAnnotationDocumentKey.savedPattern(13L)
            val secondaryLayer = layer(id = 42L, owner = PatternAnnotationOwner.Project(7L, secondaryKey))
            route.documents.value += projectDocument(id = 52L, savedPatternId = 13L, isPrimary = false)
            val exporter = mockk<PatternPdfExporter>(relaxed = true)
            val sourceUri = mockk<Uri>()
            val destinationUri = mockk<Uri>()
            val style = mockk<PatternAnnotationRenderStyle>()
            val allAnnotations = listOf(31L, 32L, 41L, 42L).map { annotation(layerId = it, page = 0) }
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, secondaryKey) } returns
                layer(id = 32L, owner = PatternAnnotationOwner.SavedPattern(13L, secondaryKey))
            every { route.annotationRepository.observePage(any(), 0) } answers {
                val layerId = firstArg<Long>()
                flowOf(allAnnotations.filter { it.layerId == layerId })
            }
            coEvery { route.annotationRepository.getForLayersForExport(any(), any(), any()) } answers {
                val layerIds = firstArg<List<Long>>()
                allAnnotations.filter { it.layerId in layerIds }
            }
            val viewModel = route.viewModel(pdfExporter = exporter)
            advanceUntilIdle()

            route.projectLayers.value = listOf(route.projectLayer.copy(isActive = false), secondaryLayer)
            advanceUntilIdle()
            exportToDestination(viewModel, sourceUri, destinationUri, style, documentId = 52L)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                exporter.export(
                    sourceUri = sourceUri,
                    destinationUri = destinationUri,
                    annotations = allAnnotations.filter { it.layerId == 32L || it.layerId == 42L },
                    trackerHighlights = emptyMap(),
                    style = style,
                    onProgress = any(),
                )
            }
        }

    @Test
    fun `history keeps only the newest 50 of 51 committed commands`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), 0) } returns flowOf(emptyList())
            var nextId = 0L
            val deletedIds = mutableListOf<Long>()
            coEvery { route.annotationRepository.insertAnnotation(any()) } answers { ++nextId }
            coEvery { route.annotationRepository.deleteAnnotation(any()) } answers {
                deletedIds += firstArg<Long>()
            }
            val viewModel = route.viewModel()
            advanceUntilIdle()

            repeat(51) { index ->
                viewModel.addTextBox("Note $index")
                advanceUntilIdle()
                assertTrue(viewModel.uiState.value.canUndo)
            }
            assertEquals(51L, nextId)
            repeat(50) {
                viewModel.undo()
                advanceUntilIdle()
            }
            assertEquals((51L downTo 2L).toList(), deletedIds)
            assertFalse(viewModel.uiState.value.canUndo)
            assertTrue(viewModel.uiState.value.canRedo)
            viewModel.undo()
            advanceUntilIdle()
            assertEquals(50, deletedIds.size)

            viewModel.addTextBox("New note")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canUndo)
            assertFalse(viewModel.uiState.value.canRedo)
        }

    @Test
    fun `hidden master trackers do not consume visible project highlight budget`() =
        runTest {
            val route = projectRoute()
            val master = listOf(trackerAnnotation(101L, 31L, 0), trackerAnnotation(102L, 31L, 0))
            val project = trackerAnnotation(103L, 41L, 0)
            every { route.annotationRepository.observePage(31L, 0) } returns flowOf(master)
            every { route.annotationRepository.observePage(41L, 0) } returns flowOf(listOf(project))
            val viewModel = route.viewModel()
            advanceUntilIdle()
            assertEquals(setOf(101L, 102L), viewModel.uiState.value.trackerHighlights.keys)

            viewModel.setMasterLayerVisible(false)
            advanceUntilIdle()
            assertEquals(setOf(103L), viewModel.uiState.value.trackerHighlights.keys)

            viewModel.setProjectLayerVisible(false)
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.trackerHighlights
                    .isEmpty(),
            )
            viewModel.setMasterLayerVisible(true)
            advanceUntilIdle()
            assertEquals(setOf(101L, 102L), viewModel.uiState.value.trackerHighlights.keys)
        }

    @Test
    fun `export retains every highlight when actual work across all pages fits`() =
        runTest {
            val fixture = PdfExportFixture()
            val trackers =
                listOf(
                    trackerAnnotation(id = 101L, layerId = 41L, page = 0),
                    trackerAnnotation(id = 102L, layerId = 41L, page = 1),
                    trackerAnnotation(id = 103L, layerId = 41L, page = 2),
                )
            every { fixture.route.annotationRepository.observePage(any(), 0) } returns flowOf(emptyList())
            coEvery { fixture.route.annotationRepository.getForLayersForExport(any(), any(), any()) } returns trackers
            val viewModel = fixture.route.viewModel(pdfExporter = fixture.exporter)
            advanceUntilIdle()

            exportToDestination(viewModel, fixture.sourceUri, fixture.destinationUri, fixture.style)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                fixture.exporter.export(
                    sourceUri = fixture.sourceUri,
                    destinationUri = fixture.destinationUri,
                    annotations = trackers,
                    trackerHighlights =
                        match { highlights ->
                            highlights.keys == setOf(101L, 102L, 103L) &&
                                highlights.values.all { it.cells.size == 1 }
                        },
                    style = fixture.style,
                    onProgress = any(),
                )
            }
        }

    @Test
    fun `export rejects excessive actual tracker highlight work instead of omitting highlights`() =
        runTest {
            val fixture = PdfExportFixture()
            val project = fixture.route.project
            project.value = project.value.copy(count = 9_999)
            val trackers = (0..2).map { index -> trackerAnnotation(101L + index, 41L, index) }
            every { fixture.route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            coEvery { fixture.route.annotationRepository.getForLayersForExport(any(), any(), any()) } returns trackers
            val viewModel = fixture.route.viewModel(pdfExporter = fixture.exporter)
            advanceUntilIdle()
            every { fixture.sourceUri.toString() } returns "file:///document-51.pdf"
            val context = PatternPdfExportContext(fixture.sourceUri, viewModel.uiState.value.owner, 51L)
            viewModel.setExportContext(context)
            viewModel.requestAnnotatedPdfExport(context)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.exportFailed)
            coVerify(exactly = 0) { fixture.exporter.preflight(any(), any(), any()) }
        }

    @Test
    fun `export rejects annotation limit plus one before exporter or success state`() =
        runTest {
            val fixture = PdfExportFixture()
            every { fixture.route.annotationRepository.observePage(any(), 0) } returns flowOf(emptyList())
            coEvery {
                fixture.route.annotationRepository.getForLayersForExport(
                    any(),
                    PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                    any(),
                )
            } returnsMany
                listOf(
                    emptyList(),
                    List(PATTERN_PDF_EXPORT_MAX_ANNOTATIONS + 1) { index ->
                        annotation(layerId = 41L, page = 0, zIndex = index.toLong())
                    },
                )
            val viewModel = fixture.route.viewModel(pdfExporter = fixture.exporter)
            advanceUntilIdle()

            exportToDestination(viewModel, fixture.sourceUri, fixture.destinationUri, fixture.style)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.exportFailed)
            assertFalse(viewModel.uiState.value.isExporting)
            coVerify(exactly = 0) { fixture.exporter.export(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `failed preflight does not request SAF destination`() = runTest { verifyPreflightDestination(fails = true) }

    @Test
    fun `successful preflight emits a destination request`() = runTest { verifyPreflightDestination(fails = false) }

    private fun TestScope.verifyPreflightDestination(fails: Boolean) {
        val route = projectRoute()
        val exporter = mockk<PatternPdfExporter>()
        val sourceUri = mockk<Uri>()
        val destinationRequests = mutableListOf<String>()
        every { sourceUri.toString() } returns "file:///document-51.pdf"
        every { route.annotationRepository.observePage(any(), 0) } returns flowOf(emptyList())
        coEvery {
            route.annotationRepository.getForLayersForExport(
                any(),
                PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                any(),
            )
        } returns emptyList()
        if (fails) {
            coEvery { exporter.preflight(sourceUri, emptyList(), emptyMap()) } throws
                PatternPdfExportLimitException(PatternPdfExportLimitReason.PAGE_COUNT)
        } else {
            coEvery { exporter.preflight(sourceUri, emptyList(), emptyMap()) } returns mockk()
        }
        val viewModel = route.viewModel(pdfExporter = exporter)
        advanceUntilIdle()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.exportDestinationRequests.collect { destinationRequests += it }
        }

        val context = PatternPdfExportContext(sourceUri, route.projectOwner, 51L)
        viewModel.setExportContext(context)
        viewModel.requestAnnotatedPdfExport(context)
        advanceUntilIdle()

        if (fails) {
            assertTrue(destinationRequests.isEmpty())
            assertTrue(viewModel.uiState.value.exportFailed)
        } else {
            assertEquals(1, destinationRequests.size)
            assertTrue(viewModel.launchExportDestination(destinationRequests.single(), context))
        }
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `detaching document hides retained annotation layers`() =
        runTest {
            val route = projectRoute()
            every { route.annotationRepository.observePage(any(), 0) } answers {
                flowOf(listOf(annotation(layerId = firstArg(), page = 0)))
            }
            val viewModel = route.viewModel()
            advanceUntilIdle()

            route.documents.value = emptyList()
            route.projectLayers.value = listOf(route.projectLayer.copy(isActive = false))
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.editableLayerId)
            assertTrue(
                viewModel.uiState.value.masterAnnotations
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.projectAnnotations
                    .isEmpty(),
            )
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationWriteFeedbackTest {
    @Test
    fun `new text and callout drafts are acknowledged only after a successful write`() =
        runTest {
            for (library in listOf(false, true)) {
                for (callout in listOf(false, true)) {
                    val fixture = WriteFixture(library)
                    advanceUntilIdle()
                    val vm = fixture.vm
                    val draftId = "retained-draft"
                    val save = {
                        if (callout) {
                            vm.addCallout("Title", "Description", PatternCalloutSymbol.NOTE, draftId = draftId)
                        } else {
                            vm.addTextBox("Retained text", draftId = draftId)
                        }
                    }
                    fixture.nextWrite = { throw IOException("insert rejected") }
                    save()
                    advanceUntilIdle()
                    assertEquals(null, vm.uiState.value.savedDraftId)
                    assertFailed(vm)

                    val gate = CompletableDeferred<Unit>()
                    fixture.nextWrite = { gate.await() }
                    save()
                    runCurrent()
                    assertTrue(vm.uiState.value.isSaving)
                    save()
                    assertEquals(null, vm.uiState.value.savedDraftId)
                    gate.complete(Unit)
                    advanceUntilIdle()

                    assertEquals(draftId, vm.uiState.value.savedDraftId)
                    assertEquals(1, fixture.rows.value.size)
                    assertEquals(2, fixture.writeAttempts)
                    save()
                    advanceUntilIdle()
                    assertEquals(1, fixture.rows.value.size)
                }
            }
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `project and library insert failure survives tool changes until dismissal and deliberate re-entry`() =
        runTest {
            for (library in listOf(false, true)) {
                val fixture = WriteFixture(library)
                val vm = fixture.vm
                advanceUntilIdle()
                fixture.nextWrite = { throw IOException("insert rejected") }
                vm.addTextBox("New text")
                advanceUntilIdle()
                assertFailed(vm)
                assertTrue(fixture.rows.value.isEmpty())
                assertFalse(vm.uiState.value.canUndo)
                vm.setActiveTool(PatternAnnotationTool.BROWSE)
                runCurrent()
                assertFailed(vm)
                vm.dismissWriteError()
                advanceUntilIdle()
                assertEquals(1, fixture.writeAttempts)
                assertEquals(PatternAnnotationWriteError.NONE, vm.uiState.value.writeError)

                vm.setActiveTool(PatternAnnotationTool.PEN)
                vm.addTextBox("Re-entered text")
                advanceUntilIdle()
                assertEquals(
                    if (library) 31L else 41L,
                    fixture.rows.value
                        .single()
                        .layerId,
                )
                assertEquals(
                    "Re-entered text",
                    (
                        fixture.rows.value
                            .single()
                            .payload as TextBoxPayload
                    ).text,
                )
                assertTrue(vm.uiState.value.canUndo)
                assertFalse(vm.uiState.value.canRedo)
            }
        }

    @Test
    fun `delayed stroke failure after Done hides unsaved preview and dismissal never replays it`() =
        runTest {
            for (library in listOf(false, true)) {
                val fixture = WriteFixture(library)
                val gate = CompletableDeferred<Unit>()
                advanceUntilIdle()
                fixture.nextWrite = {
                    gate.await()
                    throw IOException("stroke rejected")
                }
                fixture.stroke()
                runCurrent()
                fixture.vm.setActiveTool(PatternAnnotationTool.BROWSE)
                runCurrent()
                gate.complete(Unit)
                advanceUntilIdle()
                assertFailed(fixture.vm)
                assertTrue(fixture.rows.value.isEmpty())
                assertTrue(fixture.vm.uiState.value.draftStroke != null)
                assertEquals(null, fixture.vm.uiState.value.inProgressAnnotation)
                fixture.vm.dismissWriteError()
                advanceUntilIdle()
                assertEquals(null, fixture.vm.uiState.value.draftStroke)
                assertEquals(1, fixture.writeAttempts)
                fixture.stroke()
                advanceUntilIdle()
                assertEquals(1, fixture.rows.value.size)
                assertTrue(fixture.vm.uiState.value.canUndo)
            }
        }

    @Test
    fun `another command failure never makes a failed stroke preview look committed`() =
        runTest {
            val fixture = WriteFixture()
            advanceUntilIdle()
            fixture.nextWrite = { throw IOException("stroke rejected") }
            fixture.stroke()
            advanceUntilIdle()
            val draft = checkNotNull(fixture.vm.uiState.value.draftStroke)
            fixture.nextWrite = { throw IOException("text rejected") }
            fixture.vm.addTextBox("New text")
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertTrue(draft === fixture.vm.uiState.value.draftStroke)
            assertEquals(null, fixture.vm.uiState.value.inProgressAnnotation)
            assertTrue(fixture.rows.value.isEmpty())
            fixture.vm.dismissWriteError()
            advanceUntilIdle()
            assertEquals(null, fixture.vm.uiState.value.draftStroke)
            assertEquals(2, fixture.writeAttempts)
        }

    @Test
    fun `dismissing an older failure preserves the next queued stroke preview and captured write`() =
        runTest {
            val fixture = WriteFixture()
            val firstGate = CompletableDeferred<Unit>()
            val nextGate = CompletableDeferred<Unit>()
            advanceUntilIdle()
            fixture.nextWrite = {
                firstGate.await()
                fixture.nextWrite = { nextGate.await() }
                throw IOException("first stroke rejected")
            }
            fixture.stroke()
            runCurrent()
            fixture.stroke(endX = 0.6f)
            runCurrent()
            val queuedDraft = fixture.vm.uiState.value.draftStroke
            firstGate.complete(Unit)
            runCurrent()
            assertTrue(fixture.vm.uiState.value.isSaving)
            assertEquals(PatternAnnotationWriteError.WRITE_FAILED, fixture.vm.uiState.value.writeError)
            fixture.vm.dismissWriteError()
            runCurrent()
            assertTrue(queuedDraft === fixture.vm.uiState.value.draftStroke)
            assertTrue(fixture.vm.uiState.value.inProgressAnnotation != null)
            nextGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(2, fixture.writeAttempts)
            assertEquals(
                41L,
                fixture.rows.value
                    .single()
                    .layerId,
            )
            assertEquals(null, fixture.vm.uiState.value.draftStroke)
            assertTrue(fixture.vm.uiState.value.canUndo)
        }

    @Test
    fun `retry saves the failed stroke after a newer stroke has already been saved`() =
        runTest {
            val fixture = WriteFixture()
            val gate = CompletableDeferred<Unit>()
            advanceUntilIdle()
            fixture.nextWrite = {
                gate.await()
                throw IOException("first stroke rejected")
            }
            fixture.stroke(endX = 0.4f)
            runCurrent()
            fixture.stroke(endX = 0.7f)
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, fixture.rows.value.size)
            assertTrue(fixture.vm.uiState.value.canRetryStroke)

            fixture.vm.retryFailedStroke()
            advanceUntilIdle()

            assertEquals(3, fixture.writeAttempts)
            assertEquals(
                setOf(0.4f, 0.7f),
                fixture.rows.value
                    .map { (it.payload as FreehandPayload).points.last().x }
                    .toSet(),
            )
            assertFalse(fixture.vm.uiState.value.canRetryStroke)
        }

    @Test
    fun `finishing the next gesture after a failure does not acknowledge the failed stroke`() =
        runTest {
            val fixture = WriteFixture()
            val gate = CompletableDeferred<Unit>()
            advanceUntilIdle()
            fixture.nextWrite = {
                gate.await()
                throw IOException("previous stroke rejected")
            }
            fixture.stroke()
            runCurrent()
            fixture.vm.beginStroke(NormalizedPatternPoint(0.6f, 0.2f))
            fixture.vm.appendStrokePoint(NormalizedPatternPoint(0.8f, 0.5f))
            gate.complete(Unit)
            advanceUntilIdle()
            fixture.vm.commitStroke(0f)
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertEquals(2, fixture.writeAttempts)
            assertEquals(
                0.6f,
                (
                    fixture.rows.value
                        .single()
                        .payload as FreehandPayload
                ).points.first().x,
            )
            assertTrue(fixture.vm.uiState.value.canUndo)
            fixture.vm.dismissWriteError()
            advanceUntilIdle()
            assertEquals(PatternAnnotationWriteError.NONE, fixture.vm.uiState.value.writeError)
            assertEquals(1, fixture.rows.value.size)
        }

    @Test
    fun `update failure preserves observed content and selection until deliberate recovery`() =
        verifyMutationFailure { it.moveSelected(0.1f, 0f) }

    @Test
    fun `delete failure preserves observed content and selection until deliberate recovery`() =
        verifyMutationFailure { it.deleteSelected() }

    @Test
    fun `clear failure preserves observed content and selection until deliberate recovery`() =
        verifyMutationFailure { it.clearEditablePage() }

    private fun verifyMutationFailure(action: (PatternAnnotationViewModel) -> Unit) =
        runTest {
            val fixture = WriteFixture()
            fixture.seedShape()
            advanceUntilIdle()
            fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.3f, 0.3f))
            runCurrent()
            val before = fixture.rows.value
            val selected = fixture.vm.uiState.value.selectedAnnotationId
            fixture.nextWrite = { throw IOException("mutation rejected") }
            action(fixture.vm)
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertEquals(before, fixture.rows.value)
            assertEquals(before, fixture.vm.uiState.value.projectAnnotations)
            assertEquals(selected, fixture.vm.uiState.value.selectedAnnotationId)
            assertFalse(fixture.vm.uiState.value.canUndo)
            fixture.vm.dismissWriteError()
            action(fixture.vm)
            advanceUntilIdle()
            assertFalse(before == fixture.rows.value)
            assertTrue(fixture.vm.uiState.value.canUndo)
            fixture.vm.undo()
            advanceUntilIdle()
            assertEquals(before, fixture.rows.value)
        }

    @Test
    fun `undo and redo failures keep their history entries and recover only on explicit action`() =
        runTest {
            val fixture = WriteFixture()
            advanceUntilIdle()
            fixture.vm.addCallout("Note", "Details", PatternCalloutSymbol.NOTE)
            advanceUntilIdle()
            val committed = fixture.rows.value
            fixture.nextWrite = { throw IOException("undo rejected") }
            fixture.vm.undo()
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertEquals(committed, fixture.rows.value)
            assertTrue(fixture.vm.uiState.value.canUndo)
            assertFalse(fixture.vm.uiState.value.canRedo)
            fixture.vm.dismissWriteError()
            runCurrent()
            assertEquals(2, fixture.writeAttempts)
            fixture.vm.undo()
            advanceUntilIdle()
            assertTrue(fixture.rows.value.isEmpty())
            assertFalse(fixture.vm.uiState.value.canUndo)
            assertTrue(fixture.vm.uiState.value.canRedo)
            fixture.nextWrite = { throw IOException("redo rejected") }
            fixture.vm.redo()
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertTrue(fixture.rows.value.isEmpty())
            assertFalse(fixture.vm.uiState.value.canUndo)
            assertTrue(fixture.vm.uiState.value.canRedo)
            fixture.vm.dismissWriteError()
            fixture.vm.redo()
            advanceUntilIdle()
            assertEquals(committed, fixture.rows.value)
            assertTrue(fixture.vm.uiState.value.canUndo)
            assertFalse(fixture.vm.uiState.value.canRedo)
            assertEquals(PatternAnnotationWriteError.NONE, fixture.vm.uiState.value.writeError)
        }

    @Test
    fun `existing text failure retains editor identity and supports retry or cancel without replay`() =
        runTest {
            val fixture = WriteFixture(library = true)
            advanceUntilIdle()
            fixture.vm.addTextBox("Original")
            advanceUntilIdle()
            fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.3f, 0.3f))
            fixture.vm.editSelectedText()
            runCurrent()
            val edit = checkNotNull(fixture.vm.uiState.value.textEdit)
            val committed = fixture.rows.value
            fixture.nextWrite = { throw IOException("transient write failure") }
            fixture.vm.saveTextEdit(edit, "Draft")
            advanceUntilIdle()
            assertFailed(fixture.vm)
            assertTrue(edit === fixture.vm.uiState.value.textEdit)
            assertEquals(committed, fixture.rows.value)
            fixture.vm.saveTextEdit(edit, "Draft")
            advanceUntilIdle()
            assertEquals(
                "Draft",
                (
                    fixture.rows.value
                        .single()
                        .payload as TextBoxPayload
                ).text,
            )
            assertEquals(null, fixture.vm.uiState.value.textEdit)
            fixture.vm.editSelectedText()
            runCurrent()
            val nextEdit = checkNotNull(fixture.vm.uiState.value.textEdit)
            fixture.nextWrite = { throw IOException("text rejected") }
            fixture.vm.saveTextEdit(nextEdit, "Abandoned")
            advanceUntilIdle()
            fixture.vm.cancelTextEdit(nextEdit)
            fixture.vm.saveTextEdit(nextEdit, "Obsolete callback")
            advanceUntilIdle()
            assertEquals(4, fixture.writeAttempts)
            assertEquals(PatternAnnotationWriteError.NONE, fixture.vm.uiState.value.writeError)
            assertEquals(
                "Draft",
                (
                    fixture.rows.value
                        .single()
                        .payload as TextBoxPayload
                ).text,
            )
        }

    @Test
    fun `late insert success and failure cannot contaminate B`() = verifyLateResults(PendingAction.INSERT, false)

    @Test
    fun `late insert success and failure cannot contaminate returned A`() =
        verifyLateResults(PendingAction.INSERT, true)

    @Test
    fun `late undo success and failure cannot contaminate B`() = verifyLateResults(PendingAction.UNDO, false)

    @Test
    fun `late undo success and failure cannot contaminate returned A`() = verifyLateResults(PendingAction.UNDO, true)

    @Test
    fun `late redo success and failure cannot contaminate B`() = verifyLateResults(PendingAction.REDO, false)

    @Test
    fun `late redo success and failure cannot contaminate returned A`() = verifyLateResults(PendingAction.REDO, true)

    private fun verifyLateResults(
        action: PendingAction,
        returnToA: Boolean,
    ) = runTest {
        for (fails in listOf(false, true)) {
            val fixture = WriteFixture()
            val gate = CompletableDeferred<Unit>()
            advanceUntilIdle()
            if (action != PendingAction.INSERT) {
                fixture.vm.addTextBox("A")
                advanceUntilIdle()
                if (action == PendingAction.REDO) {
                    fixture.vm.undo()
                    advanceUntilIdle()
                }
            }
            fixture.nextWrite = {
                gate.await()
                if (fails) throw IOException("old context rejected")
            }
            when (action) {
                PendingAction.INSERT -> fixture.vm.addTextBox("Delayed A")
                PendingAction.UNDO -> fixture.vm.undo()
                PendingAction.REDO -> fixture.vm.redo()
            }
            runCurrent()
            fixture.switchToB()
            runCurrent()
            if (returnToA) {
                fixture.route.projectLayers.value = listOf(fixture.route.projectLayer)
                runCurrent()
            }
            assertEquals(if (returnToA) 41L else 42L, fixture.vm.uiState.value.editableLayerId)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(PatternAnnotationWriteError.NONE, fixture.vm.uiState.value.writeError)
            assertFalse(fixture.vm.uiState.value.canUndo)
            assertFalse(fixture.vm.uiState.value.canRedo)
            assertFalse(fixture.vm.uiState.value.isSaving)
            val afterOldResult = fixture.rows.value
            fixture.vm.undo()
            fixture.vm.redo()
            advanceUntilIdle()
            assertEquals(afterOldResult, fixture.rows.value)
            fixture.stroke()
            advanceUntilIdle()
            assertTrue(fixture.vm.uiState.value.canUndo)
            fixture.vm.undo()
            advanceUntilIdle()
            assertEquals(afterOldResult, fixture.rows.value)
        }
    }

    @Test
    fun `mutation and redo cancellation preserve rows and history without ordinary feedback`() =
        runTest {
            val actions: List<(PatternAnnotationViewModel) -> Unit> =
                listOf({ it.moveSelected(0.1f, 0f) }, { it.clearEditablePage() }, { it.redo() })
            for ((index, action) in actions.withIndex()) {
                val fixture = WriteFixture()
                fixture.seedShape()
                advanceUntilIdle()
                fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.3f, 0.3f))
                if (index == 2) {
                    fixture.vm.deleteSelected()
                    advanceUntilIdle()
                    fixture.vm.undo()
                }
                advanceUntilIdle()
                val before = fixture.rows.value
                val state = fixture.vm.uiState.value
                fixture.nextWrite = { throw CancellationException("cancelled") }
                action(fixture.vm)
                advanceUntilIdle()
                assertEquals(before, fixture.rows.value)
                assertEquals(state.canUndo, fixture.vm.uiState.value.canUndo)
                assertEquals(state.canRedo, fixture.vm.uiState.value.canRedo)
                assertEquals(PatternAnnotationWriteError.NONE, fixture.vm.uiState.value.writeError)
                assertFalse(fixture.vm.uiState.value.isSaving)
            }
        }

    private fun assertFailed(vm: PatternAnnotationViewModel) {
        assertEquals(PatternAnnotationWriteError.WRITE_FAILED, vm.uiState.value.writeError)
        assertFalse(vm.uiState.value.isSaving)
    }

    private enum class PendingAction { INSERT, UNDO, REDO }

    private class WriteFixture(
        library: Boolean = false,
    ) {
        val route = projectRoute()
        val rows = MutableStateFlow<List<PatternAnnotation>>(emptyList())
        var nextWrite: suspend () -> Unit = {}
        var writeAttempts = 0
        private var nextId = 100L
        val vm: PatternAnnotationViewModel

        init {
            val repository = route.annotationRepository
            every { repository.observePage(any(), any()) } answers {
                val layerId = firstArg<Long>()
                val page = secondArg<Int>()
                rows.map { current -> current.filter { it.layerId == layerId && it.page == page } }
            }
            coEvery { repository.insertAnnotation(any()) } coAnswers {
                beforeWrite()
                val inserted = firstArg<PatternAnnotation>().copy(id = ++nextId)
                rows.value += inserted
                inserted.id
            }
            coEvery { repository.updateAnnotation(any()) } coAnswers {
                beforeWrite()
                val after = firstArg<PatternAnnotation>()
                rows.value = rows.value.map { if (it.id == after.id) after else it }
            }
            coEvery { repository.deleteAnnotation(any()) } coAnswers {
                beforeWrite()
                rows.value = rows.value.filterNot { it.id == firstArg<Long>() }
            }
            coEvery { repository.clearPage(any(), any()) } coAnswers {
                beforeWrite()
                val (removed, remaining) =
                    rows.value.partition {
                        it.layerId == firstArg<Long>() &&
                            it.page == secondArg<Int>()
                    }
                rows.value = remaining
                removed
            }
            coEvery { repository.restoreBatch(any()) } coAnswers {
                beforeWrite()
                val restored = firstArg<List<PatternAnnotation>>()
                rows.value = rows.value.filterNot { row -> restored.any { it.id == row.id } } + restored
            }
            coEvery { repository.updateText(any(), any(), any()) } coAnswers {
                beforeWrite()
                val before = secondArg<PatternAnnotation>()
                val after = before.copy(payload = (before.payload as TextBoxPayload).copy(text = thirdArg()))
                rows.value = rows.value.map { if (it.id == after.id) after else it }
                after
            }
            vm =
                if (library) {
                    PatternAnnotationViewModel(
                        SavedStateHandle(mapOf("savedPatternId" to 12L)),
                        route.counterRepository,
                        route.layerRepository,
                        repository,
                        route.projectDocumentRepository,
                    )
                } else {
                    route.viewModel()
                }
        }

        private suspend fun beforeWrite() {
            writeAttempts++
            val pending = nextWrite
            nextWrite = {}
            pending()
        }

        fun seedShape() {
            rows.value =
                listOf(
                    PatternAnnotation(
                        id = 99L,
                        layerId = 41L,
                        page = 0,
                        kind = PatternAnnotationKind.LINE,
                        payload =
                            ShapePayload(
                                NormalizedPatternPoint(0.2f, 0.2f),
                                NormalizedPatternPoint(0.4f, 0.4f),
                                0,
                                2f,
                            ),
                        zIndex = 0L,
                    ),
                )
        }

        fun stroke(endX: Float = 0.4f) {
            vm.setActiveTool(PatternAnnotationTool.PEN)
            vm.beginStroke(NormalizedPatternPoint(0.1f, 0.2f))
            vm.appendStrokePoint(NormalizedPatternPoint(endX, 0.5f))
            vm.commitStroke(0f)
        }

        fun switchToB() {
            val key = PatternAnnotationDocumentKey.savedPattern(13L)
            route.documents.value += projectDocument(id = 52L, savedPatternId = 13L, isPrimary = false)
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, key) } returns
                layer(32L, PatternAnnotationOwner.SavedPattern(13L, key))
            route.projectLayers.value = listOf(layer(42L, PatternAnnotationOwner.Project(7L, key)))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationExportOwnershipTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `project request keeps ordered layers but reads persisted content again after picker`() =
        runTest {
            val fixture = ExportFixture(this)
            fixture.rows = listOf(annotation(31L, 0), annotation(41L, 2))
            val request = fixture.ready()
            fixture.vm.setMasterLayerVisible(false)
            fixture.vm.setProjectLayerVisible(false)
            fixture.vm.setActiveTool(PatternAnnotationTool.PEN)
            fixture.vm.beginStroke(NormalizedPatternPoint(0.1f, 0.1f))
            fixture.vm.appendStrokePoint(NormalizedPatternPoint(0.8f, 0.8f))
            fixture.rows = listOf(annotation(31L, 1), annotation(41L, 3))
            val persisted = fixture.rows
            val gate = CompletableDeferred<Unit>()
            fixture.onExport = { gate.await() }

            fixture.deliver(request)
            runCurrent()
            fixture.rows = emptyList()
            assertEquals(listOf(listOf(31L, 41L), listOf(31L, 41L)), fixture.queries)
            assertEquals(persisted, fixture.exports.single().annotations)
            assertEquals(fixture.context.sourceUri, fixture.exports.single().source)
            assertTrue(fixture.vm.uiState.value.isExporting)
            coVerify(exactly = 0) { fixture.route.annotationRepository.insertAnnotation(any()) }
            gate.complete(Unit)
            runCurrent()
            assertFalse(fixture.vm.uiState.value.isExporting)
            assertFalse(fixture.vm.uiState.value.exportFailed)
        }

    @Test
    fun `Library request exports only its captured master and supports source only export`() =
        runTest {
            val fixture = ExportFixture(this, library = true)
            fixture.rows = listOf(annotation(31L, 0), annotation(41L, 0))
            val first = fixture.ready()
            fixture.vm.setMasterLayerVisible(false)
            fixture.deliver(first)
            runCurrent()
            assertEquals(
                listOf(annotation(31L, 0).layerId),
                fixture.exports
                    .single()
                    .annotations
                    .map { it.layerId },
            )
            assertEquals(listOf(listOf(31L), listOf(31L)), fixture.queries)

            val second = fixture.ready()
            fixture.vm.setMasterLayerVisible(true)
            fixture.deliver(second)
            runCurrent()
            assertTrue(
                fixture.exports
                    .last()
                    .annotations
                    .isEmpty(),
            )
            assertEquals(listOf(emptyList<Long>(), emptyList<Long>()), fixture.queries.takeLast(2))
        }

    @Test
    fun `project hidden master remains excluded when toggled during preflight`() =
        runTest {
            val fixture = ExportFixture(this)
            val gate = CompletableDeferred<Unit>()
            fixture.vm.setMasterLayerVisible(false)
            fixture.onPreflight = { gate.await() }
            fixture.request()
            runCurrent()
            fixture.vm.setMasterLayerVisible(true)
            gate.complete(Unit)
            runCurrent()
            val request = fixture.requests.single()
            assertTrue(fixture.vm.launchExportDestination(request, fixture.context))
            fixture.deliver(request)
            runCurrent()
            assertEquals(listOf(listOf(41L), listOf(41L)), fixture.queries)
        }

    @Test
    fun `cancelled destination is consumed and deliberate reentry works`() =
        runTest {
            val fixture = ExportFixture(this)
            val request = fixture.ready()
            fixture.deliver(request, destination = null)
            fixture.deliver(request)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            assertFalse(fixture.vm.uiState.value.exportFailed)
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `duplicate launch and destination cannot replay or finish a running export`() =
        runTest {
            val fixture = ExportFixture(this)
            val gate = CompletableDeferred<Unit>()
            fixture.onExport = { gate.await() }
            val request = fixture.ready()
            assertFalse(fixture.vm.launchExportDestination(request, fixture.context))
            fixture.deliver(request)
            fixture.deliver(request)
            fixture.deliver(request, destination = null)
            runCurrent()
            assertEquals(1, fixture.exports.size)
            assertTrue(fixture.vm.uiState.value.isExporting)
            gate.complete(Unit)
            runCurrent()
            fixture.deliver(request)
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `unlaunched destination and unknown restored token cannot write`() =
        runTest {
            val fixture = ExportFixture(this)
            fixture.request()
            runCurrent()
            fixture.deliver(fixture.requests.single())
            fixture.deliver("unrestorable-token")
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            assertTrue(fixture.vm.launchExportDestination(fixture.requests.single(), fixture.context))
            fixture.deliver(fixture.requests.single())
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `restored callback in a new ViewModel cannot consume its newer request`() =
        runTest {
            val original = ExportFixture(this, library = true)
            val old = original.ready()
            val restored = ExportFixture(this, library = true)
            val fresh = restored.ready()
            restored.deliver(old)
            restored.deliver(old, destination = null)
            runCurrent()
            assertTrue(restored.exports.isEmpty())
            restored.deliver(fresh)
            runCurrent()
            assertEquals(1, restored.exports.size)
        }

    @Test
    fun `recreated viewer rebinds the same context and completes its existing picker request once`() =
        runTest {
            val fixture = ExportFixture(this)
            val request = fixture.ready()
            fixture.vm.setExportContext(fixture.context)
            fixture.deliver(request)
            runCurrent()
            fixture.deliver(request)
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `leaving viewer declines saved callback even after same context reenters`() =
        runTest {
            val fixture = ExportFixture(this)
            val request = fixture.ready()
            fixture.vm.setExportContext(null)
            fixture.vm.setExportContext(fixture.context)
            fixture.deliver(request)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `pending A callback is rejected on B and after A B A`() =
        runTest {
            val fixture = ExportFixture(this)
            val old = fixture.ready()
            fixture.switchToB()
            fixture.deliver(old)
            runCurrent()
            fixture.switchToA()
            fixture.deliver(old)
            fixture.deliver(old, destination = null)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(listOf(31L, 41L), fixture.queries.last())
        }

    @Test
    fun `same URI with different owner cannot accept old request`() =
        runTest {
            val fixture = ExportFixture(this)
            val old = fixture.ready()
            fixture.switchToB(sameUri = true)
            fixture.deliver(old)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(listOf(32L, 42L), fixture.queries.last())
        }

    @Test
    fun `Library owner replacement with identical source cannot receive previous owners result`() =
        runTest {
            val original = ExportFixture(this, library = true)
            val old = original.ready()
            val replacement = ExportFixture(this, library = true, libraryId = 13L)
            replacement.context = replacement.context.copy(sourceUri = original.source)
            replacement.vm.setExportContext(replacement.context)
            val fresh = replacement.ready()
            replacement.deliver(old)
            replacement.deliver(old, destination = null)
            runCurrent()
            assertTrue(replacement.exports.isEmpty())
            replacement.deliver(fresh)
            runCurrent()
            assertEquals(listOf(31L), replacement.queries.last())
            assertEquals(1, replacement.exports.size)
        }

    @Test
    fun `layer observation invalidates pending export before viewer context updates`() =
        runTest {
            val fixture = ExportFixture(this)
            val old = fixture.ready()
            fixture.switchToB(updateViewer = false)
            fixture.deliver(old)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.switchToA()
            fixture.deliver(old)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `same owner and URI with changed project relation invalidates pending request`() =
        runTest {
            val fixture = ExportFixture(this)
            val old = fixture.ready()
            fixture.route.documents.value =
                listOf(
                    fixture.route.documents.value
                        .single()
                        .copy(id = 99L),
                )
            fixture.context = fixture.context.copy(projectDocumentId = 99L)
            fixture.vm.setExportContext(fixture.context)
            runCurrent()
            fixture.deliver(old)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(1, fixture.exports.size)
        }

    @Test
    fun `source owner or relation mismatch cannot start preflight`() =
        runTest {
            val fixture = ExportFixture(this)
            val wrongSource = mockk<Uri>()
            every { wrongSource.toString() } returns "file:///wrong.pdf"
            val invalidContexts =
                listOf(
                    fixture.context.copy(sourceUri = wrongSource),
                    fixture.context.copy(owner = PatternAnnotationOwner.Project(8L, fixture.context.owner.documentKey)),
                    fixture.context.copy(projectDocumentId = 99L),
                )
            invalidContexts.forEach {
                fixture.vm.setExportContext(it)
                fixture.vm.requestAnnotatedPdfExport(it)
                runCurrent()
                assertTrue(fixture.queries.isEmpty())
                coVerify(exactly = 0) { fixture.exporter.preflight(any(), any(), any()) }
            }
            runCurrent()
            assertTrue(fixture.queries.isEmpty())
            coVerify(exactly = 0) { fixture.exporter.preflight(any(), any(), any()) }
        }

    @Test
    fun `callback displayed context mismatch consumes request before writing`() =
        runTest {
            val fixture = ExportFixture(this)
            val request = fixture.ready()
            fixture.vm.exportAnnotatedPdf(
                request,
                fixture.context.copy(projectDocumentId = 99L),
                fixture.destination,
                fixture.style,
            )
            fixture.deliver(request)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
        }

    @Test
    fun `queued preflight event cannot launch after A B A`() =
        runTest {
            val fixture = ExportFixture(this)
            fixture.request()
            runCurrent()
            val old = fixture.requests.single()
            fixture.switchToB()
            fixture.switchToA()
            assertFalse(fixture.vm.launchExportDestination(old, fixture.context))
            fixture.deliver(old)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
        }

    @Test
    fun `delayed preflight success cannot emit or clear newer busy state`() = runTest { verifyLatePreflight(false) }

    @Test
    fun `delayed preflight failure cannot emit or clear newer busy state`() = runTest { verifyLatePreflight(true) }

    private fun TestScope.verifyLatePreflight(fails: Boolean) {
        val fixture = ExportFixture(this)
        val oldGate = CompletableDeferred<Unit>()
        fixture.onPreflight = {
            withContext(NonCancellable) {
                oldGate.await()
                if (fails) throw IOException("obsolete preflight")
            }
        }
        fixture.request()
        runCurrent()
        fixture.switchToB()
        fixture.switchToA()
        val freshGate = CompletableDeferred<Unit>()
        fixture.onPreflight = { freshGate.await() }
        fixture.request()
        runCurrent()
        oldGate.complete(Unit)
        runCurrent()
        assertTrue(fixture.requests.isEmpty())
        assertTrue(fixture.vm.uiState.value.isExporting)
        assertFalse(fixture.vm.uiState.value.exportFailed)
        freshGate.complete(Unit)
        runCurrent()
        assertEquals(1, fixture.requests.size)
    }

    @Test
    fun `late progress and completion cannot clear newer export busy state`() = runTest { verifyLateExport(false) }

    @Test
    fun `late progress and failure cannot clear newer export busy state`() = runTest { verifyLateExport(true) }

    private fun TestScope.verifyLateExport(fails: Boolean) {
        val fixture = ExportFixture(this)
        val oldGate = CompletableDeferred<Unit>()
        fixture.onExport = { progress ->
            withContext(NonCancellable) {
                oldGate.await()
                progress(PatternPdfExportProgress(8, 8))
                if (fails) throw IOException("obsolete export")
            }
        }
        val old = fixture.ready()
        fixture.deliver(old)
        runCurrent()
        fixture.switchToB(sameUri = true)
        fixture.switchToA()
        val freshGate = CompletableDeferred<Unit>()
        fixture.onExport = { progress ->
            progress(PatternPdfExportProgress(1, 2))
            freshGate.await()
        }
        val fresh = fixture.ready()
        fixture.deliver(fresh)
        runCurrent()
        fixture.deliver(old, destination = null)
        oldGate.complete(Unit)
        runCurrent()
        assertTrue(fixture.vm.uiState.value.isExporting)
        assertFalse(fixture.vm.uiState.value.exportFailed)
        assertEquals(1, fixture.vm.uiState.value.exportCompletedPages)
        assertEquals(2, fixture.vm.uiState.value.exportTotalPages)
        freshGate.complete(Unit)
        runCurrent()
        assertFalse(fixture.vm.uiState.value.isExporting)
    }

    @Test
    fun `old completion cannot clear a newer failure`() =
        runTest {
            val fixture = ExportFixture(this)
            val gate = CompletableDeferred<Unit>()
            fixture.onExport = { withContext(NonCancellable) { gate.await() } }
            fixture.deliver(fixture.ready())
            runCurrent()
            fixture.switchToB()
            fixture.onExport = { throw IOException("current failure") }
            fixture.deliver(fixture.ready())
            runCurrent()
            assertTrue(fixture.vm.uiState.value.exportFailed)
            gate.complete(Unit)
            runCurrent()
            assertTrue(fixture.vm.uiState.value.exportFailed)
        }

    @Test
    fun `delayed annotation read is cancelled before native export after context changes`() =
        runTest {
            val fixture = ExportFixture(this)
            val request = fixture.ready()
            val gate = CompletableDeferred<Unit>()
            coEvery { fixture.route.annotationRepository.getForLayersForExport(any(), any(), any()) } coAnswers {
                withContext(NonCancellable) { gate.await() }
                emptyList()
            }
            fixture.deliver(request)
            runCurrent()
            fixture.switchToB()
            gate.complete(Unit)
            runCurrent()
            assertTrue(fixture.exports.isEmpty())
            assertFalse(fixture.vm.uiState.value.isExporting)
            assertFalse(fixture.vm.uiState.value.exportFailed)
        }

    @Test
    fun `valid progress stays busy until export returns and late progress is suppressed`() =
        runTest {
            val fixture = ExportFixture(this)
            val gate = CompletableDeferred<Unit>()
            var reportProgress: (PatternPdfExportProgress) -> Unit = {}
            fixture.onExport = { progress ->
                reportProgress = progress
                progress(PatternPdfExportProgress(2, 2))
                gate.await()
            }
            fixture.deliver(fixture.ready())
            runCurrent()
            assertTrue(fixture.vm.uiState.value.isExporting)
            assertEquals(2, fixture.vm.uiState.value.exportCompletedPages)
            gate.complete(Unit)
            runCurrent()
            reportProgress(PatternPdfExportProgress(99, 99))
            runCurrent()
            assertFalse(fixture.vm.uiState.value.isExporting)
            assertEquals(2, fixture.vm.uiState.value.exportCompletedPages)
        }

    @Test
    fun `preflight and export cancellation remain cancelled without error and allow reentry`() =
        runTest {
            val fixture = ExportFixture(this)
            var operation: kotlinx.coroutines.Job? = null
            fixture.onPreflight = {
                operation = kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]
                throw CancellationException("preflight cancelled")
            }
            fixture.request()
            runCurrent()
            assertTrue(operation?.isCancelled == true)
            assertTrue(fixture.requests.isEmpty())
            assertFalse(fixture.vm.uiState.value.exportFailed)
            fixture.onPreflight = {}
            fixture.onExport = {
                operation = kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]
                throw CancellationException("export cancelled")
            }
            fixture.deliver(fixture.ready())
            runCurrent()
            assertTrue(operation?.isCancelled == true)
            assertFalse(fixture.vm.uiState.value.isExporting)
            assertFalse(fixture.vm.uiState.value.exportFailed)
            fixture.onExport = {}
            fixture.deliver(fixture.ready())
            runCurrent()
            assertEquals(2, fixture.exports.size)
        }

    @Test
    fun `preflight and export failures recover only through new menu requests`() =
        runTest {
            val fixture = ExportFixture(this, library = true)
            fixture.onPreflight = { throw IOException("preflight failed") }
            fixture.request()
            runCurrent()
            assertTrue(fixture.requests.isEmpty())
            assertTrue(fixture.vm.uiState.value.exportFailed)
            fixture.onPreflight = {}
            fixture.onExport = { throw IOException("export failed") }
            val failed = fixture.ready()
            assertFalse(fixture.vm.uiState.value.exportFailed)
            fixture.deliver(failed)
            runCurrent()
            assertTrue(fixture.vm.uiState.value.exportFailed)
            fixture.deliver(failed)
            runCurrent()
            assertEquals(1, fixture.exports.size)
            fixture.onExport = {}
            fixture.deliver(fixture.ready())
            runCurrent()
            assertFalse(fixture.vm.uiState.value.exportFailed)
            assertEquals(2, fixture.exports.size)
        }

    @Test
    fun `visible export failure clears on document change`() =
        runTest {
            val fixture = ExportFixture(this)
            fixture.onExport = { throw IOException("export failed") }
            fixture.deliver(fixture.ready())
            runCurrent()
            assertTrue(fixture.vm.uiState.value.exportFailed)
            fixture.switchToB()
            assertFalse(fixture.vm.uiState.value.exportFailed)
        }

    @Test
    fun `export analytics waits for writing and excludes cancelled or failed destinations`() =
        runTest {
            val fixture = ExportFixture(this)
            val cancelled = fixture.ready()
            verify(exactly = 0) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED) }
            fixture.deliver(cancelled, destination = null)
            verify(exactly = 1) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_CANCELLED) }
            verify(exactly = 0) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED) }

            fixture.onExport = { throw IOException("write failed") }
            fixture.deliver(fixture.ready())
            runCurrent()
            verify(exactly = 1) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_FAILED) }
            verify(exactly = 0) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED) }

            val written = CompletableDeferred<Unit>()
            fixture.onExport = { written.await() }
            fixture.deliver(fixture.ready())
            runCurrent()
            verify(exactly = 0) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED) }
            written.complete(Unit)
            runCurrent()
            verify(exactly = 1) { fixture.analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED) }
        }

    private class ExportFixture(
        val scope: TestScope,
        library: Boolean = false,
        libraryId: Long = 12L,
    ) {
        val route = projectRoute()
        val exporter = mockk<PatternPdfExporter>()
        val analytics = mockk<UsageAnalytics>(relaxed = true)
        val source = mockk<Uri>()
        val destination = mockk<Uri>()
        val style = mockk<PatternAnnotationRenderStyle>()
        var context =
            PatternPdfExportContext(
                source,
                if (library) {
                    PatternAnnotationOwner.SavedPattern(libraryId, PatternAnnotationDocumentKey.savedPattern(libraryId))
                } else {
                    route.projectOwner
                },
                if (library) null else 51L,
            )
        var rows = emptyList<PatternAnnotation>()
        val requests = mutableListOf<String>()
        val queries = mutableListOf<List<Long>>()
        val exports = mutableListOf<ExportCall>()
        var onPreflight: suspend () -> Unit = {}
        var onExport: suspend ((PatternPdfExportProgress) -> Unit) -> Unit = {}
        val vm: PatternAnnotationViewModel

        init {
            every { source.toString() } returns "file:///document-51.pdf"
            if (library) {
                val owner = context.owner as PatternAnnotationOwner.SavedPattern
                coEvery { route.layerRepository.getOrCreateMasterLayer(libraryId, owner.documentKey) } returns
                    layer(31L, owner)
            }
            every { route.annotationRepository.observePage(any(), any()) } returns flowOf(emptyList())
            coEvery { route.annotationRepository.getForLayersForExport(any(), any(), any()) } answers {
                val ids = firstArg<List<Long>>()
                queries += ids
                ids.flatMap { id -> rows.filter { it.layerId == id } }
            }
            coEvery { exporter.preflight(any(), any(), any()) } coAnswers {
                onPreflight()
                mockk()
            }
            coEvery { exporter.export(any(), any(), any(), any(), any(), any()) } coAnswers {
                exports += ExportCall(firstArg(), thirdArg())
                onExport(arg(5))
            }
            vm =
                if (library) {
                    PatternAnnotationViewModel(
                        SavedStateHandle(mapOf("savedPatternId" to libraryId)),
                        route.counterRepository,
                        route.layerRepository,
                        route.annotationRepository,
                        route.projectDocumentRepository,
                        pdfExporter = exporter,
                        analytics = analytics,
                    )
                } else {
                    route.viewModel(exporter, analytics)
                }
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
                vm.exportDestinationRequests.collect { requests += it }
            }
            scope.runCurrent()
            vm.setExportContext(context)
        }

        fun request() = vm.requestAnnotatedPdfExport(context)

        fun ready(): String {
            val count = requests.size
            request()
            scope.runCurrent()
            assertEquals(count + 1, requests.size)
            return requests.last().also { assertTrue(vm.launchExportDestination(it, context)) }
        }

        fun deliver(
            id: String,
            destination: Uri? = this.destination,
        ) = vm.exportAnnotatedPdf(id, context, destination, style)

        fun switchToB(
            sameUri: Boolean = false,
            updateViewer: Boolean = true,
        ) {
            val document =
                projectDocument(52L, 13L, false).let {
                    if (sameUri) it.copy(localPdfUri = source.toString()) else it
                }
            val owner = PatternAnnotationOwner.Project(7L, document.documentKey)
            route.documents.value += document
            coEvery { route.layerRepository.getOrCreateMasterLayer(13L, document.documentKey) } returns
                layer(32L, PatternAnnotationOwner.SavedPattern(13L, document.documentKey))
            route.projectLayers.value = listOf(layer(42L, owner))
            val uri =
                if (sameUri) {
                    source
                } else {
                    mockk<Uri>().also { uri ->
                        every { uri.toString() } returns document.localPdfUri
                    }
                }
            if (updateViewer) {
                context = PatternPdfExportContext(uri, owner, document.id)
                vm.setExportContext(context)
            }
            scope.runCurrent()
        }

        fun switchToA() {
            route.projectLayers.value = listOf(route.projectLayer)
            context = PatternPdfExportContext(source, route.projectOwner, 51L)
            vm.setExportContext(context)
            scope.runCurrent()
        }
    }

    private data class ExportCall(
        val source: Uri,
        val annotations: List<PatternAnnotation>,
    )
}

// Project-reitin jaettu alustus: sama mock-kokoonpano toistui kolmessa testissä
private class ProjectRoute(
    val counterRepository: CounterRepository,
    val layerRepository: PatternAnnotationLayerRepository,
    val annotationRepository: PatternAnnotationRepository,
    val projectDocumentRepository: ProjectDocumentRepository,
    val projectOwner: PatternAnnotationOwner.Project,
    val projectLayer: PatternAnnotationLayer,
    val projectLayers: MutableStateFlow<List<PatternAnnotationLayer>>,
    val documents: MutableStateFlow<List<ProjectDocument>>,
    val project: MutableStateFlow<CounterProject>,
) {
    fun viewModel(
        pdfExporter: PatternPdfExporter? = null,
        analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) = PatternAnnotationViewModel(
        SavedStateHandle(mapOf("projectId" to 7L)),
        counterRepository,
        layerRepository,
        annotationRepository,
        projectDocumentRepository,
        pdfExporter = pdfExporter,
        analytics = analytics,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.exportToDestination(
    viewModel: PatternAnnotationViewModel,
    sourceUri: Uri,
    destinationUri: Uri,
    style: PatternAnnotationRenderStyle,
    documentId: Long = 51L,
) {
    every { sourceUri.toString() } returns "file:///document-$documentId.pdf"
    val context = PatternPdfExportContext(sourceUri, viewModel.uiState.value.owner, documentId)
    val requests = mutableListOf<String>()
    val collector =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.exportDestinationRequests.collect { requests += it }
        }
    viewModel.setExportContext(context)
    viewModel.requestAnnotatedPdfExport(context)
    advanceUntilIdle()
    assertTrue(viewModel.launchExportDestination(requests.single(), context))
    viewModel.exportAnnotatedPdf(requests.single(), context, destinationUri, style)
    collector.cancel()
}

private data class PdfExportFixture(
    val route: ProjectRoute = projectRoute(),
    val exporter: PatternPdfExporter = mockk(relaxed = true),
    val sourceUri: Uri = mockk(),
    val destinationUri: Uri = mockk(),
    val style: PatternAnnotationRenderStyle = mockk(),
)

private fun projectRoute(): ProjectRoute {
    val counterRepository = mockk<CounterRepository>()
    val layerRepository = mockk<PatternAnnotationLayerRepository>()
    val annotationRepository = mockk<PatternAnnotationRepository>()
    val projectDocumentRepository = mockk<ProjectDocumentRepository>()
    val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
    val projectOwner = PatternAnnotationOwner.Project(7L, documentKey)
    val projectLayer = layer(id = 41L, owner = projectOwner)
    val projectLayers = MutableStateFlow(listOf(projectLayer))
    val documents = MutableStateFlow(listOf(projectDocument()))
    val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
    val project = MutableStateFlow(CounterProject(id = 7L, linkedPatternId = 12L))

    every { counterRepository.observeProject(7L) } returns project
    coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
    every { layerRepository.observeLayers(any()) } returns projectLayers
    every { projectDocumentRepository.observeDocuments(7L) } returns documents

    return ProjectRoute(
        counterRepository = counterRepository,
        layerRepository = layerRepository,
        annotationRepository = annotationRepository,
        projectDocumentRepository = projectDocumentRepository,
        projectOwner = projectOwner,
        projectLayer = projectLayer,
        projectLayers = projectLayers,
        documents = documents,
        project = project,
    )
}

private fun projectDocument(
    id: Long = 51L,
    savedPatternId: Long = 12L,
    isPrimary: Boolean = true,
) = ProjectDocument(
    id = id,
    projectId = 7L,
    savedPatternId = savedPatternId,
    documentKey = PatternAnnotationDocumentKey.savedPattern(savedPatternId),
    label = "Document $id",
    localPdfUri = "file:///document-$id.pdf",
    sortOrder = if (isPrimary) 0 else 1,
    isPrimary = isPrimary,
    currentPage = 0,
    rowMapping = null,
    readingLineEnabled = false,
    readingLineYFraction = 0.5f,
    readingLineFollowCurrentRow = false,
    verticalReadingGuideEnabled = false,
    verticalReadingGuideXFraction = 0.5f,
    createdAt = 1_000L,
    updatedAt = 1_000L,
)

private fun layer(
    id: Long,
    owner: PatternAnnotationOwner,
) = PatternAnnotationLayer(
    id = id,
    owner = owner,
    isActive = true,
    createdAt = 1_000L,
    updatedAt = 1_000L,
)

private fun annotation(
    layerId: Long,
    page: Int,
    zIndex: Long = 0L,
) = PatternAnnotation(
    id = layerId,
    layerId = layerId,
    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    page = page,
    kind = PatternAnnotationKind.FREEHAND,
    payload =
        FreehandPayload(
            points = listOf(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f)),
            argb = 0xFF000000.toInt(),
            strokeWidth = 2f,
        ),
    zIndex = zIndex,
)

private fun trackerAnnotation(
    id: Long,
    layerId: Long,
    page: Int,
) = PatternAnnotation(
    id = id,
    layerId = layerId,
    page = page,
    kind = PatternAnnotationKind.CHART_TRACKER,
    payload =
        ChartTrackerPayload(
            region =
                ChartRegionPayload(
                    bounds = NormalizedPatternBounds(0.1f, 0.1f, 0.9f, 0.9f),
                    name = "Large chart",
                    rows = 100,
                    columns = 100,
                    rowDirection = ChartRowDirection.TOP_TO_BOTTOM,
                    columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                ),
            trackingMode = ChartTrackingMode.COMPLETED_CELLS,
            counterType = ChartCounterType.MAIN,
            counterStartValue = 0,
            gridStartIndex = 0,
            wrapAtEnd = false,
            highlightArgb = 0xFF00FF00.toInt(),
            highlightAlpha = 0.4f,
        ),
    zIndex = 0L,
)
// CPD-ON
