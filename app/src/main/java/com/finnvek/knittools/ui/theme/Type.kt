package com.finnvek.knittools.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun outfitFont(weight: FontWeight) =
    Font(
        resId = R.font.outfit,
        weight = weight,
        loadingStrategy = FontLoadingStrategy.OptionalLocal,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val OutfitFontFamily =
    FontFamily(
        listOf(
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
            FontWeight.ExtraBold,
        ).flatMap { weight ->
            listOf(
                outfitFont(weight),
                Font(DeviceFontFamilyName("sans-serif"), weight = weight),
            )
        },
    )

val AppTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
            ),
        // All-caps labelit (CURRENT ROW, nav-labelit)
        labelSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
    )

val Typography.counterExtraName: TextStyle
    get() = titleMedium.copy(fontSize = 17.sp)

val Typography.counterExtraValue: TextStyle
    get() = headlineMedium.copy(fontWeight = FontWeight.Bold)

val Typography.projectActionsSectionHeader: TextStyle
    get() = labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)

val Typography.insightsKicker: TextStyle
    get() = labelMedium.copy(fontSize = InsightsDimens.KickerFontSize, fontWeight = FontWeight.Medium)

val Typography.insightsHeroLead: TextStyle
    get() = titleMedium.copy(fontSize = InsightsDimens.HeroLeadFontSize, fontWeight = FontWeight.Medium)

val Typography.insightsTrend: TextStyle
    get() = bodySmall.copy(fontSize = InsightsDimens.TrendFontSize, fontWeight = FontWeight.Medium)

val Typography.insightsStatValue: TextStyle
    get() =
        titleLarge.copy(
            fontSize = InsightsDimens.StatValueFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = InsightsDimens.StatValueLetterSpacing,
        )

val Typography.insightsProjectName: TextStyle
    get() = titleMedium.copy(fontSize = InsightsDimens.ProjectNameFontSize)

val Typography.insightsProjectSub: TextStyle
    get() = bodySmall.copy(fontSize = InsightsDimens.ProjectSubFontSize, fontWeight = FontWeight.Medium)

val Typography.insightsProjectDuration: TextStyle
    get() = titleMedium.copy(fontSize = InsightsDimens.ProjectDurationFontSize, fontWeight = FontWeight.Bold)

val Typography.insightsEmptyTitle: TextStyle
    get() = headlineMedium.copy(fontSize = InsightsDimens.EmptyTitleFontSize)

val Typography.insightsEmptyBody: TextStyle
    get() = bodyLarge.copy(fontSize = InsightsDimens.EmptyBodyFontSize)

val Typography.insightsEmptyRangeTitle: TextStyle
    get() = headlineSmall.copy(fontSize = InsightsDimens.EmptyRangeTitleFontSize)

val Typography.insightsProCardTitle: TextStyle
    get() = titleMedium.copy(fontSize = InsightsDimens.ProCardTitleFontSize)

val Typography.insightsProCardBody: TextStyle
    get() = bodyMedium.copy(fontSize = InsightsDimens.ProCardBodyFontSize)

val Typography.insightsChartAxis: TextStyle
    get() = labelMedium.copy(fontSize = InsightsDimens.ChartAxisLabelFontSize)

val Typography.insightsHeroPrimaryNumber: TextStyle
    get() =
        displayLarge.copy(
            fontSize = InsightsDimens.HeroPrimaryNumberFontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = InsightsDimens.HeroNumberLetterSpacing,
        )

val Typography.insightsHeroSecondaryNumber: TextStyle
    get() =
        displayMedium.copy(
            fontSize = InsightsDimens.HeroSecondaryNumberFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = InsightsDimens.HeroNumberLetterSpacing,
        )

val Typography.insightsHeroPrimaryUnit: TextStyle
    get() = headlineMedium.copy(fontSize = InsightsDimens.HeroPrimaryUnitFontSize)

val Typography.insightsHeroSecondaryUnit: TextStyle
    get() = titleLarge.copy(fontSize = InsightsDimens.HeroSecondaryUnitFontSize)
