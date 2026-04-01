package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

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
            label = { Text(stringResource(R.string.unit_cm)) },
        )
        FilterChip(
            selected = useImperial,
            onClick = { onToggle(true) },
            label = { Text(stringResource(R.string.unit_inches)) },
        )
    }
}
