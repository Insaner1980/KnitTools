package com.finnvek.knittools.ai.ocr

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

    // High confidence: number + g/gr/grams
    internal fun extractWeight(text: String): String {
        val pattern = Regex("""(\d+)\s*(?:g(?:r(?:ams?)?)?)\b""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1) ?: ""
    }

    // High confidence: number + m/meters/mtr or yds/yards
    internal fun extractLength(text: String): String {
        val metricPattern = Regex("""(\d+)\s*(?:m(?:eters?|tr)?)\b""", RegexOption.IGNORE_CASE)
        val imperialPattern = Regex("""(\d+)\s*(?:y(?:a?rds?|ds?))\b""", RegexOption.IGNORE_CASE)
        return metricPattern.find(text)?.groupValues?.get(1)
            ?: imperialPattern.find(text)?.groupValues?.get(1)
            ?: ""
    }

    // High confidence: number + mm or "US X" pattern
    internal fun extractNeedleSize(text: String): String {
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
                """(\d+)\s*(?:sts?|stitches?).*?(\d+)\s*(?:rows?).*?(?:=\s*)?(\d+)\s*(?:cm|in)""",
                RegexOption.IGNORE_CASE,
            )
        val match = pattern.find(text) ?: return ""
        val sts = match.groupValues[1]
        val rows = match.groupValues[2]
        val unit = match.groupValues[3]
        return "$sts sts × $rows rows = ${unit}cm"
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
