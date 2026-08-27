package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.formatPercentForDisplay
import com.finnvek.knittools.ui.components.DurationHero
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

/** Aikaväli omana rivinään: sijamuodoton väliviivanotaatio, ei osa virkettä. */
@Composable
internal fun InsightsRangeKicker(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontSize = InsightsDimens.KickerFontSize,
                fontWeight = FontWeight.Medium,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
            MaterialTheme.typography.titleMedium.copy(
                fontSize = InsightsDimens.HeroLeadFontSize,
                fontWeight = FontWeight.Medium,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
    )
}

/**
 * Projektien määrä on konkreettisempi päätös kuin roikkuva gerundi, ja se on
 * rakenteeltaan lajineutraali. Ilman istuntoja lause jätetään kokonaan pois.
 */
@Composable
private fun heroTrailingText(state: InsightsUiState): String? {
    // Suodatinpilleri kertoo jo valitun projektin nimen, joten hero ei toista sitä.
    if (state.selectedProjectName != null) return null
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

internal enum class StreakStatLabel {
    DAY_STREAK,
    BEST_STREAK,
}

internal data class StreakStatDisplay(
    val value: Int,
    val label: StreakStatLabel,
)

/**
 * Tilastosarake ei johda demotivoivalla nollalla, jos paras putki on olemassa:
 * "0 DAY STREAK" oli monelle ensimmäinen luku jonka he lukivat.
 */
internal fun streakStatDisplay(
    currentStreak: Int,
    bestStreak: Int,
): StreakStatDisplay =
    when {
        currentStreak > 0 -> StreakStatDisplay(currentStreak, StreakStatLabel.DAY_STREAK)
        bestStreak > 0 -> StreakStatDisplay(bestStreak, StreakStatLabel.BEST_STREAK)
        else -> StreakStatDisplay(0, StreakStatLabel.DAY_STREAK)
    }

/**
 * Sama kynnys kuin tilastosarakkeella: yhden päivän "paras putki" on merkityksetön
 * kummassakin paikassa. Trendirivi kertoo parhaan vain kun sarake näyttää nykyisen.
 */
internal fun allTimeBestStreakTrend(
    currentStreak: Int,
    bestStreak: Int,
): Int? = bestStreak.takeIf { currentStreak > 0 && it > MINIMUM_MEANINGFUL_STREAK }

/**
 * Toissijaiset luvut yhtenä rivinä. Sarake jätetään pois kun sillä ei ole kerrottavaa:
 * ajatusviiva min/riviä varten ja "0 day streak" olivat tyhjiä lukuja jotka veivät
 * saman tilan kuin oikeat.
 *
 * Sarakkeet ladotaan vasemmalta kiinteällä välillä eikä leviteä koko leveydelle:
 * painotettuina kaksi jäljelle jäänyttä saraketta hakeutuivat vastakkaisiin reunoihin
 * ja väliin jäi kolo, ja yksi sarake jäi yksin vasemmalle.
 */
@Composable
internal fun InsightsStatsRow(state: InsightsUiState) {
    val locale = rememberCurrentLocale()
    val streakDisplay = streakStatDisplay(state.currentStreak, state.bestStreak)
    val showsRows = state.totalRows > 0
    val showsPace = showsRows && state.minutesPerRow != MinutesPerRowDisplay.Unavailable
    val showsStreak = state.canUseStreak && streakDisplay.value > MINIMUM_MEANINGFUL_STREAK
    // Nollasarake ei kerro mitään, joten koko rivi jää pois kun mikään sarake ei
    // yllä kertomaan. Aiemmin yhden minuutin istunto ilman rivejä jätti ruudulle
    // yksinäisen "0 ROWS" -sarakkeen.
    if (!showsRows && !showsPace && !showsStreak) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = InsightsDimens.StatsRowTopPadding,
                    bottom = InsightsDimens.StatsRowBottomPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(InsightsDimens.StatColumnGap),
    ) {
        if (showsRows) {
            InsightsStat(
                value = formatIntegerForDisplay(state.totalRows.toLong(), locale),
                label = stringResource(R.string.insights_stat_rows),
            )
        }
        if (showsPace) {
            InsightsStat(
                value = minutesPerRowText(state.minutesPerRow),
                label = stringResource(R.string.insights_stat_min_per_row),
            )
        }
        if (showsStreak) {
            InsightsStat(
                value = formatIntegerForDisplay(streakDisplay.value.toLong(), locale),
                label =
                    stringResource(
                        when (streakDisplay.label) {
                            StreakStatLabel.DAY_STREAK -> R.string.insights_stat_day_streak
                            StreakStatLabel.BEST_STREAK -> R.string.insights_stat_best_streak
                        },
                    ),
            )
        }
    }
}

/** Yhden päivän putki ei ole putki: se on tämä päivä. */
internal const val MINIMUM_MEANINGFUL_STREAK = 1

/**
 * Yksi vaimea rivi tilastojen alla: kesken oleva jakso suhteessa edelliseen, tai
 * All Time -näkymässä paras putki, joka muuten laskettaisiin turhaan.
 */
@Composable
internal fun InsightsTrendLine(state: InsightsUiState) {
    val text = trendLineText(state) ?: return
    Text(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                fontSize = InsightsDimens.TrendFontSize,
                fontWeight = FontWeight.Medium,
            ),
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = InsightsDimens.TrendBottomPadding),
    )
}

@Composable
private fun trendLineText(state: InsightsUiState): String? {
    val locale = rememberCurrentLocale()
    if (state.timeRange == TimeRange.ALL_TIME) {
        if (!state.canUseStreak) return null
        return allTimeBestStreakTrend(state.currentStreak, state.bestStreak)?.let { bestStreak ->
            pluralStringResource(R.plurals.insights_best_streak, bestStreak, bestStreak)
        }
    }
    val trend = state.trend ?: return null
    val percent = formatPercentForDisplay(trend.percentChange / 100.0, locale)
    val isWeek = state.timeRange == TimeRange.THIS_WEEK
    return when (trend.direction) {
        InsightsTrendDirection.UP ->
            stringResource(
                if (isWeek) R.string.insights_trend_more_week else R.string.insights_trend_more_month,
                percent,
            )

        InsightsTrendDirection.DOWN ->
            stringResource(
                if (isWeek) R.string.insights_trend_less_week else R.string.insights_trend_less_month,
                percent,
            )

        InsightsTrendDirection.FLAT ->
            stringResource(if (isWeek) R.string.insights_trend_same_week else R.string.insights_trend_same_month)
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
            // Sama all-caps-rooli kuin Libraryn osiomerkeillä: labelMedium oli visuaalisesti
            // melkein yhtä painava kuin itse luku.
            text = label.localizedUppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = InsightsDimens.StatLabelTopMargin),
        )
    }
}

/**
 * Osion otsikko Libraryn ja Toolsin tapaan: pienet versaalit `brandWine`-värillä.
 * Aiempi 20 sp lihavoitu otsikko oli yksi neljästä kilpailevasta äänenvoimakkuudesta
 * heron alla; kategoriamerkkinä se on hiljaisempi ja tunnistettavasti samaa sovellusta.
 */
@Composable
internal fun InsightsSectionHeader(
    title: String,
    meta: String? = null,
    metaIsLive: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = InsightsDimens.SectionTopPadding,
                    bottom = InsightsDimens.SectionHeaderBottomPadding,
                ).semantics { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.localizedUppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.knitToolsColors.brandWine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                // Kaavion lukema asuu tällä rivillä, joten uusi arvo on luettava ilman
                // uutta fokusointia kun valinta siirtyy vedolla tai ruudunlukijan toiminnolla.
                modifier =
                    if (metaIsLive) {
                        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                    } else {
                        Modifier
                    },
            )
        }
    }
}

/** Yksi projektirivi: väripiste, nimi ja alarivi sekä kesto oikeassa reunassa. Napautus avaa projektin laskurin. */
@Composable
internal fun InsightsProjectRow(
    project: ProjectTime,
    today: LocalDate,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = InsightsDimens.ProjectRowMinHeight)
                .padding(vertical = InsightsDimens.ProjectRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(InsightsDimens.ProjectDotSize)
                    .background(
                        yarnColorForId(project.projectId, MaterialTheme.knitToolsColors.yarnPalette),
                        CircleShape,
                    ),
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
        Column(horizontalAlignment = Alignment.End) {
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
}

/**
 * Suodatetussa näkymässä "Where the time went" on pelkkää toistoa, mutta sen mukana
 * katosi ainoa tieto jota mikään muu ei kerro: milloin projektia on viimeksi tehty.
 * Yksi rivi listaosion tilalle.
 */
@Composable
internal fun InsightsLastWorkedNote(
    project: ProjectTime,
    today: LocalDate,
) {
    Text(
        text =
            stringResource(
                R.string.insights_last_worked_format,
                relativeDayText(project.lastSessionAt, today),
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.SectionTopPadding),
    )
}

/**
 * Koko aikaväli yhtenä lankana: vaakasuora palkki, joka on jaettu projektien
 * väreihin niiden osuuksien mukaan. Viisi erillistä osuuspalkkia ei voinut verrata
 * keskenään — osuudet piti lukea prosenteista. Yksi palkki vastaa "mihin aika meni"
 * -kysymykseen yhdellä silmäyksellä, ja se kantaa neulepinnan reilusti paremmin kuin
 * 3 dp viiva.
 */
@Composable
internal fun InsightsProjectMixBar(
    projects: List<ProjectTime>,
    totalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    if (projects.isEmpty() || totalMinutes <= 0) return
    val palette = MaterialTheme.knitToolsColors.yarnPalette
    val density = LocalDensity.current
    val shadowColor =
        MaterialTheme.colorScheme.scrim.copy(alpha = InsightsDimens.ChartStitchShadowAlpha)
    val shape = RoundedCornerShape(InsightsDimens.MixBarCorner)

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = InsightsDimens.MixBarTopPadding,
                    bottom = InsightsDimens.MixBarBottomPadding,
                ).height(InsightsDimens.MixBarHeight)
                .clip(shape),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { InsightsDimens.MixBarHeight.toPx() }
        val lattice =
            remember(widthPx, heightPx, density) {
                with(density) {
                    knitStitchLattice(
                        widthPx = widthPx,
                        heightPx = heightPx,
                        targetStitchWidthPx = InsightsDimens.ChartStitchTargetWidth.toPx(),
                    )
                }
            }
        Row(modifier = Modifier.fillMaxSize()) {
            projects.forEach { project ->
                val share = project.totalMinutes / totalMinutes.toFloat()
                if (share <= 0f) return@forEach
                Box(
                    modifier =
                        Modifier
                            .weight(share)
                            .fillMaxHeight()
                            .background(yarnColorForId(project.projectId, palette)),
                )
            }
        }
        // Neulepinta koko palkin yli kerralla, jolloin silmukat jatkuvat
        // värirajojen poikki kuten raidallisessa neuleessa.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = lattice.path,
                color = shadowColor,
                style = Stroke(width = lattice.strokeWidthPx, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * "Tänään" tarkoittaa kalenteripäivää, ei kulunutta 24 tuntia. Vertailupäivä tulee
 * tilasta, jotta teksti päivittyy vuorokauden vaihtuessa eikä jää composablen
 * ensimmäisen piirron kellonaikaan.
 */
@Composable
internal fun relativeDayText(
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
                    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
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

/**
 * Aikavälillä ei ole istuntoja, mutta muualla on. Sama kaksirivinen muoto kuin
 * [InsightsEmptyState]lla: yksinäinen kuiva lause ison tyhjän yläpuolella näytti
 * keskenjääneeltä, ei tarkoitukselliselta.
 */
@Composable
internal fun InsightsRangeEmptyNote(timeRange: TimeRange) {
// CPD-ON
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = InsightsDimens.EmptyRangeNoteVerticalPadding),
    ) {
        Text(
            text =
                stringResource(
                    when (timeRange) {
                        TimeRange.THIS_WEEK -> R.string.insights_nothing_yet_week
                        TimeRange.THIS_MONTH -> R.string.insights_nothing_yet_month
                        TimeRange.ALL_TIME -> R.string.insights_nothing_yet_all_time
                    },
                ),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = InsightsDimens.EmptyRangeTitleFontSize,
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

/** Kaavion paikalla ilman Pro-oikeutta. Oikeita pylväitä ei luovuteta eikä keksittyä dataa piirretä. */
@Composable
internal fun InsightsProChartPrompt(onProUpgrade: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = InsightsDimens.SectionTopPadding),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.insights_pro_chart_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = InsightsDimens.ProCardTitleFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = InsightsDimens.ProCardPadding),
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
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = InsightsDimens.ProCardPadding),
        )
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
