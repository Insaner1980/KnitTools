package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedResultNumber(
    targetValue: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { 6.dp.roundToPx() }

    AnimatedContent(
        targetState = targetValue,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(200, easing = FastOutSlowInEasing)) { offsetPx }
            ).togetherWith(
                fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { -offsetPx },
            )
        },
        label = "resultNumber",
    ) { value ->
        content(value)
    }
}
