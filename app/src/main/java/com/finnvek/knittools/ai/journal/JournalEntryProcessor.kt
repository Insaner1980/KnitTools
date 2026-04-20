package com.finnvek.knittools.ai.journal

import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import javax.inject.Inject
import javax.inject.Singleton

sealed class JournalProcessResult {
    /** AI siivosi tekstin onnistuneesti. */
    data class Success(
        val cleaned: String,
    ) : JournalProcessResult()

    /** AI-kutsu ei onnistunut — raakateksti palautetaan silti tallennettavaksi. */
    data class Fallback(
        val raw: String,
        val reason: Reason,
    ) : JournalProcessResult() {
        enum class Reason { NoPro, QuotaExhausted, ApiError }
    }
}

/**
 * Rakentaa promptin, kutsuu Gemini 2.5 Flash Litea ja hoitaa Pro/quota/virhe-fallbackin.
 * EI koskaan heitä — aina palauttaa jomman kumman tuloksen, jolloin käyttäjän teksti ei häviä.
 */
@Singleton
class JournalEntryProcessor
    @Inject
    constructor(
        private val geminiAiService: GeminiAiService,
        private val aiQuotaManager: AiQuotaManager,
        private val proManager: ProManager,
    ) {
        suspend fun process(rawText: String): JournalProcessResult {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) {
                return JournalProcessResult.Fallback(
                    rawText,
                    JournalProcessResult.Fallback.Reason.ApiError,
                )
            }

            if (!proManager.hasFeature(ProFeature.AI_FEATURES)) {
                return JournalProcessResult.Fallback(trimmed, JournalProcessResult.Fallback.Reason.NoPro)
            }
            if (!aiQuotaManager.hasQuota()) {
                return JournalProcessResult.Fallback(trimmed, JournalProcessResult.Fallback.Reason.QuotaExhausted)
            }

            val prompt = buildPrompt(trimmed)
            val response = geminiAiService.generateText(prompt)
            val cleaned = response?.let { postProcess(it) }

            return if (cleaned.isNullOrBlank()) {
                JournalProcessResult.Fallback(trimmed, JournalProcessResult.Fallback.Reason.ApiError)
            } else {
                aiQuotaManager.recordCall()
                JournalProcessResult.Success(cleaned)
            }
        }

        private fun buildPrompt(rawText: String): String =
            """
            You receive a short note from a knitter about their current knitting session. Your job is to format it as a journal entry WITHOUT changing the meaning or rewording the content.

            Rules:
            1. PRESERVE the user's original wording. Do not paraphrase, summarize, or rewrite.
            2. Fix obvious punctuation and capitalization issues (especially from speech-to-text).
            3. Do NOT add new information the user did not provide.
            4. Do NOT interpret emotions, guess intent, or add context.
            5. If the text is in Finnish, keep it in Finnish. If English, keep it in English. Match the user's language.

            Respond with ONLY the cleaned text. No preamble, no quotation marks.

            ---
            $rawText
            """.trimIndent()

        private fun postProcess(response: String): String =
            response
                .trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
    }
