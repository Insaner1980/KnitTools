package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.ui.theme.ProjectListDimens

@Composable
fun ProjectProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = ProjectListDimens.ProgressTrackHeight,
) {
    // Pohja piirretään onSurfacen alfalla eikä taustavärillä: taustan päällä taustaväri oli näkymätön.
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = ProjectListDimens.ProgressTrackAlpha)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
