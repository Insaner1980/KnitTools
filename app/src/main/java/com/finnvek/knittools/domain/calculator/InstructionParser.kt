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
    private const val NUMBER_START = """(?<![\d.\-])"""
    private const val DECIMAL = """\d++(?:\.\d++)?+"""
    private const val INTEGER = """\d++(?!\.\d)"""
    private const val STITCHES = """(?:STITCH(?:ES)?|STS?)\b"""
    private const val ROWS = """(?:ROWS?|R)\b"""
    private const val LENGTH_UNIT = """(?:CM|IN(?:CH(?:ES)?)?)\b"""
    private const val PER_INCH = """(?:PER|/)\s*(?:INCH|IN)\b"""
    private const val EVERY_NTH_CHANGE =
        """\b(?<action>INCREASE|DECREASE|INC|DEC)\s+1\s*$STITCHES\s+""" +
            """(?:(?:IN|ON)\s+)?EVERY\s+(?<every>$INTEGER)\w*\s+$STITCHES"""

    private val everyNthWithTrailingCount =
        Regex("""$EVERY_NTH_CHANGE.*?(?:ACROSS|OVER)\s+(?<total>$INTEGER)(?:\s*$STITCHES|(?=\s*(?:[,.;)]|$)))""")
    private val everyNthWithLeadingCount =
        Regex("""$NUMBER_START(?<total>$INTEGER)\s*$STITCHES\s*+[,;:]?\s*+$EVERY_NTH_CHANGE""")
    private val explicitGaugeBasis = Regex("""(?:\b(?:PER|OVER|TO|IN|ACROSS)\s+|[=/]\s*)($DECIMAL)\s*($LENGTH_UNIT)""")
    private val gaugeStitchesPattern = Regex("""$NUMBER_START($DECIMAL)\s*$STITCHES""")
    private val gaugeRowsPattern = Regex("""^\s*(?:AND|[,/&X])?\s*($DECIMAL)\s*$ROWS""")
    private val perInchStitches = Regex("""$NUMBER_START($DECIMAL)\s*$STITCHES\s*$PER_INCH""")
    private val perInchRows = Regex("""$NUMBER_START($DECIMAL)\s*$ROWS\s*$PER_INCH""")
    private val bareGaugePair = Regex("""\b(?:GAUGE|TENSION)\s*+:?\s*+($DECIMAL)\s*(?:[/,X]|PER\b)?\s*($DECIMAL)""")

    fun parse(instruction: String): ParsedInstruction = parseWithRegex(instruction)

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
            ?: ParsedInstruction.Failure("parse_failed", ParsedInstruction.ErrorType.PARSE_FAILED)
    }

    // --- Increase/Decrease -patternien tunnistus ---

    @Suppress(
        "CyclomaticComplexMethod",
        "ReturnCount",
        "kotlin:S5843",
    ) // Fallback-parseri tunnistaa useita neuleohjeiden kirjoitusasuja yhdellä lausekkeella.
    private fun parseIncreaseDecrease(upper: String): ParsedInstruction? =
        parseIncreaseDecreaseEveryNth(upper)
            ?: parseIncreaseDecreaseAcross(upper)
            ?: parseIncreaseDecreaseToTarget(upper)
            ?: parseIncreaseDecreaseReverse(upper)
            ?: parseImplicitDecreaseEveryNth(upper)

    private fun parseIncreaseDecreaseEveryNth(upper: String): ParsedInstruction.IncreaseDecrease? {
        val match = everyNthWithTrailingCount.find(upper) ?: everyNthWithLeadingCount.find(upper) ?: return null
        val every = match.groups["every"]?.value?.toIntOrNull()
        val total = match.groups["total"]?.value?.toIntOrNull()
        if (every == null || total == null || every <= 0) return null
        val isIncrease = match.groups["action"]?.value?.startsWith("INC") == true
        return ParsedInstruction.IncreaseDecrease(total, total / every, isIncrease)
    }

    private fun parseIncreaseDecreaseAcross(upper: String): ParsedInstruction.IncreaseDecrease? {
        // "increase/decrease X stitches evenly across/over Y stitches"
        val incDecAcross =
            Regex(
                """\b(INCREASE|DECREASE|INC|DEC)\s+($INTEGER).*?(?:ACROSS|OVER|FROM|IN|ON)\s+($INTEGER)\s*$STITCHES""",
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
            Regex(
                """\b(INCREASE|DECREASE|INC|DEC)\s+(?:EVENLY\s+)?TO\s+($INTEGER).*?(?:FROM|CURRENTLY|NOW)\s+($INTEGER)""",
            )
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
            Regex("""$NUMBER_START($INTEGER)\s*$STITCHES.*?\b(INCREASE|DECREASE|INC|DEC)\s+($INTEGER)""")
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
                """\b(K2TOG|SSK|P2TOG|SK2P|S2KP)\s+(?:EVERY|EACH)\s+($INTEGER)\w*\s+$STITCHES.*?$NUMBER_START($INTEGER)\s*$STITCHES""",
            )
        val match = k2togPattern.find(upper) ?: return null
        val every = match.groupValues[2].toIntOrNull()
        val total = match.groupValues[3].toIntOrNull()
        if (every == null || total == null || every <= 0) return null
        val stitchesRemoved = if (match.groupValues[1] in setOf("SK2P", "S2KP")) 2L else 1L
        val change = total / every * stitchesRemoved
        if (change > Int.MAX_VALUE) return null
        return ParsedInstruction.IncreaseDecrease(total, change.toInt(), false)
    }

    // --- Gauge-patternien tunnistus ---

    private fun parseGauge(upper: String): ParsedInstruction? {
        val explicit = parseGaugeStandard(upper) ?: parseGaugePerInch(upper) ?: parseGaugeSwatch(upper)
        if (explicit != null) return explicit
        if (explicitGaugeBasis.findAll(upper).any { basis ->
                val standardLength = if (basis.groupValues[2] == "CM") 10.0 else 4.0
                basis.groupValues[1].toDoubleOrNull() != standardLength
            }
        ) {
            return null
        }
        return parseGaugeTension(upper) ?: parseGaugeFallback(upper) ?: parseGaugeBareNumbers(upper)
    }

    // "X stitches/sts and Y rows per 10cm/4in" tai "X sts, Y rows = 10cm"
    @Suppress("kotlin:S5843") // Gauge-fallback hyväksyy useita "sts/rows per 10 cm / 4 in" -muotoja.
    private fun parseGaugeStandard(upper: String): ParsedInstruction.Gauge? {
        val pattern =
            Regex(
                // CPD-OFF: Erilliset kielioppisaannot pidetaan eksplisiittisina.
                """$NUMBER_START($DECIMAL)\s*$STITCHES\s*(?:AND|,|&|X|/)\s*($DECIMAL)\s*$ROWS""" +
                    """\s*(?:(?:=|PER|TO|OVER|IN)\s*)?(10\s*CM|4\s*IN(?:CH(?:ES)?)?)\b""",
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
                """\b(?:TENSION|GAUGE)\s*(?::\s*)?($DECIMAL)\s*$STITCHES\s*(?:AND|,|&|X|/)\s*($DECIMAL)\s*$ROWS""",
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
        val sharedUnit =
            Regex(
                """$NUMBER_START($DECIMAL)\s*$STITCHES\s*(?:AND|,|&|X|/)\s*($DECIMAL)\s*$ROWS\s*$PER_INCH""",
            ).find(upper)
        val piSt = perInchStitches.find(upper)
        val piRow = perInchRows.find(upper)
        val stitches = (sharedUnit?.groupValues?.get(1) ?: piSt?.groupValues?.get(1))?.toDoubleOrNull() ?: return null
        val rows = (sharedUnit?.groupValues?.get(2) ?: piRow?.groupValues?.get(1))?.toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches * 4,
            rowsPer10cm = rows * 4,
            unit = ParsedInstruction.GaugeUnit.PER_4_INCHES,
        )
    }

    // "gauge X/Y" tai "gauge: X, Y" — pelkät kaksi lukua gauge-kontekstissa
    private fun parseGaugeBareNumbers(upper: String): ParsedInstruction.Gauge? {
        if (perInchStitches.containsMatchIn(upper) || perInchRows.containsMatchIn(upper)) return null
        val match = bareGaugePair.find(upper) ?: return null
        val stitches = match.groupValues[1].toDoubleOrNull() ?: return null
        val rows = match.groupValues[2].toDoubleOrNull() ?: return null
        return ParsedInstruction.Gauge(
            stitchesPer10cm = stitches,
            rowsPer10cm = rows,
            unit = detectGaugeUnit(upper.substring(match.range.last + 1)),
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

    private fun parseMeasuredSwatch(upper: String): ParsedInstruction? {
        // "width/wide X", "measured width is X"
        val widthP =
            Regex("""\b(?:WIDTH|WIDE)\s*(?:(?:IS|=|:)\s*)?($DECIMAL)(?:\s*($LENGTH_UNIT))?""")
        val measuredWidthP = Regex("""\bMEASURED\s*(?:(?:IS|=|:)\s*)?($DECIMAL)\s*($LENGTH_UNIT)""")
        // "height/tall/long X"
        val heightP = Regex("""\b(?:HEIGHT|TALL|LONG)\s*(?:(?:IS|=|:)\s*)?($DECIMAL)(?:\s*($LENGTH_UNIT))?""")
        // "X stitches/sts" (swatch-kontekstissa)
        val swStP = Regex("""$NUMBER_START($INTEGER)\s*$STITCHES""")
        // "X rows" (swatch-kontekstissa)
        val swRowP = Regex("""$NUMBER_START($INTEGER)\s*$ROWS""")

        val widthMatch = widthP.find(upper) ?: measuredWidthP.find(upper)
        val heightMatch = heightP.find(upper)
        val swW = widthMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val swH = heightMatch?.groupValues?.get(1)?.toDoubleOrNull()

        // Swatch: vähintään leveys tai korkeus + silmukat/rivit
        if (swW != null || swH != null) {
            val swSt = swStP.firstGroupInt(upper)
            val swR = swRowP.firstGroupInt(upper)
            val widthUnit = widthMatch?.groupValues?.get(2)?.let(::detectLengthUnit)
            val heightUnit = heightMatch?.groupValues?.get(2)?.let(::detectLengthUnit)
            val useCentimeters = ParsedInstruction.LengthUnit.CM in listOf(widthUnit, heightUnit)
            return ParsedInstruction.GaugeSwatch(
                width =
                    if (useCentimeters &&
                        widthUnit == ParsedInstruction.LengthUnit.INCHES
                    ) {
                        swW?.times(2.54)
                    } else {
                        swW
                    },
                stitches = swSt,
                height =
                    if (useCentimeters &&
                        heightUnit == ParsedInstruction.LengthUnit.INCHES
                    ) {
                        swH?.times(2.54)
                    } else {
                        swH
                    },
                rows = swR,
                lengthUnit =
                    if (useCentimeters) {
                        ParsedInstruction.LengthUnit.CM
                    } else {
                        widthUnit ?: heightUnit ?: detectLengthUnit(upper)
                    },
            )
        }

        return null
    }

    private fun parseGaugeSwatch(upper: String): ParsedInstruction? {
        parseMeasuredSwatch(upper)?.let { return it }
        val swStP = Regex("""$NUMBER_START($INTEGER)\s*$STITCHES""")
        val swRowP = Regex("""$NUMBER_START($INTEGER)\s*$ROWS""")

        // "swatch: X cm, Y stitches" tai "my swatch is X cm with Y stitches"
        if (upper.contains("SWATCH") || upper.contains("MEASURED")) {
            val swSt = swStP.firstGroupInt(upper)
            val swR = swRowP.firstGroupInt(upper)
            // Ensimmäinen cm/in-luku on leveys
            val width = Regex("""$NUMBER_START($DECIMAL)\s*$LENGTH_UNIT""").firstGroupDouble(upper)
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
            Regex("""$NUMBER_START($INTEGER)\s*$STITCHES\s+(?:OVER|IN|ACROSS)\s+($DECIMAL)\s*$LENGTH_UNIT""")
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
                """\b(?:GOT|GET|GETTING|HAVE)\s+($INTEGER)\s*$STITCHES\s+(?:IN|OVER|PER|ACROSS)\s+($DECIMAL)""",
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

    private fun Regex.firstGroupInt(text: String): Int? = find(text)?.groupValues?.get(1)?.toIntOrNull()

    private fun Regex.firstGroupDouble(text: String): Double? = find(text)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun detectGaugeUnit(text: String): ParsedInstruction.GaugeUnit =
        if (Regex("""(?<![\d.])4\s*IN(?:CH(?:ES)?)?\b""").containsMatchIn(text)) {
            ParsedInstruction.GaugeUnit.PER_4_INCHES
        } else {
            ParsedInstruction.GaugeUnit.PER_10_CM
        }

    private fun detectLengthUnit(text: String): ParsedInstruction.LengthUnit? =
        when {
            text.contains("CM") -> ParsedInstruction.LengthUnit.CM
            Regex("""\bINCH(?:ES)?\b|(?:\d\s*|^)IN\b""").containsMatchIn(text) -> ParsedInstruction.LengthUnit.INCHES
            else -> null
        }

    private fun parseGaugeUnit(value: String?): ParsedInstruction.GaugeUnit =
        if (value?.contains("4", ignoreCase = true) == true) {
            ParsedInstruction.GaugeUnit.PER_4_INCHES
        } else {
            ParsedInstruction.GaugeUnit.PER_10_CM
        }

    private fun oversizedInputFailure(text: String): ParsedInstruction.Failure? =
        if (text.length > MAX_INPUT_LENGTH) {
            ParsedInstruction.Failure("input_too_long", ParsedInstruction.ErrorType.PARSE_FAILED)
        } else {
            null
        }

    const val MAX_INPUT_LENGTH = 10_000
}
