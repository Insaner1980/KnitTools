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

    data class GaugeSwatch(
        val width: Double? = null,
        val stitches: Int? = null,
        val height: Double? = null,
        val rows: Int? = null,
    ) : ParsedInstruction()

    data class Failure(
        val reason: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
    ) : ParsedInstruction()

    enum class ErrorType { BUSY, QUOTA, UNAVAILABLE, PARSE_FAILED, UNKNOWN }
}

object InstructionParser {
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 500L
    private const val MAX_DELAY_MS = 8000L

    // Kattava Nano-prompt joka käsittelee kaikki yleisimmät englanninkieliset neulontaohjeet
    private val SYSTEM_PROMPT =
        """
        You are a knitting instruction parser. Extract numbers from the instruction.

        RULES:
        - Only output key:value lines. No explanations.
        - If you need to calculate (e.g. "increase to 108 from 96"), do the math: CHANGE = 108 - 96 = 12.
        - Abbreviations: st/sts = stitches, r = rows, inc = increase, dec = decrease, k2tog/ssk/p2tog = decrease.
        - "per 4 inches" and "per 10cm" both mean gauge units. Convert if needed (1 inch = 2.54cm).
        - TOLERATE TYPOS: "stiches" = stitches, "guage" = gauge, "increse" = increase, "mesured" = measured, "widht" = width, "hight" = height, "accross" = across. Always try to understand the intent even with spelling errors.

        FORMAT A — Increase or decrease stitches:
        Inputs like: "increase 12 stitches evenly across 96", "dec 8 sts over 120 sts",
        "increase evenly to 108 from 96", "k2tog every 12th st (96 sts)", "inc 1 st in every 8th st across 96"
        Respond:
        TYPE: INCREASE or DECREASE
        CURRENT: <current stitch count>
        CHANGE: <number of stitches to add or remove>

        FORMAT B — Gauge / tension:
        Inputs like: "22 sts and 30 rows = 10cm", "gauge: 5.5 sts per inch",
        "tension: 28 sts x 36 rows to 10cm on 4mm", "20 stitches = 4 inches",
        "gauge 22/30", "5 sts/inch, 7 rows/inch"
        Respond:
        GAUGE_STITCHES: <stitches per 10cm>
        GAUGE_ROWS: <rows per 10cm>
        If input is per inch, multiply by 4. If per 4 inches, use as-is for 10cm approximation.

        FORMAT C — Swatch measurement:
        Inputs like: "my swatch is 12cm wide with 26 stitches", "measured width is 30cm",
        "I got 24 sts in 10cm", "swatch: 13.5cm, 30 stitches, 15cm tall, 38 rows",
        "width 30 cm", "26 stitches over 12cm"
        Respond:
        SWATCH_WIDTH: <width in cm or inches, just the number>
        SWATCH_STITCHES: <stitch count>
        SWATCH_HEIGHT: <height in cm or inches, just the number>
        SWATCH_ROWS: <row count>
        Omit any line where the value is not mentioned.

        If nothing matches, respond: CANNOT_PARSE
        """.trimIndent()

    @Suppress("TooGenericExceptionCaught")
    suspend fun parse(instruction: String): ParsedInstruction {
        if (instruction.isBlank()) return ParsedInstruction.Failure("Empty instruction")

        val model: GenerativeModel
        try {
            model = Generation.getClient()
        } catch (_: Exception) {
            // Nano ei saatavilla — yritä regexiä suoraan
            return parseWithRegex(instruction.uppercase())
                .takeIf { it !is ParsedInstruction.Failure }
                ?: ParsedInstruction.Failure("unavailable", ParsedInstruction.ErrorType.UNAVAILABLE)
        }

        return try {
            ensureFeatureReady(model)
            val result = runWithRetry(model, instruction)
            val parsed = parseResponse(result)
            // Jos Nano-vastauksen parsinta epäonnistui, yritä suoraan käyttäjän syötettä
            if (parsed is ParsedInstruction.Failure) {
                val directParse = parseWithRegex(instruction.uppercase())
                if (directParse !is ParsedInstruction.Failure) return directParse
            }
            parsed
        } catch (e: GenAiException) {
            // Nano-virhe — yritä regexiä
            val directParse = parseWithRegex(instruction.uppercase())
            if (directParse !is ParsedInstruction.Failure) return directParse
            val msg = e.message?.lowercase() ?: ""
            when {
                msg.contains("quota") -> ParsedInstruction.Failure("quota", ParsedInstruction.ErrorType.QUOTA)
                msg.contains("busy") -> ParsedInstruction.Failure("busy", ParsedInstruction.ErrorType.BUSY)
                else -> ParsedInstruction.Failure(e.message ?: "unknown", ParsedInstruction.ErrorType.UNKNOWN)
            }
        } catch (_: Exception) {
            val directParse = parseWithRegex(instruction.uppercase())
            if (directParse !is ParsedInstruction.Failure) return directParse
            ParsedInstruction.Failure("unknown", ParsedInstruction.ErrorType.UNKNOWN)
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
                    // Latausvirhe ei estä käyttöä — malli voi olla jo osittain saatavilla
                }
            }

            FeatureStatus.AVAILABLE -> { /* Malli valmiina, ei toimenpiteitä */ }

            else -> {
                error("Feature unavailable")
            }
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
        throw lastException ?: error("Max retries exceeded")
    }

    // --- Nano-vastauksen parsinta (key:value) ---

    internal fun parseResponse(response: String): ParsedInstruction {
        val text = response.trim()
        val lines =
            text.lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim() else "" to ""
            }

        // Format A: Increase/Decrease
        if (lines.containsKey("TYPE") && lines.containsKey("CURRENT") && lines.containsKey("CHANGE")) {
            val isIncrease = lines["TYPE"]?.uppercase()?.contains("INCREASE") == true
            val current = lines["CURRENT"]?.filter { it.isDigit() }?.toIntOrNull()
            val change = lines["CHANGE"]?.filter { it.isDigit() }?.toIntOrNull()
            if (current != null && change != null) {
                return ParsedInstruction.IncreaseDecrease(current, change, isIncrease)
            }
        }

        // Format B: Gauge
        if (lines.containsKey("GAUGE_STITCHES") && lines.containsKey("GAUGE_ROWS")) {
            val stitches = lines["GAUGE_STITCHES"]?.toDoubleOrNull()
            val rows = lines["GAUGE_ROWS"]?.toDoubleOrNull()
            if (stitches != null && rows != null) {
                return ParsedInstruction.Gauge(stitches, rows)
            }
        }

        // Format C: Swatch
        val swW = lines["SWATCH_WIDTH"]?.toDoubleOrNull()
        val swS = lines["SWATCH_STITCHES"]?.toIntOrNull()
        val swH = lines["SWATCH_HEIGHT"]?.toDoubleOrNull()
        val swR = lines["SWATCH_ROWS"]?.toIntOrNull()
        if (swW != null || swS != null || swH != null || swR != null) {
            return ParsedInstruction.GaugeSwatch(width = swW, stitches = swS, height = swH, rows = swR)
        }

        // Vapaamuotoinen Nano-vastaus — yritä regexiä
        return parseWithRegex(text.uppercase())
    }

    // --- Typojen korjaus ennen regex-parsintaa ---

    private val TYPO_FIXES =
        listOf(
            // Stitches
            "STICHES" to "STITCHES",
            "STITCES" to "STITCHES",
            "STITHCES" to "STITCHES",
            "SITCHES" to "STITCHES",
            "STTCHES" to "STITCHES",
            "STTICHES" to "STITCHES",
            "STICHERS" to "STITCHES",
            "STICHS" to "STITCHES",
            "STICHTES" to "STITCHES",
            // Gauge
            "GUAGE" to "GAUGE",
            "GAGE" to "GAUGE",
            "GAUJE" to "GAUGE",
            "GUAGUE" to "GAUGE",
            // Increase
            "INCRESE" to "INCREASE",
            "INCREESE" to "INCREASE",
            "INCRASE" to "INCREASE",
            "INCERASE" to "INCREASE",
            "INSCREASE" to "INCREASE",
            "INCLEASE" to "INCREASE",
            // Decrease
            "DECRESE" to "DECREASE",
            "DECREESE" to "DECREASE",
            "DECRASE" to "DECREASE",
            "DEACREASE" to "DECREASE",
            "DESCREASE" to "DECREASE",
            // Measured
            "MESURED" to "MEASURED",
            "MEASURD" to "MEASURED",
            "MEASSURED" to "MEASURED",
            "MESAURED" to "MEASURED",
            // Width/Height
            "WIDHT" to "WIDTH",
            "WDITH" to "WIDTH",
            "WITDH" to "WIDTH",
            "HIGHT" to "HEIGHT",
            "HEIGTH" to "HEIGHT",
            "HEIGCHT" to "HEIGHT",
            // Across
            "ACCROSS" to "ACROSS",
            "ACORSS" to "ACROSS",
            // Rows
            "ROEWS" to "ROWS",
            "RWOS" to "ROWS",
            "ROWES" to "ROWS",
            // Tension
            "TENSHION" to "TENSION",
            "TENISON" to "TENSION",
            // Evenly
            "EVENLEY" to "EVENLY",
            "EVNELY" to "EVENLY",
            "EVANLY" to "EVENLY",
            // Swatch
            "SWACH" to "SWATCH",
            "SWTACH" to "SWATCH",
            "SWACTH" to "SWATCH",
            // Currently
            "CURRENLY" to "CURRENTLY",
            "CURENTLY" to "CURRENTLY",
        )

    private fun fixTypos(text: String): String {
        var result = text
        for ((typo, fix) in TYPO_FIXES) {
            result = result.replace(typo, fix)
        }
        return result
    }

    // --- Regex fallback — parsitaan suoraan englanninkielisestä tekstistä ---

    internal fun parseWithRegex(text: String): ParsedInstruction {
        val upper = fixTypos(text.uppercase())

        // Yritetään jokaista kategoriaa järjestyksessä
        return parseIncreaseDecrease(upper)
            ?: parseGauge(upper)
            ?: parseGaugeSwatch(upper)
            ?: ParsedInstruction.Failure("parse_failed", ParsedInstruction.ErrorType.PARSE_FAILED)
    }

    // --- Increase/Decrease -patternien tunnistus ---

    private fun parseIncreaseDecrease(upper: String): ParsedInstruction? {
        // "increase/decrease X stitches evenly across/over Y stitches"
        val incDecAcross =
            Regex("""(INCREASE|DECREASE|INC|DEC)\s+(\d+)\s*(?:STITCHES?|STS?)?.*?(?:ACROSS|OVER|FROM|IN|ON)\s+(\d+)""")
        incDecAcross.find(upper)?.let { m ->
            val isInc = m.groupValues[1].startsWith("INC")
            val change = m.groupValues[2].toIntOrNull()
            val current = m.groupValues[3].toIntOrNull()
            if (change != null && current != null) return ParsedInstruction.IncreaseDecrease(current, change, isInc)
        }

        // "increase/decrease to X from Y" tai "increase to X stitches (currently Y)"
        val incDecTo =
            Regex("""(INCREASE|DECREASE|INC|DEC)\s+(?:EVENLY\s+)?TO\s+(\d+).*?(?:FROM|CURRENTLY|NOW)\s+(\d+)""")
        incDecTo.find(upper)?.let { m ->
            val isInc = m.groupValues[1].startsWith("INC")
            val target = m.groupValues[2].toIntOrNull()
            val current = m.groupValues[3].toIntOrNull()
            if (target != null && current != null) {
                val change = kotlin.math.abs(target - current)
                return ParsedInstruction.IncreaseDecrease(current, change, isInc)
            }
        }

        // "Y stitches, increase/decrease X" (käänteinen)
        val incDecReverse =
            Regex("""(\d+)\s*(?:STITCHES?|STS?).*?(INCREASE|DECREASE|INC|DEC)\s+(\d+)""")
        incDecReverse.find(upper)?.let { m ->
            val current = m.groupValues[1].toIntOrNull()
            val isInc = m.groupValues[2].startsWith("INC")
            val change = m.groupValues[3].toIntOrNull()
            if (current != null && change != null) return ParsedInstruction.IncreaseDecrease(current, change, isInc)
        }

        // "k2tog every Nth stitch (Y sts)" — implisiittinen decrease
        val k2togPattern =
            Regex(
                """(?:K2TOG|SSK|P2TOG|SK2P|S2KP)\s+(?:EVERY|EACH)\s+(\d+)\w*\s+(?:ST|STITCH).*?(\d+)\s*(?:STS?|STITCHES?)""",
            )
        k2togPattern.find(upper)?.let { m ->
            val every = m.groupValues[1].toIntOrNull()
            val total = m.groupValues[2].toIntOrNull()
            if (every != null && total != null && every > 0) {
                val decreases = total / every
                return ParsedInstruction.IncreaseDecrease(total, decreases, false)
            }
        }

        return null
    }

    // --- Gauge-patternien tunnistus ---

    private fun parseGauge(upper: String): ParsedInstruction? =
        parseGaugeStandard(upper)
            ?: parseGaugeTension(upper)
            ?: parseGaugePerInch(upper)
            ?: parseGaugeBareNumbers(upper)
            ?: parseGaugeFallback(upper)

    // "X stitches/sts and Y rows per 10cm/4in" tai "X sts, Y rows = 10cm"
    private fun parseGaugeStandard(upper: String): ParsedInstruction.Gauge? {
        val pattern =
            Regex(
                """(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:AND|,|&|X|/)\s*(\d+(?:\.\d+)?)\s*(?:ROWS?|R)\s*(?:=|PER|TO|OVER|IN)?\s*(?:10\s*CM|4\s*IN|4\s*INCHES?)""",
            )
        val m = pattern.find(upper) ?: return null
        val stitches = m.groupValues[1].toDoubleOrNull() ?: return null
        val rows = m.groupValues[2].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(stitches, rows)
    }

    // "tension/gauge: X sts x Y rows to 10cm on Xmm needles"
    private fun parseGaugeTension(upper: String): ParsedInstruction.Gauge? {
        val pattern =
            Regex(
                """(?:TENSION|GAUGE)\s*:?\s*(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:AND|,|&|X|/)\s*(\d+(?:\.\d+)?)\s*(?:ROWS?|R)""",
            )
        val m = pattern.find(upper) ?: return null
        val stitches = m.groupValues[1].toDoubleOrNull() ?: return null
        val rows = m.groupValues[2].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(stitches, rows)
    }

    // "X sts/inch" tai "X stitches per inch" — kertaa 4:llä (≈ per 10cm)
    private fun parseGaugePerInch(upper: String): ParsedInstruction.Gauge? {
        val piSt =
            Regex("""(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:PER|/)\s*(?:INCH|IN)\b""").find(upper) ?: return null
        val piRow = Regex("""(\d+(?:\.\d+)?)\s*(?:ROWS?|R)\s*(?:PER|/)\s*(?:INCH|IN)\b""").find(upper) ?: return null
        val stitches = piSt.groupValues[1].toDoubleOrNull() ?: return null
        val rows = piRow.groupValues[1].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(stitches * 4, rows * 4)
    }

    // "gauge X/Y" tai "gauge: X, Y" — pelkät kaksi lukua gauge-kontekstissa
    private fun parseGaugeBareNumbers(upper: String): ParsedInstruction.Gauge? {
        if (!upper.contains("GAUGE") && !upper.contains("TENSION")) return null
        val numbers =
            Regex("""(\d+(?:\.\d+)?)""")
                .findAll(upper)
                .mapNotNull { it.groupValues[1].toDoubleOrNull() }
                .toList()
        if (numbers.size < 2) return null
        return ParsedInstruction.Gauge(numbers[0], numbers[1])
    }

    // "X sts and Y rows" ilman kontekstia — vain jos ei ole inc/dec avainsanoja
    private fun parseGaugeFallback(upper: String): ParsedInstruction.Gauge? {
        if (
            upper.contains("INCREASE") ||
            upper.contains("DECREASE") ||
            upper.contains("INC ") ||
            upper.contains("DEC ")
        ) {
            return null
        }
        val stM = Regex("""(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)""").find(upper) ?: return null
        val rowM = Regex("""(\d+(?:\.\d+)?)\s*(?:ROWS?|R\b)""").find(upper) ?: return null
        val stitches = stM.groupValues[1].toDoubleOrNull() ?: return null
        val rows = rowM.groupValues[1].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(stitches, rows)
    }

    // --- Swatch-patternien tunnistus ---

    private fun parseGaugeSwatch(upper: String): ParsedInstruction? {
        // "width/wide X", "measured width is X"
        val widthP = Regex("""(?:WIDTH|WIDE|MEASURED\s*(?:WIDTH)?)\s*(?:IS|=|:)?\s*(\d+(?:\.\d+)?)""")
        // "height/tall/long X"
        val heightP = Regex("""(?:HEIGHT|TALL|LONG|MEASURED\s*HEIGHT)\s*(?:IS|=|:)?\s*(\d+(?:\.\d+)?)""")
        // "X stitches/sts" (swatch-kontekstissa)
        val swStP = Regex("""(\d+)\s*(?:STITCHES?|STS?)""")
        // "X rows" (swatch-kontekstissa)
        val swRowP = Regex("""(\d+)\s*ROWS?""")

        val swW =
            widthP
                .find(upper)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        val swH =
            heightP
                .find(upper)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()

        // Swatch: vähintään leveys tai korkeus + silmukat/rivit
        if (swW != null || swH != null) {
            val swSt =
                swStP
                    .find(upper)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            val swR =
                swRowP
                    .find(upper)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            return ParsedInstruction.GaugeSwatch(width = swW, stitches = swSt, height = swH, rows = swR)
        }

        // "swatch: X cm, Y stitches" tai "my swatch is X cm with Y stitches"
        if (upper.contains("SWATCH") || upper.contains("MEASURED")) {
            val swSt =
                swStP
                    .find(upper)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            val swR =
                swRowP
                    .find(upper)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            // Ensimmäinen cm/in-luku on leveys
            val cmOrIn = Regex("""(\d+(?:\.\d+)?)\s*(?:CM|IN|INCHES?)""").find(upper)
            val width = cmOrIn?.groupValues?.get(1)?.toDoubleOrNull()
            if (width != null || swSt != null || swR != null) {
                return ParsedInstruction.GaugeSwatch(width = width, stitches = swSt, height = null, rows = swR)
            }
        }

        // "X stitches over/in Y cm" — lyhyt swatch-muoto
        val stsOverCm =
            Regex("""(\d+)\s*(?:STITCHES?|STS?)\s+(?:OVER|IN|ACROSS)\s+(\d+(?:\.\d+)?)\s*(?:CM|IN|INCHES?)""")
        stsOverCm.find(upper)?.let { m ->
            val sts = m.groupValues[1].toIntOrNull()
            val width = m.groupValues[2].toDoubleOrNull()
            if (sts != null && width != null) return ParsedInstruction.GaugeSwatch(width = width, stitches = sts)
        }

        // "I got X sts in Y cm"
        val gotSts =
            Regex(
                """(?:GOT|GET|GETTING|HAVE)\s+(\d+)\s*(?:STITCHES?|STS?)\s+(?:IN|OVER|PER|ACROSS)\s+(\d+(?:\.\d+)?)""",
            )
        gotSts.find(upper)?.let { m ->
            val sts = m.groupValues[1].toIntOrNull()
            val width = m.groupValues[2].toDoubleOrNull()
            if (sts != null && width != null) return ParsedInstruction.GaugeSwatch(width = width, stitches = sts)
        }

        return null
    }
}
