package com.finnvek.knittools.ai

import android.graphics.Bitmap
import org.json.JSONObject

/**
 * Hakee neulontaohjeen PDF-sivukuvasta Gemini Flash Litellä.
 * Yksi multimodal-kutsu korvaa vanhan OCR → Nano -putken.
 */
object PatternInstructionGemini {
    data class InstructionResult(
        val instruction: String?,
        val positionPercent: Int?,
    )

    private fun buildPrompt(rowNumber: Int): String =
        """
        You are a knitting pattern reader. Look at this pattern page image and find the instruction for the requested row.

        Return ONLY a JSON object:
        {
          "instruction": "the exact instruction text for the row, or null if not found",
          "positionPercent": "estimated vertical position of the row on the page as a number 0-100, or null if not determinable"
        }

        Rules:
        - Return the instruction exactly as written in the pattern, preserving abbreviations (K, P, SSK, K2tog, YO, etc.)
        - If the row uses a reference like "repeat Row 2", resolve it if the referenced row is visible on this page
        - If the instruction spans multiple lines, join them with a single space
        - If the row number does not appear on this page, return {"instruction": null, "positionPercent": null}
        - positionPercent 0 = top of page, 100 = bottom of page
        - Do not add explanations or commentary
        - Do not translate or modify abbreviations

        Row number: $rowNumber
        """.trimIndent()

    suspend fun getInstruction(
        geminiAiService: GeminiAiService,
        pageBitmap: Bitmap,
        rowNumber: Int,
    ): InstructionResult? {
        val response =
            geminiAiService.generateFromImage(pageBitmap, buildPrompt(rowNumber))
                ?: return null
        return parseResponse(response)
    }

    internal fun parseResponse(response: String): InstructionResult? {
        val jsonText = extractJson(response) ?: return null
        return try {
            val json = JSONObject(jsonText)
            val instruction =
                if (json.isNull("instruction")) {
                    null
                } else {
                    json.optString("instruction").takeIf {
                        it.isNotBlank() &&
                            it != "null"
                    }
                }
            val position =
                if (json.isNull("positionPercent")) {
                    null
                } else {
                    json.optInt("positionPercent", -1).takeIf {
                        it in
                            0..100
                    }
                }
            InstructionResult(instruction = instruction, positionPercent = position)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJson(response: String): String? {
        val trimmed = response.trim()
        if (trimmed.startsWith("{")) return trimmed

        val codeBlockPattern = Regex("""```(?:json)?\s*\n?(.*?)\n?```""", RegexOption.DOT_MATCHES_ALL)
        codeBlockPattern.find(trimmed)?.let { return it.groupValues[1].trim() }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)

        return null
    }
}
