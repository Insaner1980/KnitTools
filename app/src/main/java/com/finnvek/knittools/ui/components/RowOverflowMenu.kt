package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Rivin toiminto: valikossa näkyvä teksti ja sen käsittelijä. */
data class RowMenuAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Listarivin ⋮-valikko (istuntohistoria, muistutukset). Muokkaus ja poisto ovat valikossa eivätkä
 * rivin omina kynä- ja roskakori-ikoneina, joihin oli helppo osua vahingossa.
 */
@Composable
fun RowOverflowMenu(
    actions: List<RowMenuAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MoreOptionsIconButton(onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
