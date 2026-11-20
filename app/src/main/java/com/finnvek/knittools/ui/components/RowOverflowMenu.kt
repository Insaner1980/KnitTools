package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Rivin toiminto: valikossa näkyvä teksti ja sen käsittelijä. */
data class RowMenuAction(
    val label: String,
    val isDestructive: Boolean = false,
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
    onExpandedChange: (Boolean) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val blockingSurfaceOpen = expanded
    SideEffect { currentOnExpandedChange(blockingSurfaceOpen) }
    DisposableEffect(Unit) { onDispose { currentOnExpandedChange(false) } }
    Box(modifier = modifier) {
        MoreOptionsIconButton(onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    colors =
                        if (action.isDestructive) {
                            MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                        } else {
                            MenuDefaults.itemColors()
                        },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
