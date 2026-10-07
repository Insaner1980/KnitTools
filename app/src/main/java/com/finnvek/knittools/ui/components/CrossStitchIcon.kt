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
            "X...o...X",
            "X..ooo..X",
            "X...o...X",
            "X.......X",
            "XXXXXXXXX",
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
            "X.....X",
            "X.ooo.X",
            "X.....X",
            "X.ooo.X",
            "X.....X",
            "X.oo..X",
            "XXXXXXX",
        ),
    ),
    PHOTOS(
        listOf(
            "XXXXXXXXX",
            "X.......X",
            "X.....o.X",
            "X.......X",
            "X..o....X",
            "X.ooo.o.X",
            "XoooooooX",
            "XXXXXXXXX",
        ),
    ),

    // Ontto kello: täytettynä kolmio luki kuusena, varsinkin projektinäkymän pienellä rivillä.
    REMINDER(
        listOf(
            "....X....",
            "..XXXXX..",
            ".X.....X.",
            ".X.....X.",
            ".X.....X.",
            "X.......X",
            "XXXXXXXXX",
            ".........",
            "...XXX...",
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
    }
}
