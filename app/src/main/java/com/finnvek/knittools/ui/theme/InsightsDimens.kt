package com.finnvek.knittools.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Insights-näytön omat mitat ja tekstitokenit. Näyttö nojaa typografiaan ja
 * hiusviivoihin korttien sijaan, joten se määrittelee omat kokonsa samaan tapaan
 * kuin laskurinäyttö [CounterDimens]-tokeneillaan. Type.kt:n perusrooleja ei muuteta.
 */
object InsightsDimens {
    // Sama vaakapehmuste kuin Libraryssa ja Toolsissa.
    val ScreenHorizontalPadding = 16.dp
    val ContentBottomPadding = 32.dp

    // Suodatinsirut
    val FiltersTopPadding = 4.dp
    val ContextRowGap = 12.dp

    // Aikaväli otsikkona app barissa. Napautusalue tulee pehmusteesta, ei omasta korkeudesta.
    val RangeTitleHorizontalPadding = 8.dp
    val RangeTitleVerticalPadding = 6.dp
    val RangeTitleIndicatorSize = 28.dp
    val FilterChipShape = RoundedCornerShape(percent = 50)
    val FilterChipHorizontalPadding = 12.dp
    val FilterChipVerticalPadding = 7.dp
    val FilterChipSpacing = 8.dp
    val FilterChipIndicatorSpacing = 4.dp
    val FilterChipIndicatorSize = 18.dp
    val FilterChipMinTouchTarget = 48.dp

    /**
     * Suodatin on toissijainen kontrolli ja useimmiten oletusarvossaan, joten se ei saa
     * olla heron jälkeen näytön äänekkäin elementti. Tämä on **visuaalinen** korkeus;
     * kosketuskohde pysyy 48 dp:ssä `minimumInteractiveComponentSize()`-laajennuksella.
     */
    val FilterPillHeight = 36.dp
    val FilterChipDotSize = 10.dp
    val MenuNeutralDotSize = 6.dp
    val FilterChipDotSpacing = 8.dp

    // Kicker
    val KickerTopPadding = 14.dp
    val KickerFontSize = 13.sp

    // Hero
    val HeroTopPadding = 6.dp
    val HeroBottomPadding = 6.dp
    val HeroLeadFontSize = 15.sp
    val HeroPrimaryNumberFontSize = 76.sp
    val HeroPrimaryNumberMinFontSize = 52.sp
    val HeroPrimaryUnitFontSize = 26.sp
    val HeroSecondaryNumberFontSize = 46.sp
    val HeroSecondaryUnitFontSize = 20.sp
    val HeroUnitToMinutesSpacing = 11.dp
    val HeroNumberLetterSpacing = (-0.055f).em

    // Tilastorivi
    val StatsRowTopPadding = 16.dp
    val StatsRowBottomPadding = 10.dp

    // Vain hero saa olla iso. Tilastot ovat toissijaisia: 28 sp kolmena sarakkeena
    // varasi 88 dp korkeutta ennen kuin kaaviota oli näkynyt.
    val StatValueFontSize = 20.sp
    val StatValueMinFontSize = 14.sp
    val StatValueLetterSpacing = (-0.03f).em
    val StatLabelTopMargin = 2.dp
    val StatColumnGap = 28.dp

    // Trendirivi
    val TrendFontSize = 13.sp
    val TrendBottomPadding = 4.dp

    // Viivat: näytöllä on tasan kaksi painoa
    @Suppress("MayBeConstant")
    val RuleStrongAlpha = 0.32f

    @Suppress("MayBeConstant")
    val RuleHairlineAlpha = 0.15f

    @Suppress("MayBeConstant")
    val ChartGridlineAlpha = 0.55f

    // Osioiden otsikot
    val SectionTopPadding = 22.dp
    val SectionHeaderBottomPadding = 12.dp

    // Kaavio
    // Plotin korkeus seuraa ämpärimäärää: kiinteä 168 dp jätti kolmen päivän viikolle
    // yhden pylvään valtavaan tyhjyyteen, mikä luki rikkinäisenä eikä harvana.
    val ChartPlotHeight = 168.dp
    val ChartPlotHeightSparse = 108.dp
    val ChartPlotHeightMedium = 140.dp
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

    // Neulepinta pylväissä: silmukkaruudukko piirretään varjoviivoina pylvään sisään.

    /** Tavoiteleveys yhdelle silmukalle; sarakemäärä pyöristetään tästä pylvään leveydelle. */
    val ChartStitchTargetWidth = 6.dp

    /** Silmukka on leveämpi kuin korkea, kuten oikeassa sileneuleessa. */
    @Suppress("MayBeConstant")
    val ChartStitchAspect = 0.78f

    /** Varjoviivan paksuus suhteessa silmukan leveyteen. */
    @Suppress("MayBeConstant")
    val ChartStitchStrokeRatio = 0.16f

    /**
     * Varjon peitto. Pidetään matalana: pinta on koriste, eikä se saa laskea pylvään
     * keskimääräistä väriä alle graafisten elementtien 3:1 kontrastin. Kermalla pinta
     * nostaa kontrastia (3,25 → 3,52); tummalla se laskee sitä, ja kapein marginaali
     * on ruosteenpunaisella 3,35 → noin 3,15.
     */
    @Suppress("MayBeConstant")
    val ChartStitchShadowAlpha = 0.14f

    /** Tätä matalampi pylväs jää sileäksi: yksi silmukkarivi lukee tahrana, ei neuleena. */
    val ChartStitchMinBarHeight = 14.dp

    // Valinnan korostus on additiivinen: pystyapuviiva sarakkeessa ja merkkiviiva
    // perusviivassa. Kaista oli 8× pylvään levyinen ja himmennys söi kontrastin.
    val ChartSelectionMarkerBand = 12.dp
    val ChartSelectionMarkerHeight = 3.dp

    // Projektit päivittäin -ruudukko
    val ProjectFabricCellGap = 1.dp
    val ProjectFabricMonthLabelHeight = 20.dp
    val ProjectFabricSelectionStroke = 1.dp

    @Suppress("MayBeConstant")
    val ProjectFabricEmptyCellAlpha = 0.6f

    val ProjectFabricDetailTopPadding = 8.dp

    // Kaavion akseli ja vihjeet
    val ChartAxisLabelFontSize = 13.sp
    val ChartHintFontSize = 12.sp
    val ChartHintTopPadding = 8.dp

    // Projektilista
    val ProjectRowMinHeight = 48.dp
    val ProjectRowVerticalPadding = 15.dp
    val ProjectRowGap = 14.dp
    val ProjectDotSize = 14.dp
    val ProjectNameFontSize = 19.sp
    val ProjectSubFontSize = 13.sp
    val ProjectSubTopMargin = 2.dp
    val ProjectDurationFontSize = 19.sp

    // Yksi lankapalkki koko aikavälistä, ei viittä erillistä osuuspalkkia.
    val MixBarHeight = 22.dp
    val MixBarCorner = 4.dp
    val MixBarTopPadding = 4.dp
    val MixBarBottomPadding = 16.dp

    // Tyhjät tilat
    val EmptyTitleFontSize = 32.sp
    val EmptyBodyFontSize = 16.sp
    val EmptyBodyTopMargin = 12.dp
    val EmptyRangeNoteVerticalPadding = 24.dp
    val EmptyRangeTitleFontSize = 22.sp

    // Pro-kortti kaavion tilalla
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
