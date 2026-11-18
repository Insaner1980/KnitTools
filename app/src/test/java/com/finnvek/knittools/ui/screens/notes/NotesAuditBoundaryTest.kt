package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.mergeProjectNotes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Scheduler-controlled UI boundaries; transaction rollback is covered by the Room matrix. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class NotesAuditBoundaryTest(
    private val prompt: Int,
) {
    private val dispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val store = ViewModelStore()
    private val project = MutableStateFlow(CounterProject(id = 1, name = "Owner", notes = "Base", notesCreated = true))
    private val handle = SavedStateHandle(mapOf("projectId" to 1L))
    private lateinit var model: NotesEditorViewModel
    private var beforeSave: CompletableDeferred<Unit>? = null
    private var afterSave: CompletableDeferred<Unit>? = null
    private var fail = false
    private var attempts = 0
    private val commits = mutableListOf<String>()
    private var callbacks = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val repository = mockk<CounterRepository>()
        every { repository.observeProject(1) } returns project
        coEvery { repository.saveProjectNotes(any(), any(), any(), any()) } coAnswers {
            assertEquals(1L, arg<Long>(0))
            attempts++
            val before = beforeSave.also { beforeSave = null }
            val after = afterSave.also { afterSave = null }
            before?.await()
            if (fail) {
                ProjectNotesSaveResult.PersistenceFailure
            } else {
                val notes = mergeProjectNotes(arg(1), arg(2), project.value.notes)
                project.value = project.value.copy(notes = notes)
                commits.add(notes)
                after?.await()
                ProjectNotesSaveResult.Saved(project.value)
            }
        }
        val pro = mockk<ProManager>()
        every { pro.proState } returns MutableStateFlow(ProState())
        every { pro.hasFeature(any()) } returns true
        model = NotesEditorViewModel(repository, pro, applicationScope, handle, mockk())
        store.put("owner", model)
    }

    @After
    fun tearDown() {
        store.clear()
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun authoredNotesBoundary() =
        runTest(dispatcher) {
            runCurrent()
            when (prompt) {
                915 -> mergeConcurrentEditorChanges()
                916, 920 -> retryFailedSave()
                917, 919, 921 -> persistNewestDraft()
                918, 922 -> preserveTypingAcrossCancelledSave()
                923, 924, 925 -> discardWithoutSaving()
                926 -> discardAroundCommit()
                927 -> flushOnClearDuringSave()
                928 -> retainDraftAfterFailedFlush()
                929 -> mergeOnceOnRepeatedClear()
                930 -> cancelPendingFlush()
                else -> error("Unmapped prompt $prompt")
            }
        }

    private fun immediate() = model.saveImmediately { callbacks++ }

    private fun draft() = model.onNotesChanged("Draft")

    private fun TestScope.mergeConcurrentEditorChanges() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, attempts)
        project.value = project.value.copy(notes = "Other editor")
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(project.value.notes.contains("Other editor"))
        assertTrue(project.value.notes.contains("Draft"))
        assertEquals(project.value.notes, model.uiState.value.notes)
    }

    private fun TestScope.retryFailedSave() {
        fail = true
        draft()
        if (prompt == 920) immediate()
        advanceUntilIdle()
        assertTrue(model.uiState.value.saveFailed)
        assertEquals("Draft", handle.get<String>("notesDraft"))
        assertEquals("Base", project.value.notes)
        assertEquals(0, callbacks)
        fail = false
        immediate()
        advanceUntilIdle()
        assertEquals(listOf("Draft"), commits)
        assertEquals(1, callbacks)
    }

    private fun TestScope.persistNewestDraft() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        if (prompt == 921) immediate() else advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, attempts)
        model.onNotesChanged("Newest")
        if (prompt != 917) immediate()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("Newest"), commits)
        assertEquals("Newest", model.uiState.value.notes)
        assertEquals(if (prompt == 917) 0 else 1, callbacks)
        assertNull(handle.get<String>("notesDraft"))
    }

    private fun TestScope.preserveTypingAcrossCancelledSave() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        if (prompt == 922) immediate() else advanceTimeBy(1000)
        runCurrent()
        model.onNotesChanged("Base")
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(commits.isEmpty())
        val late = CompletableDeferred<Unit>()
        afterSave = late
        draft()
        if (prompt == 922) immediate() else advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("Draft"), commits)
        model.onNotesChanged("Newest")
        late.complete(Unit)
        runCurrent()
        assertEquals("Newest", model.uiState.value.notes)
        advanceUntilIdle()
        // The cancelled acknowledgement leaves Base as the editor base. Preserve both
        // the durable unacknowledged block and newer typing under the repository merge contract.
        assertEquals("Newest\n\n---\n\nDraft", project.value.notes)
        assertEquals(listOf("Draft", "Newest\n\n---\n\nDraft"), commits)
        assertFalse(model.uiState.value.saveFailed)
    }

    private fun TestScope.discardWithoutSaving() {
        draft()
        project.value = project.value.copy(notes = "Other editor")
        runCurrent()
        model.discardChanges()
        if (prompt == 925) model.discardChanges()
        store.clear()
        advanceUntilIdle()
        assertEquals("Other editor", project.value.notes)
        assertEquals(0, attempts)
        assertNull(handle.get<String>("notesDraft"))
        assertNull(handle.get<String>("notesDraftBase"))
    }

    private fun TestScope.discardAroundCommit() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        immediate()
        runCurrent()
        model.discardChanges()
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(commits.isEmpty())
        val late = CompletableDeferred<Unit>()
        afterSave = late
        draft()
        immediate()
        runCurrent()
        assertEquals("Draft", project.value.notes)
        model.discardChanges()
        late.complete(Unit)
        store.clear()
        advanceUntilIdle()
        assertEquals(listOf("Draft"), commits)
        assertEquals("Draft", project.value.notes)
    }

    private fun TestScope.flushOnClearDuringSave() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        immediate()
        runCurrent()
        assertEquals(1, attempts)
        store.clear()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, attempts)
        assertEquals(listOf("Draft"), commits)
        assertEquals(0, callbacks)
    }

    private fun TestScope.retainDraftAfterFailedFlush() {
        fail = true
        draft()
        store.clear()
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertTrue(commits.isEmpty())
        assertEquals("Base", project.value.notes)
        assertEquals("Draft", handle.get<String>("notesDraft"))
    }

    private fun TestScope.mergeOnceOnRepeatedClear() {
        draft()
        project.value = project.value.copy(notes = "Other editor")
        store.clear()
        store.clear()
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertEquals(1, commits.size)
        assertTrue(project.value.notes.contains("Other editor"))
        assertTrue(project.value.notes.contains("Draft"))
    }

    private fun TestScope.cancelPendingFlush() {
        val gate = CompletableDeferred<Unit>()
        beforeSave = gate
        draft()
        store.clear()
        runCurrent()
        val flush = applicationScope.coroutineContext[Job]!!.children.single()
        flush.cancel()
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(flush.isCancelled)
        assertTrue(commits.isEmpty())
        assertEquals("Base", project.value.notes)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = (915..930).map { arrayOf(it) }
    }
}
