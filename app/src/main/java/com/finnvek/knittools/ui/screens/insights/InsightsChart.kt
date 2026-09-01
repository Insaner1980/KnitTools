package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.uppercaseForDisplay
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Päivä- tai kuukausipylväät. Korkein pylväs on itse asteikko, joten erillistä
 * maksimileimaa ei piirretä — se toisti valitun lukeman aina kun korkein oli valittuna.
 */
@Composable
@Suppress("kotlin:S107") // Kaavion mittakaava-, valinta- ja saavutettavuustiedot pidetään eksplisiittisinä.
internal fun InsightsChart(
    buckets: List<InsightsChartBucket>,
    interval: PaceGroupingInterval,
    timeRange: TimeRange,
    today: LocalDate,
    selectedIndex: Int?,
    contentDescription: String,
    onSelectBucket: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = buckets.maxOfOrNull { it.totalMinutes } ?: 0
    val nextLabel = stringResource(R.string.insights_chart_a11y_next)
    val previousLabel = stringResource(R.string.insights_chart_a11y_previous)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                // Kaavio luetaan yhtenä elementtinä, mutta se ei saa olla umpi:
                // valinta liikkuu ruudunlukijalla omilla toiminnoillaan.
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    customActions =
                        listOf(
                            CustomAccessibilityAction(nextLabel) {
                                moveChartSelection(buckets, selectedIndex, STEP_NEXT, onSelectBucket)
                            },
                            CustomAccessibilityAction(previousLabel) {
                                moveChartSelection(buckets, selectedIndex, STEP_PREVIOUS, onSelectBucket)
                            },
                        )
                },
    ) {
        ChartPlot(
            buckets = buckets,
            timeRange = timeRange,
            selectedIndex = selectedIndex,
            maxMinutes = maxMinutes,
            onSelectBucket = onSelectBucket,
        )
        ChartAxisLabels(
            buckets = buckets,
            interval = interval,
            timeRange = timeRange,
            today = today,
        )
    }
}

/**
 * Siirtää valintaa yhden ämpärin verran ruudunlukijan toiminnolle. Palauttaa false,
 * kun mitään ei liikahda — tyhjässä kaaviossa ja reunassa — jolloin alusta osaa
 * kertoa ettei liike jatku, samoin kuin sisäänrakennetut vieritystoiminnot.
 * Ilman valintaa molemmat suunnat osuvat ensimmäiseen ämpäriin, koska
 * askeleelle ei ole vielä lähtöpistettä.
 */
internal fun moveChartSelection(
    buckets: List<InsightsChartBucket>,
    selectedIndex: Int?,
    step: Int,
    onSelectBucket: (Int) -> Unit,
): Boolean {
    if (buckets.isEmpty()) return false
    val target = if (selectedIndex == null) 0 else (selectedIndex + step).coerceIn(0, buckets.lastIndex)
    if (target == selectedIndex) return false
    onSelectBucket(target)
    return true
}

@Composable
@Suppress("kotlin:S3776") // Piirtoalueen ehdot kuvaavat suoraan pylväiden valinta- ja akselitiloja.
private fun ChartPlot(
    buckets: List<InsightsChartBucket>,
    timeRange: TimeRange,
    selectedIndex: Int?,
    maxMinutes: Int,
    onSelectBucket: (Int) -> Unit,
) {
    val barWidth = chartBarWidth(timeRange)
    val fallbackBarColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.ChartGridlineAlpha)
    val selectionColor = MaterialTheme.colorScheme.onSurface
    // Pinon värit tulevat samasta apurista kuin projektilistan pisteet, jolloin
    // kaavio ja lista luetaan samoilla väreillä.
    // Silmukkaruudukko on sama joka pylväälle: rakennetaan kerran ja leikataan
    // jokaisen pylvään siluetin sisään. 31 pylvään kuukausikaaviossa tämä on yksi
    // Path kehykseltä eikä tuhansia drawLine-kutsuja.
    val density = LocalDensity.current
    val stitchLattice =
        remember(barWidth, density) {
            with(density) {
                knitStitchLattice(
                    widthPx = barWidth.toPx(),
                    heightPx =
                        (InsightsDimens.ChartPlotHeight + InsightsDimens.ChartSelectionMarkerBand).toPx(),
                    targetStitchWidthPx = InsightsDimens.ChartStitchTargetWidth.toPx(),
                )
            }
        }
    val stitchShadowColor =
        MaterialTheme.colorScheme.scrim.copy(alpha = InsightsDimens.ChartStitchShadowAlpha)
    val yarnPalette = MaterialTheme.knitToolsColors.yarnPalette
    val segmentColors =
        remember(buckets, yarnPalette) {
            buckets
                .flatMap { bucket -> bucket.segments }
                .map { it.projectId }
                .distinct()
                .associateWith { yarnColorForId(it, yarnPalette) }
        }

    val haptics = LocalHapticFeedback.current

    // Eleiden oma sarakehaku; piirto laskee pitchin erikseen omasta DrawScopen koostaan.
    fun bucketIndexAt(
        x: Float,
        width: Int,
    ): Int {
        val pitch = width / buckets.size.toFloat()
        return (x / pitch).toInt().coerceIn(0, buckets.lastIndex)
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    chartPlotHeight(
                        bucketCount = buckets.size,
                        activeBucketCount = buckets.count { it.totalMinutes > 0 },
                    ) + InsightsDimens.ChartSelectionMarkerBand,
                )
                // Dokumentoitu poikkeus: kuukausinäkymässä 28-31 osumakohdetta ei mahdu
                // 48dp minimiin puhelimen leveydelle. Kosketus on täydentävä polku, sillä
                // asteikkoteksti ja lukemarivi kertovat arvot ilman vuorovaikutusta.
                .pointerInput(buckets.size) {
                    detectTapGestures { offset ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        onSelectBucket(bucketIndexAt(offset.x, size.width))
                    }
                }
                // Vetoele tekee kapeista kuukausipylväistä käytettäviä: 12dp osumatarkkuus
                // ei riitä napautukselle, mutta sormen seuraaminen ei vaadi tarkkuutta.
                .pointerInput(buckets.size) {
                    // Viimeksi valittu sarake pidetään eleen omassa muuttujassa. Parametrina
                    // saatu selectedIndex jäisi eleen käynnistyshetken arvoon, jolloin naksu
                    // toistuisi joka osoitintapahtumalla eikä vain sarakkeen vaihtuessa.
                    var lastSelectedIndex = -1
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (buckets.isEmpty()) return@detectHorizontalDragGestures
                            lastSelectedIndex = bucketIndexAt(offset.x, size.width)
                            onSelectBucket(lastSelectedIndex)
                        },
                    ) { change, _ ->
                        if (buckets.isEmpty()) return@detectHorizontalDragGestures
                        val index = bucketIndexAt(change.position.x, size.width)
                        if (index != lastSelectedIndex) {
                            lastSelectedIndex = index
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectBucket(index)
                        }
                    }
                },
    ) {
        val gridStrokePx = InsightsDimens.ChartGridlineStroke.toPx()
        val markerBandPx = InsightsDimens.ChartSelectionMarkerBand.toPx()
        // Perusviiva jättää alaosaan kaistan valinnan merkille.
        val baselineY = size.height - markerBandPx
        val barBottomY = baselineY - gridStrokePx
        if (buckets.isEmpty()) {
            drawChartGridlines(gridColor, baselineY, gridStrokePx)
            return@Canvas
        }
        val pitch = size.width / buckets.size.toFloat()
        // Valinta ilmaistaan merkillä perusviivassa. Pylväiden himmentäminen pudotti
        // tumman paletin alle graafisten elementtien 3:1 kontrastin, eikä mikään alfa
        // korjannut sitä — 3,3:1 täydellä peitolla ei voi vaimentua yli kolmeen.
        // Pystyapuviiva taas työntyi pylvään yläpuolelle ja luki tuntosarvena.
        val selectedBucketIndex = selectedIndex?.takeIf { it in buckets.indices }
        // Pylväiden väliin jää aina rako, jottei tiheä akseli lukeudu yhtenä möykkynä.
        val minGapPx = InsightsDimens.ChartBarMinGap.toPx()
        val barWidthPx = minOf(barWidth.toPx(), pitch - minGapPx).coerceAtLeast(1f)
        val cornerPx = InsightsDimens.ChartBarCorner.toPx()
        val minBarHeightPx = InsightsDimens.ChartMinBarHeight.toPx()
        val stitchMinBarHeightPx = InsightsDimens.ChartStitchMinBarHeight.toPx()
        val latticeHeightPx =
            (InsightsDimens.ChartPlotHeight + InsightsDimens.ChartSelectionMarkerBand).toPx()
        // Maksimiarvo päättyy tasan yläapuviivaan: viivan paksuus varataan pois
        // mitta-alueesta, jottei korkein pylväs työnny sen yli.
        val plotHeightPx = (barBottomY - gridStrokePx).coerceAtLeast(0f)

        buckets.forEachIndexed { index, bucket ->
            val left = pitch * index + (pitch - barWidthPx) / 2f
            // Tyhjä päivä on tyhjä sarake: jatkuva perusviiva kertoo sen jo, ja
            // erillinen tikku lukeutui pieneksi arvoksi.
            if (bucket.totalMinutes <= 0 || maxMinutes <= 0) return@forEachIndexed
            val barHeight =
                maxOf(minBarHeightPx, plotHeightPx * bucket.totalMinutes / maxMinutes.toFloat())
            val topLeft = Offset(left, barBottomY - barHeight)
            val barSize = Size(barWidthPx, barHeight)
            drawStackedBar(
                segments = bucket.segments,
                totalMinutes = bucket.totalMinutes,
                colors = segmentColors,
                fallbackColor = fallbackBarColor,
                topLeft = topLeft,
                barSize = barSize,
                cornerPx = cornerPx,
            )
            // Neulepinta pylväiden päälle, mutta niiden siluetin sisään. Matalimmat
            // pylväät jätetään sileäksi: yksi silmukkarivi lukee tahrana.
            if (barHeight >= stitchMinBarHeightPx) {
                clipPath(barPath(topLeft, barSize, cornerPx)) {
                    // Ruudukon alareuna perusviivaan, jolloin silmukat lepäävät rivikorkeudella
                    // ja katkeavat pylvään yläreunassa kesken rivin — kuten kesken jäänyt työ.
                    translate(left = topLeft.x, top = barBottomY - latticeHeightPx) {
                        drawPath(
                            path = stitchLattice.path,
                            color = stitchShadowColor,
                            style = Stroke(width = stitchLattice.strokeWidthPx, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }

        // Apuviivat pylväiden päälle, jottei korkein pylväs peitä maksimiviivaa.
        drawChartGridlines(gridColor, baselineY, gridStrokePx)

        // Merkkiviiva apuviivojen jälkeen, jottei perusviiva peitä sitä.
        selectedBucketIndex?.let { index ->
            val markerHeightPx = InsightsDimens.ChartSelectionMarkerHeight.toPx()
            val markerLeft = pitch * index + (pitch - barWidthPx) / 2f
            drawRect(
                color = selectionColor,
                topLeft = Offset(markerLeft, baselineY + (markerBandPx - markerHeightPx) / 2f),
                size = Size(barWidthPx, markerHeightPx),
            )
        }
    }
}

/**
 * Pylväs, joka on jaettu projektien värisiin osiin. Osat piirretään pylvään
 * silhuetin sisään, jolloin vain ylin osa saa pyöristetyn lakin ja pino pysyy
 * yhtenä muotona. Viimeinen osa ottaa pyöristysjäännöksen, jottei osien väliin
 * jää pikselin rakoa.
 */
private fun DrawScope.drawStackedBar(
    segments: List<InsightsChartSegment>,
    totalMinutes: Int,
    colors: Map<Long, Color>,
    fallbackColor: Color,
    topLeft: Offset,
    barSize: Size,
    cornerPx: Float,
) {
    if (segments.isEmpty() || totalMinutes <= 0) {
        drawBarFromBaseline(fallbackColor, topLeft, barSize, cornerPx)
        return
    }
    clipPath(barPath(topLeft, barSize, cornerPx)) {
        var bottom = topLeft.y + barSize.height
        segments.forEachIndexed { index, segment ->
            val height =
                if (index == segments.lastIndex) {
                    bottom - topLeft.y
                } else {
                    barSize.height * segment.minutes / totalMinutes.toFloat()
                }
            if (height <= 0f) return@forEachIndexed
            drawRect(
                color = colors[segment.projectId] ?: fallbackColor,
                topLeft = Offset(topLeft.x, bottom - height),
                size = Size(barSize.width, height),
            )
            bottom -= height
        }
    }
}

/**
 * Pylväs, jonka vain yläkulmat ovat pyöristetyt. Nelikulmainen pohja pitää pylvään
 * kiinni perusviivassa; kaikkien kulmien pyöristys sai matalat pylväät leijumaan.
 */
private fun DrawScope.drawBarFromBaseline(
    color: Color,
    topLeft: Offset,
    barSize: Size,
    cornerPx: Float,
) {
    if (barSize.width <= 0f || barSize.height <= 0f) return
    drawPath(path = barPath(topLeft, barSize, cornerPx), color = color)
}

/**
 * Silmukan mitoitus yhdelle pylväälle. Erillinen puhtaana funktiona, koska
 * [knitStitchLattice] rakentaa Compose-Pathin jota ei voi luoda JVM-testissä.
 */
internal data class KnitStitchMetrics(
    val columns: Int,
    val stitchWidthPx: Float,
    val stitchHeightPx: Float,
    val strokeWidthPx: Float,
)

/**
 * Sarakemäärä pyöristetään tavoiteleveydestä, ja silmukka on leveämpi kuin korkea
 * kuten oikeassa sileneuleessa. Viivan paksuus seuraa **silmukkaa** eikä pylvästä:
 * pylvään leveyteen sidottuna kapea kuukausipylväs olisi ollut lähes umpivarjoa ja
 * menettänyt kontrastinsa taustaan.
 */
internal fun knitStitchMetrics(
    widthPx: Float,
    targetStitchWidthPx: Float,
): KnitStitchMetrics {
    val columns =
        if (widthPx <= 0f || targetStitchWidthPx <= 0f) {
            MIN_STITCH_COLUMNS
        } else {
            (widthPx / targetStitchWidthPx).roundToInt().coerceAtLeast(MIN_STITCH_COLUMNS)
        }
    val stitchWidth = (widthPx / columns).coerceAtLeast(1f)
    return KnitStitchMetrics(
        columns = columns,
        stitchWidthPx = stitchWidth,
        stitchHeightPx = (stitchWidth * InsightsDimens.ChartStitchAspect).coerceAtLeast(1f),
        strokeWidthPx = (stitchWidth * InsightsDimens.ChartStitchStrokeRatio).coerceAtLeast(1f),
    )
}

internal data class KnitStitchLattice(
    val path: Path,
    val strokeWidthPx: Float,
)

/**
 * Sileneuleen silmukkaruudukko yhden pylvään levyisenä ja koko plotin korkuisena.
 * Silmukka on ⋁: jalat tulevat alas yläkulmista ja kohtaavat alareunan keskellä.
 * Sarakkeet ovat kohdakkain kuten oikeassa sileneuleessa, ja rivit ladotaan alhaalta
 * ylös, jolloin ruudukon alareuna osuu aina perusviivaan.
 */
internal fun knitStitchLattice(
    widthPx: Float,
    heightPx: Float,
    targetStitchWidthPx: Float,
): KnitStitchLattice {
    val metrics = knitStitchMetrics(widthPx, targetStitchWidthPx)
    val path = Path()
    if (widthPx <= 0f || heightPx <= 0f) return KnitStitchLattice(path, metrics.strokeWidthPx)

    var bottom = heightPx
    while (bottom > 0f) {
        val top = bottom - metrics.stitchHeightPx
        for (column in 0 until metrics.columns) {
            val left = column * metrics.stitchWidthPx
            path.moveTo(left, top)
            path.lineTo(left + metrics.stitchWidthPx / 2f, bottom)
            path.lineTo(left + metrics.stitchWidthPx, top)
        }
        bottom -= metrics.stitchHeightPx
    }
    return KnitStitchLattice(path = path, strokeWidthPx = metrics.strokeWidthPx)
}

private fun barPath(
    topLeft: Offset,
    barSize: Size,
    cornerPx: Float,
): Path {
    val radius =
        CornerRadius(cornerPx.coerceAtMost(barSize.width / 2f).coerceAtMost(barSize.height))
    return Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(offset = topLeft, size = barSize),
                topLeft = radius,
                topRight = radius,
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius.Zero,
            ),
        )
    }
}

private fun DrawScope.drawChartGridlines(
    color: Color,
    baselineY: Float,
    strokeWidth: Float,
) {
    drawLine(
        color = color,
        start = Offset(0f, strokeWidth / 2f),
        end = Offset(size.width, strokeWidth / 2f),
        strokeWidth = strokeWidth,
    )
    drawLine(
        color = color,
        start = Offset(0f, baselineY - strokeWidth / 2f),
        end = Offset(size.width, baselineY - strokeWidth / 2f),
        strokeWidth = strokeWidth,
    )
}

/**
 * Akselileimat sijoitetaan ämpärin kohdalle painotetuilla soluilla, jolloin leima
 * osuu pylvään keskelle myös kun ämpäreitä on 31. Leima saa levitä naapurisolun
 * tyhjän tilan päälle, koska kapea solu ei mahduta edes kahta numeroa.
 */
@Composable
private fun ChartAxisLabels(
    buckets: List<InsightsChartBucket>,
    interval: PaceGroupingInterval,
    timeRange: TimeRange,
    today: LocalDate,
) {
    if (buckets.isEmpty()) return
    val maxLabels =
        when {
            timeRange == TimeRange.THIS_WEEK -> buckets.size
            interval == PaceGroupingInterval.MONTH -> MONTH_AXIS_MAX_LABELS
            interval == PaceGroupingInterval.WEEK -> WEEK_AXIS_MAX_LABELS
            else -> DAY_AXIS_MAX_LABELS
        }
    val lastIndex = buckets.lastIndex
    // Oikea reuna on "tänään" vain kun se todella on tämä päivä.
    val showsToday =
        interval == PaceGroupingInterval.DAY && buckets[lastIndex].bucketStart == today
    val labelled =
        remember(buckets.size, maxLabels, showsToday) {
            val clearance = todayLabelClearance(buckets.size)
            axisLabelIndices(buckets.size, maxLabels)
                .filterNot { showsToday && lastIndex - it < clearance }
                .toSet()
        }
    // Reunoihin pinnataan vain tiheällä akselilla, jossa uloimmat solut ovat kapeita.
    // Kahdella ämpärillä solu on puoli kaaviota ja pinnattu leima jäi näkyvästi
    // oman pylväänsä ohi.
    val pinsToEdges = buckets.size > SPARSE_AXIS_MAX_BUCKETS

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(InsightsDimens.ChartAxisBandHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buckets.forEachIndexed { index, bucket ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment =
                    when {
                        !pinsToEdges -> Alignment.Center
                        index == 0 -> Alignment.CenterStart
                        index == lastIndex -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
            ) {
                if (showsToday && index == lastIndex) {
                    ChartAxisLabel(text = stringResource(R.string.insights_chart_axis_today))
                } else if (index in labelled) {
                    ChartAxisLabel(text = axisLabel(bucket.bucketStart, interval, timeRange))
                }
            }
        }
    }
}

@Composable
private fun ChartAxisLabel(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontSize = InsightsDimens.ChartAxisLabelFontSize,
                fontWeight = FontWeight.SemiBold,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = Modifier.wrapContentWidth(unbounded = true),
    )
}

/** Pylvään leveys vaihtelee aikavälin ämpärimäärän mukaan; pyöristys on kaikilla sama. */
private fun chartBarWidth(timeRange: TimeRange): Dp =
    when (timeRange) {
        TimeRange.THIS_WEEK -> InsightsDimens.ChartWeekBarWidth
        TimeRange.THIS_MONTH -> InsightsDimens.ChartMonthBarWidth
        TimeRange.ALL_TIME -> InsightsDimens.ChartAllTimeBarWidth
    }

@Composable
internal fun bucketLabel(
    bucketStart: LocalDate,
    interval: PaceGroupingInterval,
): String {
    val locale = currentInsightsLocale()
    val skeleton =
        when (interval) {
            PaceGroupingInterval.MONTH -> "yMMMM"
            PaceGroupingInterval.WEEK -> "MMMd"
            PaceGroupingInterval.DAY -> "EMMMd"
        }
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    val label = bucketStart.format(formatter)
    // Versaalit sopivat viikonpäivälyhenteelle, eivät kuukauden nimelle: "HEINÄKUU 2026" huutaa.
    return if (interval == PaceGroupingInterval.MONTH) label else label.uppercaseForDisplay(locale)
}

/**
 * Tick-leima on lyhin mahdollinen: viikolla viikonpäivä, kuukaudessa päivän numero,
 * All Timessa kuukauden lyhenne. Lyhenne on oikea muoto akselilla, vaikka se ei
 * kelpaa lauseeseen.
 */
@Composable
private fun axisLabel(
    bucketStart: LocalDate,
    interval: PaceGroupingInterval,
    timeRange: TimeRange,
): String {
    val locale: Locale = currentInsightsLocale()
    val skeleton =
        when {
            interval == PaceGroupingInterval.MONTH -> "MMM"
            // Viikkoakselilla pelkkä päivänumero olisi monitulkintainen kuukausirajan yli.
            interval == PaceGroupingInterval.WEEK -> "MMMd"
            timeRange == TimeRange.THIS_WEEK -> "E"
            else -> "d"
        }
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return bucketStart.format(formatter)
}

internal const val STEP_NEXT = 1
internal const val STEP_PREVIOUS = -1
private const val DAY_AXIS_MAX_LABELS = 5
private const val MONTH_AXIS_MAX_LABELS = 6
private const val WEEK_AXIS_MAX_LABELS = 4

/**
 * "today" on moninkertaisesti numeroleiman levyinen ja se piirretään oikeaan reunaan
 * ilman solurajaa, joten sen alta on siivottava leimoja pois. Ilman tätä 18 päivän
 * akselille tuli "17oday".
 *
 * Varaus on nolla lyhyillä aikaväleillä: viikkonäkymässä solu on yli 50 dp leveä ja
 * leima on kolmen merkin viikonpäivä, jolloin törmäystä ei voi tulla. Kiinteä kolmen
 * solun varaus söi kolmen päivän viikolta **kaikki** leimat ja jätti akselille
 * pelkän "today".
 */
internal fun todayLabelClearance(bucketCount: Int): Int =
    if (bucketCount <= TODAY_CLEARANCE_MIN_BUCKETS) 0 else TODAY_LABEL_CLEARANCE

/**
 * Harva kaavio ei tarvitse täyttä korkeutta: yksi pylväs 168 dp:n plotissa näytti
 * virheeltä, ei vähäiseltä datalta.
 *
 * Ratkaiseva luku on **montako ämpäriä kantaa dataa**, ei akselin pituus: kahdeksantoista
 * päivän akseli, jolla on kaksi pylvästä, on yhtä tyhjä kuin kahden ämpärin akseli.
 */
internal fun chartPlotHeight(
    bucketCount: Int,
    activeBucketCount: Int,
): Dp =
    when {
        activeBucketCount <= SPARSE_PLOT_MAX_BARS -> InsightsDimens.ChartPlotHeightSparse
        bucketCount <= SPARSE_PLOT_MAX_BUCKETS -> InsightsDimens.ChartPlotHeightSparse
        activeBucketCount <= MEDIUM_PLOT_MAX_BARS -> InsightsDimens.ChartPlotHeightMedium
        bucketCount <= MEDIUM_PLOT_MAX_BUCKETS -> InsightsDimens.ChartPlotHeightMedium
        else -> InsightsDimens.ChartPlotHeight
    }

internal const val TODAY_LABEL_CLEARANCE = 3
private const val TODAY_CLEARANCE_MIN_BUCKETS = 7
private const val SPARSE_PLOT_MAX_BUCKETS = 4
private const val MEDIUM_PLOT_MAX_BUCKETS = 12
private const val SPARSE_PLOT_MAX_BARS = 2
private const val MEDIUM_PLOT_MAX_BARS = 5

/** Kapeinkin pylväs tarvitsee kaksi saraketta, jotta pinta lukee neuleena eikä raitana. */
internal const val MIN_STITCH_COLUMNS = 2

/**
 * Tätä harvemmalla akselilla jokainen leima keskitetään oman pylväänsä kohdalle.
 * Viiden ämpärin viikkokaaviossa solu on 72 dp leveä, jolloin reunaan pinnattu leima
 * jäi 14–18 dp oman pylväänsä ohi. Kuukausiakselilla (19–31 ämpäriä) pinnaus on
 * edelleen tarpeen, jottei ensimmäinen ja viimeinen leima leikkaudu plotin reunaan.
 */
internal const val SPARSE_AXIS_MAX_BUCKETS = 8
