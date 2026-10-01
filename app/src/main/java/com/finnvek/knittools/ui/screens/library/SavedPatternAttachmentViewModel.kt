package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.ViewModel
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class SavedPatternAttachmentState(
    val inFlight: Boolean = false,
    val result: SavedPatternMetadataMutationResult? = null,
)

internal class SavedPatternAttachmentViewModel : ViewModel() {
    private val _state = MutableStateFlow(SavedPatternAttachmentState())
    val state = _state.asStateFlow()

    fun attach(
        expectedExistingId: Long?,
        startAttachment: (Long?, (SavedPatternMetadataMutationResult) -> Unit) -> Unit,
    ) {
        if (_state.value.inFlight || _state.value.result != null) return
        _state.value = SavedPatternAttachmentState(inFlight = true)
        startAttachment(expectedExistingId) { result ->
            _state.value = SavedPatternAttachmentState(result = result)
        }
    }

    fun consumeResult() {
        _state.value = _state.value.copy(result = null)
    }
}
