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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.ChartSymbolData
import com.finnvek.knittools.domain.model.ChartSymbol
import com.finnvek.knittools.domain.model.ChartSymbolCategory
import com.finnvek.knittools.ui.components.InfoNote
import com.finnvek.knittools.ui.components.ToolScreenScaffold

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
                    Text(
                        text = categoryNames[category] ?: category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
        Canvas(modifier = Modifier.size(32.dp)) {
            drawSymbol(symbol.id, tint)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = symbol.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = symbol.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Piirtofunktiot neulontakaavion symboleille
private fun DrawScope.drawSymbol(
    id: String,
    tint: Color,
) {
    val s = size.minDimension
    val stroke = Stroke(width = 2f)
    val pad = s * 0.1f

    // Pohjaruutu
    drawRect(tint, topLeft = Offset(pad, pad), size = Size(s - 2 * pad, s - 2 * pad), style = stroke)

    when (id) {
        "knit" -> { /* Pelkkä pohjaruutu, ei lisäpiirtoa */ }

        // Tyhjä ruutu = oikealta puolelta neulottu silmukka
        "purl" -> {
            // Piste keskellä
            drawCircle(tint, radius = s * 0.08f, center = center)
        }

        "yarn_over" -> {
            drawCircle(tint, radius = s * 0.2f, center = center, style = stroke)
        }

        "knit_tbl" -> {
            drawVShape(tint, pad, s)
        }

        "purl_tbl" -> {
            drawCircle(tint, radius = s * 0.06f, center = center)
            drawLine(
                tint,
                Offset(center.x, center.y + s * 0.12f),
                Offset(center.x, s - pad - s * 0.1f),
                strokeWidth = 2f,
            )
        }

        "slip_knitwise", "slip_purlwise" -> {
            // Vaakaviiva
            drawLine(tint, Offset(pad + s * 0.15f, center.y), Offset(s - pad - s * 0.15f, center.y), strokeWidth = 2f)
        }

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
            drawCircle(tint, radius = s * 0.06f, center = Offset(s * 0.35f, s * 0.65f))
        }

        "s2kp" -> {
            // Käännetty V (centered double decrease)
            drawLine(
                tint,
                Offset(pad + s * 0.1f, pad + s * 0.15f),
                Offset(center.x, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(s - pad - s * 0.1f, pad + s * 0.15f),
                Offset(center.x, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "k3tog" -> {
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(center.x, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "sk2p" -> {
            drawLine(
                tint,
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(center.x, s - pad - s * 0.15f),
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "m1l" -> {
            // Vasemmalle kallistuva viiva + palkki alhaalla
            drawLine(
                tint,
                Offset(s - pad - s * 0.15f, s - pad - s * 0.2f),
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "m1r" -> {
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.2f),
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                strokeWidth = 2f,
            )
            drawLine(
                tint,
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 2f,
            )
        }

        "kfb" -> {
            drawVShape(tint, pad, s)
        }

        "lifted_inc_left", "lifted_inc_right" -> {
            val xStart = if (id == "lifted_inc_left") pad + s * 0.2f else s - pad - s * 0.2f
            val xEnd = if (id == "lifted_inc_left") s - pad - s * 0.2f else pad + s * 0.2f
            drawLine(tint, Offset(xStart, s - pad - s * 0.2f), Offset(xEnd, pad + s * 0.2f), strokeWidth = 2f)
            // Nuoli
            drawCircle(tint, radius = s * 0.05f, center = Offset(xEnd, pad + s * 0.2f))
        }

        "cable_2_2_left", "cable_3_3_left" -> {
            // Ristikkäiset viivat — vasen päälle
            drawLine(
                tint,
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 3f,
            )
            drawLine(
                tint,
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 1.5f,
            )
        }

        "cable_2_2_right", "cable_3_3_right" -> {
            // Ristikkäiset viivat — oikea päälle
            drawLine(
                tint,
                Offset(s - pad - s * 0.15f, pad + s * 0.15f),
                Offset(pad + s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 3f,
            )
            drawLine(
                tint,
                Offset(pad + s * 0.15f, pad + s * 0.15f),
                Offset(s - pad - s * 0.15f, s - pad - s * 0.15f),
                strokeWidth = 1.5f,
            )
        }

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

// V-muoto symboleille knit_tbl ja kfb
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
