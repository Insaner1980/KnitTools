package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle

@Composable
fun RollingCounter(
    count: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayMedium,
    contentDescription: String = count.toString(),
) {
    val countStr = count.toString()
    var previousCount by remember { mutableIntStateOf(count) }
    val goingUp = count >= previousCount

    LaunchedEffect(count) {
        previousCount = count
    }

    Row(
        modifier =
            modifier.clearAndSetSemantics {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        countStr.forEachIndexed { index, char ->
            val key = "${countStr.length}-$index-$char"
            AnimatedContent(
                targetState = key,
                transitionSpec = {
                    val direction = if (goingUp) 1 else -1
                    slideInVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullHeight -> direction * fullHeight },
                    ) togetherWith
                        slideOutVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetY = { fullHeight -> -direction * fullHeight },
                        )
                },
                label = "digit_$index",
            ) { targetKey ->
                // Uutetaan näytettävä merkki avaimesta ("length-index-char")
                val displayChar = targetKey.substringAfterLast('-')
                Text(
                    text = displayChar,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
