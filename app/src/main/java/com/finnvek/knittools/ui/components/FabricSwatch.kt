package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.ui.theme.FabricSwatchDimens
import kotlin.math.ceil

/** Tilkun pinta: projektin käsityötyyppi tai lanka, jolla ei ole kuvaa. */
enum class FabricKind { KNIT, CROCHET, YARN }

/**
 * Neulepintatilkku ikonin sijaan. Neule on pystysuuntaisia ⋁-silmukoita ja virkkaus vinoja
 * säiepareja, joten käsityötyypin erottaa myös pienessä koossa. Valo tulee ylävasemmalta
 * kuten laskurin kolmiulotteisissa napeissa.
 * [stitchScale] pitää silmukan perusyksikön vakiona, vaikka tilkun korkeus muuttuu.
 */
@Composable
fun FabricSwatch(
    kind: FabricKind,
    color: Color,
    modifier: Modifier = Modifier,
    stitchScale: Dp? = null,
) {
    Spacer(
        modifier.clipToBounds().drawWithCache {
            if (size.minDimension <= 0f) return@drawWithCache onDrawBehind { }
            val fabric =
                when (kind) {
                    FabricKind.KNIT -> knitFabric(size, color, stitchScale?.toPx())
                    FabricKind.CROCHET -> crochetFabric(size, color, stitchScale?.toPx())
                    FabricKind.YARN -> yarnFabric(size, color, stitchScale?.toPx())
                }
            val vignette =
                Brush.radialGradient(
                    FabricSwatchDimens.VignetteClearStop to Color.Transparent,
                    1f to Color.Black.copy(alpha = FabricSwatchDimens.VignetteAlpha),
                    center =
                        Offset(
                            size.width * FabricSwatchDimens.VignetteCenterX,
                            size.height * FabricSwatchDimens.VignetteCenterY,
                        ),
                    radius = size.minDimension * FabricSwatchDimens.VignetteRadius,
                )
            onDrawBehind {
                drawRect(fabric.background)
                fabric.strands.forEach { strand ->
                    drawFabricStrand(strand, fabric.edge, fabric.edgeWidth)
                }
                drawRect(vignette)
            }
        },
    )
}

private class Fabric(
    val background: Color,
    val edge: Color,
    val edgeWidth: Float,
    val strands: List<FabricStrand>,
)

private fun knitFabric(
    size: Size,
    color: Color,
    stitchUnit: Float?,
): Fabric {
    val unit = stitchUnit ?: size.minDimension / FabricSwatchDimens.ProjectReferenceSize
    val width = FabricSwatchDimens.KnitStitchWidth * unit
    val height = width * FabricSwatchDimens.KnitRowRatio
    val half = width * FabricSwatchDimens.KnitLegHalfThickness
    val lightLeg = color.shade(KNIT_LIGHT_LEG)
    val darkLeg = color.shade(KNIT_DARK_LEG)
    val leftFill = crossGradient(half, lightLeg.shade(KNIT_LEG_HIGHLIGHT), lightLeg)
    val rightFill = crossGradient(half, darkLeg, darkLeg.shade(KNIT_LEG_SHADOW))
    val columns = ceil(size.width / width).toInt() + 1
    val strands = mutableListOf<FabricStrand>()
    var row = -1
    while (row * height < size.height + height) {
        val y = row * height
        for (column in -1 until columns) {
            val x = column * width + width / 2f
            for (side in SIDES) {
                strands +=
                    strandBetween(
                        from =
                            Offset(
                                x + side * width * FabricSwatchDimens.KnitLegTopInset,
                                y - height * FabricSwatchDimens.KnitLegTopLift,
                            ),
                        to =
                            Offset(
                                x + side * width * FabricSwatchDimens.KnitLegBottomInset,
                                y + height * FabricSwatchDimens.KnitLegBottomDrop,
                            ),
                        thickness = half * 2f,
                        fill = if (side < 0f) leftFill else rightFill,
                        extraLength = width * FabricSwatchDimens.KnitLegExtraLength,
                    )
            }
        }
        row++
    }
    return Fabric(
        background = color.shade(FABRIC_GAP),
        edge = color.shade(KNIT_EDGE),
        edgeWidth = width * FabricSwatchDimens.KnitEdgeWidth,
        strands = strands,
    )
}

private fun crochetFabric(
    size: Size,
    color: Color,
    stitchUnit: Float?,
): Fabric {
    val unit = stitchUnit ?: size.minDimension / FabricSwatchDimens.ProjectReferenceSize
    val width = FabricSwatchDimens.CrochetStitchWidth * unit
    val height = width * FabricSwatchDimens.CrochetRowRatio
    val thickness = width * FabricSwatchDimens.CrochetStrandThickness
    val shortThickness = thickness * FabricSwatchDimens.CrochetShortStrandRatio
    val longFill = crossGradient(thickness / 2f, color.shade(CROCHET_MID_TOP), color.shade(CROCHET_MID_BOTTOM))
    val shortFill =
        crossGradient(shortThickness / 2f, color.shade(CROCHET_LIGHT_TOP), color.shade(CROCHET_LIGHT_BOTTOM))
    val strands = mutableListOf<FabricStrand>()
    var row = -1
    while (row * height < size.height + height * 2f) {
        val y = row * height
        var x = -width + if (row % 2 == 0) 0f else width / 2f
        while (x < size.width + width) {
            val x0 = x + width * FabricSwatchDimens.CrochetStartX
            val y0 = y + height * FabricSwatchDimens.CrochetStartDrop
            // pitkä säie kallistuu oikealle, lyhyt nousee sen puolivälistä: yhdessä vino ⋌-pari
            strands +=
                strandBetween(
                    Offset(x0, y0),
                    Offset(
                        x0 + width * FabricSwatchDimens.CrochetLongReach,
                        y0 - height * FabricSwatchDimens.CrochetLongRise,
                    ),
                    thickness,
                    longFill,
                )
            strands +=
                strandBetween(
                    Offset(
                        x0 + width * FabricSwatchDimens.CrochetShortStartX,
                        y0 - height * FabricSwatchDimens.CrochetShortStartRise,
                    ),
                    Offset(
                        x0 + width * FabricSwatchDimens.CrochetShortReach,
                        y0 - height * FabricSwatchDimens.CrochetShortRise,
                    ),
                    shortThickness,
                    shortFill,
                )
            x += width
        }
        row++
    }
    return Fabric(
        background = color.shade(FABRIC_GAP),
        edge = color.shade(CROCHET_EDGE),
        edgeWidth = width * FabricSwatchDimens.CrochetEdgeWidth,
        strands = strands,
    )
}

private fun yarnFabric(
    size: Size,
    color: Color,
    stitchUnit: Float?,
): Fabric {
    val unit = stitchUnit ?: size.minDimension / FabricSwatchDimens.YarnReferenceSize
    val ply = FabricSwatchDimens.YarnPly * unit
    val step = ply * FabricSwatchDimens.YarnRowStep
    val columnStep = ply * FabricSwatchDimens.YarnColumnStep
    val halfLength = ply * FabricSwatchDimens.YarnStrandHalfLength
    val halfThickness = ply * FabricSwatchDimens.YarnStrandHalfThickness
    val light = color.shade(YARN_LIGHT)
    val fill =
        Brush.linearGradient(
            0f to light.shade(YARN_HIGHLIGHT),
            0.5f to light,
            1f to color.shade(YARN_DARK),
            start = Offset(-halfLength, -halfThickness),
            end = Offset(halfLength, halfThickness),
        )
    val strands = mutableListOf<FabricStrand>()
    var row = -2
    while (row * step < size.height + step * 2f) {
        var column = -2
        while (column * columnStep < size.width + ply * 4f) {
            val cx = column * columnStep + if (row % 2 == 0) 0f else columnStep / 2f
            strands +=
                FabricStrand(
                    center = Offset(cx, row * step),
                    halfLength = halfLength,
                    halfThickness = halfThickness,
                    degrees = FabricSwatchDimens.YarnStrandAngle,
                    fill = fill,
                )
            column++
        }
        row++
    }
    return Fabric(color.shade(YARN_GAP), color.shade(YARN_EDGE), ply * FabricSwatchDimens.YarnEdgeWidth, strands)
}

private val SIDES = floatArrayOf(-1f, 1f)

// Sävytyskertoimet (mockupin mukaiset). Silmukoiden välit ovat tummimpia, valon puoleiset jalat vaaleimpia.
private const val FABRIC_GAP = -0.34f
private const val KNIT_LIGHT_LEG = 0.16f
private const val KNIT_DARK_LEG = -0.02f
private const val KNIT_LEG_HIGHLIGHT = 0.12f
private const val KNIT_LEG_SHADOW = -0.1f
private const val KNIT_EDGE = -0.28f
private const val CROCHET_MID_TOP = 0.14f
private const val CROCHET_MID_BOTTOM = -0.08f
private const val CROCHET_LIGHT_TOP = 0.3f
private const val CROCHET_LIGHT_BOTTOM = 0.04f
private const val CROCHET_EDGE = -0.32f
private const val YARN_GAP = -0.3f
private const val YARN_LIGHT = 0.1f
private const val YARN_HIGHLIGHT = 0.16f
private const val YARN_DARK = -0.06f
private const val YARN_EDGE = -0.34f
