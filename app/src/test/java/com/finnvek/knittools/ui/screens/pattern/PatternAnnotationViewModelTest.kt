package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
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
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.repositoryReadRetryDelayMillis
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("projectId" to 7L, "savedPatternId" to 12L)),
                counterRepository,
                layerRepository,
                annotationRepository,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("projectId" to 0L)),
                counterRepository,
                layerRepository,
                annotationRepository,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("savedPatternId" to -1L)),
                counterRepository,
                layerRepository,
                annotationRepository,
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
            // Linkin kadotessa kerrokset kysytään legacy-avaimella, mutta aktiivinen
            // kerros palauttaa omistajalle alkuperäisen dokumenttiavaimen
            val legacyOwner =
                PatternAnnotationOwner.Project(7L, PatternAnnotationDocumentKey.legacyProject(7L))
            every { route.layerRepository.observeLayers(legacyOwner) } returns
                flowOf(listOf(route.projectLayer))
            every { route.annotationRepository.observePage(31L, 0) } returns flowOf(emptyList())
            every { route.annotationRepository.observePage(41L, 0) } returns
                flowOf(listOf(annotation(layerId = 41L, page = 0)))
            val viewModel = route.viewModel()
            advanceUntilIdle()

            route.project.value = route.project.value.copy(linkedPatternId = null)
            advanceUntilIdle()

            verify { route.layerRepository.observeLayers(legacyOwner) }
            assertEquals(route.projectOwner, viewModel.uiState.value.owner)
            assertEquals(41L, viewModel.uiState.value.editableLayerId)
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
            every { layerRepository.observeLayers(projectOwner) } returns flowOf(listOf(projectLayer))
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

    // Project-reitin jaettu alustus: sama mock-kokoonpano toistui kolmessa testissä
    private class ProjectRoute(
        val counterRepository: CounterRepository,
        val layerRepository: PatternAnnotationLayerRepository,
        val annotationRepository: PatternAnnotationRepository,
        val projectOwner: PatternAnnotationOwner.Project,
        val projectLayer: PatternAnnotationLayer,
        val project: MutableStateFlow<CounterProject>,
    ) {
        fun viewModel() =
            PatternAnnotationViewModel(
                SavedStateHandle(mapOf("projectId" to 7L)),
                counterRepository,
                layerRepository,
                annotationRepository,
            )
    }

    private fun projectRoute(): ProjectRoute {
        val counterRepository = mockk<CounterRepository>()
        val layerRepository = mockk<PatternAnnotationLayerRepository>()
        val annotationRepository = mockk<PatternAnnotationRepository>()
        val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
        val projectOwner = PatternAnnotationOwner.Project(7L, documentKey)
        val projectLayer = layer(id = 41L, owner = projectOwner)
        val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
        val project = MutableStateFlow(CounterProject(id = 7L, linkedPatternId = 12L))

        every { counterRepository.observeProject(7L) } returns project
        coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
        every { layerRepository.observeLayers(projectOwner) } returns flowOf(listOf(projectLayer))

        return ProjectRoute(
            counterRepository = counterRepository,
            layerRepository = layerRepository,
            annotationRepository = annotationRepository,
            projectOwner = projectOwner,
            projectLayer = projectLayer,
            project = project,
        )
    }

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
}
// CPD-ON
