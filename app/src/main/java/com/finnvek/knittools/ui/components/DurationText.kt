package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplay
import com.finnvek.knittools.domain.calculator.DurationShape
import com.finnvek.knittools.ui.screens.counter.counterMainNumberFittedFontSize
import com.finnvek.knittools.ui.theme.InsightsDimens

/**
 * Yksi paikka, jossa [DurationDisplay]-slotit käännetään lokalisoiduksi tekstiksi.
 * Lukua ja yksikköä ei liimata yhteen Kotlinissa, vaan muoto tulee resurssista.
 */
@Composable
fun durationText(display: DurationDisplay): String {
    val locale = rememberCurrentLocale()
    val format = durationFormat(display)
    return String.format(locale, format, *durationArguments(display).toTypedArray())
}

/**
 * Hero-esitys kestolle: sama lokalisoitu muoto jaettuna ajoihin, joissa luku ja
 * yksikkö saavat eri kokonsa. Ensimmäinen luku vie suurimman koon, joten alle
 * tunnin kesto nostaa minuutit hero-kokoon ja tasatunneilla minuuttiajot jäävät pois.
 *
 * Numerot kutistuvat automaattisesti [InsightsDimens.HeroPrimaryNumberMinFontSize]
 * -kokoon asti, jottei kolminumeroinen tuntimäärä rivity kapeimmalla tuetulla leveydellä.
 */
@Composable
fun DurationHero(
    display: DurationDisplay,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val locale = rememberCurrentLocale()
    val format = durationFormat(display)
    val numbers = durationArguments(display).map { String.format(locale, "%d", it) }
    val runs = durationRuns(format, numbers)
    val spacingPx = with(LocalDensity.current) { InsightsDimens.HeroUnitToMinutesSpacing.roundToPx() }

    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val fittedPrimarySize =
            counterMainNumberFittedFontSize(
                maxFontSize = InsightsDimens.HeroPrimaryNumberFontSize,
                minFontSize = InsightsDimens.HeroPrimaryNumberMinFontSize,
                maxWidthPx = constraints.maxWidth,
                measureWidth = { candidate ->
                    val candidateScale = candidate.value / InsightsDimens.HeroPrimaryNumberFontSize.value
                    val runsWidth =
                        runs.sumOf { run ->
                            textMeasurer
                                .measure(
                                    text = run.text,
                                    style = heroRunStyle(run, candidateScale, color),
                                    softWrap = false,
                                    maxLines = 1,
                                ).size.width
                        }
                    runsWidth + spacingPx * runs.count { it.startsSecondaryPair }
                },
            )
        val scale = fittedPrimarySize.value / InsightsDimens.HeroPrimaryNumberFontSize.value

        // Kaikki ajot samalle perusviivalle, vaikka kokoja on neljä
        Row(verticalAlignment = Alignment.Bottom) {
            runs.forEach { run ->
                if (run.startsSecondaryPair) {
                    Spacer(modifier = Modifier.width(InsightsDimens.HeroUnitToMinutesSpacing))
                }
                Text(
                    text = run.text,
                    style = heroRunStyle(run, scale, color),
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** Yksittäinen ajo hero-kestossa: joko luku tai sitä seuraava yksikkö. */
internal data class DurationRun(
    val text: String,
    val isNumber: Boolean,
    val isSecondary: Boolean,
    val startsSecondaryPair: Boolean = false,
)

/**
 * Pilkkoo lokalisoidun muotoilumerkkijonon ajoihin. Paikanvaraajat tunnistetaan
 * muodoista `%d` ja `%1$d`, joten kääntäjä voi vaihtaa lukujen ja yksiköiden
 * järjestystä ilman että esityskoot menevät sekaisin.
 */
internal fun durationRuns(
    format: String,
    numbers: List<String>,
): List<DurationRun> {
    val parsed = mutableListOf<DurationRun>()
    val literal = StringBuilder()
    var index = 0
    var nextArgumentIndex = 0
    var currentTier = 0

    fun flushLiteral() {
        val text = literal.toString().trim()
        literal.setLength(0)
        if (text.isNotEmpty()) {
            parsed += DurationRun(text = text, isNumber = false, isSecondary = currentTier > 0)
        }
    }

    while (index < format.length) {
        val match = if (format[index] == '%') PLACEHOLDER_PATTERN.matchAt(format, index) else null
        if (match == null) {
            literal.append(format[index])
            index++
            continue
        }
        val argumentIndex =
            match.groupValues[1]
                .takeIf { it.isNotEmpty() }
                ?.toInt()
                ?.minus(1) ?: nextArgumentIndex
        nextArgumentIndex = argumentIndex + 1
        flushLiteral()
        currentTier = argumentIndex.coerceAtMost(1)
        parsed +=
            DurationRun(
                text = numbers.getOrElse(argumentIndex) { "" },
                isNumber = true,
                isSecondary = currentTier > 0,
            )
        index += match.value.length
    }
    flushLiteral()

    val runs = parsed.filter { it.text.isNotEmpty() }
    val firstSecondaryIndex = runs.indexOfFirst { it.isSecondary }
    return runs.mapIndexed { runIndex, run ->
        if (runIndex == firstSecondaryIndex) run.copy(startsSecondaryPair = true) else run
    }
}

private val PLACEHOLDER_PATTERN = Regex("""%(?:(\d+)\$)?d""")

private fun heroRunStyle(
    run: DurationRun,
    scale: Float,
    color: Color,
): TextStyle {
    val baseSize =
        when {
            run.isNumber && !run.isSecondary -> InsightsDimens.HeroPrimaryNumberFontSize
            run.isNumber -> InsightsDimens.HeroSecondaryNumberFontSize
            !run.isSecondary -> InsightsDimens.HeroPrimaryUnitFontSize
            else -> InsightsDimens.HeroSecondaryUnitFontSize
        }
    val weight =
        when {
            run.isNumber && !run.isSecondary -> FontWeight.ExtraBold
            run.isNumber -> FontWeight.Bold
            else -> FontWeight.SemiBold
        }
    // Ei tabulaarinumeroita: hero on yksi luku, ei sarake. Tasalevyinen ykkönen
    // repi "1 h" auki samalla kun "9h" pysyi tiiviinä.
    return TextStyle(
        color = color,
        fontSize = (baseSize.value * scale).sp,
        fontWeight = weight,
        letterSpacing = if (run.isNumber) InsightsDimens.HeroNumberLetterSpacing else TextUnit.Unspecified,
    )
}

@Composable
private fun durationFormat(display: DurationDisplay): String {
    val resources = LocalResources.current
    return when (display.shape) {
        DurationShape.MINUTES_ONLY ->
            resources.getQuantityString(R.plurals.insights_duration_minutes, display.minutes ?: 0)

        DurationShape.WHOLE_HOURS ->
            resources.getQuantityString(R.plurals.insights_duration_hours, display.hours ?: 0)

        DurationShape.HOURS_AND_MINUTES ->
            resources.getString(R.string.insights_duration_hours_minutes)
    }
}

private fun durationArguments(display: DurationDisplay): List<Int> =
    when (display.shape) {
        DurationShape.MINUTES_ONLY -> listOf(display.minutes ?: 0)
        DurationShape.WHOLE_HOURS -> listOf(display.hours ?: 0)
        DurationShape.HOURS_AND_MINUTES -> listOf(display.hours ?: 0, display.minutes ?: 0)
    }
