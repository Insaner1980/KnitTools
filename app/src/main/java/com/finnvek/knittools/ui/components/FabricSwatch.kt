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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.finnvek.knittools.ui.theme.FabricSwatchDimens
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot

/** Tilkun pinta: projektin käsityötyyppi tai lanka, jolla ei ole kuvaa. */
enum class FabricKind { KNIT, CROCHET, YARN }

/**
 * Neulepintatilkku ikonin sijaan. Neule on pystysuuntaisia ⋁-silmukoita ja virkkaus vinoja
 * säiepareja, joten käsityötyypin erottaa myös pienessä koossa. Valo tulee ylävasemmalta
 * kuten laskurin kolmiulotteisissa napeissa.
 */
@Composable
fun FabricSwatch(
    kind: FabricKind,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier.clipToBounds().drawWithCache {
            val fabric =
                when (kind) {
                    FabricKind.KNIT -> knitFabric(size, color)
                    FabricKind.CROCHET -> crochetFabric(size, color)
                    FabricKind.YARN -> yarnFabric(size, color)
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
                    translate(strand.center.x, strand.center.y) {
                        rotate(strand.degrees, pivot = Offset.Zero) {
                            val topLeft = Offset(-strand.halfLength, -strand.halfThickness)
                            val ovalSize = Size(strand.halfLength * 2f, strand.halfThickness * 2f)
                            drawOval(strand.fill, topLeft, ovalSize)
                            drawOval(fabric.edge, topLeft, ovalSize, style = Stroke(fabric.edgeWidth))
                        }
                    }
                }
                drawRect(vignette)
            }
        },
    )
}

/** Yksi säie tai silmukan jalka: soikio, jonka pitkä akseli on kulmassa [degrees]. */
private class FabricStrand(
    val center: Offset,
    val halfLength: Float,
    val halfThickness: Float,
    val degrees: Float,
    val fill: Brush,
)

private class Fabric(
    val background: Color,
    val edge: Color,
    val edgeWidth: Float,
    val strands: List<FabricStrand>,
)

/** Säie kahden pisteen välillä. Liukuväri on säikeen omassa koordinaatistossa poikittain. */
private fun strandBetween(
    from: Offset,
    to: Offset,
    thickness: Float,
    fill: Brush,
    extraLength: Float = 0f,
): FabricStrand {
    val delta = to - from
    return FabricStrand(
        center = (from + to) / 2f,
        halfLength = hypot(delta.x, delta.y) / 2f + extraLength,
        halfThickness = thickness / 2f,
        degrees = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat(),
        fill = fill,
    )
}

// Sävytys samaan tapaan kuin mockupissa: positiivinen vaalentaa kohti valkoista, negatiivinen tummentaa.
private fun Color.shade(amount: Float): Color =
    if (amount >= 0f) {
        Color(red + (1f - red) * amount, green + (1f - green) * amount, blue + (1f - blue) * amount, alpha)
    } else {
        Color(red * (1f + amount), green * (1f + amount), blue * (1f + amount), alpha)
    }

private fun crossGradient(
    halfThickness: Float,
    top: Color,
    bottom: Color,
): Brush = Brush.verticalGradient(listOf(top, bottom), startY = -halfThickness, endY = halfThickness)

private fun knitFabric(
    size: Size,
    color: Color,
): Fabric {
    val unit = size.minDimension / FabricSwatchDimens.ProjectReferenceSize
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
): Fabric {
    val unit = size.minDimension / FabricSwatchDimens.ProjectReferenceSize
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
): Fabric {
    val unit = size.minDimension / FabricSwatchDimens.YarnReferenceSize
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
