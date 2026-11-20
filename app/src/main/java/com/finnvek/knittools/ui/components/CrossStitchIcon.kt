package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.ui.theme.CrossStitchDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

private const val CENTER_FILL_ROW = "X...o...X"
private const val ROUNDED_END_ROW = "..XXXXX.."
private const val EMPTY_WIDE_ROW = "X.......X"
private const val EMPTY_NARROW_ROW = "X.....X"
private const val ROUNDED_SIDE_ROW = ".X.....X."
private const val FULL_ROW = "XXXXXXXXX"
private const val KEY_ROW = "X.X.X.X.X"
private const val EMPTY_ROW = "........."
private const val CHART_BAR_ROW = ".X..X..X."

/**
 * Projektin sisällön kuvakkeet ristipistoina. X on ääriviivan pisto, o himmeämpi täytepisto ja
 * piste tyhjä ruutu. Sama kuvake on laskurin laatassa ja projektinäkymän rivillä.
 */
enum class CrossStitchGlyph(
    internal val rows: List<String>,
) {
    // Taitettu kulma ja neulekaavion kuvio erottavat kaavan Notesin viivoitetusta sivusta.
    PATTERN(
        listOf(
            "XXXXXX...",
            "X....XX..",
            "X....X.X.",
            "X....XXXX",
            CENTER_FILL_ROW,
            "X..ooo..X",
            CENTER_FILL_ROW,
            EMPTY_WIDE_ROW,
            FULL_ROW,
        ),
    ),

    // Kerrattu lanka leveänä vinona säikeenä, ei kerää: keräpiirrokset on rajattu pois.
    YARN(
        listOf(
            "Xo.......",
            "oXo......",
            ".oXo.....",
            "..oXo....",
            "...oXo...",
            "....oXo..",
            ".....oXo.",
            "......oXo",
            ".......oX",
        ),
    ),
    NOTES(
        listOf(
            "XXXXXXX",
            EMPTY_NARROW_ROW,
            "X.ooo.X",
            EMPTY_NARROW_ROW,
            "X.ooo.X",
            EMPTY_NARROW_ROW,
            "X.oo..X",
            "XXXXXXX",
        ),
    ),
    PHOTOS(
        listOf(
            FULL_ROW,
            EMPTY_WIDE_ROW,
            "X.....o.X",
            EMPTY_WIDE_ROW,
            "X..o....X",
            "X.ooo.o.X",
            "XoooooooX",
            FULL_ROW,
        ),
    ),

    // Ontto kello: täytettynä kolmio luki kuusena, varsinkin projektinäkymän pienellä rivillä.
    REMINDER(
        listOf(
            "....X....",
            ROUNDED_END_ROW,
            ROUNDED_SIDE_ROW,
            ROUNDED_SIDE_ROW,
            ROUNDED_SIDE_ROW,
            EMPTY_WIDE_ROW,
            FULL_ROW,
            EMPTY_ROW,
            "...XXX...",
        ),
    ),
    COUNTER(
        listOf(
            ROUNDED_END_ROW,
            ROUNDED_SIDE_ROW,
            CENTER_FILL_ROW,
            CENTER_FILL_ROW,
            "X.ooooo.X",
            CENTER_FILL_ROW,
            CENTER_FILL_ROW,
            ROUNDED_SIDE_ROW,
            ROUNDED_END_ROW,
        ),
    ),
    CLOCK(
        listOf(
            ROUNDED_END_ROW,
            ROUNDED_SIDE_ROW,
            CENTER_FILL_ROW,
            CENTER_FILL_ROW,
            "X...ooo.X",
            EMPTY_WIDE_ROW,
            EMPTY_WIDE_ROW,
            ROUNDED_SIDE_ROW,
            ROUNDED_END_ROW,
        ),
    ),

    // Laskin: näyttörivi ja kaksi näppäinriviä; käytetään onboardingin työkalupakissa.
    CALCULATOR(
        listOf(
            FULL_ROW,
            EMPTY_WIDE_ROW,
            "X.ooooo.X",
            EMPTY_WIDE_ROW,
            KEY_ROW,
            EMPTY_WIDE_ROW,
            KEY_ROW,
            EMPTY_WIDE_ROW,
            FULL_ROW,
        ),
    ),

    // Pylväät perusviivalla kuten Insightsin kaaviossa.
    CHART(
        listOf(
            ".......X.",
            ".......X.",
            "....X..X.",
            "....X..X.",
            CHART_BAR_ROW,
            CHART_BAR_ROW,
            CHART_BAR_ROW,
            EMPTY_ROW,
            FULL_ROW,
        ),
    ),
}

/** Kuvakkeen oma korostusväri teematokeneista; laskurin laatta ja projektinäkymän rivi jakavat sen. */
@Composable
fun CrossStitchGlyph.accentColor(): Color =
    when (this) {
        CrossStitchGlyph.PATTERN -> MaterialTheme.colorScheme.primary
        CrossStitchGlyph.YARN -> MaterialTheme.colorScheme.secondary
        CrossStitchGlyph.NOTES -> MaterialTheme.knitToolsColors.brandWine
        CrossStitchGlyph.PHOTOS -> MaterialTheme.colorScheme.tertiary
        CrossStitchGlyph.REMINDER -> MaterialTheme.knitToolsColors.tealAccent
        CrossStitchGlyph.COUNTER -> MaterialTheme.colorScheme.primary
        CrossStitchGlyph.CLOCK -> MaterialTheme.knitToolsColors.tealAccent
        CrossStitchGlyph.CALCULATOR -> MaterialTheme.colorScheme.secondary
        CrossStitchGlyph.CHART -> MaterialTheme.colorScheme.tertiary
    }

/** Koristekuvake: merkitys tulee aina viereisestä tekstistä, joten sillä ei ole omaa kuvausta. */
@Composable
fun CrossStitchIcon(
    glyph: CrossStitchGlyph,
    modifier: Modifier = Modifier,
    color: Color = glyph.accentColor(),
) {
    Spacer(
        modifier.drawWithCache {
            val cell = size.minDimension / CrossStitchDimens.GridCells
            val columns = glyph.rows.maxOf { it.length }
            val originX = (size.width - columns * cell) / 2f
            val originY = (size.height - glyph.rows.size * cell) / 2f
            val span = cell * CrossStitchDimens.StitchSpan
            val stroke = maxOf(cell * CrossStitchDimens.StitchStrokeRatio, CrossStitchDimens.MinStitchStroke.toPx())
            val softColor = color.copy(alpha = color.alpha * CrossStitchDimens.SoftStitchAlpha)
            onDrawBehind {
                glyph.rows.forEachIndexed { y, row ->
                    row.forEachIndexed { x, stitch ->
                        if (stitch == '.') return@forEachIndexed
                        val stitchColor = if (stitch == 'o') softColor else color
                        val left = originX + (x + CrossStitchDimens.StitchInset) * cell
                        val top = originY + (y + CrossStitchDimens.StitchInset) * cell
                        val right = left + span
                        val bottom = top + span
                        drawLine(stitchColor, Offset(left, top), Offset(right, bottom), stroke, StrokeCap.Round)
                        drawLine(stitchColor, Offset(right, top), Offset(left, bottom), stroke, StrokeCap.Round)
                    }
                }
            }
        },
    )
}

/** Koko näytön tyhjä tila: sisällön ristipistokuvake, valinnainen otsikko ja ohjeteksti. */
@Composable
fun CrossStitchEmptyState(
    glyph: CrossStitchGlyph,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(CrossStitchDimens.EmptyStatePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CrossStitchIcon(glyph, Modifier.size(CrossStitchDimens.EmptyStateGlyphSize))
        Spacer(Modifier.height(CrossStitchDimens.EmptyStateGlyphGap))
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(CrossStitchDimens.EmptyStateTextGap))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.knitToolsColors.emptyStateText,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(CrossStitchDimens.EmptyStateActionGap))
            action()
        }
    }
}
