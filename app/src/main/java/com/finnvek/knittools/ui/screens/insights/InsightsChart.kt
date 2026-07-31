package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.uppercaseForDisplay
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Päivä- tai kuukausipylväät asteikolla. Käyttäjä näkee arvon ilman kosketusta:
 * maksimi on merkitty apuviivalle ja valittu ämpäri lukemarivillä.
 */
@Composable
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
    // Vihje on kertakäyttöinen opaste: kun käyttäjä on kerran koskenut kaavioon,
    // rivi on pelkkää kohinaa lukeman alla.
    var hasInteracted by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ChartReadout(
            bucket = selectedIndex?.let(buckets::getOrNull),
            interval = interval,
            fallbackLabel = buckets.firstOrNull()?.bucketStart,
            hasAnyData = maxMinutes > 0,
        )
        val nextLabel = stringResource(R.string.insights_chart_a11y_next)
        val previousLabel = stringResource(R.string.insights_chart_a11y_previous)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // Kaavio luetaan yhtenä elementtinä, mutta se ei saa olla umpi:
                    // valinta liikkuu ruudunlukijalla omilla toiminnoillaan.
                    .clearAndSetSemantics {
                        this.contentDescription = contentDescription
                        customActions =
                            listOf(
                                CustomAccessibilityAction(nextLabel) {
                                    if (buckets.isEmpty()) {
                                        false
                                    } else {
                                        onSelectBucket(((selectedIndex ?: -1) + 1).coerceAtMost(buckets.lastIndex))
                                        true
                                    }
                                },
                                CustomAccessibilityAction(previousLabel) {
                                    if (buckets.isEmpty()) {
                                        false
                                    } else {
                                        onSelectBucket(((selectedIndex ?: 0) - 1).coerceAtLeast(0))
                                        true
                                    }
                                },
                            )
                    },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(InsightsDimens.ChartScaleLabelBand))
                ChartPlot(
                    buckets = buckets,
                    timeRange = timeRange,
                    selectedIndex = selectedIndex,
                    maxMinutes = maxMinutes,
                    onSelectBucket = { index ->
                        hasInteracted = true
                        onSelectBucket(index)
                    },
                )
                ChartAxisLabels(
                    buckets = buckets,
                    interval = interval,
                    timeRange = timeRange,
                    today = today,
                )
            }
            if (maxMinutes > 0) {
                // Maksimi istuu kiinni yläapuviivassa, jolloin sitä ei lueta
                // oikeanpuoleisimman pylvään arvoksi eikä valitun päivän pariksi.
                Text(
                    text =
                        stringResource(
                            R.string.insights_chart_max_format,
                            durationText(DurationDisplayFormatter.fromMinutes(maxMinutes)),
                        ),
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontSize = InsightsDimens.ChartScaleLabelFontSize,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        if (!hasInteracted) {
            Text(
                text =
                    stringResource(
                        if (interval == PaceGroupingInterval.MONTH) {
                            R.string.insights_chart_hint_month
                        } else {
                            R.string.insights_chart_hint_day
                        },
                    ),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = InsightsDimens.ChartHintFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                modifier = Modifier.padding(top = InsightsDimens.ChartHintTopPadding),
            )
        }
    }
}

/**
 * Valitun ämpärin lukema. Rivillä on vain yksi kesto, jotta kahta eri merkityksistä
 * lukua ei lueta välinä; asteikon maksimi asuu plotin yläreunassa.
 */
@Composable
private fun ChartReadout(
    bucket: InsightsChartBucket?,
    interval: PaceGroupingInterval,
    fallbackLabel: LocalDate?,
    hasAnyData: Boolean,
) {
    val label = bucket?.bucketStart ?: fallbackLabel
    val labelText = label?.let { bucketLabel(it, interval) }.orEmpty()
    val hasValue = bucket != null && bucket.totalMinutes > 0
    val stacked = LocalDensity.current.fontScale > InsightsDimens.ChartReadoutStackFontScale

    // Lukema on live region kummassakin asettelussa: kun valinta siirtyy
    // ruudunlukijan toiminnolla, uusi arvo luetaan ilman uutta fokusointia.
    if (stacked) {
        // Suurilla fonttikoeilla kolme yhden rivin tekstiä ei mahdu vierekkäin.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = InsightsDimens.ReadoutTopPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            ChartReadoutValue(bucket = bucket, hasValue = hasValue, hasAnyData = hasAnyData)
            ChartReadoutContext(labelText = labelText, rows = bucket?.totalRows ?: 0, modifier = Modifier)
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = InsightsDimens.ReadoutTopPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            ChartReadoutValue(
                bucket = bucket,
                hasValue = hasValue,
                hasAnyData = hasAnyData,
                modifier = Modifier.alignByBaseline(),
            )
            ChartReadoutContext(
                labelText = labelText,
                rows = bucket?.totalRows ?: 0,
                modifier =
                    Modifier
                        .weight(1f)
                        .alignByBaseline()
                        .padding(start = InsightsDimens.ReadoutLabelGap),
            )
        }
    }
}

@Composable
private fun ChartReadoutValue(
    bucket: InsightsChartBucket?,
    hasValue: Boolean,
    hasAnyData: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hasValue && bucket != null) {
        Text(
            text = durationText(DurationDisplayFormatter.fromMinutes(bucket.totalMinutes)),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = InsightsDimens.ReadoutDurationFontSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = modifier,
        )
    } else {
        Text(
            text =
                stringResource(
                    if (hasAnyData) R.string.insights_no_time_logged else R.string.insights_no_sessions_yet,
                ),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = InsightsDimens.ReadoutRowsFontSize),
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            maxLines = 1,
            modifier = modifier,
        )
    }
}

@Composable
private fun ChartReadoutContext(
    labelText: String,
    rows: Int,
    modifier: Modifier,
) {
    Text(
        text = readoutContextText(labelText, rows),
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontSize = InsightsDimens.ReadoutLabelFontSize,
                fontWeight = FontWeight.SemiBold,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** "WED, 29 JUL" tai "WED, 29 JUL, 12 rows" — sama yhdistelmämuoto kuin projektiriveillä. */
@Composable
private fun readoutContextText(
    labelText: String,
    rows: Int,
): String =
    if (rows > 0) {
        stringResource(
            R.string.insights_project_sub_format,
            labelText,
            pluralStringResource(R.plurals.insights_rows_count, rows, rows),
        )
    } else {
        labelText
    }

@Composable
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
    val selectionBandColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.ChartSelectionBandAlpha)
    // Pinon värit tulevat samasta apurista kuin projektilistan pisteet, jolloin
    // kaavio ja lista luetaan samoilla väreillä.
    val segmentColors =
        remember(buckets) {
            buckets
                .flatMap { bucket -> bucket.segments }
                .map { it.projectId }
                .distinct()
                .associateWith(::yarnColorForId)
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
                .height(InsightsDimens.ChartPlotHeight + InsightsDimens.ChartSelectionMarkerBand)
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
                    var lastIndex = -1
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (buckets.isEmpty()) return@detectHorizontalDragGestures
                            lastIndex = bucketIndexAt(offset.x, size.width)
                            onSelectBucket(lastIndex)
                        },
                    ) { change, _ ->
                        if (buckets.isEmpty()) return@detectHorizontalDragGestures
                        val index = bucketIndexAt(change.position.x, size.width)
                        if (index != lastIndex) {
                            lastIndex = index
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
        // Valintakaista pylväiden alle, jotta pylväät jäävät sen päälle eikä kaista
        // himmennä muita sarakkeita — se korostaa vain valitun taustan.
        selectedIndex?.takeIf { it in buckets.indices }?.let { index ->
            drawRect(
                color = selectionBandColor,
                topLeft = Offset(pitch * index, 0f),
                size = Size(pitch, baselineY),
            )
        }
        // Pylväiden väliin jää aina rako, jottei tiheä akseli lukeudu yhtenä möykkynä.
        val minGapPx = InsightsDimens.ChartBarMinGap.toPx()
        val barWidthPx = minOf(barWidth.toPx(), pitch - minGapPx).coerceAtLeast(1f)
        val cornerPx = InsightsDimens.ChartBarCorner.toPx()
        val minBarHeightPx = InsightsDimens.ChartMinBarHeight.toPx()
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
            drawStackedBar(
                segments = bucket.segments,
                totalMinutes = bucket.totalMinutes,
                colors = segmentColors,
                fallbackColor = fallbackBarColor,
                topLeft = Offset(left, barBottomY - barHeight),
                barSize = Size(barWidthPx, barHeight),
                cornerPx = cornerPx,
            )
        }

        // Apuviivat pylväiden päälle, jottei korkein pylväs peitä maksimiviivaa.
        drawChartGridlines(gridColor, baselineY, gridStrokePx)

        // Merkkiviiva apuviivojen jälkeen, jottei perusviiva peitä sitä.
        selectedIndex?.takeIf { it in buckets.indices }?.let { index ->
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
            else -> DAY_AXIS_MAX_LABELS
        }
    val labelled = remember(buckets.size, maxLabels) { axisLabelIndices(buckets.size, maxLabels).toSet() }
    val lastIndex = buckets.lastIndex

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
                    when (index) {
                        0 -> Alignment.CenterStart
                        lastIndex -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
            ) {
                // Oikea reuna on "tänään" vain kun se todella on tämä päivä.
                val isToday = interval == PaceGroupingInterval.DAY && index == lastIndex && bucket.bucketStart == today
                if (isToday) {
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
    val skeleton = if (interval == PaceGroupingInterval.MONTH) "yMMMM" else "EMMMd"
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
            timeRange == TimeRange.THIS_WEEK -> "E"
            else -> "d"
        }
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return bucketStart.format(formatter)
}

private const val DAY_AXIS_MAX_LABELS = 5
private const val MONTH_AXIS_MAX_LABELS = 6
