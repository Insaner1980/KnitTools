package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import com.finnvek.knittools.ai.PatternInstructionGemini
import com.finnvek.knittools.repository.PatternInstructionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternViewerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var instructionRepository: PatternInstructionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        instructionRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PatternViewerViewModel =
        PatternViewerViewModel(
            instructionRepository = instructionRepository,
        )

    @Test
    fun `valid viewer context loads instruction from Gemini`() =
        runTest {
            coEvery { instructionRepository.getInstruction(any(), 8) } returns
                PatternInstructionGemini.InstructionResult("SSK, knit across", 50)
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 8,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )
            advanceUntilIdle()

            val state = viewModel.instructionState.value
            assertEquals("SSK, knit across", state.instruction)
            assertEquals(50, state.positionPercent)
            assertFalse(state.isLoading)
            assertEquals(8, state.rowNumber)
        }

    @Test
    fun `disabled viewer context clears instruction state`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 2,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = false,
            )
            advanceUntilIdle()

            val state = viewModel.instructionState.value
            assertNull(state.instruction)
            assertFalse(state.isLoading)
            assertFalse(state.canDisplayInstruction)
        }

    @Test
    fun `row 0 does not trigger Gemini call`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 0,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )
            advanceUntilIdle()

            val state = viewModel.instructionState.value
            assertNull(state.instruction)
            assertNull(state.positionPercent)
        }

    @Test
    fun `Gemini returning null hides instruction`() =
        runTest {
            coEvery { instructionRepository.getInstruction(any(), 5) } returns null
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 5,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )
            advanceUntilIdle()

            val state = viewModel.instructionState.value
            assertNull(state.instruction)
            assertNull(state.positionPercent)
        }

    @Test
    fun `clearInstructionCaches resets state`() =
        runTest {
            coEvery { instructionRepository.getInstruction(any(), 1) } returns
                PatternInstructionGemini.InstructionResult("Knit", 30)
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 1,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )
            advanceUntilIdle()
            assertEquals("Knit", viewModel.instructionState.value.instruction)

            viewModel.clearInstructionCaches()
            assertNull(viewModel.instructionState.value.instruction)
            assertFalse(viewModel.instructionState.value.isLoading)
        }

    @Test
    fun `instruction explanation is cached by instruction text`() =
        runTest {
            coEvery { instructionRepository.explainInstruction("K2tog, YO") } returns
                "Knit 2 together, then yarn over."
            val viewModel = createViewModel()

            viewModel.onInstructionTapped("K2tog, YO")
            advanceUntilIdle()

            val firstState = viewModel.explanationState.value
            assertEquals("Knit 2 together, then yarn over.", firstState.explanation)
            assertTrue(firstState.isVisible)

            viewModel.onInstructionTapped("K2tog, YO")
            assertFalse(viewModel.explanationState.value.isVisible)

            viewModel.onInstructionTapped("K2tog, YO")
            advanceUntilIdle()

            val secondState = viewModel.explanationState.value
            assertEquals("Knit 2 together, then yarn over.", secondState.explanation)
            assertTrue(secondState.isVisible)
            coVerify(exactly = 1) { instructionRepository.explainInstruction("K2tog, YO") }
        }

    @Test
    fun `row change hides visible explanation`() =
        runTest {
            coEvery { instructionRepository.getInstruction(any(), any()) } returns
                PatternInstructionGemini.InstructionResult("SSK", 42)
            coEvery { instructionRepository.explainInstruction("SSK") } returns
                "Slip, slip, knit for a left-leaning decrease."
            val viewModel = createViewModel()

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 8,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )
            advanceUntilIdle()
            viewModel.onInstructionTapped("SSK")
            advanceUntilIdle()
            assertTrue(viewModel.explanationState.value.isVisible)

            viewModel.onViewerContextChanged(
                patternUri = "pattern://one",
                currentPage = 0,
                currentRow = 9,
                renderedBitmap = mockk<Bitmap>(),
                canDisplayInstruction = true,
            )

            assertFalse(viewModel.explanationState.value.isVisible)
            assertNull(viewModel.explanationState.value.explanation)
        }
}
