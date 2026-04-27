package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.CombinedInstructionResult
import com.finnvek.knittools.ai.PatternInstructionGemini
import com.finnvek.knittools.repository.CombineInstructionsOutcome
import com.finnvek.knittools.repository.PatternInstructionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstructionDisplayState(
    val instruction: String? = null,
    val isLoading: Boolean = false,
    val rowNumber: Int = 0,
    val canDisplayInstruction: Boolean = false,
    val positionPercent: Int? = null,
)

data class ExplanationState(
    val explanation: String? = null,
    val isLoading: Boolean = false,
    val isVisible: Boolean = false,
    val forInstruction: String = "",
)

data class CombineState(
    val result: CombinedInstructionResult? = null,
    val isLoading: Boolean = false,
    val isVisible: Boolean = false,
    @param:StringRes @field:StringRes val messageResId: Int? = null,
)

@HiltViewModel
class PatternViewerViewModel
    @Inject
    constructor(
        private val instructionRepository: PatternInstructionRepository,
    ) : ViewModel() {
        private val _instructionState = MutableStateFlow(InstructionDisplayState())
        val instructionState: StateFlow<InstructionDisplayState> = _instructionState
        private val _explanationState = MutableStateFlow(ExplanationState())
        val explanationState: StateFlow<ExplanationState> = _explanationState
        private val _combineState = MutableStateFlow(CombineState())
        val combineState: StateFlow<CombineState> = _combineState.asStateFlow()
        private var instructionJob: Job? = null
        private var prefetchJob: Job? = null
        private var explanationJob: Job? = null
        private var combineJob: Job? = null
        private var activeRequestId = 0L
        private var cachedPatternUri: String? = null

        // In-memory cache: "page:row" → result
        private val instructionCache = mutableMapOf<String, PatternInstructionGemini.InstructionResult>()
        private val explanationCache = mutableMapOf<String, String>()
        private val combineCache = mutableMapOf<Int, CombinedInstructionResult>()

        fun onViewerContextChanged(
            patternUri: String?,
            currentPage: Int,
            currentRow: Int,
            renderedBitmap: Bitmap?,
            canDisplayInstruction: Boolean,
        ) {
            val previousRow = _instructionState.value.rowNumber
            if (patternUri != cachedPatternUri) {
                cachedPatternUri = patternUri
                instructionCache.clear()
                explanationCache.clear()
                combineCache.clear()
                instructionJob?.cancel()
                prefetchJob?.cancel()
                combineJob?.cancel()
                hideExplanation()
                onCombineSheetDismissed()
            }

            if (patternUri.isNullOrBlank() || renderedBitmap == null || !canDisplayInstruction || currentRow <= 0) {
                instructionJob?.cancel()
                prefetchJob?.cancel()
                activeRequestId += 1
                hideExplanation()
                _instructionState.value =
                    InstructionDisplayState(
                        rowNumber = currentRow,
                        canDisplayInstruction = canDisplayInstruction,
                    )
                return
            }

            if (previousRow != currentRow) {
                hideExplanation()
            }

            instructionJob?.cancel()
            prefetchJob?.cancel()
            val requestId = ++activeRequestId
            val cacheKey = cacheKey(currentPage, currentRow)

            // Cache-osuma — näytä välittömästi
            instructionCache[cacheKey]?.let { cached ->
                _instructionState.value =
                    InstructionDisplayState(
                        instruction = cached.instruction,
                        positionPercent = cached.positionPercent,
                        rowNumber = currentRow,
                        canDisplayInstruction = true,
                    )
                prefetchRow(currentRow + 1, currentPage, renderedBitmap)
                return
            }

            instructionJob =
                viewModelScope.launch {
                    // Näytä edellinen ohje samalle riville tai loading
                    _instructionState.value =
                        InstructionDisplayState(
                            instruction =
                                _instructionState.value.instruction.takeIf {
                                    _instructionState.value.rowNumber == currentRow
                                },
                            isLoading = true,
                            rowNumber = currentRow,
                            canDisplayInstruction = true,
                        )

                    val result =
                        instructionRepository.getInstruction(
                            pageBitmap = renderedBitmap,
                            rowNumber = currentRow,
                        )
                    ensureActive()
                    if (requestId != activeRequestId) return@launch

                    if (result != null) {
                        instructionCache[cacheKey] = result
                        _instructionState.value =
                            InstructionDisplayState(
                                instruction = result.instruction,
                                positionPercent = result.positionPercent,
                                rowNumber = currentRow,
                                canDisplayInstruction = true,
                            )
                    } else {
                        _instructionState.value =
                            InstructionDisplayState(
                                rowNumber = currentRow,
                                canDisplayInstruction = true,
                            )
                    }

                    // Esihae seuraava rivi
                    prefetchRow(currentRow + 1, currentPage, renderedBitmap)
                }
        }

        fun onInstructionTapped(instructionText: String) {
            if (instructionText.isBlank()) return

            val currentState = _explanationState.value
            if (currentState.isVisible && currentState.forInstruction == instructionText) {
                hideExplanation()
                return
            }

            explanationCache[instructionText]?.let { cached ->
                _explanationState.value =
                    ExplanationState(
                        explanation = cached,
                        isVisible = true,
                        forInstruction = instructionText,
                    )
                return
            }

            explanationJob?.cancel()
            explanationJob =
                viewModelScope.launch {
                    _explanationState.value =
                        ExplanationState(
                            isLoading = true,
                            isVisible = true,
                            forInstruction = instructionText,
                        )

                    val result = instructionRepository.explainInstruction(instructionText)
                    ensureActive()

                    if (result != null) {
                        explanationCache[instructionText] = result
                        _explanationState.value =
                            ExplanationState(
                                explanation = result,
                                isVisible = true,
                                forInstruction = instructionText,
                            )
                    } else {
                        _explanationState.value = ExplanationState()
                    }
                }
        }

        private fun prefetchRow(
            row: Int,
            page: Int,
            bitmap: Bitmap,
        ) {
            if (row <= 0) return
            val key = cacheKey(page, row)
            if (instructionCache.containsKey(key)) return

            prefetchJob?.cancel()
            prefetchJob =
                viewModelScope.launch {
                    val result =
                        instructionRepository.getInstruction(
                            pageBitmap = bitmap,
                            rowNumber = row,
                        )
                    if (result != null) {
                        instructionCache[key] = result
                    }
                }
        }

        fun clearInstructionCaches() {
            instructionJob?.cancel()
            prefetchJob?.cancel()
            explanationJob?.cancel()
            combineJob?.cancel()
            activeRequestId += 1
            instructionCache.clear()
            explanationCache.clear()
            combineCache.clear()
            _instructionState.value = InstructionDisplayState()
            _explanationState.value = ExplanationState()
            _combineState.value = CombineState()
        }

        fun onCombineInstructionsTapped(
            currentPage: Int,
            pageBitmap: Bitmap,
        ) {
            combineCache[currentPage]?.let { cached ->
                _combineState.value =
                    CombineState(
                        result = cached,
                        isVisible = true,
                    )
                return
            }

            combineJob?.cancel()
            combineJob =
                viewModelScope.launch {
                    _combineState.value = CombineState(isLoading = true, isVisible = true)

                    val result = instructionRepository.combineInstructions(pageBitmap)
                    ensureActive()

                    when (result) {
                        is CombineInstructionsOutcome.Success -> {
                            combineCache[currentPage] = result.result
                            _combineState.value =
                                CombineState(
                                    result = result.result,
                                    isVisible = true,
                                )
                        }

                        CombineInstructionsOutcome.Offline -> {
                            _combineState.value =
                                CombineState(
                                    isVisible = true,
                                    messageResId = R.string.pattern_combine_requires_internet,
                                )
                        }

                        CombineInstructionsOutcome.QuotaExhausted -> {
                            _combineState.value =
                                CombineState(
                                    isVisible = true,
                                    messageResId = R.string.ai_quota_exhausted,
                                )
                        }

                        CombineInstructionsOutcome.Failed -> {
                            _combineState.value =
                                CombineState(
                                    isVisible = true,
                                    messageResId = R.string.ai_error_busy,
                                )
                        }
                    }
                }
        }

        fun onCombineSheetDismissed() {
            combineJob?.cancel()
            _combineState.value = CombineState()
        }

        private fun cacheKey(
            page: Int,
            row: Int,
        ) = "$page:$row"

        private fun hideExplanation() {
            explanationJob?.cancel()
            _explanationState.value = ExplanationState()
        }
    }
