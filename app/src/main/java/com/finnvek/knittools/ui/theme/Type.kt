package com.finnvek.knittools.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun outfitFont(weight: FontWeight) =
    Font(
        resId = R.font.outfit,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val OutfitFontFamily =
    FontFamily(
        outfitFont(FontWeight.Normal),
        outfitFont(FontWeight.Medium),
        outfitFont(FontWeight.SemiBold),
        outfitFont(FontWeight.Bold),
        outfitFont(FontWeight.ExtraBold),
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
        // All-caps labelit (CURRENT ROW, QUICK TIP, nav-labelit)
        labelSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
    )
