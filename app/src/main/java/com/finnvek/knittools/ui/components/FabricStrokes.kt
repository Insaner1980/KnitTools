package com.finnvek.knittools.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.atan2
import kotlin.math.hypot

/** Yksi säie tai silmukan jalka: soikio, jonka pitkä akseli on kulmassa [degrees]. */
internal class FabricStrand(
    val center: Offset,
    val halfLength: Float,
    val halfThickness: Float,
    val degrees: Float,
    val fill: Brush,
)

internal fun DrawScope.drawFabricStrand(
    strand: FabricStrand,
    edge: Color,
    edgeWidth: Float,
    filled: Boolean = true,
) {
    translate(strand.center.x, strand.center.y) {
        rotate(strand.degrees, pivot = Offset.Zero) {
            val topLeft = Offset(-strand.halfLength, -strand.halfThickness)
            val ovalSize = Size(strand.halfLength * 2f, strand.halfThickness * 2f)
            if (filled) drawOval(strand.fill, topLeft, ovalSize)
            drawOval(edge, topLeft, ovalSize, style = Stroke(edgeWidth))
        }
    }
}

/** Säie kahden pisteen välillä. Liukuväri on säikeen omassa koordinaatistossa poikittain. */
internal fun strandBetween(
    from: Offset,
    to: Offset,
    thickness: Float,
    fill: Brush,
    extraLength: Float = 0f,
): FabricStrand {
    val delta = to - from
    return FabricStrand(
        center = (from + to) / 2f,
        halfLength = hypot(delta.x, delta.y) / 2f + extraLength,
        halfThickness = thickness / 2f,
        degrees = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat(),
        fill = fill,
    )
}

// Positiivinen kerroin vaalentaa kohti valkoista, negatiivinen tummentaa.
internal fun Color.shade(amount: Float): Color =
    if (amount >= 0f) {
        Color(red + (1f - red) * amount, green + (1f - green) * amount, blue + (1f - blue) * amount, alpha)
    } else {
        Color(red * (1f + amount), green * (1f + amount), blue * (1f + amount), alpha)
    }

internal fun crossGradient(
    halfThickness: Float,
    top: Color,
    bottom: Color,
): Brush = Brush.verticalGradient(listOf(top, bottom), startY = -halfThickness, endY = halfThickness)
