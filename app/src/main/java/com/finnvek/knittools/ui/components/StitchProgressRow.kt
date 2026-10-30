package com.finnvek.knittools.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.theme.OnboardingDimens

/** Valmistuva silmukka täyttyy ylhäältä alas; [current] on nollasta alkava silmukan indeksi. */
@Composable
fun StitchProgressRow(
    total: Int,
    done: Int,
    craft: CraftType,
    color: Color,
    modifier: Modifier = Modifier,
    current: Int? = null,
    animateLastOnEntry: Boolean = false,
) {
    val description = stringResource(R.string.stitch_progress_a11y, done, total)
    val currentColor = MaterialTheme.colorScheme.primary
    val upcomingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OnboardingDimens.UPCOMING_STITCH_ALPHA)
    // Aloitus muistetaan: juhlintamerkin tallennus ei saa katkaista käynnistynyttä animaatiota.
    var entryAnimationStarted by remember { mutableStateOf(!animateLastOnEntry) }
    LaunchedEffect(Unit) { entryAnimationStarted = true }
    val fills =
        List(total) { index ->
            val completed = index < done && (entryAnimationStarted || index != done - 1)
            animateFloatAsState(
                targetValue = if (completed) 1f else 0f,
                animationSpec = tween(OnboardingDimens.GROWTH_DURATION_MILLIS),
                label = "stitchFill",
            )
        }
    Spacer(
        modifier
            .width(OnboardingDimens.StitchWidth * total + OnboardingDimens.StitchGap * (total - 1).coerceAtLeast(0))
            .height(OnboardingDimens.StitchHeight)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(done.toFloat(), 0f..total.toFloat())
            }.drawWithCache {
                val width = OnboardingDimens.StitchWidth.toPx()
                val gap = OnboardingDimens.StitchGap.toPx()
                val outlineWidth = OnboardingDimens.StitchOutlineWidth.toPx()
                val strands =
                    if (craft ==
                        CraftType.CROCHET
                    ) {
                        crochetProgressStrands(width, color)
                    } else {
                        knitProgressStrands(width, color)
                    }
                val edge = color.shade(if (craft == CraftType.CROCHET) CROCHET_EDGE else KNIT_EDGE)
                onDrawBehind {
                    fills.forEachIndexed { index, fill ->
                        translate(left = index * (width + gap)) {
                            val outline = if (index == current) currentColor else upcomingColor
                            if (fill.value < 1f) {
                                strands.forEach { drawFabricStrand(it, outline, outlineWidth, filled = false) }
                            }
                            if (fill.value > 0f) {
                                clipRect(bottom = size.height * fill.value) {
                                    strands.forEach { drawFabricStrand(it, edge, width * FILLED_EDGE_RATIO) }
                                }
                            }
                        }
                    }
                }
            },
    )
}

private fun knitProgressStrands(
    width: Float,
    color: Color,
): List<FabricStrand> {
    val thickness = width * KNIT_THICKNESS_RATIO
    val light = color.shade(KNIT_LIGHT)
    val dark = color.shade(KNIT_DARK)
    val leftFill = crossGradient(thickness / 2f, light.shade(KNIT_HIGHLIGHT), light)
    val rightFill = crossGradient(thickness / 2f, dark, dark.shade(KNIT_SHADOW))
    return listOf(-1f, 1f).map { side ->
        strandBetween(
            from = Offset(width / 2f + side * width * KNIT_TOP_X, width * KNIT_TOP_Y),
            to = Offset(width / 2f + side * width * KNIT_BOTTOM_X, width * KNIT_BOTTOM_Y),
            thickness = thickness,
            fill = if (side < 0f) leftFill else rightFill,
            extraLength = width * KNIT_EXTRA_LENGTH,
        )
    }
}

private fun crochetProgressStrands(
    width: Float,
    color: Color,
): List<FabricStrand> {
    val origin = Offset(width * CROCHET_START_X, width * CROCHET_START_Y)
    val longThickness = width * CROCHET_LONG_THICKNESS
    val shortThickness = width * CROCHET_SHORT_THICKNESS
    return listOf(
        strandBetween(
            from = origin,
            to = origin + Offset(width * CROCHET_LONG_X, -width * CROCHET_LONG_Y),
            thickness = longThickness,
            fill = crossGradient(longThickness / 2f, color.shade(CROCHET_MID_TOP), color.shade(CROCHET_MID_BOTTOM)),
        ),
        strandBetween(
            from = origin + Offset(width * CROCHET_SHORT_START_X, -width * CROCHET_SHORT_START_Y),
            to = origin + Offset(width * CROCHET_SHORT_END_X, -width * CROCHET_SHORT_END_Y),
            thickness = shortThickness,
            fill =
                crossGradient(
                    shortThickness / 2f,
                    color.shade(CROCHET_LIGHT_TOP),
                    color.shade(CROCHET_LIGHT_BOTTOM),
                ),
        ),
    )
}

// Hyväksytyn mockupin säikeiden suhteet ja varjostus, samassa koordinaatistossa kuin silmukan leveys.
private const val KNIT_TOP_X = 0.47f
private const val KNIT_TOP_Y = 0.225f
private const val KNIT_BOTTOM_X = 0.08f
private const val KNIT_BOTTOM_Y = 1.13f
private const val KNIT_THICKNESS_RATIO = 0.30f
private const val KNIT_EXTRA_LENGTH = 0.04f
private const val FILLED_EDGE_RATIO = 0.05f
private const val KNIT_LIGHT = 0.16f
private const val KNIT_DARK = -0.02f
private const val KNIT_HIGHLIGHT = 0.12f
private const val KNIT_SHADOW = -0.1f
private const val KNIT_EDGE = -0.28f
private const val CROCHET_START_X = 0.08f
private const val CROCHET_START_Y = 1.09f
private const val CROCHET_LONG_X = 0.6f
private const val CROCHET_LONG_Y = 0.77f
private const val CROCHET_LONG_THICKNESS = 0.34f
private const val CROCHET_SHORT_START_X = 0.36f
private const val CROCHET_SHORT_START_Y = 0.32f
private const val CROCHET_SHORT_END_X = 0.86f
private const val CROCHET_SHORT_END_Y = 0.73f
private const val CROCHET_SHORT_THICKNESS = 0.29f
private const val CROCHET_MID_TOP = 0.14f
private const val CROCHET_MID_BOTTOM = -0.08f
private const val CROCHET_LIGHT_TOP = 0.3f
private const val CROCHET_LIGHT_BOTTOM = 0.04f
private const val CROCHET_EDGE = -0.32f
