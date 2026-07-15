package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.ui.theme.SectionLabelStyle

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = text.localizedUppercase(),
        style = SectionLabelStyle,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}
