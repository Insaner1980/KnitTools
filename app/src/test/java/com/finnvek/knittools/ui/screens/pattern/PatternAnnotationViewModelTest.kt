package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    @Test
    fun `library route edits only master layer and follows current page`() =
        runTest {
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
            every { annotationRepository.observePage(31L, any()) } answers {
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

            viewModel.setCurrentPage(3)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.currentPage)
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
            val counterRepository = mockk<CounterRepository>()
            val layerRepository = mockk<PatternAnnotationLayerRepository>()
            val annotationRepository = mockk<PatternAnnotationRepository>()
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val projectOwner = PatternAnnotationOwner.Project(7L, documentKey)
            val projectLayer = layer(id = 41L, owner = projectOwner)
            val masterLayer = layer(id = 31L, owner = PatternAnnotationOwner.SavedPattern(12L, documentKey))
            every { counterRepository.observeProject(7L) } returns
                flowOf(CounterProject(id = 7L, linkedPatternId = 12L))
            every { layerRepository.observeLayers(projectOwner) } returns flowOf(listOf(projectLayer))
            coEvery { layerRepository.getOrCreateMasterLayer(12L, documentKey) } returns masterLayer
            every { annotationRepository.observePage(31L, 0) } returns flowOf(listOf(annotation(31L, 0)))
            every { annotationRepository.observePage(41L, 0) } returns flowOf(listOf(annotation(41L, 0)))

            val viewModel =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("projectId" to 7L)),
                    counterRepository,
                    layerRepository,
                    annotationRepository,
                )
            advanceUntilIdle()

            assertEquals(projectOwner, viewModel.uiState.value.owner)
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
            advanceTimeBy(PatternAnnotationViewModel.RETRY_DELAY_MS)
            advanceUntilIdle()

            assertEquals(PatternAnnotationLoadError.NONE, viewModel.uiState.value.loadError)
            assertEquals(31L, viewModel.uiState.value.editableLayerId)
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
    ) = PatternAnnotation(
        id = layerId,
        layerId = layerId,
        page = page,
        kind = PatternAnnotationKind.FREEHAND,
        payload =
            FreehandPayload(
                points = listOf(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f)),
                argb = 0xFF000000.toInt(),
                strokeWidth = 2f,
            ),
        zIndex = 0L,
    )
}
