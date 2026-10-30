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
    val CountSize = 34.sp
    val LargeCountSize = 48.sp
    val ProPromptSize = 12.sp
}
