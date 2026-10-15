package com.finnvek.knittools.ui.components.care

import androidx.annotation.StringRes
import com.finnvek.knittools.R

enum class CareSymbol(
    @param:StringRes val labelRes: Int,
    val bitPosition: Int,
) {
    // Washing (bits 0-4)
    WASH_30(R.string.care_wash_30, 0),
    WASH_40(R.string.care_wash_40, 1),
    WASH_60(R.string.care_wash_60, 2),
    WASH_HAND(R.string.care_wash_hand, 3),
    WASH_DO_NOT(R.string.care_wash_do_not, 4),

    // Bleaching (bits 5-7)
    BLEACH_ANY(R.string.care_bleach_any, 5),
    BLEACH_NON_CHLORINE(R.string.care_bleach_non_chlorine, 6),
    BLEACH_DO_NOT(R.string.care_bleach_do_not, 7),

    // Drying (bits 8-11)
    DRY_TUMBLE_LOW(R.string.care_dry_tumble_low, 8),
    DRY_TUMBLE_NORMAL(R.string.care_dry_tumble_normal, 9),
    DRY_FLAT(R.string.care_dry_flat, 10),
    DRY_DO_NOT_TUMBLE(R.string.care_dry_do_not_tumble, 11),

    // Ironing (bits 12-15)
    IRON_LOW(R.string.care_iron_low, 12),
    IRON_MEDIUM(R.string.care_iron_medium, 13),
    IRON_HIGH(R.string.care_iron_high, 14),
    IRON_DO_NOT(R.string.care_iron_do_not, 15),

    // Dry cleaning (bits 16-19)
    DRYCLEAN_ANY(R.string.care_dryclean_any, 16),
    DRYCLEAN_P(R.string.care_dryclean_p, 17),
    DRYCLEAN_F(R.string.care_dryclean_f, 18),
    DRYCLEAN_DO_NOT(R.string.care_dryclean_do_not, 19),
    ;

    val bitMask: Long get() = 1L shl bitPosition
}

fun Long.hasCareSymbol(symbol: CareSymbol): Boolean = (this and symbol.bitMask) != 0L
