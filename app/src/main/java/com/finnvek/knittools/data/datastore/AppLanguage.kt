package com.finnvek.knittools.data.datastore

import java.util.Locale

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

    /**
     * Palauttaa englanninkielisen kielen nimen AI-prompteja varten
     * (esim. "Finnish", "German"). SYSTEM seuraa laitteen oletuslocalea.
     */
    fun promptLanguageName(): String {
        val tag = languageTag ?: Locale.getDefault().language.ifBlank { "en" }
        val locale = Locale.forLanguageTag(tag)
        val raw = locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }
        return raw.replaceFirstChar { it.uppercase() }
    }

    companion object {
        fun fromValue(value: String?): AppLanguage = entries.firstOrNull { it.value == value } ?: SYSTEM

        fun fromLanguageTag(languageTag: String?): AppLanguage {
            val primaryTag =
                languageTag
                    ?.split(',')
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
            if (primaryTag.isBlank()) return SYSTEM

            entries.firstOrNull { it.languageTag == primaryTag }?.let { return it }

            val language = Locale.forLanguageTag(primaryTag).language
            return entries.firstOrNull { it.languageTag == language } ?: SYSTEM
        }
    }
}
