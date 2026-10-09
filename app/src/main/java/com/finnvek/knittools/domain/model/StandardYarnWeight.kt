package com.finnvek.knittools.domain.model

/**
 * Langan vakiopaksuudet Ravelryn nimillä, jotta käsin lisätty ja Ravelrystä tuotu lanka tallentuvat
 * samalla arvolla. [ply] on australialais-brittiläinen säiemäärä vihjeeksi valintalistaan.
 */
enum class StandardYarnWeight(
    val label: String,
    val ply: String,
) {
    LACE("Lace", "1–3"),
    FINGERING("Fingering", "4"),
    SPORT("Sport", "5"),
    DK("DK", "8"),
    WORSTED("Worsted", "10"),
    ARAN("Aran", "10"),
    BULKY("Bulky", "12"),
    SUPER_BULKY("Super Bulky", "14"),
    ;

    companion object {
        /** Tyhjä arvo tai vakiopaksuus; muu teksti (esim. Ravelryn "Light Fingering") on käyttäjän oma. */
        fun isStandardOrBlank(value: String): Boolean =
            value.isBlank() || entries.any { it.label.equals(value.trim(), ignoreCase = true) }
    }
}
