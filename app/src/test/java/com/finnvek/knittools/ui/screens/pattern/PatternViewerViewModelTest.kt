package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.graphics.Bitmap
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
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
    private lateinit var geminiAiService: GeminiAiService
    private lateinit var aiQuotaManager: AiQuotaManager
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        geminiAiService = mockk(relaxed = true)
        aiQuotaManager = mockk(relaxed = true)
        coEvery { aiQuotaManager.hasQuota() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PatternViewerViewModel =
        PatternViewerViewModel(
            context = context,
            geminiAiService = geminiAiService,
            aiQuotaManager = aiQuotaManager,
        )

    @Test
    fun `valid viewer context loads instruction from Gemini`() =
        runTest {
            coEvery { geminiAiService.generateFromImage(any(), any()) } returns
                """{"instruction": "SSK, knit across", "positionPercent": 50}"""
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
            coEvery { geminiAiService.generateFromImage(any(), any()) } returns null
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
            coEvery { geminiAiService.generateFromImage(any(), any()) } returns
                """{"instruction": "Knit", "positionPercent": 30}"""
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
            coEvery { geminiAiService.explainInstruction("K2tog, YO") } returns
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
            coVerify(exactly = 1) { geminiAiService.explainInstruction("K2tog, YO") }
        }

    @Test
    fun `row change hides visible explanation`() =
        runTest {
            coEvery { geminiAiService.generateFromImage(any(), any()) } returns
                """{"instruction": "SSK", "positionPercent": 42}"""
            coEvery { geminiAiService.explainInstruction("SSK") } returns
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
