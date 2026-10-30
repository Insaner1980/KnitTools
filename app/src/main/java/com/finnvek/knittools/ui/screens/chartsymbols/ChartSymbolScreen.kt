package com.finnvek.knittools.ui.screens.chartsymbols

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.ChartSymbolData
import com.finnvek.knittools.domain.model.ChartSymbol
import com.finnvek.knittools.domain.model.ChartSymbolCategory
import com.finnvek.knittools.ui.components.InfoNote
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.layout.ContentWidthPolicy

@Composable
fun ChartSymbolScreen(onBack: () -> Unit) {
    val grouped = remember { ChartSymbolData.byCategory() }

    val categoryNames =
        mapOf(
            ChartSymbolCategory.BASIC to stringResource(R.string.category_basic),
            ChartSymbolCategory.DECREASES to stringResource(R.string.category_decreases),
            ChartSymbolCategory.INCREASES to stringResource(R.string.category_increases),
            ChartSymbolCategory.CABLES to stringResource(R.string.category_cables),
            ChartSymbolCategory.OTHER to stringResource(R.string.category_other),
        )

    ToolScreenScaffold(
        contentWidthPolicy = ContentWidthPolicy.ListDetail,
        title = stringResource(R.string.chart_symbols_title),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                InfoNote(
                    text = stringResource(R.string.chart_symbols_disclaimer),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            ChartSymbolCategory.entries.forEach { category ->
                val symbols = grouped[category] ?: return@forEach

                item {
                    SectionLabel(
                        text = categoryNames[category] ?: category.name,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }

                items(symbols, key = { it.id }) { symbol ->
                    ChartSymbolRow(symbol)
                }
            }
        }
    }
}

@Composable
private fun ChartSymbolRow(symbol: ChartSymbol) {
    val tint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(width = (32 * chartSymbolWidthUnits(symbol.id)).dp, height = 32.dp)) {
            drawSymbol(symbol.id, tint)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(symbol.nameResId),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(symbol.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Piirtofunktiot neulontakaavion symboleille
internal fun DrawScope.drawSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension
    val stroke = Stroke(width = 2f)
    val pad = s * 0.1f

    if (id.startsWith("cable_")) {
        drawCable(id, tint)
        return
    }
    // Pohjaruutu
    drawRect(tint, topLeft = Offset(pad, pad), size = Size(s - 2 * pad, s - 2 * pad), style = stroke)

    when (id) {
        "knit", "purl", "yarn_over", "knit_tbl", "purl_tbl", "slip_purlwise" -> drawBasicSymbol(id, tint)
        "k2tog", "ssk", "p2tog", "s2kp", "k3tog", "sk2p" -> drawDecreaseSymbol(id, tint)
        "m1l", "m1r", "lifted_inc_left", "lifted_inc_right", "kfb" -> drawIncreaseSymbol(id, tint)
        "no_stitch", "marker", "repeat" -> drawOtherSymbol(id, tint)
    }
}

private fun DrawScope.drawBasicSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension
    val pad = s * 0.1f
    val stroke = Stroke(width = 2f)

    when (id) {
        // Tyhjä ruutu = oikealta puolelta neulottu silmukka
        "knit" -> { /* Pelkkä pohjaruutu, ei lisäpiirtoa */ }

        "purl" -> {
            // Piste keskellä
            drawCircle(tint, radius = s * 0.08f, center = center)
        }

        "yarn_over" -> {
            drawCircle(tint, radius = s * 0.2f, center = center, style = stroke)
        }

        "knit_tbl", "purl_tbl" -> {
            // CYC twisted loop: the legs cross below the loop.
            val loop =
                Path().apply {
                    moveTo(s * 0.3f, s * 0.75f)
                    cubicTo(s * 0.8f, s * 0.4f, s * 0.6f, s * 0.2f, s * 0.5f, s * 0.28f)
                    cubicTo(s * 0.25f, s * 0.4f, s * 0.5f, s * 0.6f, s * 0.7f, s * 0.75f)
                }
            drawPath(loop, tint, style = stroke)
            if (id == "purl_tbl") drawCircle(tint, s * 0.045f, Offset(s * 0.5f, s * 0.4f))
        }

        "slip_purlwise" -> {
            // Alaspäin osoittava V on yleisin nostetun silmukan merkki (Vogue Knitting); aiemmin
            // molemmat nostot piirtyivät samana vaakaviivana eikä niitä erottanut toisistaan.
            val top = pad + s * 0.2f
            val bottom = s - pad - s * 0.2f
            drawLine(tint, Offset(pad + s * 0.2f, top), Offset(center.x, bottom), strokeWidth = 2f)
            drawLine(tint, Offset(center.x, bottom), Offset(s - pad - s * 0.2f, top), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawDecreaseSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension
    val pad = s * 0.1f

    when (id) {
        "k2tog" -> {
            // Oikealle kallistuva viiva (right-leaning)
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "ssk" -> {
            // Vasemmalle kallistuva viiva (left-leaning)
            drawLine(
                tint,
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "p2tog" -> {
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawCircle(tint, radius = s * 0.06f, center = Offset(s * 0.35f, s * 0.35f))
        }

        "s2kp" -> {
            // Keskitetty kaksoiskavennus: Λ ja keskiviiva. Ilman keskiviivaa merkki olisi sama kuin
            // takareunasta neulottu silmukka, ja alaspäin osoittava V on nurin nostetun silmukan merkki.
            drawVShape(tint, pad, s)
            drawLine(
                tint,
                Offset(center.x, s - pad - s * 0.15f),
                Offset(center.x, pad + s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "k3tog", "sk2p" -> {
            val left = s * 0.3f
            val right = s * 0.7f
            val topX = if (id == "k3tog") right else left
            val bottomX = if (id == "k3tog") left else right
            drawLine(tint, Offset(bottomX, s * 0.7f), Offset(topX, s * 0.3f), strokeWidth = 2f)
            drawLine(tint, center, Offset(center.x, s * 0.7f), strokeWidth = 2f)
            drawLine(tint, center, Offset(topX, s * 0.7f), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawIncreaseSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension

    when (id) {
        "m1l", "m1r", "lifted_inc_left", "lifted_inc_right" -> {
            // CYC lists this branched slant for both M1 and lifted increases.
            val leftLeaning = id == "m1l" || id == "lifted_inc_left"
            val topX = if (leftLeaning) s * 0.28f else s * 0.72f
            val bottomX = s - topX
            drawLine(tint, Offset(bottomX, s * 0.75f), Offset(topX, s * 0.25f), strokeWidth = 2f)
            drawLine(tint, center, Offset(bottomX, s * 0.3f), strokeWidth = 2f)
        }

        "kfb" -> {
            // CYC K1fb: upright stem with an upper-left branch.
            drawLine(tint, Offset(s * 0.55f, s * 0.25f), Offset(s * 0.55f, s * 0.75f), strokeWidth = 2f)
            drawLine(tint, Offset(s * 0.3f, s * 0.3f), Offset(s * 0.55f, s * 0.5f), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawOtherSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension
    val pad = s * 0.1f

    when (id) {
        "no_stitch" -> {
            // Harmaa täytetty ruutu
            drawRect(tint.copy(alpha = 0.3f), topLeft = Offset(pad, pad), size = Size(s - 2 * pad, s - 2 * pad))
        }

        "marker" -> {
            // Pystyviiva
            drawLine(tint, Offset(center.x, pad + s * 0.1f), Offset(center.x, s - pad - s * 0.1f), strokeWidth = 3f)
        }

        "repeat" -> {
            // Hakasulkeet
            drawLine(
                tint,
                Offset(pad + s * 0.2f, pad + s * 0.15f),
                Offset(pad + s * 0.2f, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(pad + s * 0.2f, pad + s * 0.15f),
                Offset(pad + s * 0.35f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(pad + s * 0.2f, s - pad - s * 0.15f),
                Offset(pad + s * 0.35f, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(s - pad - s * 0.2f, pad + s * 0.15f),
                Offset(s - pad - s * 0.2f, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(s - pad - s * 0.2f, pad + s * 0.15f),
                Offset(s - pad - s * 0.35f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(s - pad - s * 0.2f, s - pad - s * 0.15f),
                Offset(
                    s - pad - s * 0.35f,
                    s - pad - s * 0.15f,
                ),
                strokeWidth = 2f,
            )
        }
    }
}

// Upward chevron forming the base of the CYC centered double decrease.
private fun DrawScope.drawVShape(
    tint: Color,
    pad: Float,
    s: Float,
) {
    drawLine(
        tint,
        Offset(pad + s * 0.15f, s - pad - s * 0.15f),
        Offset(center.x, pad + s * 0.15f),
        strokeWidth = 2f,
    )
    drawLine(
        tint,
        Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
        Offset(center.x, pad + s * 0.15f),
        strokeWidth = 2f,
    )
}

internal fun chartSymbolWidthUnits(id: String): Int =
    when {
        id.startsWith("cable_3_3") -> 3
        id.startsWith("cable_2_2") -> 2
        else -> 1
    }

private fun DrawScope.drawCable(
    id: String,
    tint: Color,
) {
    // Version 1: the uninterrupted band is the group held in front.
    val w = size.width
    val h = size.height
    val leftCross = id.endsWith("left")

    fun x(fraction: Float): Float = if (leftCross) w * (1f - fraction) else w * fraction
    val top = h * 0.25f
    val bottom = h * 0.75f
    val cells = if (id.startsWith("cable_3_3")) 6 else 4
    for (i in 0..cells) {
        val tick = w * (0.05f + 0.9f * i / cells)
        drawLine(tint, Offset(tick, top - h * 0.08f), Offset(tick, top), 2f)
        drawLine(tint, Offset(tick, bottom), Offset(tick, bottom + h * 0.08f), 2f)
    }
    drawLine(tint, Offset(x(0.05f), top), Offset(x(0.95f), top), 2f)
    drawLine(tint, Offset(x(0.05f), bottom), Offset(x(0.95f), bottom), 2f)
    drawLine(tint, Offset(x(0.05f), bottom), Offset(x(0.5f), top), 2f)
    drawLine(tint, Offset(x(0.5f), bottom), Offset(x(0.95f), top), 2f)
    drawLine(tint, Offset(x(0.05f), top), Offset(x(0.275f), h * 0.5f), 2f)
    drawLine(tint, Offset(x(0.725f), h * 0.5f), Offset(x(0.95f), bottom), 2f)
}
