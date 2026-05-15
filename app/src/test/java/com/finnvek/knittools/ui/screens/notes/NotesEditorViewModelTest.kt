package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager
    private lateinit var quota: AiQuotaManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        quota = mockk()
        every { proManager.hasFeature(ProFeature.NOTES) } returns true
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
        val project =
            CounterProject(
                id = 1L,
                name = "Test",
                count = count,
                notes = notes,
            )
        coEvery { repository.observeProject(1L) } returns flowOf(project)
        coEvery { repository.saveProjectNotes(1L, any(), any()) } answers {
            project.copy(notes = arg(2))
        }
        return NotesEditorViewModel(
            repository = repository,
            proManager = proManager,
            aiQuotaManager = quota,
            ioDispatcher = testDispatcher,
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
        )
    }

    @Test
    fun `appendJournalEntry on empty notes creates entry without separator`() =
        runTest {
            val viewModel = vm(notes = "", count = 5)
            advanceUntilIdle()
            viewModel.appendJournalEntry("Switched to smaller needles.")
            val result = viewModel.uiState.value.notes
            assertTrue(result.contains("Row 5"))
            assertTrue(result.contains("Switched to smaller needles."))
            assertTrue("should not start with separator", !result.startsWith("---"))
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `appendJournalEntry on existing notes adds separator`() =
        runTest {
            val viewModel = vm(notes = "Previous entry.", count = 42)
            advanceUntilIdle()
            viewModel.appendJournalEntry("New entry.")
            val result = viewModel.uiState.value.notes
            assertTrue(result.startsWith("Previous entry."))
            assertTrue(result.contains("\n\n---\n\n"))
            assertTrue(result.contains("Row 42"))
            assertTrue(result.endsWith("New entry."))
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `appendJournalEntry with zero row omits Row label`() =
        runTest {
            val viewModel = vm(notes = "", count = 0)
            advanceUntilIdle()
            viewModel.appendJournalEntry("Some text.")
            val result = viewModel.uiState.value.notes
            assertTrue("should not contain 'Row'", !result.contains("Row "))
            assertTrue(result.endsWith("Some text."))
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `initial state loads project row and Pro flags`() =
        runTest {
            val viewModel = vm(notes = "hello", count = 12)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertEquals(12, state.currentRow)
            assertEquals("hello", state.notes)
            assertTrue(state.isPro)
            assertTrue(state.isAiAvailable)
        }

    @Test
    fun `missing project marks editor for fallback and skips saves`() =
        runTest {
            coEvery { repository.observeProject(1L) } returns flowOf(null)
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    aiQuotaManager = quota,
                    ioDispatcher = testDispatcher,
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
                )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isMissingProject)

            viewModel.onNotesChanged("Should not persist")
            viewModel.saveImmediately()

            coVerify(exactly = 0) { repository.updateProjectNotes(any(), any()) }
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }
        }

    @Test
    fun `observed row updates while local note edits stay intact`() =
        runTest {
            val emitExternal = CompletableDeferred<Unit>()
            coEvery { repository.observeProject(1L) } returns
                flow {
                    emit(CounterProject(id = 1L, name = "Test", count = 3, notes = "Base"))
                    emitExternal.await()
                    emit(CounterProject(id = 1L, name = "Test", count = 9, notes = "External edit"))
                }
            coEvery { repository.saveProjectNotes(1L, any(), any()) } answers {
                CounterProject(id = 1L, name = "Test", count = 9, notes = arg(2))
            }
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    aiQuotaManager = quota,
                    ioDispatcher = testDispatcher,
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
                )
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            emitExternal.complete(Unit)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals("Local edit", state.notes)
            assertEquals(9, state.currentRow)
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `saveImmediately waits for repository save before callback`() =
        runTest {
            val events = mutableListOf<String>()
            val viewModel = vm(notes = "Base", count = 4)
            advanceUntilIdle()
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit")
            } coAnswers {
                events += "saved"
                CounterProject(id = 1L, name = "Test", count = 4, notes = "Local edit")
            }

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately { events += "callback" }
            advanceUntilIdle()

            assertEquals(listOf("saved", "callback"), events)
        }

    @Test
    fun `debounced save uses original persisted notes as merge base`() =
        runTest {
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit")
            } returns CounterProject(id = 1L, name = "Test", count = 4, notes = "Local edit")
            val viewModel = vm(notes = "Base", count = 4)
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            advanceTimeBy(1_000)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit") }
        }

    @Test
    fun `non-pro editor does not persist note changes`() =
        runTest {
            every { proManager.hasFeature(ProFeature.NOTES) } returns false
            every { proManager.hasFeature(ProFeature.AI_FEATURES) } returns false
            val viewModel = vm(notes = "Base", count = 4)
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately()
            advanceUntilIdle()

            assertEquals("Base", viewModel.uiState.value.notes)
            coVerify(exactly = 0) { repository.updateProjectNotes(any(), any()) }
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }
        }
}
