package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

@Composable
fun UnitToggle(
    useImperial: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SegmentedToggle(
            options =
                listOf(
                    stringResource(R.string.unit_cm),
                    stringResource(R.string.unit_inches),
                ),
            selectedIndex = if (useImperial) 1 else 0,
            onSelect = { onToggle(it == 1) },
        )
    }
}
