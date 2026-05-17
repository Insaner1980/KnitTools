package com.finnvek.knittools.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

@Composable
fun skeinCountText(quantity: Int): String = stringResource(skeinCountStringRes(quantity), quantity)

@StringRes
fun skeinCountStringRes(quantity: Int): Int =
    if (quantity == 1) {
        R.string.skein_count_one
    } else {
        R.string.skein_count_many
    }
