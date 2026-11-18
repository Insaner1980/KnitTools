package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Deterministinen väri ID:n perusteella lankapaletista. Sama ID saa aina saman
 * paikan paletissa riippumatta listan järjestyksestä tai valitusta aikavälistä.
 *
 * Paletti annetaan kutsujalta, koska teemat tarvitsevat eri sävyt: kermataustalla
 * vaaleat langat jäivät alle 3:1 kontrastin. Kutsu sitä UI:sta aina
 * `MaterialTheme.knitToolsColors.yarnPalette`illa — oletusarvo on tumman teeman
 * paletti, jotta funktio pysyy puhtaana ja testattavana.
 */
fun yarnColorForId(
    id: Long,
    palette: List<Color> = YarnColors,
): Color = palette[id.mod(palette.size)]
