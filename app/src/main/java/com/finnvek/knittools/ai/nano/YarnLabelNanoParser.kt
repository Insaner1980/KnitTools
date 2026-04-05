package com.finnvek.knittools.ai.nano

import com.finnvek.knittools.ai.ocr.ParsedYarnLabel
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.min
import kotlin.math.pow

/**
 * Käyttää Gemini Nanoa OCR-tekstin parsintaan. Toimii millä tahansa kielellä.
 * Fallback: palauttaa null jos Nano ei ole saatavilla → kutsuja käyttää regex-parseria.
 */
object YarnLabelNanoParser {
    private const val MAX_RETRIES = 2
    private const val BASE_DELAY_MS = 500L
    private const val MAX_DELAY_MS = 4000L

    private val SYSTEM_PROMPT =
        """
        You are a yarn label data extractor. Given OCR text from a yarn ball label (in any language), extract ONLY these fields. Respond EXACTLY in this format, one field per line. Leave value empty if not found.

        BRAND: <manufacturer/brand name, e.g. "Novita", "Drops", "Schachenmayr">
        YARN_NAME: <product/yarn name, e.g. "Nalle", "Alpaca", "Catania">
        WEIGHT_GRAMS: <skein weight in grams, number only>
        LENGTH_METERS: <yarn length in meters, number only>
        NEEDLE_SIZE: <recommended needle size in mm, e.g. "4.0" or "3.5 - 4.0">
        FIBER: <fiber content with percentages, e.g. "75% Wool, 25% Polyamide">
        COLOR_NAME: <color name>
        COLOR_NUMBER: <color number/code>
        DYE_LOT: <dye lot/bath number>
        WEIGHT_CATEGORY: <Lace/Fingering/Sport/DK/Worsted/Aran/Bulky/Super Bulky>
        GAUGE: <e.g. "22 sts x 30 rows = 10cm">

        Important rules:
        - IGNORE care/washing instructions (hand wash, machine wash, do not bleach, iron, dry clean symbols or text). These are NOT yarn data.
        - IGNORE barcodes, EAN codes, and website URLs
        - Convert ounces to grams (1 oz = 28.35g), yards to meters (1 yd = 0.914m)
        - For WEIGHT_GRAMS and LENGTH_METERS, output the number only (no units)
        - BRAND is the company that makes the yarn, YARN_NAME is the product line name
        - If text is in Finnish, Swedish, German, etc., translate fiber names to English (villa=Wool, puuvilla=Cotton, akryyli=Acrylic, silkki=Silk, pellava=Linen, alpakka=Alpaca)
        - Only output the key: value lines, nothing else

        OCR ERROR HANDLING — the input text comes from camera OCR and may contain errors:
        - Fix broken/split words: "Dro ps" → "Drops", "Bab y Meri no" → "Baby Merino", "Wo ol" → "Wool"
        - Fix O/0 confusion: "1O0g" → "100g", "5O m" → "50 m"
        - Fix l/1/I confusion: "l00g" → "100g", "Iength" → "length"
        - Fix common brand misspellings: "Novlta" → "Novita", "Schachenmayr" variations
        - "m" near a number usually means meters, "g" means grams
        - Numbers like "50 g / 175 m" are weight/length even if OCR garbles surrounding text
        - If multiple potential brand names appear, pick the most prominent/known one
        """.trimIndent()

    /**
     * Palauttaa ParsedYarnLabel jos Nano on käytettävissä ja parsinta onnistuu, muuten null.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun parse(ocrText: String): ParsedYarnLabel? {
        if (ocrText.isBlank()) return null

        val model: GenerativeModel
        try {
            model = Generation.getClient()
        } catch (_: Exception) {
            return null
        }

        return try {
            val status = model.checkStatus()
            when (status) {
                FeatureStatus.AVAILABLE -> { /* valmis */ }

                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                    try {
                        model.download().firstOrNull()
                    } catch (_: Exception) {
                        // jatka silti
                    }
                }

                else -> {
                    return null
                }
            }

            val response = runWithRetry(model, ocrText) ?: return null
            parseResponse(response)
        } catch (_: Exception) {
            null
        } finally {
            model.close()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runWithRetry(
        model: GenerativeModel,
        ocrText: String,
    ): String? {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val prompt = "$SYSTEM_PROMPT\n\nYarn label text:\n$ocrText"
                val response = model.generateContent(prompt)
                return response.candidates.firstOrNull()?.text
            } catch (e: GenAiException) {
                val msg = e.message?.lowercase() ?: ""
                if (msg.contains("busy") || msg.contains("quota")) {
                    val delayMs = min(BASE_DELAY_MS * 2.0.pow(attempt).toLong(), MAX_DELAY_MS)
                    delay(delayMs)
                } else {
                    return null
                }
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    internal fun parseResponse(response: String): ParsedYarnLabel? {
        val fields =
            response.trim().lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim() else "" to ""
            }

        // Tarkista onko edes jotain löytynyt
        val hasData = fields.values.any { it.isNotBlank() && it != "empty" }
        if (!hasData) return null

        return ParsedYarnLabel(
            brand = fields["BRAND"] ?: "",
            yarnName = fields["YARN_NAME"] ?: "",
            weightGrams = fields["WEIGHT_GRAMS"] ?: "",
            lengthMeters = fields["LENGTH_METERS"] ?: "",
            needleSize = fields["NEEDLE_SIZE"] ?: "",
            fiberContent = fields["FIBER"] ?: "",
            colorName = fields["COLOR_NAME"] ?: "",
            colorNumber = fields["COLOR_NUMBER"] ?: "",
            dyeLot = fields["DYE_LOT"] ?: "",
            weightCategory = fields["WEIGHT_CATEGORY"] ?: "",
            gaugeInfo = fields["GAUGE"] ?: "",
        )
    }
}
