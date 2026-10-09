package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Tilamerkki. Oletuksena aktiivinen tila (`actionContainer`); muut tilat, kuten varastossa oleva lanka,
 * antavat omat värinsä, jotta muoto ja koko pysyvät kaikkialla samoina.
 */
@Composable
fun BadgePill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.knitToolsColors.actionContainer,
    contentColor: Color = MaterialTheme.knitToolsColors.primaryReadable,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(containerColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.localizedUppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
