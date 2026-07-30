package com.finnvek.knittools.ui.screens.insights

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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
