package com.finnvek.knittools.ai.ocr

import kotlin.math.roundToInt

data class ParsedYarnLabel(
    val brand: String = "",
    val yarnName: String = "",
    val fiberContent: String = "",
    val weightGrams: String = "",
    val lengthMeters: String = "",
    val needleSize: String = "",
    val gaugeInfo: String = "",
    val colorName: String = "",
    val colorNumber: String = "",
    val dyeLot: String = "",
    val weightCategory: String = "",
)

object YarnLabelParser {
    fun parse(rawText: String): ParsedYarnLabel {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val fullText = lines.joinToString(" ")

        return ParsedYarnLabel(
            weightGrams = extractWeight(fullText),
            lengthMeters = extractLength(fullText),
            needleSize = extractNeedleSize(fullText),
            gaugeInfo = extractGauge(fullText),
            fiberContent = extractFiber(fullText),
            colorNumber = extractColorNumber(fullText),
            dyeLot = extractDyeLot(fullText),
            weightCategory = extractWeightCategory(fullText),
            brand = extractBrand(lines),
            yarnName = extractYarnName(lines),
            colorName = extractColorName(lines, fullText),
        )
    }

    // High confidence: number + g/gr/grams tai oz/ounces (muunnetaan grammoiksi)
    internal fun extractWeight(text: String): String {
        val gramPattern = Regex("""(\d+)\s*(?:g(?:r(?:ams?)?)?)\b""", RegexOption.IGNORE_CASE)
        gramPattern.find(text)?.let { return it.groupValues[1] }

        val ozPattern = Regex("""(\d+(?:[.,]\d+)?)\s*(?:oz|ounces?)\b""", RegexOption.IGNORE_CASE)
        ozPattern.find(text)?.let {
            val oz = it.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return ""
            return (oz * GRAMS_PER_OUNCE).roundToInt().toString()
        }

        return ""
    }

    // High confidence: number + m/meters/mtr or yds/yards (imperial → metrit)
    internal fun extractLength(text: String): String {
        val metricPattern = Regex("""(\d+)\s*(?:m(?:eters?|tr)?)\b""", RegexOption.IGNORE_CASE)
        metricPattern.find(text)?.let { return it.groupValues[1] }

        val imperialPattern = Regex("""(\d+)\s*(?:y(?:a?rds?|ds?))\b""", RegexOption.IGNORE_CASE)
        imperialPattern.find(text)?.let {
            val yards = it.groupValues[1].toDoubleOrNull() ?: return ""
            return (yards * METERS_PER_YARD).roundToInt().toString()
        }

        return ""
    }

    // High confidence: number + mm (tukee myös "3.5 - 4mm" aluetta) tai "US X" pattern
    internal fun extractNeedleSize(text: String): String {
        val rangePattern = Regex("""(\d+(?:[.,]\d+)?)\s*-\s*(\d+(?:[.,]\d+)?)\s*mm""", RegexOption.IGNORE_CASE)
        rangePattern.find(text)?.let {
            val low = it.groupValues[1].replace(',', '.')
            val high = it.groupValues[2].replace(',', '.')
            return "$low - ${high}mm"
        }

        val mmPattern = Regex("""(\d+(?:[.,]\d+)?)\s*mm""", RegexOption.IGNORE_CASE)
        val usPattern = Regex("""US\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        return mmPattern
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.replace(',', '.')
            ?: usPattern.find(text)?.let { "US ${it.groupValues[1]}" }
            ?: ""
    }

    // Medium confidence: number + sts/stitches pattern
    internal fun extractGauge(text: String): String {
        val pattern =
            Regex(
                """(\d+)\s*(?:sts?|stitches?).*?(\d+)\s*(?:rows?).*?(?:=\s*)?(\d+)\s*(cm|in)""",
                RegexOption.IGNORE_CASE,
            )
        val match = pattern.find(text) ?: return ""
        val sts = match.groupValues[1]
        val rows = match.groupValues[2]
        val size = match.groupValues[3]
        val unit = match.groupValues[4]
        return "$sts sts × $rows rows = $size $unit"
    }

    // Medium confidence: percentage + fiber keyword
    internal fun extractFiber(text: String): String {
        val fibers =
            listOf(
                "wool",
                "merino",
                "cotton",
                "acrylic",
                "polyester",
                "nylon",
                "silk",
                "alpaca",
                "cashmere",
                "mohair",
                "linen",
                "bamboo",
                "viscose",
                "polyamide",
            )
        val pattern =
            Regex(
                """(\d+)\s*%\s*(${fibers.joinToString("|")})""",
                RegexOption.IGNORE_CASE,
            )
        val matches = pattern.findAll(text).toList()
        if (matches.isEmpty()) return ""
        return matches.joinToString(", ") {
            "${it.groupValues[1]}% ${it.groupValues[2].replaceFirstChar { c ->
                c.uppercase()
            }}"
        }
    }

    // Medium confidence: short number near "col" or "color"
    internal fun extractColorNumber(text: String): String {
        val pattern = Regex("""(?:col(?:ou?r)?\.?\s*(?:#|no?\.?)?\s*)(\d{1,5})""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1) ?: ""
    }

    // Medium confidence: number near "lot" or "dye lot"
    internal fun extractDyeLot(text: String): String {
        val pattern = Regex("""(?:(?:dye\s*)?lot\.?\s*(?:#|no?\.?)?\s*)(\d{2,10})""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1) ?: ""
    }

    // Medium confidence: keyword match
    internal fun extractWeightCategory(text: String): String {
        val categories =
            listOf(
                "Super Bulky",
                "Bulky",
                "Aran",
                "Worsted",
                "DK",
                "Sport",
                "Fingering",
                "Lace",
            )
        val lower = text.lowercase()
        return categories.firstOrNull { lower.contains(it.lowercase()) } ?: ""
    }

    // Low confidence: first line that looks like a brand name
    internal fun extractBrand(lines: List<String>): String =
        lines.firstOrNull { line ->
            line.length in 2..30 &&
                !line.contains(Regex("""\d+\s*[gm]""", RegexOption.IGNORE_CASE)) &&
                !line.contains("%")
        } ?: ""

    // Low confidence: second substantial text line (often yarn name)
    internal fun extractYarnName(lines: List<String>): String {
        val candidates =
            lines.filter { line ->
                line.length in 2..40 &&
                    !line.contains(Regex("""\d+\s*[gm]""", RegexOption.IGNORE_CASE)) &&
                    !line.contains("%")
            }
        return candidates.getOrNull(1) ?: ""
    }

    private const val GRAMS_PER_OUNCE = 28.3495
    private const val METERS_PER_YARD = 0.9144

    // Low confidence: text near color number
    internal fun extractColorName(
        lines: List<String>,
        fullText: String,
    ): String {
        val colorNumber = extractColorNumber(fullText)
        if (colorNumber.isBlank()) return ""
        val lineWithColor = lines.firstOrNull { it.contains(colorNumber) } ?: return ""
        val cleaned =
            lineWithColor
                .replace(Regex("""(?:col(?:ou?r)?\.?\s*(?:#|no?\.?)?\s*)\d+""", RegexOption.IGNORE_CASE), "")
                .trim()
        return cleaned.takeIf { it.length in 2..30 } ?: ""
    }
}
