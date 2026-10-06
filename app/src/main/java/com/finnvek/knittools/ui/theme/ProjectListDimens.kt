package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

object ProjectListDimens {
    val CardSpacing = 10.dp
    val CardPaddingStart = 10.dp
    val CardPaddingVertical = 10.dp
    val CardPaddingEnd = 14.dp
    val ThumbnailSize = 72.dp
    val ThumbnailSizeCompact = 56.dp
    val ThumbnailCornerRadius = 14.dp
    val ThumbnailTextGap = 14.dp

    // Kompakti asettelu vasta selvästi suurennetulla fontilla; lievä suurennus mahtuu normaaliin riviin.
    @Suppress("MayBeConstant")
    val CompactFontScaleThreshold = 1.3f

    @Suppress("MayBeConstant")
    val CompactMaxWidthDp = 320f

    val ScreenHorizontalPadding = 16.dp
    val ListTopPadding = 8.dp

    // Uusi projekti -painike (64 dp + 16 dp reunus) ja 32 dp väli: viimeisen kortin palkki jää näkyviin.
    val ListBottomPadding = 112.dp
    val HeroPadding = 20.dp
    val HeroContentGap = 12.dp

    val HeroActionTouchSize = 72.dp
    val HeroActionVisualSize = 64.dp
    val ItemLineGap = 4.dp
    val ProgressGroupTopGap = 8.dp
    val ProgressTrackHeight = 4.dp

    // Edistymispalkin pohja: onSurface tällä alfalla erottuu sekä taustalta että kortilta molemmissa teemoissa.
    @Suppress("MayBeConstant")
    val ProgressTrackAlpha = 0.15f

    val FooterActionTouchSize = 48.dp

    val SectionTopSpacing = 20.dp
    val SectionBottomSpacing = 8.dp
    val CreateButtonVisualSize = 64.dp
    val CreateButtonMargin = 16.dp
}
