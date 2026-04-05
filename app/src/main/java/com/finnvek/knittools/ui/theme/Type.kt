package com.finnvek.knittools.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun manropeFont(weight: FontWeight) =
    Font(
        resId = R.font.manrope,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val ManropeFontFamily =
    FontFamily(
        manropeFont(FontWeight.Normal),
        manropeFont(FontWeight.Medium),
        manropeFont(FontWeight.SemiBold),
        manropeFont(FontWeight.Bold),
        manropeFont(FontWeight.ExtraBold),
    )

val AppTypography =
    Typography(
        // Rivilaskurin päänumero
        displayLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontFeatureSettings = "tnum",
            ),
        // Hero-tulosnumerot (yardage jne.)
        displayMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontFeatureSettings = "tnum",
            ),
        // Näytön pääotsikko ("Welcome back to your atelier.")
        headlineLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
            ),
        // Neulenotaatio ("K14, M1...")
        headlineMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        // Korttien otsikot, osionpäät
        headlineSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        // Työkalujen nimet hub-listoissa
        titleLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        // Alaotsikot, projektinnimi
        titleMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        // Syötekentän arvo
        titleSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
        // Leipäteksti, kuvaukset
        bodyLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        // Toissijaiset kuvaukset
        bodyMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        // Info/huomautus (kursiivi)
        bodySmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        // Painikkeiden teksti
        labelLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        // Chip-teksti, navigaatio-labelit
        labelMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        // All-caps labelit ("TOTAL ROWS", "QUICK TIP")
        labelSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 1.5.sp,
            ),
    )
