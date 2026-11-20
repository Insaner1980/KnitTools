package com.finnvek.knittools.domain.model

data class YarnCard(
    val id: Long = 0,
    val brand: String = "",
    val yarnName: String = "",
    val fiberContent: String = "",
    val weightGrams: String = "",
    val lengthMeters: String = "",
    val needleSize: String = "",
    val gaugeInfo: String = "",
    val colorName: String = "",
    val colorNumber: String = "",
    val dyeLot: String = "",
    val weightCategory: String = "",
    val careSymbols: Long = 0L,
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val quantityInStash: Int = 1,
    val status: String = YarnCardStatus.IN_STASH,
    val linkedProjectId: Long? = null,
)

fun YarnCard.displayName(fallbackName: (Long) -> String): String {
    val trimmedBrand = brand.trim()
    val trimmedName = yarnName.trim()
    return when {
        trimmedBrand.isEmpty() -> trimmedName.ifEmpty { fallbackName(id) }
        trimmedName.isEmpty() -> trimmedBrand
        trimmedName.equals(trimmedBrand, ignoreCase = true) -> trimmedName
        trimmedName.startsWith("$trimmedBrand ", ignoreCase = true) -> trimmedName
        else -> "$trimmedBrand $trimmedName"
    }
}
