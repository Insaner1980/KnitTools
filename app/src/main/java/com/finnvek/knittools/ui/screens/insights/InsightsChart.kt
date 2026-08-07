package com.finnvek.knittools.ui.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.util.lerp
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
 * Päiväpylväät asteikolla. Käyttäjä näkee arvon ilman kosketusta:
 * maksimi on merkitty apuviivalle ja valittu ämpäri lukemarivillä.
 */
@Composable
internal fun InsightsChart(
    buckets: List<InsightsChartBucket>,
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
    // Yksi valintapolku kaikille tavoille valita: kosketus, veto ja ruudunlukijan
    // omat toiminnot. Erillinen raaka onSelectBucket jätti vihjeen ikuisesti
    // näkyviin sille, joka käyttää kaaviota pelkällä ruudunlukijalla.
    val selectBucket: (Int) -> Unit = { index ->
        hasInteracted = true
        onSelectBucket(index)
    }
    // Asteikkoleima on yksi rivi sp-tekstiä kaavion päällä: sen korkeus kasvaa
    // fonttiskaalan mukana, joten kiinteä kaista leikkasi leiman plotin päälle heti
    // kun järjestelmän fonttikoko oli suurennettu. Skaalataan kaista samalla
    // kertoimella; pienemmillä asetuksilla pidetään suunniteltu kiinteä mitta.
    val scaleLabelBand =
        InsightsDimens.ChartScaleLabelBand * LocalDensity.current.fontScale.coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        ChartReadout(
            readout =
                selectedIndex?.let(buckets::getOrNull)?.let { bucket ->
                    InsightsReadout(bucket.bucketStart, bucket.totalMinutes, bucket.totalRows)
                },
            fallbackDate = buckets.firstOrNull()?.bucketStart,
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
                                    moveSelection(buckets.size, selectedIndex, STEP_NEXT, selectBucket)
                                },
                                CustomAccessibilityAction(previousLabel) {
                                    moveSelection(buckets.size, selectedIndex, STEP_PREVIOUS, selectBucket)
                                },
                            )
                    },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(scaleLabelBand))
                ChartPlot(
                    buckets = buckets,
                    timeRange = timeRange,
                    selectedIndex = selectedIndex,
                    maxMinutes = maxMinutes,
                    onSelectBucket = selectBucket,
                )
                ChartAxisLabels(
                    buckets = buckets,
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
                        R.string.insights_chart_hint_day,
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
 * Siirtää valintaa yhden kohteen verran ruudunlukijan toiminnolle. Palauttaa false,
 * kun mitään ei liikahda — tyhjässä listassa ja reunassa — jolloin alusta osaa
 * kertoa ettei liike jatku, samoin kuin sisäänrakennetut vieritystoiminnot.
 * Ilman valintaa molemmat suunnat osuvat ensimmäiseen ämpäriin, koska
 * askeleelle ei ole vielä lähtöpistettä. Sama logiikka palvelee pylväitä ja fabricin
 * näkyvää päiväsliceä.
 */
internal fun moveSelection(
    itemCount: Int,
    selectedIndex: Int?,
    step: Int,
    onSelect: (Int) -> Unit,
): Boolean {
    if (itemCount <= 0) return false
    val target = if (selectedIndex == null) 0 else (selectedIndex + step).coerceIn(0, itemCount - 1)
    if (target == selectedIndex) return false
    onSelect(target)
    return true
}

/** Valittu pylväs säilyttää täyden värinsä, kun muut himmenevät valinnan ympäriltä. */
internal fun chartBarAlpha(
    barIndex: Int,
    selectedIndex: Int?,
    transitionProgress: Float,
): Float =
    if (selectedIndex == null || barIndex == selectedIndex) {
        1f
    } else {
        lerp(1f, InsightsDimens.ChartUnselectedBarAlpha, transitionProgress)
    }

/** Kasvu ankkuroidaan perusviivaan, joten koko pino saavuttaa lopuksi täyden korkeutensa. */
internal fun chartBarGrowthHeight(
    finalHeight: Float,
    progress: Float,
): Float = finalHeight * progress

/**
 * Valitun ämpärin lukema. Rivillä on vain yksi kesto, jotta kahta eri merkityksistä
 * lukua ei lueta välinä; asteikon maksimi asuu plotin yläreunassa.
 */
internal data class InsightsReadout(
    val date: LocalDate,
    val totalMinutes: Int,
    val totalRows: Int,
)

@Composable
internal fun ChartReadout(
    readout: InsightsReadout?,
    fallbackDate: LocalDate?,
    hasAnyData: Boolean,
) {
    val labelText = (readout?.date ?: fallbackDate)?.let { bucketLabel(it) }.orEmpty()
    val hasValue = readout != null && readout.totalMinutes > 0
    val stacked = LocalDensity.current.fontScale > InsightsDimens.ChartReadoutStackFontScale

    // Lukema on live region kummassakin asettelussa: kun valinta siirtyy
    // ruudunlukijan toiminnolla, uusi arvo luetaan ilman uutta fokusointia.
    // Säiliö yhdistää jälkeläistensä tekstit, jotta live region istuu solmussa
    // jolla on itsellään luettavaa sisältöä eikä pelkän asettelun päällä.
    if (stacked) {
        // Suurilla fonttikoeilla kolme yhden rivin tekstiä ei mahdu vierekkäin.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = InsightsDimens.ReadoutTopPadding)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            ChartReadoutValue(readout = readout, hasValue = hasValue, hasAnyData = hasAnyData)
            ChartReadoutContext(labelText = labelText, rows = readout?.totalRows ?: 0)
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = InsightsDimens.ReadoutTopPadding)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            ChartReadoutValue(
                readout = readout,
                hasValue = hasValue,
                hasAnyData = hasAnyData,
                modifier = Modifier.alignByBaseline(),
            )
            ChartReadoutContext(
                labelText = labelText,
                rows = readout?.totalRows ?: 0,
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
    readout: InsightsReadout?,
    hasValue: Boolean,
    hasAnyData: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hasValue && readout != null) {
        Text(
            text = durationText(DurationDisplayFormatter.fromMinutes(readout.totalMinutes)),
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
    modifier: Modifier = Modifier,
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
    val dimmingProgress = remember { Animatable(0f) }
    val barGrowthProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedIndex) {
        dimmingProgress.snapTo(0f)
        if (selectedIndex != null) {
            dimmingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
        }
    }
    LaunchedEffect(buckets) {
        barGrowthProgress.snapTo(0f)
        barGrowthProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        )
    }
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
        // Valinta ratkaistaan kerran: sama sarake säilyttää täyden värin ja saa
        // apuviivojen jälkeen perusviivamerkin.
        val selectedBucketIndex = selectedIndex?.takeIf { it in buckets.indices }
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
            val finalBarHeight =
                maxOf(minBarHeightPx, plotHeightPx * bucket.totalMinutes / maxMinutes.toFloat())
            val barHeight =
                chartBarGrowthHeight(
                    finalHeight = finalBarHeight,
                    progress = barGrowthProgress.value,
                )
            drawStackedBar(
                segments = bucket.segments,
                totalMinutes = bucket.totalMinutes,
                colors = segmentColors,
                fallbackColor = fallbackBarColor,
                topLeft = Offset(left, barBottomY - barHeight),
                barSize = Size(barWidthPx, barHeight),
                cornerPx = cornerPx,
                alpha = chartBarAlpha(index, selectedBucketIndex, dimmingProgress.value),
            )
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
    alpha: Float,
) {
    if (segments.isEmpty() || totalMinutes <= 0) {
        drawBarFromBaseline(
            color = fallbackColor.copy(alpha = fallbackColor.alpha * alpha),
            topLeft = topLeft,
            barSize = barSize,
            cornerPx = cornerPx,
        )
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
            val color = colors[segment.projectId] ?: fallbackColor
            drawRect(
                color = color.copy(alpha = color.alpha * alpha),
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
    timeRange: TimeRange,
    today: LocalDate,
) {
    if (buckets.isEmpty()) return
    val maxLabels =
        when {
            timeRange == TimeRange.THIS_WEEK -> buckets.size
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
                val isToday = index == lastIndex && bucket.bucketStart == today
                if (isToday) {
                    ChartAxisLabel(text = stringResource(R.string.insights_chart_axis_today))
                } else if (index in labelled) {
                    ChartAxisLabel(text = axisLabel(bucket.bucketStart, timeRange))
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
    if (timeRange == TimeRange.THIS_WEEK) {
        InsightsDimens.ChartWeekBarWidth
    } else {
        InsightsDimens.ChartMonthBarWidth
    }

@Composable
internal fun bucketLabel(bucketStart: LocalDate): String {
    val locale = currentInsightsLocale()
    val skeleton = "EMMMd"
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    val label = bucketStart.format(formatter)
    return label.uppercaseForDisplay(locale)
}

/**
 * Tick-leima on lyhin mahdollinen: viikolla viikonpäivä ja kuukaudessa päivän numero.
 */
@Composable
private fun axisLabel(
    bucketStart: LocalDate,
    timeRange: TimeRange,
): String {
    val locale: Locale = currentInsightsLocale()
    val skeleton =
        if (timeRange == TimeRange.THIS_WEEK) "E" else "d"
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return bucketStart.format(formatter)
}

internal const val STEP_NEXT = 1
internal const val STEP_PREVIOUS = -1
private const val DAY_AXIS_MAX_LABELS = 5
