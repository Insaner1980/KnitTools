package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.finnvek.knittools.ui.theme.knitToolsColors

/** Kortin pohja; monivalinnassa valittu kortti saa saman korostuksen kaikissa listoissa. */
@Composable
fun cardContainerColor(selected: Boolean = false): Color =
    if (selected) MaterialTheme.knitToolsColors.selectedCardContainer else MaterialTheme.knitToolsColors.cardContainer
