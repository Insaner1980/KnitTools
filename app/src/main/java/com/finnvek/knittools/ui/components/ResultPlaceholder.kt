package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Tuloksen paikka ennen kuin laskimessa on tarpeeksi syötteitä. Katkoviiva kertoo tyhjästä
 * paikasta, joten ruudun alaosa ei näytä keskeneräiseltä eikä tulos ilmesty tyhjästä.
 */
@Composable
fun ResultPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.knitToolsColors.emptyStateText
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = ComponentDimens.ResultPlaceholderStrokeWidth.toPx()
                    val dash = ComponentDimens.ResultPlaceholderDash.toPx()
                    val radius = ComponentDimens.ResultPlaceholderCornerRadius.toPx()
                    drawRoundRect(
                        color = color,
                        cornerRadius = CornerRadius(radius),
                        style =
                            Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                            ),
                    )
                }.padding(ComponentDimens.ContentPadding),
    )
}
