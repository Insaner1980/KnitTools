package com.finnvek.knittools.ui.screens.insights

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Kuinka monta kuukausipylvästä All Time näyttää enintään. */
internal const val ALL_TIME_MONTH_BUCKET_LIMIT = 12

/** Yhden projektin osuus yhdestä pylväästä. Väri haetaan projektin id:llä. */
data class InsightsChartSegment(
    val projectId: Long,
    val minutes: Int,
)

/**
 * Yhden pylvään mitatut arvot. Nolla-ämpärit ovat mukana, jotta kaavio ei näytä
 * rikkinäiseltä. [segments] pinoaa päivän ajan projekteittain, jolloin kaavio kertoo
 * mihin aika meni eikä vain paljonko sitä kului; niiden summa on [totalMinutes].
 */
data class InsightsChartBucket(
    val bucketStart: LocalDate,
    val totalMinutes: Int,
    val totalRows: Int,
    val segments: List<InsightsChartSegment> = emptyList(),
)

/** Kaavion akseli: ryhmittelyväli ja kaikki ämpärialut järjestyksessä. */
data class InsightsChartAxis(
    val interval: PaceGroupingInterval,
    val bucketStarts: List<LocalDate>,
)

enum class InsightsTrendDirection {
    UP,
    DOWN,
    FLAT,
}

/** Muutos edelliseen jaksoon. Suuruus on itseisarvo, suunta erikseen. */
data class InsightsTrend(
    val percentChange: Int,
    val direction: InsightsTrendDirection,
)

/**
 * Ratkaisee kaavion ämpärit valitulle aikavälille.
 *
 * Akseli päättyy aina tähän päivään. Tulevia päiviä ei piirretä, koska nollapylväs
 * väittäisi ettei käyttäjä tehnyt mitään päivänä jota ei ole vielä ollut, ja
 * ämpärimäärä vastaa näin osion "x / y päivää" -lukemaa.
 *
 * All Time ryhmittelee kuukausiin, mutta jos dataa on alle kahdelta kuukaudelta,
 * palataan päiväämpäreihin ensimmäisestä istunnosta alkaen, jottei kaavio ole yksi
 * yksinäinen pylväs eikä ala tyhjillä päivillä ennen ensimmäistä istuntoa.
 */
internal fun insightsChartAxis(
    timeRange: TimeRange,
    today: LocalDate,
    firstSessionDate: LocalDate?,
    firstDayOfWeek: DayOfWeek,
): InsightsChartAxis =
    when (timeRange) {
        TimeRange.THIS_WEEK ->
            dayAxis(today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)), today)

        TimeRange.THIS_MONTH -> dayAxis(today.withDayOfMonth(1), today)

        TimeRange.ALL_TIME -> allTimeAxis(today, firstSessionDate)
    }

private fun dayAxis(
    start: LocalDate,
    today: LocalDate,
): InsightsChartAxis {
    val firstDay = if (start.isAfter(today)) today else start
    val dayCount = ChronoUnit.DAYS.between(firstDay, today).toInt() + 1
    return InsightsChartAxis(
        interval = PaceGroupingInterval.DAY,
        bucketStarts = (0 until dayCount).map { firstDay.plusDays(it.toLong()) },
    )
}

private fun allTimeAxis(
    today: LocalDate,
    firstSessionDate: LocalDate?,
): InsightsChartAxis {
    val currentMonth = today.withDayOfMonth(1)
    val firstMonth = firstSessionDate?.withDayOfMonth(1) ?: return dayAxis(currentMonth, today)
    val monthSpan = ChronoUnit.MONTHS.between(firstMonth, currentMonth) + 1
    if (monthSpan < 2) return dayAxis(firstSessionDate, today)

    val earliestShownMonth = currentMonth.minusMonths(ALL_TIME_MONTH_BUCKET_LIMIT - 1L)
    val start = if (firstMonth.isAfter(earliestShownMonth)) firstMonth else earliestShownMonth
    val bucketCount = ChronoUnit.MONTHS.between(start, currentMonth).toInt() + 1
    return InsightsChartAxis(
        interval = PaceGroupingInterval.MONTH,
        bucketStarts = (0 until bucketCount).map { start.plusMonths(it.toLong()) },
    )
}

/** Täydentää mitatut ämpärit akselin mukaisiksi niin, että tyhjät päivät saavat nollan. */
internal fun fillChartBuckets(
    axis: InsightsChartAxis,
    measured: Map<LocalDate, InsightsChartBucket>,
): List<InsightsChartBucket> =
    axis.bucketStarts.map { bucketStart ->
        measured[bucketStart] ?: InsightsChartBucket(bucketStart, totalMinutes = 0, totalRows = 0)
    }

/**
 * Oletusvalinta on viimeisin ämpäri, jossa on dataa. Suurin ämpäri toistaisi
 * asteikkolukeman ja tämä päivä tuottaisi tyhjän lukeman aina ennen päivän ensimmäistä istuntoa.
 */
internal fun defaultSelectedBucketIndex(buckets: List<InsightsChartBucket>): Int? =
    buckets.indexOfLast { it.totalMinutes > 0 }.takeIf { it >= 0 }

/**
 * Ilman edellisen jakson minuutteja prosenttimuutosta ei ole olemassa: nollalla
 * jakaminen tuottaisi äärettömän kasvun, joka näyttäisi ensimmäisellä viikolla
 * mielivaltaiselta luvulta. Silloin rivi jätetään kokonaan pois.
 */
internal fun insightsTrend(
    currentMinutes: Int,
    previousMinutes: Int,
): InsightsTrend? {
    if (previousMinutes <= 0) return null
    val change = ((currentMinutes - previousMinutes) * 100.0 / previousMinutes).roundToInt()
    val direction =
        when {
            change > 0 -> InsightsTrendDirection.UP
            change < 0 -> InsightsTrendDirection.DOWN
            else -> InsightsTrendDirection.FLAT
        }
    return InsightsTrend(percentChange = abs(change), direction = direction)
}

/**
 * Edellinen jakso katkaistaan samaan kuluneeseen päivämäärään: keskiviikkoa
 * verrataan viime viikon keskiviikkoon asti, ei koko viikkoon. Muuten kesken
 * oleva jakso näyttäisi aina laskulta.
 *
 * Mittatapa on sama kuin nykyisellä jaksolla: sekunnit summataan ikkunan yli ja
 * pyöristetään minuuteiksi vasta kerran lopuksi. Päiväkohtaisten minuuttien
 * summaaminen kasvatti vertailukohtaa jokaisen aktiivisen päivän pyöristyksellä
 * ja vinoutti trendin systemaattisesti laskun suuntaan.
 *
 * Puhdas funktio päivämäärien vertailulle: [today] ratkaisee kuinka monta
 * päivää nykyisestä jaksosta on kulunut, ja sama määrä päiviä lasketaan
 * edellisestä jaksosta alkaen [previousStart]:sta.
 *
 * Ikkuna katkaistaan aina viimeistään [currentStart]:iin. Kuukausilla kuluneet
 * päivät lasketaan nykyisestä kuukaudesta mutta lisätään edellisen kuukauden
 * alkuun, joten lyhyempi edellinen kuukausi vuotaisi muuten nykyiseen: esimerkiksi
 * 31.3. ikkuna olisi 1.2.–4.3. ja laskisi maaliskuun alun sekä vertailukohtaan
 * että nykyiseen jaksoon.
 */
internal fun previousPeriodMinutes(
    dailySeconds: Map<LocalDate, Long>,
    previousStart: LocalDate,
    currentStart: LocalDate,
    today: LocalDate,
): Int {
    val elapsedDays = ChronoUnit.DAYS.between(currentStart, today) + 1
    val previousEndExclusive = minOf(previousStart.plusDays(elapsedDays), currentStart)
    val windowSeconds =
        dailySeconds
            .entries
            .filter { (date, _) -> date >= previousStart && date < previousEndExclusive }
            .sumOf { (_, seconds) -> seconds }
    return secondsToDisplayMinutes(windowSeconds)
}

/**
 * Akselille mahtuu vain kourallinen leimoja. Askel lasketaan ylöspäin pyöristäen,
 * jolloin leimoja on aina korkeintaan [maxLabels] ja ensimmäinen ämpäri on aina
 * leimattu — akselin vasen pää on käyttäjän ankkuri.
 */
internal fun axisLabelIndices(
    bucketCount: Int,
    maxLabels: Int,
): List<Int> {
    if (bucketCount <= 0 || maxLabels <= 0) return emptyList()
    if (bucketCount <= maxLabels) return (0 until bucketCount).toList()
    val step = ceil(bucketCount / maxLabels.toDouble()).toInt().coerceAtLeast(1)
    return (0 until bucketCount step step).toList()
}
