package com.finnvek.knittools.ui.screens.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryTopBar(
    isSelectMode: Boolean,
    selectedCount: Int,
    @StringRes titleRes: Int,
    onExitSelectMode: () -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
) {
    if (isSelectMode) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.n_selected, selectedCount),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onExitSelectMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            },
            actions = {
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.select_all))
                }
            },
            colors = transparentTopAppBarColors(),
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            colors = transparentTopAppBarColors(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentTopAppBarColors() =
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
