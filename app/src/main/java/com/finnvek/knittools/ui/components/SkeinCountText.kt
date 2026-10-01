package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.finnvek.knittools.R

@Composable
fun skeinCountText(quantity: Int): String = pluralStringResource(R.plurals.skein_count, quantity, quantity)
