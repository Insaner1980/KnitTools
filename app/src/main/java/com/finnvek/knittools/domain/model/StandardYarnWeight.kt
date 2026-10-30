package com.finnvek.knittools.domain.model

/**
 * Langan vakiopaksuudet Ravelryn nimillä, jotta käsin lisätty ja Ravelrystä tuotu lanka tallentuvat
 * samalla arvolla. Thickness categories do not specify yarn strand construction.
 */
enum class StandardYarnWeight(
    val label: String,
) {
    LACE("Lace"),
    FINGERING("Fingering"),
    SPORT("Sport"),
    DK("DK"),
    WORSTED("Worsted"),
    ARAN("Aran"),
    BULKY("Bulky"),
    SUPER_BULKY("Super Bulky"),
    ;

    companion object {
        /** Tyhjä arvo tai vakiopaksuus; muu teksti (esim. Ravelryn "Light Fingering") on käyttäjän oma. */
        fun isStandardOrBlank(value: String): Boolean =
            value.isBlank() || entries.any { it.label.equals(value.trim(), ignoreCase = true) }
    }
}
