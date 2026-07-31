package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Insights-näytön omat mitat ja tekstitokenit. Näyttö nojaa typografiaan ja
 * hiusviivoihin korttien sijaan, joten se määrittelee omat kokonsa samaan tapaan
 * kuin laskurinäyttö [CounterDimens]-tokeneillaan. Type.kt:n perusrooleja ei muuteta.
 */
object InsightsDimens {
    val ScreenHorizontalPadding = 20.dp
    val ContentBottomPadding = 32.dp

    // Suodatinsirut
    val FiltersTopPadding = 12.dp

    // Kicker
    val KickerTopPadding = 14.dp
    val KickerFontSize = 13.sp

    // Hero
    val HeroTopPadding = 8.dp
    val HeroBottomPadding = 6.dp
    val HeroRangeTopMargin = 6.dp
    val HeroLeadFontSize = 20.sp
    val HeroPrimaryNumberFontSize = 76.sp
    val HeroPrimaryNumberMinFontSize = 52.sp
    val HeroPrimaryUnitFontSize = 26.sp
    val HeroSecondaryNumberFontSize = 46.sp
    val HeroSecondaryUnitFontSize = 20.sp
    val HeroUnitToMinutesSpacing = 11.dp
    val HeroNumberLetterSpacing = (-0.055f).em

    // Tilastorivi
    val StatsRowTopPadding = 14.dp
    val StatsRowBottomPadding = 14.dp
    val StatValueFontSize = 40.sp
    val StatValueMinFontSize = 22.sp
    val StatValueLetterSpacing = (-0.03f).em
    val StatLabelTopMargin = 9.dp

    // Trendirivi
    val TrendFontSize = 14.sp
    val TrendBottomPadding = 14.dp

    // Viivat: näytöllä on tasan kaksi painoa
    @Suppress("MayBeConstant")
    val RuleStrongAlpha = 0.32f

    @Suppress("MayBeConstant")
    val RuleHairlineAlpha = 0.15f

    @Suppress("MayBeConstant")
    val ChartGridlineAlpha = 0.55f

    // Osioiden otsikot
    val SectionTopPadding = 20.dp
    val SectionHeaderBottomPadding = 10.dp
    val SectionTitleFontSize = 20.sp
    val SectionTitleLetterSpacing = (-0.01f).em
    val SectionMetaFontSize = 13.sp

    // Kaavio
    val ChartPlotHeight = 168.dp
    val ChartAxisBandHeight = 22.dp
    val ChartGridlineStroke = 1.dp
    val ChartMinBarHeight = 9.dp
    val ChartBarMinGap = 2.dp

    /**
     * Sama hillitty pyöristys kaikilla aikaväleillä. Aiemmin jokainen säde oli tasan
     * puolet pylvään leveydestä, jolloin joka pylväs oli puoliympyräkupoli ja lakki
     * söi korkeuseroista niin paljon ettei arvoja voinut vertailla.
     */
    val ChartBarCorner = 3.dp
    val ChartWeekBarWidth = 34.dp
    val ChartMonthBarWidth = 10.dp
    val ChartAllTimeBarWidth = 22.dp

    // Valinnan korostus: vaimea kaista pylvään takana ja merkkiviiva perusviivassa
    val ChartSelectionMarkerBand = 12.dp
    val ChartSelectionMarkerHeight = 3.dp

    @Suppress("MayBeConstant")
    val ChartSelectionBandAlpha = 0.08f

    // Lukema kaavion yllä
    val ReadoutTopPadding = 2.dp
    val ReadoutLabelGap = 12.dp
    val ReadoutLabelFontSize = 14.sp
    val ReadoutDurationFontSize = 28.sp
    val ReadoutRowsFontSize = 15.sp
    val ChartScaleLabelFontSize = 13.sp
    val ChartAxisLabelFontSize = 13.sp
    val ChartHintFontSize = 12.sp
    val ChartHintTopPadding = 8.dp
    val ChartScaleLabelBand = 18.dp

    @Suppress("MayBeConstant")
    val ChartReadoutStackFontScale = 1.3f

    // Projektilista
    val ProjectRowVerticalPadding = 15.dp
    val ProjectRowGap = 14.dp
    val ProjectDotSize = 14.dp
    val ProjectNameFontSize = 19.sp
    val ProjectSubFontSize = 13.sp
    val ProjectSubTopMargin = 2.dp
    val ProjectDurationFontSize = 19.sp

    // Tyhjät tilat
    val EmptyTitleFontSize = 32.sp
    val EmptyBodyFontSize = 16.sp
    val EmptyBodyTopMargin = 12.dp
    val EmptyRangeNoteVerticalPadding = 24.dp

    // Pro-kortti kaavion tilalla
    val ProCardBorderWidth = 1.dp
    val ProCardCornerRadius = 16.dp
    val ProCardPadding = 24.dp
    val ProCardTitleFontSize = 17.sp
    val ProCardBodyFontSize = 15.sp
    val ProCardBodyTopMargin = 8.dp

    // Latausskeleton
    val SkeletonCornerRadius = 12.dp
    val SkeletonHeroHeight = 152.dp
    val SkeletonStatsHeight = 64.dp
    val SkeletonSpacing = 16.dp

    @Suppress("MayBeConstant")
    val SkeletonAlpha = 0.12f
}
