package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager
    private lateinit var quota: AiQuotaManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        quota = mockk()
        every { proManager.hasFeature(ProFeature.AI_FEATURES) } returns true
        coEvery { quota.hasQuota() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(
        notes: String = "",
        count: Int = 0,
    ): NotesEditorViewModel {
        coEvery { repository.getProject(1L) } returns
            CounterProjectEntity(
                id = 1L,
                name = "Test",
                count = count,
                notes = notes,
            )
        return NotesEditorViewModel(
            repository = repository,
            proManager = proManager,
            aiQuotaManager = quota,
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
        )
    }

    @Test
    fun `appendJournalEntry on empty notes creates entry without separator`() =
        runTest {
            val viewModel = vm(notes = "", count = 5)
            viewModel.appendJournalEntry("Switched to smaller needles.")
            val result = viewModel.uiState.value.notes
            assertTrue(result.contains("Row 5"))
            assertTrue(result.contains("Switched to smaller needles."))
            assertTrue("should not start with separator", !result.startsWith("---"))
        }

    @Test
    fun `appendJournalEntry on existing notes adds separator`() =
        runTest {
            val viewModel = vm(notes = "Previous entry.", count = 42)
            viewModel.appendJournalEntry("New entry.")
            val result = viewModel.uiState.value.notes
            assertTrue(result.startsWith("Previous entry."))
            assertTrue(result.contains("\n\n---\n\n"))
            assertTrue(result.contains("Row 42"))
            assertTrue(result.endsWith("New entry."))
        }

    @Test
    fun `appendJournalEntry with zero row omits Row label`() =
        runTest {
            val viewModel = vm(notes = "", count = 0)
            viewModel.appendJournalEntry("Some text.")
            val result = viewModel.uiState.value.notes
            assertTrue("should not contain 'Row'", !result.contains("Row "))
            assertTrue(result.endsWith("Some text."))
        }

    @Test
    fun `initial state loads project row and Pro flags`() =
        runTest {
            val viewModel = vm(notes = "hello", count = 12)
            val state = viewModel.uiState.value
            assertEquals(12, state.currentRow)
            assertEquals("hello", state.notes)
            assertTrue(state.isPro)
            assertTrue(state.isAiAvailable)
        }
}
