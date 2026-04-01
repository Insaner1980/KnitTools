package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun UnitToggle(
    useImperial: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        FilterChip(
            selected = !useImperial,
            onClick = { onToggle(false) },
            label = { Text("cm") },
        )
        FilterChip(
            selected = useImperial,
            onClick = { onToggle(true) },
            label = { Text("inches") },
        )
    }
}
