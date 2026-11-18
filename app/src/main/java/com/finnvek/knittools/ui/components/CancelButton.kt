package com.finnvek.knittools.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

/**
 * Dialogin ja sheetin peruutus neutraalilla värillä. Oranssina Cancel näytti päätoiminnolta,
 * varsinkin kun vahvistus oli vielä käytöstä poistettu; nyt vahvistus on ainoa oranssi toiminto.
 */
@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Peruutuksen oma sanamuoto, esim. Pro-kehotteen "Not now"; oletuksena Cancel.
    text: String? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(text ?: stringResource(R.string.cancel))
    }
}
