package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.DurationHero
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Vahva viiva hero-lohkon alla. */
@Composable
internal fun InsightsStrongRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.RuleStrongAlpha),
    )
}

/** Hiusviiva osioiden ja listarivien välissä. */
@Composable
internal fun InsightsHairline(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.RuleHairlineAlpha),
    )
}

/**
 * Näytön ainoa hallitseva elementti. Numero ei enää katkaise virkettä: aikaväli on
 * johdannossa ja projektit päätöslauseessa, jolloin molemmat puoliskot ovat
 * kokonaisia lauseenosia ja erillistä aikavälikuvatekstiä ei tarvita.
 */
@Composable
internal fun InsightsHero(state: InsightsUiState) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.HeroTopPadding, bottom = InsightsDimens.HeroBottomPadding),
    ) {
        HeroSentenceText(text = heroLeadInText(state))
        DurationHero(
            display = state.totalDuration,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        heroTrailingText(state)?.let { HeroSentenceText(text = it) }
    }
}

@Composable
private fun HeroSentenceText(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.headlineSmall.copy(
                fontSize = InsightsDimens.HeroLeadFontSize,
                fontWeight = FontWeight.Medium,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** All Time nimeää ensimmäisen istunnon kuukauden; viikko ja kuukausi kertovat itsensä. */
@Composable
private fun heroLeadInText(state: InsightsUiState): String =
    when (state.timeRange) {
        TimeRange.THIS_WEEK -> stringResource(R.string.insights_hero_lead_in_week)
        TimeRange.THIS_MONTH -> stringResource(R.string.insights_hero_lead_in_month)
        TimeRange.ALL_TIME ->
            stringResource(R.string.insights_hero_lead_in_all_time, heroSinceMonthText(state))
    }

@Composable
private fun heroSinceMonthText(state: InsightsUiState): String {
    val locale = rememberCurrentLocale()
    val formatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMM"), locale)
        }
    return (state.rangeStart ?: state.rangeEnd).format(formatter)
}

/**
 * Projektien määrä on konkreettisempi päätös kuin roikkuva gerundi, ja se on
 * rakenteeltaan lajineutraali. Ilman istuntoja lause jätetään kokonaan pois.
 */
@Composable
private fun heroTrailingText(state: InsightsUiState): String? {
    val projectName = state.selectedProjectName
    if (projectName != null) {
        return stringResource(R.string.insights_hero_trailing_project, projectName)
    }
    val projectCount = state.timePerProject.size
    if (projectCount == 0) return null
    return pluralStringResource(
        R.plurals.insights_hero_trailing_projects,
        projectCount,
        projectCount,
    )
}

/** Verbi seuraa valinnan käsityölajia; neulontaa ei kovakoodata. */
@Composable
internal fun craftVerbText(craftMix: InsightsCraftMix): String =
    stringResource(
        when (craftMix) {
            InsightsCraftMix.KNITTING -> R.string.insights_craft_verb_knitting
            InsightsCraftMix.CROCHET -> R.string.insights_craft_verb_crocheting
            InsightsCraftMix.MIXED -> R.string.insights_craft_verb_making
        },
    )

/** Kolme yhtä leveää saraketta ilman taustoja. Ilman Pro-tilausta putki jätetään pois. */
@Composable
internal fun InsightsStatsRow(state: InsightsUiState) {
    val locale = rememberCurrentLocale()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = InsightsDimens.StatsRowTopPadding,
                    bottom = InsightsDimens.StatsRowBottomPadding,
                ),
    ) {
        InsightsStat(
            value = formatIntegerForDisplay(state.totalRows.toLong(), locale),
            label = stringResource(R.string.insights_stat_rows),
            modifier = Modifier.weight(1f),
        )
        InsightsStat(
            value = minutesPerRowText(state.minutesPerRow),
            label = stringResource(R.string.insights_stat_min_per_row),
            modifier = Modifier.weight(1f),
        )
        if (state.canUseStreak) {
            InsightsStat(
                value = formatIntegerForDisplay(state.currentStreak.toLong(), locale),
                label = stringResource(R.string.insights_stat_day_streak),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun minutesPerRowText(display: MinutesPerRowDisplay): String {
    val locale = rememberCurrentLocale()
    return when (display) {
        MinutesPerRowDisplay.Unavailable -> stringResource(R.string.insights_stat_no_value)
        MinutesPerRowDisplay.UnderOneMinute -> stringResource(R.string.insights_stat_under_one)
        is MinutesPerRowDisplay.Minutes -> formatIntegerForDisplay(display.minutes.toLong(), locale)
    }
}

@Composable
private fun InsightsStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BasicText(
            text = value,
            style =
                TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = InsightsDimens.StatValueFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = InsightsDimens.StatValueLetterSpacing,
                    fontFeatureSettings = "tnum",
                ),
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = InsightsDimens.StatValueMinFontSize,
                    maxFontSize = InsightsDimens.StatValueFontSize,
                ),
            maxLines = 1,
        )
        Text(
            text = label.localizedUppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = InsightsDimens.StatLabelTopMargin),
        )
    }
}

/** Osion otsikko ja valinnainen mittaluku oikealla. */
@Composable
internal fun InsightsSectionHeader(
    title: String,
    meta: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = InsightsDimens.SectionTopPadding,
                    bottom = InsightsDimens.SectionHeaderBottomPadding,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = InsightsDimens.SectionTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = InsightsDimens.SectionTitleLetterSpacing,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta != null) {
            Text(
                text = meta,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = InsightsDimens.SectionMetaFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
            )
        }
    }
}

/** Yksi projektirivi: väripiste, nimi ja alarivi sekä kesto oikeassa reunassa. */
@Composable
internal fun InsightsProjectRow(
    project: ProjectTime,
    today: LocalDate,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = InsightsDimens.ProjectRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(InsightsDimens.ProjectDotSize)
                    .background(yarnColorForId(project.projectId), CircleShape),
        )
        Spacer(modifier = Modifier.width(InsightsDimens.ProjectRowGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    project.projectName
                        ?: stringResource(R.string.new_project_name_format, project.projectId),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = InsightsDimens.ProjectNameFontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.insights_project_sub_format,
                        pluralStringResource(
                            R.plurals.insights_rows_count,
                            project.totalRows,
                            project.totalRows,
                        ),
                        relativeDayText(project.lastSessionAt, today),
                    ),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = InsightsDimens.ProjectSubFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = InsightsDimens.ProjectSubTopMargin),
            )
        }
        Spacer(modifier = Modifier.width(InsightsDimens.ProjectRowGap))
        Text(
            text = durationText(DurationDisplayFormatter.fromMinutes(project.totalMinutes)),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = InsightsDimens.ProjectDurationFontSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * "Tänään" tarkoittaa kalenteripäivää, ei kulunutta 24 tuntia. Vertailupäivä tulee
 * tilasta, jotta teksti päivittyy vuorokauden vaihtuessa eikä jää composablen
 * ensimmäisen piirron kellonaikaan.
 */
@Composable
private fun relativeDayText(
    timestamp: Long,
    today: LocalDate,
): String {
    val sessionDate =
        remember(timestamp) {
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    val days = ChronoUnit.DAYS.between(sessionDate, today).toInt()
    return when {
        days <= 0 -> stringResource(R.string.relative_time_today)
        days == 1 -> stringResource(R.string.relative_time_yesterday)
        days < 7 -> stringResource(R.string.relative_time_days_ago, days)
        days < 30 -> stringResource(R.string.relative_time_weeks_ago, days / 7)
        else -> stringResource(R.string.relative_time_months_ago, days / 30)
    }
}

/** Käyttäjä ei ole koskaan tallentanut istuntoa: ei kaaviota, ei nollatilastoja. */
@Composable
internal fun InsightsEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.HeroTopPadding),
    ) {
        Text(
            text = stringResource(R.string.insights_empty_title),
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = InsightsDimens.EmptyTitleFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = InsightsDimens.EmptyBodyFontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = InsightsDimens.EmptyBodyTopMargin),
        )
    }
}

/** Aikavälillä ei ole istuntoja, mutta muualla on. Yksi vaimea rivi listan tilalla. */
@Composable
internal fun InsightsRangeEmptyNote(timeRange: TimeRange) {
    Text(
        text =
            stringResource(
                when (timeRange) {
                    TimeRange.THIS_WEEK -> R.string.insights_nothing_yet_week
                    TimeRange.THIS_MONTH -> R.string.insights_nothing_yet_month
                    TimeRange.ALL_TIME -> R.string.insights_nothing_yet_all_time
                },
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = InsightsDimens.EmptyRangeNoteVerticalPadding),
    )
}

/** Kaavion tilalla ilman Pro-tilausta. Oikeita pylväitä ei himmennetä eikä keksittyä dataa piirretä. */
@Composable
internal fun InsightsProChartCard(onProUpgrade: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.SectionTopPadding)
                .border(
                    width = InsightsDimens.ProCardBorderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(InsightsDimens.ProCardCornerRadius),
                ).padding(InsightsDimens.ProCardPadding),
    ) {
        Text(
            text = stringResource(R.string.insights_pro_chart_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = InsightsDimens.ProCardTitleFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.insights_pro_chart_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = InsightsDimens.ProCardBodyFontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = InsightsDimens.ProCardBodyTopMargin),
        )
        TextButton(onClick = onProUpgrade, contentPadding = PaddingValues(0.dp)) {
            Text(text = stringResource(R.string.upgrade_to_pro))
        }
    }
}

/** Latausskeleton lopullisen asettelun mitoissa, jottei näyttö hyppää datan saapuessa. */
@Composable
internal fun InsightsSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(height = InsightsDimens.SkeletonHeroHeight)
        Spacer(modifier = Modifier.height(InsightsDimens.SkeletonSpacing))
        SkeletonBlock(height = InsightsDimens.SkeletonStatsHeight)
        Spacer(modifier = Modifier.height(InsightsDimens.SkeletonSpacing))
        SkeletonBlock(height = InsightsDimens.ChartPlotHeight)
    }
}

@Composable
private fun SkeletonBlock(height: Dp) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.SkeletonAlpha),
                    shape = RoundedCornerShape(InsightsDimens.SkeletonCornerRadius),
                ),
    )
}

/** Kannustava lopetus, keskitettynä listan alle. */
@Composable
internal fun InsightsFooterNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = InsightsDimens.SectionTopPadding),
    )
}
