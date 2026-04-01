package com.finnvek.knittools.ai.nano

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.min
import kotlin.math.pow

sealed class ParsedInstruction {
    data class IncreaseDecrease(
        val currentStitches: Int,
        val changeBy: Int,
        val isIncrease: Boolean,
    ) : ParsedInstruction()

    data class Gauge(
        val stitchesPer10cm: Double,
        val rowsPer10cm: Double,
    ) : ParsedInstruction()

    data class Failure(
        val reason: String,
    ) : ParsedInstruction()
}

object InstructionParser {
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 500L
    private const val MAX_DELAY_MS = 8000L

    private val SYSTEM_PROMPT =
        """
        You are a knitting instruction parser. Extract numerical values from the instruction.

        If the instruction is about increasing or decreasing stitches, respond EXACTLY in this format:
        TYPE: INCREASE or DECREASE
        CURRENT: <number of current stitches>
        CHANGE: <number of stitches to add or remove>

        If the instruction is about gauge, respond EXACTLY in this format:
        GAUGE_STITCHES: <stitches per 10cm or 4 inches>
        GAUGE_ROWS: <rows per 10cm or 4 inches>

        If you cannot parse the instruction, respond: CANNOT_PARSE

        Only output the values, nothing else.
        """.trimIndent()

    @Suppress("TooGenericExceptionCaught")
    suspend fun parse(instruction: String): ParsedInstruction {
        if (instruction.isBlank()) return ParsedInstruction.Failure("Empty instruction")

        val model: GenerativeModel
        try {
            model = Generation.getClient()
        } catch (_: Exception) {
            return ParsedInstruction.Failure("Gemini Nano not available")
        }

        return try {
            ensureFeatureReady(model)
            val result = runWithRetry(model, instruction)
            parseResponse(result)
        } catch (e: GenAiException) {
            ParsedInstruction.Failure("AI error: ${e.message}")
        } catch (_: Exception) {
            ParsedInstruction.Failure("Unexpected error")
        } finally {
            model.close()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun ensureFeatureReady(model: GenerativeModel) {
        val status = model.checkStatus()
        when (status) {
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                try {
                    model.download().firstOrNull()
                } catch (_: Exception) {
                    // Download may fail — proceed anyway, generateContent will
                    // wait for download if still in progress
                }
            }

            FeatureStatus.AVAILABLE -> { /* ready */ }

            else -> error("Feature unavailable")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runWithRetry(
        model: GenerativeModel,
        instruction: String,
    ): String {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val prompt = "$SYSTEM_PROMPT\n\nInstruction: $instruction"
                val response = model.generateContent(prompt)
                return response.candidates.firstOrNull()?.text ?: ""
            } catch (e: GenAiException) {
                lastException = e
                val errorMessage = e.message?.lowercase() ?: ""
                if (errorMessage.contains("busy") || errorMessage.contains("quota")) {
                    val delayMs = min(BASE_DELAY_MS * 2.0.pow(attempt).toLong(), MAX_DELAY_MS)
                    delay(delayMs)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Max retries exceeded")
    }

    internal fun parseResponse(response: String): ParsedInstruction {
        val lines =
            response.trim().lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim() else "" to ""
            }

        if (lines.containsKey("TYPE") && lines.containsKey("CURRENT") && lines.containsKey("CHANGE")) {
            val isIncrease = lines["TYPE"]?.uppercase()?.contains("INCREASE") == true
            val current = lines["CURRENT"]?.toIntOrNull()
            val change = lines["CHANGE"]?.toIntOrNull()
            if (current != null && change != null) {
                return ParsedInstruction.IncreaseDecrease(current, change, isIncrease)
            }
        }

        if (lines.containsKey("GAUGE_STITCHES") && lines.containsKey("GAUGE_ROWS")) {
            val stitches = lines["GAUGE_STITCHES"]?.toDoubleOrNull()
            val rows = lines["GAUGE_ROWS"]?.toDoubleOrNull()
            if (stitches != null && rows != null) {
                return ParsedInstruction.Gauge(stitches, rows)
            }
        }

        return ParsedInstruction.Failure("Could not parse instruction")
    }
}
