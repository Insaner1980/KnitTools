package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = stringResource(R.string.pro_feature),
        modifier = modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}
