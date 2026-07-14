package com.finnvek.knittools.ui.components.care

import androidx.annotation.StringRes
import com.finnvek.knittools.R

enum class CareCategory { WASHING, BLEACHING, DRYING, IRONING, DRY_CLEANING }

enum class CareSymbol(
    val category: CareCategory,
    @param:StringRes val labelRes: Int,
    val bitPosition: Int,
) {
    // Washing (bits 0-4)
    WASH_30(CareCategory.WASHING, R.string.care_wash_30, 0),
    WASH_40(CareCategory.WASHING, R.string.care_wash_40, 1),
    WASH_60(CareCategory.WASHING, R.string.care_wash_60, 2),
    WASH_HAND(CareCategory.WASHING, R.string.care_wash_hand, 3),
    WASH_DO_NOT(CareCategory.WASHING, R.string.care_wash_do_not, 4),

    // Bleaching (bits 5-7)
    BLEACH_ANY(CareCategory.BLEACHING, R.string.care_bleach_any, 5),
    BLEACH_NON_CHLORINE(CareCategory.BLEACHING, R.string.care_bleach_non_chlorine, 6),
    BLEACH_DO_NOT(CareCategory.BLEACHING, R.string.care_bleach_do_not, 7),

    // Drying (bits 8-11)
    DRY_TUMBLE_LOW(CareCategory.DRYING, R.string.care_dry_tumble_low, 8),
    DRY_TUMBLE_NORMAL(CareCategory.DRYING, R.string.care_dry_tumble_normal, 9),
    DRY_FLAT(CareCategory.DRYING, R.string.care_dry_flat, 10),
    DRY_DO_NOT_TUMBLE(CareCategory.DRYING, R.string.care_dry_do_not_tumble, 11),

    // Ironing (bits 12-15)
    IRON_LOW(CareCategory.IRONING, R.string.care_iron_low, 12),
    IRON_MEDIUM(CareCategory.IRONING, R.string.care_iron_medium, 13),
    IRON_HIGH(CareCategory.IRONING, R.string.care_iron_high, 14),
    IRON_DO_NOT(CareCategory.IRONING, R.string.care_iron_do_not, 15),

    // Dry cleaning (bits 16-19)
    DRYCLEAN_ANY(CareCategory.DRY_CLEANING, R.string.care_dryclean_any, 16),
    DRYCLEAN_P(CareCategory.DRY_CLEANING, R.string.care_dryclean_p, 17),
    DRYCLEAN_F(CareCategory.DRY_CLEANING, R.string.care_dryclean_f, 18),
    DRYCLEAN_DO_NOT(CareCategory.DRY_CLEANING, R.string.care_dryclean_do_not, 19),
    ;

    val bitMask: Long get() = 1L shl bitPosition
}

fun Long.hasCareSymbol(symbol: CareSymbol): Boolean = (this and symbol.bitMask) != 0L

fun Long.toggleCareSymbol(symbol: CareSymbol): Long = this xor symbol.bitMask
