package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Tekstikenttä suoraan näkymän taustalla, kuten hakukenttä. Sheetissä käytetään
 * [cardTextFieldColors]-apuria, jonka kenttä on sheetin pintaa vaaleampi.
 */
@Composable
internal fun highContainerTextFieldColors(): TextFieldColors =
    fieldColors(MaterialTheme.knitToolsColors.screenFieldContainer)

/** Tekstikenttä kortin ([com.finnvek.knittools.ui.theme.KnitToolsExtendedColors.cardContainer]) tai sheetin sisällä. */
@Composable
internal fun cardTextFieldColors(): TextFieldColors = fieldColors(MaterialTheme.knitToolsColors.inputFieldContainer)

// Käytöstä poistettu kenttä on haaleampi kuin aktiivinen: oletusarvoilla se näytti samalta ja
// käyttäjä yritti kirjoittaa kenttään, joka ei vielä toimi.
@Composable
private fun fieldColors(container: Color): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container.copy(alpha = DISABLED_FIELD_CONTAINER_ALPHA),
        errorContainerColor = container,
        focusedIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
        unfocusedIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
        disabledIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
        errorIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
        errorCursorColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorSupportingTextColor = MaterialTheme.colorScheme.error,
    )

private const val DISABLED_FIELD_CONTAINER_ALPHA = 0.4f
