package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

/** Savimaisten kuvitusten ja hienovaraisten sisääntuloanimaatioiden mitat (onboarding ja Pro-sivu). */
object IllustrationDimens {
    val Height = 220.dp
    val WelcomeHeight = 140.dp
    val CompactHeight = 170.dp
    val ProPageHeight = 150.dp
    val LogoSize = 128.dp
    val EmptyStateHeight = 140.dp
    val Rise = 16.dp
    val EntranceRise = 10.dp
    const val ENTER_MILLIS = 450
    const val BOUNCE_SCALE_FROM = 0.88f
    const val BOUNCE_DAMPING = 0.55f

    /** Listarivien porrastettu sisääntulo: lyhyt ja hienovarainen, jottei lukeminen viivästy. */
    const val ENTRANCE_MILLIS = 320
    const val ENTRANCE_STAGGER_MILLIS = 55
    const val ENTRANCE_BASE_DELAY_MILLIS = 120
    const val SELECTION_MILLIS = 180
}
