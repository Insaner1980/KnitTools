package com.finnvek.knittools.data.datastore

enum class AppLanguage(
    val value: String,
    val languageTag: String?,
) {
    SYSTEM("system", null),
    FINNISH("fi", "fi"),
    ENGLISH("en", "en"),
    SWEDISH("sv", "sv"),
    GERMAN("de", "de"),
    FRENCH("fr", "fr"),
    SPANISH("es", "es"),
    PORTUGUESE("pt", "pt"),
    ITALIAN("it", "it"),
    NORWEGIAN("nb", "nb"),
    DANISH("da", "da"),
    DUTCH("nl", "nl"),
    ;

    companion object {
        fun fromValue(value: String?): AppLanguage = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
