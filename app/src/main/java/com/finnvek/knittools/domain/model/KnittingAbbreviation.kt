package com.finnvek.knittools.domain.model

import androidx.annotation.StringRes

data class KnittingAbbreviation(
    val abbreviation: String,
    @StringRes val meaningResId: Int,
    @StringRes val descriptionResId: Int,
)
