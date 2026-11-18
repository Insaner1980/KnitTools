package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

object ProjectOverviewDimens {
    val ScreenHorizontalPadding = 16.dp
    val HeroPhotoHeight = 210.dp

    // Ilman kuvaa paikkamerkki on matala, jottei tyhjä laatikko vie puolta ensimmäisestä näkymästä.
    val NoPhotoHeight = 96.dp

    // Paikkamerkin neulepinta on taustaa: täydellä peitolla se kilpaili lisäyspillerin kanssa.
    const val PLACEHOLDER_FABRIC_ALPHA = 0.55f

    // Kerratun langan säikeet ovat paikkamerkissä isoja, ja samalla peitolla ne lukivat renkaina.
    const val PLACEHOLDER_YARN_FABRIC_ALPHA = 0.3f

    // Langan määrän 3D-napit: pienennetty laskurin nappi, kosketusalue pysyy 48 dp:nä.
    val StepperVisualSize = 44.dp
    val StepperTouchSize = 48.dp
    val TopContentGap = 8.dp
    val HeroPhotoCornerRadius = 24.dp
    val HeaderTopGap = 18.dp
    val PillHorizontalPadding = 12.dp
    val SectionTopGap = 20.dp
    val RowMinHeight = 56.dp
    val ActionTouchSize = 48.dp
    val YarnSwatchSize = 48.dp
    val YarnGridMinWidthForFourColumns = 340.dp
    val ContentGap = 8.dp

    // Langan tietorivien väli: ilman korttia ja jakoviivoja rivit erottuvat pelkällä välillä.
    val DetailRowGap = 12.dp
    val DetailLabelGap = 16.dp
    val ContentBottomPadding = 32.dp
    val SheetCornerRadius = 18.dp
}
