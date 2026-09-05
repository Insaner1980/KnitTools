package com.finnvek.knittools.domain.calculator

sealed class ParsedInstruction {
    enum class GaugeUnit { PER_10_CM, PER_4_INCHES }

    enum class LengthUnit { CM, INCHES }

    data class IncreaseDecrease(
        val currentStitches: Int,
        val changeBy: Int,
        val isIncrease: Boolean,
    ) : ParsedInstruction()

    data class Gauge(
        val stitchesPer10cm: Double,
        val rowsPer10cm: Double,
        val unit: GaugeUnit = GaugeUnit.PER_10_CM,
    ) : ParsedInstruction()

    data class GaugeSwatch(
        val width: Double? = null,
        val stitches: Int? = null,
        val height: Double? = null,
        val rows: Int? = null,
        val lengthUnit: LengthUnit? = null,
    ) : ParsedInstruction()

    data class Failure(
        val reason: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
    ) : ParsedInstruction()

    enum class ErrorType { PARSE_FAILED, UNKNOWN }
}

object InstructionParser {
    private val gaugeStitchesPattern = Regex("""(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)""")
    private val gaugeRowsPattern = Regex("""^\s*(?:AND|[,/&X])?\s*(\d+(?:\.\d+)?)\s*(?:ROWS?|R\b)""")

    fun parse(instruction: String): ParsedInstruction = parseWithRegex(instruction)

    // --- Avain-arvo-vastauksen parsinta (key:value) ---

    internal fun parseResponse(response: String): ParsedInstruction {
        oversizedInputFailure(response)?.let { return it }
        val text = response.trim()
        return parseKeyValueResponse(text) ?: parseWithRegex(text.uppercase())
    }

    private fun parseKeyValueResponse(text: String): ParsedInstruction? {
        val lines = parseKeyValueLines(text)

        parseIncreaseDecreaseResponse(lines)?.let { return it }
        parseGaugeResponse(lines)?.let { return it }
        return parseGaugeSwatchResponse(lines)
    }

    private fun parseKeyValueLines(text: String): Map<String, String> {
        val values = linkedMapOf<String, String>()
        text.lines().forEach { line ->
            val delimiter = line.indexOf(':')
            if (delimiter < 0) return@forEach
            val key = line.substring(0, delimiter).trim().uppercase()
            if (key in values) return emptyMap()
            values[key] = line.substring(delimiter + 1).trim()
        }
        return values
    }

    private fun parseIncreaseDecreaseResponse(lines: Map<String, String>): ParsedInstruction.IncreaseDecrease? {
        if (lines.containsKey("TYPE") && lines.containsKey("CURRENT") && lines.containsKey("CHANGE")) {
            val isIncrease = lines["TYPE"]?.uppercase()?.contains("INCREASE") == true
            val current = lines["CURRENT"]?.filter { it.isDigit() }?.toIntOrNull()
            val change = lines["CHANGE"]?.filter { it.isDigit() }?.toIntOrNull()
            if (current != null && change != null) {
                return ParsedInstruction.IncreaseDecrease(current, change, isIncrease)
            }
        }
        return null
    }

    private fun parseGaugeResponse(lines: Map<String, String>): ParsedInstruction.Gauge? {
        if (lines.containsKey("GAUGE_STITCHES") && lines.containsKey("GAUGE_ROWS")) {
            val stitches = lines["GAUGE_STITCHES"]?.toDoubleOrNull()
            val rows = lines["GAUGE_ROWS"]?.toDoubleOrNull()
            if (stitches != null && rows != null) {
                return ParsedInstruction.Gauge(
                    stitchesPer10cm = stitches,
                    rowsPer10cm = rows,
                    unit = parseGaugeUnit(lines["GAUGE_UNIT"]),
                )
            }
        }
        return null
    }

    private fun parseGaugeSwatchResponse(lines: Map<String, String>): ParsedInstruction.GaugeSwatch? {
        val swW = lines["SWATCH_WIDTH"]?.toDoubleOrNull()
        val swS = lines["SWATCH_STITCHES"]?.toIntOrNull()
        val swH = lines["SWATCH_HEIGHT"]?.toDoubleOrNull()
        val swR = lines["SWATCH_ROWS"]?.toIntOrNull()
        if (swW != null || swS != null || swH != null || swR != null) {
            return ParsedInstruction.GaugeSwatch(
                width = swW,
                stitches = swS,
                height = swH,
                rows = swR,
                lengthUnit = parseLengthUnit(lines["SWATCH_UNIT"]),
            )
        }
        return null
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

    private val typoRegexes =
        TYPO_FIXES.map { (typo, fix) ->
            Regex("""\b${Regex.escape(typo)}\b""") to fix
        }

    private fun fixTypos(text: String): String {
        var result = text
        for ((pattern, fix) in typoRegexes) {
            result = pattern.replace(result, fix)
        }
        return result
    }

    // --- Regex fallback — parsitaan suoraan englanninkielisestä tekstistä ---

    internal fun parseWithRegex(text: String): ParsedInstruction {
        oversizedInputFailure(text)?.let { return it }
        val upper = fixTypos(text.uppercase())

        // Yritetään jokaista kategoriaa järjestyksessä
        return parseIncreaseDecrease(upper)
            ?: parseGauge(upper)
            ?: parseGaugeSwatch(upper)
            ?: ParsedInstruction.Failure("parse_failed", ParsedInstruction.ErrorType.PARSE_FAILED)
    }

    // --- Increase/Decrease -patternien tunnistus ---

    @Suppress(
        "CyclomaticComplexMethod",
        "ReturnCount",
        "kotlin:S5843",
    ) // Fallback-parseri tunnistaa useita neuleohjeiden kirjoitusasuja yhdellä lausekkeella.
    private fun parseIncreaseDecrease(upper: String): ParsedInstruction? =
        parseIncreaseEveryNth(upper)
            ?: parseIncreaseDecreaseAcross(upper)
            ?: parseIncreaseDecreaseToTarget(upper)
            ?: parseIncreaseDecreaseReverse(upper)
            ?: parseImplicitDecreaseEveryNth(upper)

    private fun parseIncreaseEveryNth(upper: String): ParsedInstruction.IncreaseDecrease? {
        val incEveryNth =
            Regex(
                """(?:INCREASE|INC)\s+1\s*(?:STITCH|ST|STS?)\s+(?:IN|ON)\s+EVERY\s+(\d+)\w*\s+(?:ST|STITCH).*?(?:ACROSS|OVER)\s+(\d+)""",
            )
        // CPD-OFF: Erilliset kielioppisaannot pidetaan eksplisiittisina.
        val match = incEveryNth.find(upper) ?: return null
        val every = match.groupValues[1].toIntOrNull()
        val total = match.groupValues[2].toIntOrNull()
        if (every == null || total == null || every <= 0) return null
        return ParsedInstruction.IncreaseDecrease(total, total / every, true)
        // CPD-ON
    }

    private fun parseIncreaseDecreaseAcross(upper: String): ParsedInstruction.IncreaseDecrease? {
        // "increase/decrease X stitches evenly across/over Y stitches"
        val incDecAcross =
            Regex(
                """(INCREASE|DECREASE|INC|DEC)\s+(\d+).*?(?:ACROSS|OVER|FROM|IN|ON)\s+(\d+)\s*(?:STITCHES?|STS?)""",
            )
        val match = incDecAcross.find(upper) ?: return null
        val isIncrease = match.groupValues[1].startsWith("INC")
        val change = match.groupValues[2].toIntOrNull() ?: return null
        val current = match.groupValues[3].toIntOrNull() ?: return null
        return ParsedInstruction.IncreaseDecrease(current, change, isIncrease)
    }

    private fun parseIncreaseDecreaseToTarget(upper: String): ParsedInstruction.IncreaseDecrease? {
        // "increase/decrease to X from Y" tai "increase to X stitches (currently Y)"
        val incDecTo =
            Regex("""(INCREASE|DECREASE|INC|DEC)\s+(?:EVENLY\s+)?TO\s+(\d+).*?(?:FROM|CURRENTLY|NOW)\s+(\d+)""")
        val match = incDecTo.find(upper) ?: return null
        val isIncrease = match.groupValues[1].startsWith("INC")
        val target = match.groupValues[2].toIntOrNull() ?: return null
        val current = match.groupValues[3].toIntOrNull() ?: return null
        val delta = target - current
        if (delta == 0 || (delta > 0) != isIncrease) return null
        return ParsedInstruction.IncreaseDecrease(current, kotlin.math.abs(delta), isIncrease)
    }

    private fun parseIncreaseDecreaseReverse(upper: String): ParsedInstruction.IncreaseDecrease? {
        // "Y stitches, increase/decrease X" (käänteinen)
        val incDecReverse =
            Regex("""(\d+)\s*(?:STITCHES?|STS?).*?(INCREASE|DECREASE|INC|DEC)\s+(\d+)""")
        val match = incDecReverse.find(upper) ?: return null
        val current = match.groupValues[1].toIntOrNull() ?: return null
        val isIncrease = match.groupValues[2].startsWith("INC")
        val change = match.groupValues[3].toIntOrNull() ?: return null
        return ParsedInstruction.IncreaseDecrease(current, change, isIncrease)
    }

    private fun parseImplicitDecreaseEveryNth(upper: String): ParsedInstruction.IncreaseDecrease? {
        // "k2tog every Nth stitch (Y sts)" — implisiittinen decrease
        val k2togPattern =
            Regex(
                """(?:K2TOG|SSK|P2TOG|SK2P|S2KP)\s+(?:EVERY|EACH)\s+(\d+)\w*\s+(?:ST|STITCH).*?(\d+)\s*(?:STS?|STITCHES?)""",
            )
        val match = k2togPattern.find(upper) ?: return null
        val every = match.groupValues[1].toIntOrNull()
        val total = match.groupValues[2].toIntOrNull()
        if (every == null || total == null || every <= 0) return null
        return ParsedInstruction.IncreaseDecrease(total, total / every, false)
    }

    // --- Gauge-patternien tunnistus ---

    private fun parseGauge(upper: String): ParsedInstruction? =
        parseGaugeStandard(upper)
            ?: parseGaugeTension(upper)
            ?: parseGaugePerInch(upper)
            ?: parseGaugeFallback(upper)
            ?: parseGaugeBareNumbers(upper)

    // "X stitches/sts and Y rows per 10cm/4in" tai "X sts, Y rows = 10cm"
    @Suppress("kotlin:S5843") // Gauge-fallback hyväksyy useita "sts/rows per 10 cm / 4 in" -muotoja.
    private fun parseGaugeStandard(upper: String): ParsedInstruction.Gauge? {
        val pattern =
            Regex(
                // CPD-OFF: Erilliset kielioppisaannot pidetaan eksplisiittisina.
                """(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:AND|,|&|X|/)\s*(\d+(?:\.\d+)?)\s*(?:ROWS?|R)\s*(?:=|PER|TO|OVER|IN)?\s*(10\s*CM|4\s*IN(?:CHES?)?)""",
            )
        val m = pattern.find(upper) ?: return null
        val stitches = m.groupValues[1].toDoubleOrNull() ?: return null
        val rows = m.groupValues[2].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches,
            rowsPer10cm = rows,
            unit = parseGaugeUnit(m.groupValues[3]),
            // CPD-ON
        )
    }

    // "tension/gauge: X sts x Y rows to 10cm on Xmm needles"
    @Suppress("kotlin:S5843") // Tension/gauge-teksti vaihtelee lähteittäin, joten regex on tarkoituksella salliva.
    private fun parseGaugeTension(upper: String): ParsedInstruction.Gauge? {
        val pattern =
            Regex(
                """(?:TENSION|GAUGE)\s*:?\s*(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:AND|,|&|X|/)\s*(\d+(?:\.\d+)?)\s*(?:ROWS?|R)""",
            )
        val m = pattern.find(upper) ?: return null
        val stitches = m.groupValues[1].toDoubleOrNull() ?: return null
        val rows = m.groupValues[2].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches,
            rowsPer10cm = rows,
            unit = detectGaugeUnit(upper),
        )
    }

    // "X sts/inch" tai "X stitches per inch" — muunnetaan per 4in -arvoksi, joka vastaa screenin imperial-yksikköä
    private fun parseGaugePerInch(upper: String): ParsedInstruction.Gauge? {
        val piSt =
            Regex("""(\d+(?:\.\d+)?)\s*(?:STITCHES?|STS?)\s*(?:PER|/)\s*(?:INCH|IN)\b""").find(upper) ?: return null
        val piRow = Regex("""(\d+(?:\.\d+)?)\s*(?:ROWS?|R)\s*(?:PER|/)\s*(?:INCH|IN)\b""").find(upper) ?: return null
        val stitches = piSt.groupValues[1].toDoubleOrNull() ?: return null
        val rows = piRow.groupValues[1].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches * 4,
            rowsPer10cm = rows * 4,
            unit = ParsedInstruction.GaugeUnit.PER_4_INCHES,
        )
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
        return ParsedInstruction.Gauge(
            stitchesPer10cm = numbers[0],
            rowsPer10cm = numbers[1],
            unit = detectGaugeUnit(upper),
        )
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
        val stitchesMatch = gaugeStitchesPattern.find(upper) ?: return null
        val textAfterStitches = upper.substring(stitchesMatch.range.last + 1)
        val rowsMatch = gaugeRowsPattern.find(textAfterStitches) ?: return null
        val stitches = stitchesMatch.groupValues[1].toDoubleOrNull() ?: return null
        val rows = rowsMatch.groupValues[1].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches,
            rowsPer10cm = rows,
            unit = detectGaugeUnit(upper),
        )
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
            return ParsedInstruction.GaugeSwatch(
                width = swW,
                stitches = swSt,
                height = swH,
                rows = swR,
                lengthUnit = detectLengthUnit(upper),
            )
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
                return ParsedInstruction.GaugeSwatch(
                    width = width,
                    stitches = swSt,
                    height = null,
                    rows = swR,
                    lengthUnit = detectLengthUnit(upper),
                )
            }
        }

        // "X stitches over/in Y cm" — lyhyt swatch-muoto
        val stsOverCm =
            Regex("""(\d+)\s*(?:STITCHES?|STS?)\s+(?:OVER|IN|ACROSS)\s+(\d+(?:\.\d+)?)\s*(?:CM|IN|INCHES?)""")
        // CPD-OFF: Erilliset kielioppisaannot pidetaan eksplisiittisina.
        stsOverCm.find(upper)?.let { m ->
            val sts = m.groupValues[1].toIntOrNull()
            val width = m.groupValues[2].toDoubleOrNull()
            if (sts != null && width != null) {
                return ParsedInstruction.GaugeSwatch(
                    width = width,
                    stitches = sts,
                    lengthUnit = detectLengthUnit(m.value),
                    // CPD-ON
                )
            }
        }

        // "I got X sts in Y cm"
        val gotSts =
            Regex(
                """(?:GOT|GET|GETTING|HAVE)\s+(\d+)\s*(?:STITCHES?|STS?)\s+(?:IN|OVER|PER|ACROSS)\s+(\d+(?:\.\d+)?)""",
            )
        gotSts.find(upper)?.let { m ->
            val sts = m.groupValues[1].toIntOrNull()
            val width = m.groupValues[2].toDoubleOrNull()
            if (sts != null && width != null) {
                return ParsedInstruction.GaugeSwatch(
                    width = width,
                    stitches = sts,
                    lengthUnit = detectLengthUnit(upper),
                )
            }
        }

        return null
    }

    private fun detectGaugeUnit(text: String): ParsedInstruction.GaugeUnit =
        if (text.contains("4 IN")) {
            ParsedInstruction.GaugeUnit.PER_4_INCHES
        } else {
            ParsedInstruction.GaugeUnit.PER_10_CM
        }

    private fun detectLengthUnit(text: String): ParsedInstruction.LengthUnit? =
        when {
            text.contains("CM") -> ParsedInstruction.LengthUnit.CM
            text.contains("INCH") || Regex("""\bIN\b""").containsMatchIn(text) -> ParsedInstruction.LengthUnit.INCHES
            else -> null
        }

    private fun parseGaugeUnit(value: String?): ParsedInstruction.GaugeUnit =
        if (value?.contains("4", ignoreCase = true) == true) {
            ParsedInstruction.GaugeUnit.PER_4_INCHES
        } else {
            ParsedInstruction.GaugeUnit.PER_10_CM
        }

    private fun parseLengthUnit(value: String?): ParsedInstruction.LengthUnit? =
        when {
            value == null -> null
            value.contains("CM", ignoreCase = true) -> ParsedInstruction.LengthUnit.CM
            value.contains("IN", ignoreCase = true) -> ParsedInstruction.LengthUnit.INCHES
            else -> null
        }

    private fun oversizedInputFailure(text: String): ParsedInstruction.Failure? =
        if (text.length > MAX_INPUT_LENGTH) {
            ParsedInstruction.Failure("input_too_long", ParsedInstruction.ErrorType.PARSE_FAILED)
        } else {
            null
        }

    private const val MAX_INPUT_LENGTH = 10_000
}
