package com.finnvek.knittools.ui.components.care

enum class CareCategory { WASHING, BLEACHING, DRYING, IRONING, DRY_CLEANING }

enum class CareSymbol(
    val category: CareCategory,
    val label: String,
    val bitPosition: Int,
) {
    // Washing (bits 0-4)
    WASH_30(CareCategory.WASHING, "30°", 0),
    WASH_40(CareCategory.WASHING, "40°", 1),
    WASH_60(CareCategory.WASHING, "60°", 2),
    WASH_HAND(CareCategory.WASHING, "Hand", 3),
    WASH_DO_NOT(CareCategory.WASHING, "Do not wash", 4),

    // Bleaching (bits 5-7)
    BLEACH_ANY(CareCategory.BLEACHING, "Any bleach", 5),
    BLEACH_NON_CHLORINE(CareCategory.BLEACHING, "Non-chlorine", 6),
    BLEACH_DO_NOT(CareCategory.BLEACHING, "Do not bleach", 7),

    // Drying (bits 8-11)
    DRY_TUMBLE_LOW(CareCategory.DRYING, "Tumble low", 8),
    DRY_TUMBLE_NORMAL(CareCategory.DRYING, "Tumble normal", 9),
    DRY_FLAT(CareCategory.DRYING, "Flat dry", 10),
    DRY_DO_NOT_TUMBLE(CareCategory.DRYING, "Do not tumble", 11),

    // Ironing (bits 12-15)
    IRON_LOW(CareCategory.IRONING, "Low", 12),
    IRON_MEDIUM(CareCategory.IRONING, "Medium", 13),
    IRON_HIGH(CareCategory.IRONING, "High", 14),
    IRON_DO_NOT(CareCategory.IRONING, "Do not iron", 15),

    // Dry cleaning (bits 16-19)
    DRYCLEAN_ANY(CareCategory.DRY_CLEANING, "Any solvent", 16),
    DRYCLEAN_P(CareCategory.DRY_CLEANING, "P solvent", 17),
    DRYCLEAN_F(CareCategory.DRY_CLEANING, "F solvent", 18),
    DRYCLEAN_DO_NOT(CareCategory.DRY_CLEANING, "Do not dry clean", 19),
    ;

    val bitMask: Long get() = 1L shl bitPosition
}

fun Long.hasCareSymbol(symbol: CareSymbol): Boolean = (this and symbol.bitMask) != 0L

fun Long.toggleCareSymbol(symbol: CareSymbol): Long = this xor symbol.bitMask
