package com.finnvek.knittools.ai

import android.graphics.Bitmap
import org.json.JSONObject

data class CombinedRow(
    val row: Int,
    val side: String?,
    val instruction: String,
)

data class CombinedInstructionResult(
    val found: Boolean,
    val title: String? = null,
    val startRow: Int? = null,
    val rows: List<CombinedRow> = emptyList(),
)

object PatternInstructionCombinerGemini {
    private fun buildPrompt(): String =
        """
        You are a knitting pattern expert. Look at this pattern page and find any sections where multiple instructions happen simultaneously (indicated by phrases like "AT THE SAME TIME", "while continuing to", "meanwhile", or similar).

        If overlapping instructions are found, combine them into a single row-by-row list. Each row should state everything that happens on that row.

        Return a JSON object:
        {
          "found": true,
          "title": "short description of what is being combined",
          "startRow": 1,
          "rows": [
            {"row": 1, "side": "RS", "instruction": "combined instruction for this row"}
          ]
        }

        If no overlapping instructions are found on this page, return:
        {"found": false}

        Rules:
        - Number rows sequentially starting from the beginning of the combined section
        - Mark each row as RS (right side) or WS (wrong side) if the pattern specifies
        - When two things happen on the same row, join them clearly
        - Preserve all stitch abbreviations exactly as written in the pattern
        - Include stitch counts in parentheses where the pattern provides them
        - If the pattern says "repeat row X", write out what that row actually is when possible from the visible page
        - Do not add rows beyond what the pattern specifies on this page
        - Return only JSON with no markdown wrapper or commentary
        """.trimIndent()

    suspend fun combine(
        geminiAiService: GeminiAiService,
        pageBitmap: Bitmap,
    ): CombinedInstructionResult? {
        val response = geminiAiService.generateFromImage(pageBitmap, buildPrompt()) ?: return null
        return parseResponse(response)
    }

    internal fun parseResponse(response: String): CombinedInstructionResult? {
        val jsonText = extractJson(response) ?: return null
        return try {
            val json = JSONObject(jsonText)
            if (!json.optBoolean("found", false)) {
                CombinedInstructionResult(found = false)
            } else {
                val rowsJson = json.optJSONArray("rows") ?: return null
                val rows =
                    buildList {
                        for (index in 0 until rowsJson.length()) {
                            val rowJson = rowsJson.optJSONObject(index) ?: continue
                            val row = rowJson.optInt("row", -1).takeIf { it > 0 } ?: continue
                            val instruction =
                                rowJson
                                    .optString("instruction")
                                    .takeIf { it.isNotBlank() && it != "null" }
                                    ?: continue
                            val side =
                                rowJson
                                    .takeIf { !it.isNull("side") }
                                    ?.optString("side")
                                    ?.trim()
                                    ?.uppercase()
                                    ?.takeIf { it == "RS" || it == "WS" }
                            add(
                                CombinedRow(
                                    row = row,
                                    side = side,
                                    instruction = instruction,
                                ),
                            )
                        }
                    }
                if (rows.isEmpty()) return null

                CombinedInstructionResult(
                    found = true,
                    title =
                        json
                            .takeIf { !it.isNull("title") }
                            ?.optString("title")
                            ?.trim()
                            ?.takeIf { it.isNotBlank() && it != "null" },
                    startRow = json.optInt("startRow", -1).takeIf { it > 0 },
                    rows = rows,
                )
            }
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
