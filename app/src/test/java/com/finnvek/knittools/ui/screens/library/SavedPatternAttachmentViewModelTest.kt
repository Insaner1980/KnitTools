package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPatternAttachmentViewModelTest {
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
