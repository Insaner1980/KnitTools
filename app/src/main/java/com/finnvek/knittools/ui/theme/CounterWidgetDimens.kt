package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kotinäytön laskuriwidgetin mitat. Glance ei käytä sovelluksen typografiarooleja, joten koot
 * ovat yhdessä paikassa koon mukaan: pieni rivi, pinottu pieni, keskikokoinen ja leveä.
 */
object CounterWidgetDimens {
    // Laskurin 3D-napit: kosketusalue vähintään 48 dp. Keskikoossa napit ovat alhaalla reunoissa,
    // leveässä luvun vieressä.
    val ButtonSize = 48.dp
    val LargeButtonSize = 56.dp
    val SmallButtonTouchSize = 48.dp
    val SmallButtonInset = 3.dp

    val HorizontalPadding = 14.dp
    val VerticalPadding = 8.dp

    // Neliössä nimi, luku, palkki, yksikkörivi ja 48 dp:n napit täyttävät 160 dp:n korkeuden: fonttikoolla 1,15
    // 8 dp:n pehmuste ja 34 sp:n luku leikkasivat nappirivin noin 5 dp:llä.
    val MediumVerticalPadding = 4.dp
    val SmallHorizontalPadding = 10.dp
    val LargeFontVerticalPadding = 2.dp

    val ProgressHeight = 4.dp
    val ProgressTopSpacing = 4.dp
    val SectionSpacing = 4.dp

    val NameSize = 13.sp
    val SmallNameSize = 11.sp
    val CaptionSize = 10.sp
    val SmallCountSize = 22.sp
    val StackedCountSize = 28.sp
    val CountSize = 30.sp
    val LargeCountSize = 48.sp
    val ProPromptSize = 12.sp
}
