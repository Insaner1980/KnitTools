package com.finnvek.knittools.ui.components.care

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun CareSymbolIcon(
    symbol: CareSymbol,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val label = stringResource(symbol.labelRes)
    Canvas(modifier = modifier.size(28.dp).semantics { contentDescription = label }) {
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        when (symbol) {
            CareSymbol.WASH_30 -> drawWashTub(tint, stroke, "30")
            CareSymbol.WASH_40 -> drawWashTub(tint, stroke, "40")
            CareSymbol.WASH_60 -> drawWashTub(tint, stroke, "60")
            CareSymbol.WASH_HAND -> drawWashTubHand(tint, stroke)
            CareSymbol.WASH_DO_NOT -> drawWashTubCrossed(tint, stroke)
            CareSymbol.BLEACH_ANY -> drawTriangle(tint, stroke)
            CareSymbol.BLEACH_NON_CHLORINE -> drawTriangleStriped(tint, stroke)
            CareSymbol.BLEACH_DO_NOT -> drawTriangleCrossed(tint, stroke)
            CareSymbol.DRY_TUMBLE_LOW -> drawSquareCircle(tint, stroke, 1)
            CareSymbol.DRY_TUMBLE_NORMAL -> drawSquareCircle(tint, stroke, 2)
            CareSymbol.DRY_FLAT -> drawSquareHorizontal(tint, stroke)
            CareSymbol.DRY_DO_NOT_TUMBLE -> drawSquareCircleCrossed(tint, stroke)
            CareSymbol.IRON_LOW -> drawIron(tint, stroke, 1)
            CareSymbol.IRON_MEDIUM -> drawIron(tint, stroke, 2)
            CareSymbol.IRON_HIGH -> drawIron(tint, stroke, 3)
            CareSymbol.IRON_DO_NOT -> drawIronCrossed(tint, stroke)
            CareSymbol.DRYCLEAN_ANY -> drawCircleLetter(tint, stroke, "A")
            CareSymbol.DRYCLEAN_P -> drawCircleLetter(tint, stroke, "P")
            CareSymbol.DRYCLEAN_F -> drawCircleLetter(tint, stroke, "F")
            CareSymbol.DRYCLEAN_DO_NOT -> drawCircleCrossed(tint, stroke)
        }
    }
}

// -- Washing: tub shape --

private fun DrawScope.drawWashTub(
    color: Color,
    stroke: Stroke,
    temp: String,
) {
    val w = size.width
    val h = size.height
    val path = tubPath(w, h)
    drawPath(path, color, style = stroke)
    drawTextCenter(color, temp, w * 0.5f, h * 0.5f)
}

private fun DrawScope.drawWashTubHand(
    color: Color,
    stroke: Stroke,
) {
    val w = size.width
    val h = size.height
    // Hand entering the wash water: wrist, thumb, palm and four fingers.
    val tub =
        Path().apply {
            moveTo(w * 0.08f, h * 0.35f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.8f, h * 0.85f)
            lineTo(w * 0.92f, h * 0.35f)
            moveTo(w * 0.08f, h * 0.42f)
            lineTo(w * 0.3f, h * 0.42f)
            moveTo(w * 0.68f, h * 0.42f)
            lineTo(w * 0.9f, h * 0.42f)
        }
    drawPath(tub, color, style = stroke)
    val hand =
        Path().apply {
            moveTo(w * 0.72f, h * 0.1f)
            lineTo(w * 0.49f, h * 0.25f)
            lineTo(w * 0.32f, h * 0.35f)
            quadraticTo(w * 0.25f, h * 0.43f, w * 0.36f, h * 0.43f)
            lineTo(w * 0.43f, h * 0.4f)
            lineTo(w * 0.36f, h * 0.65f)
            quadraticTo(w * 0.35f, h * 0.72f, w * 0.42f, h * 0.67f)
            lineTo(w * 0.5f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.69f)
            quadraticTo(w * 0.47f, h * 0.76f, w * 0.53f, h * 0.69f)
            lineTo(w * 0.58f, h * 0.5f)
            lineTo(w * 0.56f, h * 0.66f)
            quadraticTo(w * 0.58f, h * 0.73f, w * 0.63f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.42f)
            lineTo(w * 0.85f, h * 0.22f)
        }
    drawPath(hand, color, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawWashTubCrossed(
    color: Color,
    stroke: Stroke,
) {
    drawPath(tubPath(size.width, size.height), color, style = stroke)
    drawCross(color, stroke)
}

private fun tubPath(
    w: Float,
    h: Float,
): Path =
    Path().apply {
        moveTo(w * 0.1f, h * 0.3f)
        lineTo(w * 0.9f, h * 0.3f)
        lineTo(w * 0.8f, h * 0.75f)
        lineTo(w * 0.2f, h * 0.75f)
        close()
        // Water line on top
        moveTo(w * 0.05f, h * 0.3f)
        lineTo(w * 0.95f, h * 0.3f)
    }

// -- Bleaching: triangle --

private fun DrawScope.drawTriangle(
    color: Color,
    stroke: Stroke,
) {
    val w = size.width
    val h = size.height
    val path =
        Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.9f, h * 0.8f)
            lineTo(w * 0.1f, h * 0.8f)
            close()
        }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawTriangleStriped(
    color: Color,
    stroke: Stroke,
) {
    drawTriangle(color, stroke)
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.35f, h * 0.4f), Offset(w * 0.55f, h * 0.7f), stroke.width)
    drawLine(color, Offset(w * 0.45f, h * 0.4f), Offset(w * 0.65f, h * 0.7f), stroke.width)
}

private fun DrawScope.drawTriangleCrossed(
    color: Color,
    stroke: Stroke,
) {
    drawTriangle(color, stroke)
    drawCross(color, stroke)
}

// -- Drying: square --

private fun DrawScope.drawSquareCircle(
    color: Color,
    stroke: Stroke,
    dots: Int,
) {
    val w = size.width
    val h = size.height
    val rect =
        androidx.compose.ui.geometry
            .Rect(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.85f)
    drawRect(color, topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height), style = stroke)
    drawCircle(color, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
    // Dots for temperature
    repeat(dots) { index ->
        val x = w * (0.5f + (index - (dots - 1) / 2f) * 0.2f)
        drawCircle(color, radius = w * 0.055f, center = Offset(x, h * 0.5f))
    }
}

private fun DrawScope.drawSquareHorizontal(
    color: Color,
    stroke: Stroke,
) {
    val w = size.width
    val h = size.height
    drawRect(color, topLeft = Offset(w * 0.15f, h * 0.15f), size = Size(w * 0.7f, h * 0.7f), style = stroke)
    drawLine(color, Offset(w * 0.25f, h * 0.5f), Offset(w * 0.75f, h * 0.5f), stroke.width)
}

private fun DrawScope.drawSquareCircleCrossed(
    color: Color,
    stroke: Stroke,
) {
    drawSquareCircle(color, stroke, 0)
    drawCross(color, stroke)
}

// -- Ironing --

private fun DrawScope.drawIron(
    color: Color,
    stroke: Stroke,
    dots: Int,
) {
    val w = size.width
    val h = size.height
    val path =
        Path().apply {
            moveTo(w * 0.15f, h * 0.7f)
            lineTo(w * 0.15f, h * 0.3f)
            lineTo(w * 0.85f, h * 0.3f)
            lineTo(w * 0.85f, h * 0.55f)
            lineTo(w * 0.95f, h * 0.7f)
            close()
        }
    drawPath(path, color, style = stroke)
    val dotY = h * 0.52f
    val spacing = w * 0.12f
    val startX = w * 0.5f - (dots - 1) * spacing / 2
    repeat(dots) { i ->
        drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(startX + i * spacing, dotY))
    }
}

private fun DrawScope.drawIronCrossed(
    color: Color,
    stroke: Stroke,
) {
    drawIron(color, stroke, 0)
    drawCross(color, stroke)
}

// -- Dry cleaning: circle --

private fun DrawScope.drawCircleLetter(
    color: Color,
    stroke: Stroke,
    letter: String,
) {
    val w = size.width
    val h = size.height
    drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
    drawTextCenter(color, letter, w * 0.5f, h * 0.5f)
}

private fun DrawScope.drawCircleCrossed(
    color: Color,
    stroke: Stroke,
) {
    val w = size.width
    val h = size.height
    drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
    drawCross(color, stroke)
}

// -- Helpers --

private fun DrawScope.drawCross(
    color: Color,
    stroke: Stroke,
) {
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.1f, h * 0.1f), Offset(w * 0.9f, h * 0.9f), stroke.width)
    drawLine(color, Offset(w * 0.9f, h * 0.1f), Offset(w * 0.1f, h * 0.9f), stroke.width)
}

private fun DrawScope.drawTextCenter(
    color: Color,
    text: String,
    cx: Float,
    cy: Float,
) {
    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint().apply {
                this.color = color.toArgb()
                textSize = size.width * 0.28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        canvas.nativeCanvas.drawText(text, cx, cy + textBounds.height() / 2f, paint)
    }
}
