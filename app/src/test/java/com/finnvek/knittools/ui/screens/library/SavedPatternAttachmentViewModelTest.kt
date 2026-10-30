package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPatternAttachmentViewModelTest {
    @Test
    fun `fresh attachment owner allows explicit project reselection and retry`() {
        val viewModel = SavedPatternAttachmentViewModel()
        assertEquals(SavedPatternAttachmentState(), viewModel.state.value)
        lateinit var complete: (SavedPatternMetadataMutationResult) -> Unit
        val expectedIds = mutableListOf<Long?>()

        viewModel.attach(42L, null) { expectedId, callback ->
            expectedIds += expectedId
            complete = callback
        }
        assertEquals(42L, viewModel.state.value.projectId)
        assertTrue(viewModel.state.value.inFlight)
        complete(SavedPatternMetadataMutationResult.PersistenceFailure)
        viewModel.consumeResult()

        viewModel.attach(43L, null) { expectedId, callback ->
            expectedIds += expectedId
            callback(SavedPatternMetadataMutationResult.ReplacementRequired(99L))
        }
        assertEquals(listOf<Long?>(null, null), expectedIds)
        assertEquals(43L, viewModel.state.value.projectId)
        assertEquals(SavedPatternMetadataMutationResult.ReplacementRequired(99L), viewModel.state.value.result)
    }

    @Test
    fun `retained owner confirms the original expected link once and consumes success`() {
        val viewModel = SavedPatternAttachmentViewModel()
        val expectedIds = mutableListOf<Long?>()
        viewModel.attach(7L, null) { expectedId, callback ->
            expectedIds += expectedId
            callback(SavedPatternMetadataMutationResult.ReplacementRequired(88L))
        }
        val pendingReplacementId =
            (viewModel.state.value.result as SavedPatternMetadataMutationResult.ReplacementRequired)
                .existingSavedPatternId
        viewModel.consumeResult()
        val retainedProjectId = requireNotNull(viewModel.state.value.projectId)
        assertEquals(7L, retainedProjectId)
        lateinit var complete: (SavedPatternMetadataMutationResult) -> Unit

        viewModel.attach(retainedProjectId, pendingReplacementId) { expectedId, callback ->
            expectedIds += expectedId
            complete = callback
        }
        viewModel.attach(retainedProjectId, pendingReplacementId) { _, _ -> error("Duplicate confirmation admitted") }
        viewModel.attach(8L, null) { _, _ -> error("Another project admitted while confirming") }
        assertEquals(listOf(null, 88L), expectedIds)
        assertEquals(7L, viewModel.state.value.projectId)
        assertTrue(viewModel.state.value.inFlight)

        complete(SavedPatternMetadataMutationResult.Attached(12L))
        assertEquals(SavedPatternMetadataMutationResult.Attached(12L), viewModel.state.value.result)
        viewModel.attach(retainedProjectId, pendingReplacementId) { _, _ -> error("Unconsumed result replaced") }
        viewModel.consumeResult()
        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.inFlight)
        assertEquals(7L, viewModel.state.value.projectId)
    }

    @Test
    fun `rejected project selection cannot replace the accepted operation owner`() {
        val viewModel = SavedPatternAttachmentViewModel()
        lateinit var complete: (SavedPatternMetadataMutationResult) -> Unit
        viewModel.attach(7L, null) { _, callback -> complete = callback }
        var secondStarted = false

        viewModel.attach(8L, null) { _, _ -> secondStarted = true }

        assertFalse(secondStarted)
        assertTrue(viewModel.state.value.inFlight)
        assertEquals(7L, viewModel.state.value.projectId)
        complete(SavedPatternMetadataMutationResult.Attached(12L))
        assertFalse(viewModel.state.value.inFlight)
        assertEquals(7L, viewModel.state.value.projectId)
        viewModel.attach(8L, null) { _, _ -> secondStarted = true }
        assertFalse(secondStarted)
        viewModel.consumeResult()
        viewModel.attach(8L, null) { _, _ -> secondStarted = true }
        assertTrue(secondStarted)
        assertEquals(8L, viewModel.state.value.projectId)
    }
}
