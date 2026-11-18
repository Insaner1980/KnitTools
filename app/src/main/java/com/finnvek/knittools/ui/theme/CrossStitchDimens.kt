package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Ristipistokuvakkeiden geometria suhteina ruudun kokoon, jotta sama kuvake näyttää samalta
 * 56 dp:n laskurilaatassa ja projektinäkymän rivillä. Mitat ovat hyväksytystä mockupista.
 */
@Suppress("MayBeConstant")
object CrossStitchDimens {
    // Kuvakkeen sivu jaetaan näin moneen ruutuun; 9 ruudun kuvio jättää kapean reunan.
    val GridCells = 9.5f

    // Ristin kärjet ruudun sisällä: alku ja pituus ruudun osuutena.
    val StitchInset = 0.18f
    val StitchSpan = 0.64f
    val StitchStrokeRatio = 0.3f

    // Täytepistot ovat himmeämpiä kuin ääriviiva, kuten ristipistotyön varjostus.
    val SoftStitchAlpha = 0.55f

    // Pienessä koossa suhteellinen pisto jäi alle pikselin ja kuvake luki haaleana viivastona.
    val MinStitchStroke = 1.2.dp
    val RowGlyphSize = 28.dp

    // Tyhjän tilan kuvake on rauhallisempi kuin vanha 240 dp:n kamerakuva.
    val EmptyStateGlyphSize = 96.dp
    val EmptyStatePadding = 32.dp
    val EmptyStateGlyphGap = 24.dp
    val EmptyStateTextGap = 8.dp
}
