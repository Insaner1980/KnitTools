package com.finnvek.knittools.ai

import android.graphics.Bitmap
import com.finnvek.knittools.ai.ocr.ParsedYarnLabel
import org.json.JSONObject

/**
 * Skannaa lankaetikettikuvan Gemini Flash Litellä ja palauttaa jäsennetyn tuloksen.
 * Yksi multimodal-kutsu korvaa vanhan OCR → Nano -putken.
 */
object YarnLabelGeminiScanner {
    private val PROMPT =
        """
        You are a yarn label reader. Analyze this photo of a yarn label and extract all visible information.

        Return ONLY a JSON object with these fields. Use null for any field not visible on the label. Do not guess or infer values that are not printed on the label.

        {
          "brand": "string or null",
          "name": "string or null",
          "fiberContent": "string or null",
          "weightCategory": "one of: LACE, FINGERING, SPORT, DK, WORSTED, ARAN, BULKY, SUPER_BULKY or null",
          "metersPerSkein": "number or null",
          "gramsPerSkein": "number or null",
          "recommendedNeedleMm": "number or null",
          "gaugeStitches": "number or null (stitches per 10cm)",
          "gaugeRows": "number or null (rows per 10cm)",
          "colorName": "string or null",
          "colorNumber": "string or null",
          "dyeLot": "string or null",
          "careInstructions": "string or null"
        }

        If the image does not show a yarn label, return: {"error": "not_a_yarn_label"}

        Important:
        - If the label shows yards instead of meters, convert to meters (1 yard = 0.9144 meters) and return meters
        - For weight category, infer from needle size and meters/gram if not explicitly stated
        - Preserve the original brand and color names exactly as printed
        - Do not translate anything
        """.trimIndent()

    /**
     * Skannaa kuvan ja palauttaa ParsedYarnLabel.
     * Palauttaa null jos kuva ei ole lankaetikettiä tai Gemini-kutsu epäonnistuu.
     */
    suspend fun scan(
        geminiAiService: GeminiAiService,
        bitmap: Bitmap,
    ): ParsedYarnLabel? {
        val response = geminiAiService.generateFromImage(bitmap, PROMPT) ?: return null
        return parseResponse(response)
    }

    internal fun parseResponse(response: String): ParsedYarnLabel? {
        val jsonText = extractJson(response) ?: return null
        return try {
            val json = JSONObject(jsonText)
            if (json.has("error")) return null

            ParsedYarnLabel(
                brand = json.optStringOrEmpty("brand"),
                yarnName = json.optStringOrEmpty("name"),
                fiberContent = json.optStringOrEmpty("fiberContent"),
                weightCategory = json.optStringOrEmpty("weightCategory"),
                weightGrams = json.optNullableInt("gramsPerSkein")?.toString() ?: "",
                lengthMeters = json.optNullableInt("metersPerSkein")?.toString() ?: "",
                needleSize = json.optNullableDouble("recommendedNeedleMm")?.let { formatNeedle(it) } ?: "",
                gaugeInfo =
                    buildGaugeInfo(
                        stitches = json.optNullableInt("gaugeStitches"),
                        rows = json.optNullableInt("gaugeRows"),
                    ),
                colorName = json.optStringOrEmpty("colorName"),
                colorNumber = json.optStringOrEmpty("colorNumber"),
                dyeLot = json.optStringOrEmpty("dyeLot"),
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Poimii JSON-objektin vastauksesta, joka voi sisältää markdown-koodiblokin.
     */
    internal fun extractJson(response: String): String? {
        val trimmed = response.trim()
        // Yritä ensin suoraa JSON-parsintaa
        if (trimmed.startsWith("{")) return trimmed

        // Markdown-koodiblokki: ```json ... ``` tai ``` ... ```
        val codeBlockPattern = Regex("""```(?:json)?\s*\n?(.*?)\n?```""", RegexOption.DOT_MATCHES_ALL)
        codeBlockPattern.find(trimmed)?.let { match ->
            return match.groupValues[1].trim()
        }

        // Etsi ensimmäinen { viimeiseen } asti
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }

        return null
    }

    private fun buildGaugeInfo(
        stitches: Int?,
        rows: Int?,
    ): String {
        if (stitches == null && rows == null) return ""
        val parts = mutableListOf<String>()
        stitches?.let { parts.add("$it sts") }
        rows?.let { parts.add("$it rows") }
        return "${parts.joinToString(" × ")} = 10cm"
    }

    private fun formatNeedle(mm: Double): String = if (mm == mm.toLong().toDouble()) "${mm.toLong()}mm" else "${mm}mm"

    private fun JSONObject.optStringOrEmpty(key: String): String {
        if (isNull(key)) return ""
        return optString(key, "").takeIf { it != "null" } ?: ""
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key, 0).takeIf { it != 0 } ?: optDouble(key).takeIf { !it.isNaN() }?.toInt()
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).takeIf { !it.isNaN() && it != 0.0 }
    }
}
