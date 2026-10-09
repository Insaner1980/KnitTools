package com.finnvek.knittools.domain.model

import java.util.Locale

/**
 * Ohjeen luettava nimi tiedostonimestä. Ladatut PDF:t ovat usein muotoa "STEP_BY_STEP_SWEATER_V3.pdf",
 * joka näkyi sellaisenaan lukijan otsikossa ja projektinäkymässä.
 */
object PatternDisplayNames {
    private const val PDF_EXTENSION = ".pdf"
    private val whitespace = Regex("\\s+")

    /** "STEP_BY_STEP_SWEATER_V3.pdf" → "Step By Step Sweater V3"; sekakirjaiminen nimi säilyttää muotonsa. */
    fun fromFileName(fileName: String): String {
        val base =
            fileName
                .trim()
                .let { if (it.endsWith(PDF_EXTENSION, ignoreCase = true)) it.dropLast(PDF_EXTENSION.length) else it }
                .replace('_', ' ')
                .replace(whitespace, " ")
                .trim()
                .trimEnd('.', ' ')
        if (base.isEmpty()) return fileName.trim()
        val mixedCase = base.any(Char::isLowerCase) && base.any(Char::isUpperCase)
        return if (mixedCase) {
            base
        } else {
            base.split(' ').joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }
    }

    /**
     * Projektin dokumentin nimike tiedostonimestä. Pitkä tiedostonimi lyhennetään nimikkeen
     * enimmäispituuteen, ettei liitos kaadu pituustarkistukseen.
     */
    fun documentLabel(fileName: String): String =
        fromFileName(fileName).take(PROJECT_DOCUMENT_LABEL_MAX_LENGTH).trimEnd()

    /** Näytettävä nimi: tiedostonimeltä näyttävä nimike siistitään, käyttäjän antama nimi pysyy ennallaan. */
    fun forDisplay(label: String): String =
        if (label.trim().endsWith(PDF_EXTENSION, ignoreCase = true)) fromFileName(label) else label
}
