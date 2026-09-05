package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun highContainerTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
        unfocusedIndicatorColor = MaterialTheme.knitToolsColors.transparentIndicator,
    )
