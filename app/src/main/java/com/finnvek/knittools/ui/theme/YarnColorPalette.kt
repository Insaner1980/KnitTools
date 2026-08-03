package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Deterministinen väri ID:n perusteella lankapaletista. Sama ID saa aina saman
 * värin riippumatta listan järjestyksestä, teemasta tai valitusta aikavälistä.
 */
fun yarnColorForId(id: Long): Color = YarnColors[id.mod(YarnColors.size)]
