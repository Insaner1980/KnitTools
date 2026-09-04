package com.finnvek.knittools.ui.screens.pattern

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
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
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ShapePayload
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

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
            assertEquals(
                2,
                viewModel.uiState.value.draftStroke
                    ?.points
                    ?.size,
            )
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
            coEvery { route.annotationRepository.getForLayers(any()) } answers {
                val layerIds = firstArg<List<Long>>()
                allAnnotations.filter { it.layerId in layerIds }
            }
            val viewModel = route.viewModel(pdfExporter = exporter)
            advanceUntilIdle()

            route.projectLayers.value = listOf(route.projectLayer.copy(isActive = false), secondaryLayer)
            advanceUntilIdle()
            viewModel.exportAnnotatedPdf(sourceUri, destinationUri, style)
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
    fun viewModel(pdfExporter: PatternPdfExporter? = null) =
        PatternAnnotationViewModel(
            SavedStateHandle(mapOf("projectId" to 7L)),
            counterRepository,
            layerRepository,
            annotationRepository,
            projectDocumentRepository,
            pdfExporter = pdfExporter,
        )
}

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
// CPD-ON
