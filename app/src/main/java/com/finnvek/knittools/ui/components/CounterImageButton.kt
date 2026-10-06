package com.finnvek.knittools.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun CounterImageButton(
    @DrawableRes imageRes: Int,
    contentDescription: String,
    visualSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualOffsetY: Dp = 0.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .semantics {
                    this.contentDescription = contentDescription
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        PressableButtonImage(imageRes, visualSize, interactionSource, enabled, visualOffsetY)
    }
}

/**
 * Kolmiulotteinen nappi, jonka vieressä on teksti. Koko pilleri on yksi kosketuskohde, ja
 * ruudunlukija kuulee vain [contentDescription]-tekstin. Painallus näkyy napissa samoin kuin
 * laskurissa. [badge] (esim. Pro-merkki) piirretään tekstin ja napin väliin; sen merkitys
 * kuuluu sisällyttää [contentDescription]-tekstiin, koska pilleri tyhjentää lastensa semantiikan.
 */
@Composable
fun LabeledCounterImageButton(
    @DrawableRes imageRes: Int,
    label: String,
    visualSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
    badge: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.knitToolsColors.actionContainer,
        shadowElevation = CounterDimens.LabeledImageButtonElevation,
        modifier =
            modifier
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.knitToolsColors.primaryReadable,
                modifier =
                    Modifier.padding(
                        start = CounterDimens.LabeledImageButtonTextStart,
                        end = CounterDimens.LabeledImageButtonTextEnd,
                    ),
            )
            if (badge != null) {
                badge()
                Spacer(modifier = Modifier.width(CounterDimens.LabeledImageButtonTextEnd))
            }
            PressableButtonImage(imageRes, visualSize, interactionSource, enabled)
        }
    }
}

// Painallus painaa napin hieman alas ja pienemmäksi, kuten fyysisen napin.
@Composable
private fun PressableButtonImage(
    @DrawableRes imageRes: Int,
    visualSize: Dp,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    visualOffsetY: Dp = 0.dp,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "counterImageButtonPress",
    )
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
            Modifier
                .size(visualSize)
                .offset(y = visualOffsetY)
                .graphicsLayer {
                    val scale = 1f - pressProgress * 0.018f
                    scaleX = scale
                    scaleY = scale
                    translationY = pressProgress * 1.5.dp.toPx()
                    alpha = if (enabled) 1f else 0.62f
                },
    )
}
