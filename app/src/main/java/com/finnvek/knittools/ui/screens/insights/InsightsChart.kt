package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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

    Column(modifier = modifier.fillMaxWidth()) {
        ChartReadout(
            bucket = selectedIndex?.let(buckets::getOrNull),
            interval = interval,
            fallbackLabel = buckets.firstOrNull()?.bucketStart,
            maxMinutes = maxMinutes,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { this.contentDescription = contentDescription },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ChartPlot(
                    buckets = buckets,
                    timeRange = timeRange,
                    selectedIndex = selectedIndex,
                    maxMinutes = maxMinutes,
                    onSelectBucket = onSelectBucket,
                )
                ChartAxisLabels(
                    firstBucketStart = buckets.firstOrNull()?.bucketStart,
                    lastBucketStart = buckets.lastOrNull()?.bucketStart,
                    interval = interval,
                    today = today,
                )
            }
        }
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

/**
 * Yksi rivi kaavion yllä: valitun ämpärin lukema isolla vasemmalla, aikavälin
 * maksimi oikeassa reunassa. Maksimi on samalla rivillä eikä omana kaistanaan
 * plotin yllä, jottei se lukeudu oikeanpuoleisimman pylvään arvoksi.
 */
@Composable
private fun ChartReadout(
    bucket: InsightsChartBucket?,
    interval: PaceGroupingInterval,
    fallbackLabel: LocalDate?,
    maxMinutes: Int,
) {
    val label = bucket?.bucketStart ?: fallbackLabel
    val labelText = label?.let { bucketLabel(it, interval) }.orEmpty()
    val hasValue = bucket != null && bucket.totalMinutes > 0
    // Kaikki ajot samalle perusviivalle. Alignment.Bottom tasaisi laatikoiden pohjat,
    // jolloin 14sp leima valui 28sp keston alapuolelle.
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.ReadoutTopPadding),
    ) {
        if (hasValue) {
            Text(
                text = durationText(DurationDisplayFormatter.fromMinutes(bucket.totalMinutes)),
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = InsightsDimens.ReadoutDurationFontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        } else {
            Text(
                text =
                    stringResource(
                        if (maxMinutes > 0) {
                            R.string.insights_no_time_logged
                        } else {
                            R.string.insights_no_sessions_yet
                        },
                    ),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = InsightsDimens.ReadoutRowsFontSize,
                    ),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Text(
            text = readoutContextText(labelText, bucket?.totalRows ?: 0),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontSize = InsightsDimens.ReadoutLabelFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Täyttää välitilan, jolloin maksimi asettuu oikeaan reunaan.
            modifier =
                Modifier
                    .weight(1f)
                    .alignByBaseline()
                    .padding(horizontal = InsightsDimens.ReadoutLabelGap),
        )
        if (maxMinutes > 0) {
            Text(
                text = durationText(DurationDisplayFormatter.fromMinutes(maxMinutes)),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = InsightsDimens.ChartScaleLabelFontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
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
    // Nollamerkki nojaa outline-rooliin, joka on mitoitettu erottumaan taustasta
    // molemmissa teemoissa. Pintasävyllä tumma teema jäi 1.6:1-kontrastiin.
    val zeroTickColor = MaterialTheme.colorScheme.outline.copy(alpha = InsightsDimens.ChartZeroTickAlpha)
    val selectionColor = MaterialTheme.colorScheme.onSurface
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

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(InsightsDimens.ChartPlotHeight + InsightsDimens.ChartSelectionDotBand)
                // Dokumentoitu poikkeus: kuukausinäkymässä 28-31 osumakohdetta ei mahdu
                // 48dp minimiin puhelimen leveydelle. Kosketus on täydentävä polku, sillä
                // asteikkoteksti ja lukemarivi kertovat arvot ilman vuorovaikutusta.
                .pointerInput(buckets.size) {
                    detectTapGestures { offset ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val pitch = size.width / buckets.size.toFloat()
                        val index = (offset.x / pitch).toInt().coerceIn(0, buckets.lastIndex)
                        onSelectBucket(index)
                    }
                },
    ) {
        val gridStrokePx = InsightsDimens.ChartGridlineStroke.toPx()
        val dotBandPx = InsightsDimens.ChartSelectionDotBand.toPx()
        // Perusviiva jättää alaosaan kaistan valinnan merkille.
        val baselineY = size.height - dotBandPx
        val barBottomY = baselineY - gridStrokePx
        if (buckets.isEmpty()) {
            drawChartGridlines(gridColor, baselineY, gridStrokePx)
            return@Canvas
        }
        val pitch = size.width / buckets.size.toFloat()
        // Pylväiden väliin jää aina rako, jottei tiheä akseli lukeudu yhtenä möykkynä.
        val minGapPx = InsightsDimens.ChartBarMinGap.toPx()
        val barWidthPx = minOf(barWidth.toPx(), pitch - minGapPx).coerceAtLeast(1f)
        val cornerPx = InsightsDimens.ChartBarCorner.toPx()
        val minBarHeightPx = InsightsDimens.ChartMinBarHeight.toPx()
        val zeroTickPx = InsightsDimens.ChartZeroTickHeight.toPx()
        // Maksimiarvo päättyy tasan yläapuviivaan: viivan paksuus varataan pois
        // mitta-alueesta, jottei korkein pylväs työnny sen yli.
        val plotHeightPx = (barBottomY - gridStrokePx).coerceAtLeast(0f)

        buckets.forEachIndexed { index, bucket ->
            val left = pitch * index + (pitch - barWidthPx) / 2f
            val isEmptyBucket = bucket.totalMinutes <= 0 || maxMinutes <= 0
            if (isEmptyBucket) {
                drawBarFromBaseline(
                    color = zeroTickColor,
                    topLeft = Offset(left, barBottomY - zeroTickPx),
                    barSize = Size(barWidthPx, zeroTickPx),
                    cornerPx = 0f,
                )
                return@forEachIndexed
            }
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

        selectedIndex?.takeIf { it in buckets.indices }?.let { index ->
            drawCircle(
                color = selectionColor,
                radius = InsightsDimens.ChartSelectionDotSize.toPx() / 2f,
                center = Offset(pitch * index + pitch / 2f, baselineY + dotBandPx / 2f),
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
 * Reunaleimat kertovat akselin todelliset päät. "Tänään" näytetään vain kun oikea
 * reuna todella on tämä päivä — muuten leima väittäisi väärää sarakkeesta.
 */
@Composable
private fun ChartAxisLabels(
    firstBucketStart: LocalDate?,
    lastBucketStart: LocalDate?,
    interval: PaceGroupingInterval,
    today: LocalDate,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(InsightsDimens.ChartAxisBandHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChartAxisLabel(text = firstBucketStart?.let { axisLabel(it, interval) }.orEmpty())
        ChartAxisLabel(
            text =
                when {
                    lastBucketStart == null || lastBucketStart == firstBucketStart -> ""
                    interval == PaceGroupingInterval.DAY && lastBucketStart == today ->
                        stringResource(R.string.insights_chart_axis_today)

                    else -> axisLabel(lastBucketStart, interval)
                },
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ChartAxisLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontSize = InsightsDimens.ChartAxisLabelFontSize,
                fontWeight = FontWeight.SemiBold,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        textAlign = textAlign,
        maxLines = 1,
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
    val skeleton = if (interval == PaceGroupingInterval.MONTH) "yMMM" else "EMMMd"
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return bucketStart.format(formatter).uppercaseForDisplay(locale)
}

@Composable
private fun axisLabel(
    bucketStart: LocalDate,
    interval: PaceGroupingInterval,
): String {
    val locale: Locale = currentInsightsLocale()
    val skeleton = if (interval == PaceGroupingInterval.MONTH) "yMMM" else "MMMd"
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return bucketStart.format(formatter)
}
