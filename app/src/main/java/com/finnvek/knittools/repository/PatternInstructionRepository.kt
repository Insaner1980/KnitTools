package com.finnvek.knittools.repository

import android.graphics.Bitmap
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.CombinedInstructionResult
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.ai.PatternInstructionGemini
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.util.NetworkStatusProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CombineInstructionsOutcome {
    data class Success(
        val result: CombinedInstructionResult,
    ) : CombineInstructionsOutcome

    data object Offline : CombineInstructionsOutcome

    data object QuotaExhausted : CombineInstructionsOutcome

    data object Failed : CombineInstructionsOutcome
}

@Singleton
class PatternInstructionRepository
    @Inject
    constructor(
        private val geminiAiService: GeminiAiService,
        private val aiQuotaManager: AiQuotaManager,
        private val preferencesManager: PreferencesManager,
        private val networkStatusProvider: NetworkStatusProvider,
    ) {
        suspend fun getInstruction(
            pageBitmap: Bitmap,
            rowNumber: Int,
        ): PatternInstructionGemini.InstructionResult? {
            val result =
                PatternInstructionGemini.getInstruction(
                    geminiAiService = geminiAiService,
                    pageBitmap = pageBitmap,
                    rowNumber = rowNumber,
                )
            if (result != null) {
                aiQuotaManager.recordCall()
            }
            return result
        }

        suspend fun explainInstruction(instructionText: String): String? {
            val language =
                preferencesManager.preferences
                    .first()
                    .appLanguage
                    .promptLanguageName()
            val result = geminiAiService.explainInstruction(instructionText, language)
            if (result != null) {
                aiQuotaManager.recordCall()
            }
            return result
        }

        suspend fun combineInstructions(pageBitmap: Bitmap): CombineInstructionsOutcome {
            if (!networkStatusProvider.isOnline()) return CombineInstructionsOutcome.Offline
            if (!aiQuotaManager.hasQuota()) return CombineInstructionsOutcome.QuotaExhausted

            val result = geminiAiService.combineInstructions(pageBitmap)
                ?: return CombineInstructionsOutcome.Failed
            aiQuotaManager.recordCall()
            return CombineInstructionsOutcome.Success(result)
        }
    }
