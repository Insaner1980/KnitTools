package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.ui.theme.ProMarkerDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun ProMarker(
    access: ProActionAccess,
    modifier: Modifier = Modifier,
) {
    val description = proMarkerDescription(access) ?: return
    val colors = MaterialTheme.knitToolsColors
    val background = if (access == ProActionAccess.Locked) colors.proMarkerContainer else Color.Transparent
    Box(
        modifier =
            modifier
                .height(ProMarkerDimens.Height)
                .background(background, RoundedCornerShape(ProMarkerDimens.CornerRadius))
                .drawWithCache {
                    val inset = ProMarkerDimens.StitchInset.toPx()
                    val radius = ProMarkerDimens.CornerRadius.toPx() - inset
                    val stitch =
                        Stroke(
                            width = ProMarkerDimens.StitchWidth.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(ProMarkerDimens.StitchDash.toPx(), ProMarkerDimens.StitchGap.toPx()),
                                ),
                        )
                    onDrawBehind {
                        drawRoundRect(
                            color = colors.proMarkerStitch,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(radius),
                            style = stitch,
                        )
                    }
                }.clearAndSetSemantics { contentDescription = description }
                .padding(horizontal = ProMarkerDimens.HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(if (access == ProActionAccess.Locked) R.string.pro_badge else R.string.pro_marker_trial),
            style = MaterialTheme.typography.labelSmall,
            color = colors.proMarkerContent,
        )
    }
}

/** Kuvaus yhdistyy tavalliseen toimintoriviin; semantiikan tyhjentävä pilleri lisää sen omaan nimeensä. */
@Composable
fun proMarkerDescription(access: ProActionAccess): String? =
    when (access) {
        ProActionAccess.Allowed,
        ProActionAccess.Unknown,
        -> null
        ProActionAccess.Locked -> stringResource(R.string.pro_badge_locked_description)
        ProActionAccess.Trial -> stringResource(R.string.pro_badge_trial_description)
    }
