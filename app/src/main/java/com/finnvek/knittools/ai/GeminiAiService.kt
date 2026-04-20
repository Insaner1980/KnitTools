package com.finnvek.knittools.ai

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jaettu palvelu Firebase AI Logic -kutsuille.
 * Kaikki AI-ominaisuudet käyttävät tätä — ei suoria Gemini-kutsuja muualta.
 */
@Singleton
class GeminiAiService
    @Inject
    constructor() {
        private val model by lazy {
            Firebase
                .ai(backend = GenerativeBackend.googleAI())
                .generativeModel(MODEL_NAME)
        }

        private val voiceModel by lazy {
            Firebase
                .ai(backend = GenerativeBackend.googleAI())
                .generativeModel(VOICE_MODEL_NAME)
        }

        /**
         * Lähettää tekstipromptin ja palauttaa vastauksen.
         * Palauttaa null jos kutsu epäonnistuu (ei verkkoa, kiintiö täynnä, API-virhe).
         */
        suspend fun generateText(prompt: String): String? = runWithRetry { model.generateContent(prompt).text }

        /**
         * Lähettää tekstipromptin äänikomentojen Flash-mallille (parempi ymmärrys).
         */
        suspend fun generateTextForVoice(prompt: String): String? =
            runWithRetry { voiceModel.generateContent(prompt).text }

        /**
         * Selittää neulontaohjeen lyhyesti selkokielellä.
         * Palauttaa null jos kutsu epäonnistuu tai vastaus on tyhjä.
         */
        suspend fun explainInstruction(instruction: String): String? {
            if (instruction.isBlank()) return null
            val prompt =
                """
                You are a knitting instructor explaining a pattern instruction to someone who knows basic knitting but may not recognize all abbreviations.

                Explain this knitting instruction in plain language. Go through each part in order. For each abbreviation, state what it means and briefly what the knitter does physically. Keep it concise - one sentence per abbreviation or short group. Do not repeat the original instruction at the start.

                Respond in English.

                Instruction: $instruction
                """.trimIndent()
            return generateText(prompt)?.trim()?.takeIf { it.isNotBlank() }
        }

        /**
         * Lähettää kuvan ja tekstipromptin (multimodal) ja palauttaa vastauksen.
         * Tarvitaan esim. Pattern Viewerin ohjeiden tunnistamiseen.
         */
        suspend fun generateFromImage(
            bitmap: Bitmap,
            prompt: String,
        ): String? =
            runWithRetry {
                val inputContent =
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                model.generateContent(inputContent).text
            }

        /**
         * Yhdistää samalla sivulla päällekkäiset neulontaohjeet yhdeksi rivilistaksi.
         */
        suspend fun combineInstructions(bitmap: Bitmap): CombinedInstructionResult? =
            PatternInstructionCombinerGemini.combine(
                geminiAiService = this,
                pageBitmap = bitmap,
            )

        /**
         * Yritä kutsua uudelleen rate limit -virheillä (429 / quota exceeded).
         * Max 2 yritystä, 3s + 6s viive.
         */
        private suspend fun runWithRetry(block: suspend () -> String?): String? {
            repeat(MAX_RETRIES) { attempt ->
                try {
                    return block()
                } catch (e: Exception) {
                    val message = e.message?.lowercase().orEmpty()
                    val isRateLimit =
                        message.contains("quota") ||
                            message.contains("rate") ||
                            message.contains("429") ||
                            message.contains("resource_exhausted")
                    if (isRateLimit && attempt < MAX_RETRIES - 1) {
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                    } else {
                        return null
                    }
                }
            }
            return null
        }

        companion object {
            private const val MODEL_NAME = "gemini-2.5-flash-lite"
            private const val VOICE_MODEL_NAME = "gemini-2.5-flash"
            private const val MAX_RETRIES = 3
            private const val RETRY_DELAY_MS = 3_000L
        }
    }
