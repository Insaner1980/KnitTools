package com.finnvek.knittools.domain.model

import androidx.annotation.StringRes

data class KnittingAbbreviation(
    val abbreviation: String,
    @param:StringRes val meaningResId: Int,
    @param:StringRes val descriptionResId: Int,
)
