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
import com.finnvek.knittools.ui.theme.ComponentDimens

@Composable
fun AnimatedResultNumber(
    targetValue: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { ComponentDimens.AnimatedResultOffset.roundToPx() }

    AnimatedContent(
        targetState = targetValue,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(tween(ComponentDimens.AnimatedResultEnterDurationMillis, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        tween(ComponentDimens.AnimatedResultEnterDurationMillis, easing = FastOutSlowInEasing),
                    ) { offsetPx }
            ).togetherWith(
                fadeOut(tween(ComponentDimens.AnimatedResultExitDurationMillis, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        tween(ComponentDimens.AnimatedResultExitDurationMillis, easing = FastOutSlowInEasing),
                    ) { -offsetPx },
            )
        },
        label = "resultNumber",
    ) { value ->
        content(value)
    }
}
