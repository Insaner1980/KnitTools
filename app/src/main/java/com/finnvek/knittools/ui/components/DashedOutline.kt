package com.finnvek.knittools.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.finnvek.knittools.ui.theme.DashedOutlineStyle

/**
 * Piirtää katkoviivaisen pyöristetyn reunan kokonaan alueen sisäpuolelle, jotta leikkaava muoto
 * ei syö puolta viivasta.
 */
fun Modifier.dashedOutline(
    color: Color,
    style: DashedOutlineStyle,
): Modifier =
    drawBehind {
        val stroke = style.width.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(style.corner.toPx()),
            style =
                Stroke(
                    width = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(style.dash.toPx(), style.gap.toPx())),
                ),
        )
    }
